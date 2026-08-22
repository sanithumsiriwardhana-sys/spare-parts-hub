package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.stockmonitoring.dto.SalesVelocity;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyLevel;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.SaleItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes Function 3's "Dynamic Urgency Score": current stock level
 * weighed against recent sales velocity, instead of a flat low-stock
 * threshold. A fast-selling item with modest stock scores higher than a
 * slow-selling item sitting at the same stock count - matching the
 * proposal's graphics-card-vs-adapter example.
 *
 * Formula:
 *   velocityPerDay = unitsSoldInWindow / LOOKBACK_DAYS
 *   urgencyScore   = (velocityPerDay / (stockCount + 1)) * 100
 *
 * The "+1" avoids division by zero for out-of-stock items.
 */
@Service
public class UrgencyScoreService {

    private static final int LOOKBACK_DAYS = 30;
    // Recalibrated from the original placeholders (50/20) after checking
    // them against realistic seed data - a genuinely fast-moving item
    // with low stock scored only ~10 under the old thresholds. These
    // values produce a real spread across all three tiers for our seed
    // data; recalibrate again once real sales history exists.
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("12");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("5");

    // How many days of expected demand a restock suggestion should cover,
    // and the floor so a suggestion is never for a trivially small amount.
    private static final int REORDER_COVERAGE_DAYS = 14;
    private static final int MIN_SUGGESTED_QUANTITY = 5;

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final RestockSuggestionRepository restockSuggestionRepository;

    public UrgencyScoreService(ProductRepository productRepository,
                               SaleItemRepository saleItemRepository,
                               RestockSuggestionRepository restockSuggestionRepository) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
        this.restockSuggestionRepository = restockSuggestionRepository;
    }

    /**
     * Recalculates and persists urgency_score for every product based on
     * sales over the last LOOKBACK_DAYS, and raises a new RestockSuggestion
     * for any product that just became CRITICAL and doesn't already have
     * one pending (UC-03 postcondition 3). Call this on a schedule (see
     * UrgencyScoreScheduler) or trigger it manually.
     */
    public void recalculateAll() {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);

        Map<Integer, Long> unitsSoldByProduct = saleItemRepository.sumQuantitySoldSince(since).stream()
                .collect(Collectors.toMap(SalesVelocity::getProductId, SalesVelocity::getUnitsSold));

        // Products that already got a suggestion within the reorder
        // coverage window shouldn't get a second one every time this
        // runs (e.g. daily/scheduled) - even after approval, the actual
        // stock hasn't arrived yet, so the product stays CRITICAL and
        // would otherwise get re-flagged immediately. Checking ANY
        // recent status (not just pending) is what fixes that - see the
        // comment on findByCreatedAtAfter.
        LocalDateTime suppressSince = LocalDateTime.now().minusDays(REORDER_COVERAGE_DAYS);
        Set<Integer> productsWithRecentSuggestion = restockSuggestionRepository
                .findByCreatedAtAfter(suppressSince).stream()
                .map(s -> s.getProduct().getProductId())
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            long unitsSold = unitsSoldByProduct.getOrDefault(product.getProductId(), 0L);
            BigDecimal score = calculateScore(unitsSold, product.getStockCount());
            product.setUrgencyScore(score);

            boolean isCritical = classify(score) == UrgencyLevel.CRITICAL;
            boolean alreadySuggestedRecently = productsWithRecentSuggestion.contains(product.getProductId());
            if (isCritical && !alreadySuggestedRecently) {
                raiseRestockSuggestion(product, unitsSold);
            }
        }
        productRepository.saveAll(products);
    }

    /**
     * Products ordered most-to-least urgent, for the supervisor dashboard
     * (SP2-06).
     */
    public List<Product> getDashboardOrderedByUrgency() {
        return productRepository.findAllByOrderByUrgencyScoreDesc();
    }

    /**
     * Colour-coded status bucket for a given score (SP2-06 task T-11.3),
     * reused for the critical stock alert (SP2-05).
     */
    public UrgencyLevel classify(BigDecimal urgencyScore) {
        if (urgencyScore.compareTo(CRITICAL_THRESHOLD) >= 0) {
            return UrgencyLevel.CRITICAL;
        }
        if (urgencyScore.compareTo(WARNING_THRESHOLD) >= 0) {
            return UrgencyLevel.WARNING;
        }
        return UrgencyLevel.SAFE;
    }

    private BigDecimal calculateScore(long unitsSold, int stockCount) {
        BigDecimal velocityPerDay = BigDecimal.valueOf(unitsSold)
                .divide(BigDecimal.valueOf(LOOKBACK_DAYS), 4, RoundingMode.HALF_UP);
        BigDecimal denominator = BigDecimal.valueOf(stockCount + 1L);
        return velocityPerDay
                .divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void raiseRestockSuggestion(Product product, long unitsSoldInWindow) {
        int suggestedQuantity = calculateSuggestedRestockQuantity(unitsSoldInWindow);

        RestockSuggestion suggestion = new RestockSuggestion();
        suggestion.setProduct(product);
        suggestion.setSuggestedQuantity(suggestedQuantity);
        suggestion.setStatus(RestockSuggestion.Status.pending);
        restockSuggestionRepository.save(suggestion);
    }

    /**
     * Same formula used when auto-raising a suggestion, exposed so the
     * product detail page (UC-03 step 8) can show a live estimate for
     * products that aren't CRITICAL yet and so have no persisted
     * suggestion to display.
     */
    public int calculateSuggestedRestockQuantity(long unitsSoldInWindow) {
        double velocityPerDay = unitsSoldInWindow / (double) LOOKBACK_DAYS;
        int suggestedQuantity = (int) Math.ceil(velocityPerDay * REORDER_COVERAGE_DAYS);
        return Math.max(suggestedQuantity, MIN_SUGGESTED_QUANTITY);
    }

    public int getLookbackDays() {
        return LOOKBACK_DAYS;
    }
}
