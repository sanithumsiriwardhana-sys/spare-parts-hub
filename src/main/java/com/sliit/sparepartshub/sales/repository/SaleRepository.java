package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    // Powers the Sales History list - the only other way to reach a
    // receipt was the one-time redirect right after checkout, which is
    // gone the moment you navigate away. Capped at 50 rather than
    // returning the whole table, since this is a "recent activity" view,
    // not a full report (that's Function 6's job).
    List<Sale> findTop50ByOrderBySoldAtDesc();

    // Dashboard widget - "how's today going" for a Sales Executive.
    // Caller passes midnight of today (LocalDate.now().atStartOfDay())
    // rather than this repository assuming a timezone.
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.soldAt >= :startOfDay")
    long countSalesSince(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Sale s WHERE s.soldAt >= :startOfDay")
    BigDecimal sumAmountSince(@Param("startOfDay") LocalDateTime startOfDay);

    // Daily Revenue Trend chart - raw rows for the window, aggregated by
    // calendar day in SalesDashboardService (day-bucketing in Java, not
    // JPQL, so every day in the window can be zero-filled even if it had
    // no sales - a chart with gaps reads as broken, not "no data").
    @Query("SELECT s FROM Sale s WHERE s.soldAt >= :since ORDER BY s.soldAt")
    List<Sale> findAllSince(@Param("since") LocalDateTime since);
}
