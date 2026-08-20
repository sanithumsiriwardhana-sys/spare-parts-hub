package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.stockmonitoring.dto.CategoryStockSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Scoped repository for the urgency-tracking module.
 *
 * NOTE: Product is a shared entity. Per CONTRIBUTING.md, whoever builds
 * Function 2 (sales/checkout) owns the canonical ProductRepository. This
 * is a deliberate local duplicate, limited to what Function 3 needs
 * (reading stock levels, writing back urgency_score) so this module isn't
 * blocked waiting on Function 2 to exist first. Consolidate with the
 * sales module's repository later if the team decides to - see
 * CONTRIBUTING.md, "Shared tables, one owner".
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Powers the supervisor dashboard (SP2-06) - most urgent items first.
    List<Product> findAllByOrderByUrgencyScoreDesc();

    // Landing-page widget: total stock grouped by category, so the
    // Supervisor can see "state of the shop" at a glance rather than one
    // meaningless total across every product type.
    @Query("SELECT new com.sliit.sparepartshub.stockmonitoring.dto.CategoryStockSummary(" +
            "p.category, SUM(p.stockCount)) " +
            "FROM Product p " +
            "GROUP BY p.category " +
            "ORDER BY p.category")
    List<CategoryStockSummary> getStockCountByCategory();
}
