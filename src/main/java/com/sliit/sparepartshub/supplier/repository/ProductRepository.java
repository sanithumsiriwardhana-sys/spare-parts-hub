package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Narrow supplier-module repository. The uploaded branch has no canonical
 * sales ProductRepository yet, so Function 5 needs this local repository for
 * product reads and stock updates. Replace with the canonical shared owner
 * once that branch is merged.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {
}
