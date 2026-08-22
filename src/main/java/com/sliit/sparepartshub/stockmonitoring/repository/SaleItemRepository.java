package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.stockmonitoring.dto.SalesVelocity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only view into sale_item, used only to compute sales velocity for
 * urgency scoring. This module does not own SaleItem (that's Function 2 /
 * the sales package) - no writes happen through this repository, only
 * the aggregate query below.
 */
public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    @Query("SELECT new com.sliit.sparepartshub.stockmonitoring.dto.SalesVelocity(" +
            "si.product.productId, SUM(si.quantity)) " +
            "FROM SaleItem si " +
            "WHERE si.sale.soldAt >= :since " +
            "GROUP BY si.product.productId")
    List<SalesVelocity> sumQuantitySoldSince(@Param("since") LocalDateTime since);

    // Product detail page (UC-03 step 8) shows the actual recent sale
    // history for one product, not just the aggregate total.
    List<SaleItem> findByProduct_ProductIdAndSale_SoldAtAfterOrderBySale_SoldAtDesc(
            Integer productId, LocalDateTime since);
}
