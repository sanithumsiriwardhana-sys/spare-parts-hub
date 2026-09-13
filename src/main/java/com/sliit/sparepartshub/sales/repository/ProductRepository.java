package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Canonical repository for Product. Per CONTRIBUTING.md ("Shared tables,
 * one owner") the sales module owns this repository since Function 2 is
 * the first to need full read/write access to Product for search and
 * checkout. stockmonitoring/repository/ProductRepository.java remains a
 * separate, deliberately-scoped read/write-urgency-only copy - see the
 * note in that file. Do not delete or merge that one without agreement,
 * per AGENTS.md ("don't refactor across function-package boundaries
 * without flagging it").
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // UC-02 steps 1-3: keyword/category/brand search for the POS screen.
    // Any filter left null is treated as "match everything" so the same
    // query serves plain keyword search, filter-only browsing, and the
    // unfiltered "show everything" landing view.
    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "   LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
            "(:brand IS NULL OR :brand = '' OR p.brand = :brand) " +
            "ORDER BY p.name")
    List<Product> search(@Param("keyword") String keyword,
                          @Param("category") String category,
                          @Param("brand") String brand);

    // Powers the category/brand filter dropdowns on the search screen.
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findDistinctBrands();
}
