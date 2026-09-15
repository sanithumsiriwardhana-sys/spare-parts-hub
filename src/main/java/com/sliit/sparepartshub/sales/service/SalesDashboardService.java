package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.sales.dto.SalesDashboardStats;
import com.sliit.sparepartshub.sales.dto.TopProductRow;
import com.sliit.sparepartshub.sales.repository.SaleItemRepository;
import com.sliit.sparepartshub.sales.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backs the Sales widget on the shared dashboard (dashboard.html,
 * gated to SALES_EXEC/ADMIN). Whoever wires /dashboard (web/
 * PageController per AGENTS.md) should call getStats() once and pass
 * the result as a single "salesStats" model attribute.
 */
@Service
public class SalesDashboardService {

    private static final int TOP_SELLERS_LIMIT = 5;

    // 30 days, not 14 - lines up with the urgency-tracking seed data
    // (database/seed_urgency_demo_data.sql, per PROJECT_SUMMARY.md),
    // which was deliberately built with 30 days of sale history. Using
    // the same window means the trend chart has real points to show
    // instead of being mostly flat/empty.
    private static final int TREND_DAYS = 30;

    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public SalesDashboardService(SaleRepository saleRepository, SaleItemRepository saleItemRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public SalesDashboardStats getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long todaySalesCount = saleRepository.countSalesSince(startOfDay);
        BigDecimal todaySalesTotal = saleRepository.sumAmountSince(startOfDay);

        BigDecimal averageOrderValue = todaySalesCount == 0
                ? BigDecimal.ZERO
                : todaySalesTotal.divide(BigDecimal.valueOf(todaySalesCount), 2, RoundingMode.HALF_UP);

        List<TopProductRow> topSellers = computeTopSellers();
        CategoryBreakdown categoryBreakdown = computeCategoryBreakdown();
        DailyTrend dailyTrend = computeDailyTrend();

        return new SalesDashboardStats(
                todaySalesCount, todaySalesTotal, averageOrderValue, topSellers,
                categoryBreakdown.labels, categoryBreakdown.revenues,
                dailyTrend.labels, dailyTrend.revenues);
    }

    // All-time, not "today" - with a small shop's daily volume, a
    // same-day top-sellers list would mostly be empty or misleadingly
    // sparse. Ranked by revenue (see TopProductRow's comment).
    private List<TopProductRow> computeTopSellers() {
        List<SaleItem> items = saleItemRepository.findAllWithProduct();

        Map<Integer, ProductAccumulator> byProduct = new LinkedHashMap<>();
        for (SaleItem item : items) {
            Integer productId = item.getProduct().getProductId();
            ProductAccumulator acc = byProduct.computeIfAbsent(productId,
                    id -> new ProductAccumulator(item.getProduct().getName()));
            acc.unitsSold += item.getQuantity();
            acc.revenue = acc.revenue.add(item.getPriceAtSale().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        return byProduct.values().stream()
                .sorted(Comparator.comparing((ProductAccumulator a) -> a.revenue).reversed())
                .limit(TOP_SELLERS_LIMIT)
                .map(a -> new TopProductRow(a.productName, a.unitsSold, a.revenue))
                .collect(Collectors.toList());
    }

    // All-time revenue grouped by product category.
    private CategoryBreakdown computeCategoryBreakdown() {
        List<SaleItem> items = saleItemRepository.findAllWithProduct();

        Map<String, BigDecimal> revenueByCategory = new LinkedHashMap<>();
        for (SaleItem item : items) {
            String category = item.getProduct().getCategory();
            BigDecimal lineRevenue = item.getPriceAtSale().multiply(BigDecimal.valueOf(item.getQuantity()));
            revenueByCategory.merge(category, lineRevenue, BigDecimal::add);
        }

        List<Map.Entry<String, BigDecimal>> sorted = revenueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            labels.add(entry.getKey());
            revenues.add(entry.getValue());
        }
        return new CategoryBreakdown(labels, revenues);
    }

    // Revenue per calendar day over the last TREND_DAYS days, with every
    // day in the window present (zero-filled if no sales that day) so
    // the line chart doesn't show misleading gaps.
    private DailyTrend computeDailyTrend() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(TREND_DAYS - 1L);

        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            revenueByDay.put(startDate.plusDays(i), BigDecimal.ZERO);
        }

        List<Sale> sales = saleRepository.findAllSince(startDate.atStartOfDay());
        for (Sale sale : sales) {
            if (sale.getSoldAt() == null) {
                continue;
            }
            LocalDate day = sale.getSoldAt().toLocalDate();
            revenueByDay.merge(day, sale.getAmount(), BigDecimal::add);
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : revenueByDay.entrySet()) {
            labels.add(entry.getKey().format(DAY_LABEL_FORMAT));
            revenues.add(entry.getValue());
        }
        return new DailyTrend(labels, revenues);
    }

    private static class ProductAccumulator {
        final String productName;
        long unitsSold = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAccumulator(String productName) {
            this.productName = productName;
        }
    }

    private static class CategoryBreakdown {
        final List<String> labels;
        final List<BigDecimal> revenues;

        CategoryBreakdown(List<String> labels, List<BigDecimal> revenues) {
            this.labels = labels;
            this.revenues = revenues;
        }
    }

    private static class DailyTrend {
        final List<String> labels;
        final List<BigDecimal> revenues;

        DailyTrend(List<String> labels, List<BigDecimal> revenues) {
            this.labels = labels;
            this.revenues = revenues;
        }
    }
}
