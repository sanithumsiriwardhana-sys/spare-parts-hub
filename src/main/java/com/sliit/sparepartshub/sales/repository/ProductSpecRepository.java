package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Canonical repository for ProductSpec - sales owns this per
 * PROJECT_SUMMARY.md ("Shared tables, one owner").
 */
public interface ProductSpecRepository extends JpaRepository<ProductSpec, Integer> {

    List<ProductSpec> findByProduct_ProductId(Integer productId);

    // Batched version used by CompatibilityService so checking one new
    // cart item against N existing items is 1 query, not N.
    List<ProductSpec> findByProduct_ProductIdIn(List<Integer> productIds);
}
