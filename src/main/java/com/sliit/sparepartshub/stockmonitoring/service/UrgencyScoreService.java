package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.stockmonitoring.dto.SalesVelocity;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyLevel;
import com.sliit.sparepartshub.stockmonitoring.repository.ProductRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.SaleItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes Function 3's "Dynamic Urgency Score": current stock level
 * weighed against recent sales velocity, instead of a flat low-stock
 * threshold. A fast-selling item with modest stock scores higher than a
 * slow-selling item sitting at the same stock count - matching the
 * proposal's graphics-card-vs-adapter example.
 *
 * Formula (tune LOOKBACK_DAYS / thresholds with the team once you have
 * real sales data to calibrate against):
 *
 *   velocityPerDay = unitsSoldInWindow / LOOKBACK_DAYS
 *   urgencyScore   = (velocityPerDay / (stockCount + 1)) * 100
 *
 * The "+1" avoids division by zero for out-of-stock items.
 */
@Service
public class UrgencyScoreService {

    private static final int LOOKBACK_DAYS = 30;
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("20");

    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    public UrgencyScoreService(ProductRepository productRepository, SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
    }

    /**
     * Recalculates and persists urgency_score for every product based on
     * sales over the last LOOKBACK_DAYS. Call this on a schedule (e.g.
     * nightly via @Scheduled) or trigger it manually from the dashboard -
     * wiring that trigger is a separate step once the controller exists.
     */
    public void recalculateAll() {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);

        Map<Integer, Long> unitsSoldByProduct = saleItemRepository.sumQuantitySoldSince(since).stream()
                .collect(Collectors.toMap(SalesVelocity::getProductId, SalesVelocity::getUnitsSold));

        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            long unitsSold = unitsSoldByProduct.getOrDefault(product.getProductId(), 0L);
            product.setUrgencyScore(calculateScore(unitsSold, product.getStockCount()));
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
}
