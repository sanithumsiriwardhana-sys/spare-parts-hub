package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {
    @EntityGraph(attributePaths = {"product", "purchaseOrder"})
    List<PurchaseOrderItem> findByPurchaseOrder_PoId(Integer poId);
}
