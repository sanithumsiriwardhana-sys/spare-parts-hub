package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Canonical, read/write repository for SaleItem. stockmonitoring's
 * SaleItemRepository stays as-is - it's a deliberate, read-only, scoped
 * copy for sales-velocity aggregation only (see the note in that file);
 * this one is the one that actually persists sales.
 */
public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    List<SaleItem> findBySale_SaleId(Integer saleId);

    // Feeds the dashboard's Top Sellers widget. Aggregation (group by
    // product, sum revenue) happens in SalesDashboardService, not here -
    // JPQL's constructor-expression syntax gets fragile mixing
    // BigDecimal * Integer inside a SUM(), so it's safer to fetch the
    // rows (with product eagerly joined, avoiding N+1 lazy loads) and
    // aggregate in Java. Fine at this project's scale; if sale_item ever
    // grows past a few thousand rows, this should become a real SQL
    // GROUP BY instead.
    @Query("SELECT si FROM SaleItem si JOIN FETCH si.product")
    List<SaleItem> findAllWithProduct();
}
