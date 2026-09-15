package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.CompatibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Canonical repository for CompatibilityRule - sales owns this per
 * PROJECT_SUMMARY.md ("Shared tables, one owner").
 */
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Integer> {

    // Only rules relevant to the spec types actually present on the
    // candidate item need to be loaded - narrows the in-memory
    // comparison in CompatibilityService instead of loading every rule
    // in the table on every Add to Cart.
    List<CompatibilityRule> findBySpecTypeIn(List<String> specTypes);
}
