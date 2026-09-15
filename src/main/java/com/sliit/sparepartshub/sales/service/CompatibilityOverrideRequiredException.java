package com.sliit.sparepartshub.sales.service;

import com.sliit.sparepartshub.sales.dto.CompatibilityConflict;

import java.util.List;

/**
 * UC-02 step 6a: thrown when the final cart has unresolved compatibility
 * conflicts and no override reason was supplied. The controller catches
 * this, returns the conflicts to the front-end, and the Sales Executive
 * either adjusts the cart or supplies an authorized override reason to
 * retry checkout.
 */
public class CompatibilityOverrideRequiredException extends RuntimeException {

    private final List<CompatibilityConflict> conflicts;

    public CompatibilityOverrideRequiredException(List<CompatibilityConflict> conflicts) {
        super("Compatibility conflicts require an override reason before checkout can complete.");
        this.conflicts = conflicts;
    }

    public List<CompatibilityConflict> getConflicts() {
        return conflicts;
    }
}
