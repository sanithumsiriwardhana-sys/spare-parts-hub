package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only view into purchase_order_item, used only to compute "incoming
 * quantity" for the product detail page (UC-03 step 8). This module does
 * not own PurchaseOrder/PurchaseOrderItem (that's Function 5 / the
 * supplier package) - no writes happen through this repository, same
 * pattern as the scoped SaleItemRepository and ProductRepository already
 * in this package. See CONTRIBUTING.md, "Shared tables, one owner".
 */
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {

    // Excludes fully "received" POs - pending/shipped/partially_received
    // all still count as stock that hasn't fully arrived yet. The schema
    // only tracks received status at the PO level, not a per-line
    // received quantity, so a partially_received PO's full ordered
    // quantity is counted here - an approximation, not exact.
    List<PurchaseOrderItem> findByProduct_ProductIdAndPurchaseOrder_StatusNot(
            Integer productId, PurchaseOrder.Status status);
}
