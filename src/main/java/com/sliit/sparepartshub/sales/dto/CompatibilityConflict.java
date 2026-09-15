package com.sliit.sparepartshub.sales.dto;

/**
 * One detected conflict between two items. Used both when adding to the
 * cart (UC-02 step 6/7, candidate vs. rest of cart) and at checkout
 * (UC-02 step 6a, whole-cart re-check) - the product IDs let checkout
 * know exactly which sale_item rows need compatibility_override_reason
 * set, not just which names to show in the warning banner.
 */
public class CompatibilityConflict {

    private final Integer productAId;
    private final String productAName;
    private final Integer productBId;
    private final String productBName;
    private final String specType;
    private final String reason;

    public CompatibilityConflict(Integer productAId, String productAName,
                                  Integer productBId, String productBName,
                                  String specType, String reason) {
        this.productAId = productAId;
        this.productAName = productAName;
        this.productBId = productBId;
        this.productBName = productBName;
        this.specType = specType;
        this.reason = reason;
    }

    public Integer getProductAId() {
        return productAId;
    }

    public String getProductAName() {
        return productAName;
    }

    public Integer getProductBId() {
        return productBId;
    }

    public String getProductBName() {
        return productBName;
    }

    public String getSpecType() {
        return specType;
    }

    public String getReason() {
        return reason;
    }
}
