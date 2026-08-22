package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.stockmonitoring.dto.ProductDetailView;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.PurchaseOrderItemRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.SaleItemRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.StockRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UC-03 steps 7-8: the Supervisor selects one product and sees its full
 * picture - recent sales, stock coverage, open customer requests,
 * incoming quantities, and a suggested restock quantity. Pulls from four
 * repositories across two other functions' tables (sale_item,
 * purchase_order_item) - all read-only, none of it owned by this module.
 */
@Service
public class ProductDetailService {

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockRequestRepository stockRequestRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final RestockSuggestionRepository restockSuggestionRepository;
    private final UrgencyScoreService urgencyScoreService;

    public ProductDetailService(ProductRepository productRepository,
                                 SaleItemRepository saleItemRepository,
                                 StockRequestRepository stockRequestRepository,
                                 PurchaseOrderItemRepository purchaseOrderItemRepository,
                                 RestockSuggestionRepository restockSuggestionRepository,
                                 UrgencyScoreService urgencyScoreService) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockRequestRepository = stockRequestRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.restockSuggestionRepository = restockSuggestionRepository;
        this.urgencyScoreService = urgencyScoreService;
    }

    public ProductDetailView getDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product id: " + productId));

        LocalDateTime since = LocalDateTime.now().minusDays(urgencyScoreService.getLookbackDays());

        List<SaleItem> recentSales = saleItemRepository
                .findByProduct_ProductIdAndSale_SoldAtAfterOrderBySale_SoldAtDesc(productId, since);
        long unitsSold = recentSales.stream().mapToLong(SaleItem::getQuantity).sum();

        Double stockCoverageDays = calculateStockCoverageDays(unitsSold, product.getStockCount());

        // "Open" = still waiting on notification or on the customer to
        // buy - fulfilled requests aren't relevant to this view.
        List<StockRequest> openStockRequests = stockRequestRepository
                .findByProduct_ProductIdOrderByRequestedAtDesc(productId).stream()
                .filter(r -> r.getStatus() != StockRequest.Status.fulfilled)
                .toList();

        int incomingQuantity = purchaseOrderItemRepository
                .findByProduct_ProductIdAndPurchaseOrder_StatusNot(productId, PurchaseOrder.Status.received)
                .stream()
                .mapToInt(PurchaseOrderItem::getQuantityOrdered)
                .sum();

        // Prefer an existing pending suggestion (the real one the
        // Supervisor would act on) over a fresh calculation - if one
        // exists, that's what step 9's approve/modify/reject acts on.
        Optional<RestockSuggestion> latestPending = restockSuggestionRepository
                .findByProduct_ProductIdOrderByCreatedAtDesc(productId).stream()
                .filter(s -> s.getStatus() == RestockSuggestion.Status.pending)
                .findFirst();

        int suggestedQuantity;
        boolean isLiveEstimate;
        if (latestPending.isPresent()) {
            suggestedQuantity = latestPending.get().getSuggestedQuantity();
            isLiveEstimate = false;
        } else {
            // No pending suggestion (product isn't CRITICAL right now) -
            // show a live estimate for reference, not persisted anywhere.
            suggestedQuantity = urgencyScoreService.calculateSuggestedRestockQuantity(unitsSold);
            isLiveEstimate = true;
        }

        return new ProductDetailView(
                product,
                urgencyScoreService.classify(product.getUrgencyScore()),
                recentSales,
                unitsSold,
                stockCoverageDays,
                openStockRequests,
                incomingQuantity,
                suggestedQuantity,
                isLiveEstimate
        );
    }

    private Double calculateStockCoverageDays(long unitsSoldInWindow, int stockCount) {
        if (unitsSoldInWindow == 0) {
            return null; // no recent demand - "days of coverage" isn't meaningful
        }
        double velocityPerDay = unitsSoldInWindow / (double) urgencyScoreService.getLookbackDays();
        return stockCount / velocityPerDay;
    }
}
