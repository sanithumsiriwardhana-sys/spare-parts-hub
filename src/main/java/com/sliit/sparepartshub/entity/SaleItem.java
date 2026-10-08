package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_item")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Integer saleItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Price captured at the moment of sale - kept separate from
    // product.price so historical receipts stay accurate if price changes.
    @Column(name = "price_at_sale", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtSale;

    // UC-02 step 6a: if a compatibility conflict is overridden by an
    // authorized Sales Executive, the reason must be recorded. Null when
    // no conflict occurred.
    @Column(name = "compatibility_override_reason", length = 255)
    private String compatibilityOverrideReason;

    // UC-02: recorded when a Sales Executive manually lowers this
    // line's price on the spot (see CartService.updatePrice /
    // CheckoutService). Null when the item was sold at its catalog
    // price. Kept distinct from compatibilityOverrideReason since the
    // two are unrelated overrides that can both apply to the same line.
    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    public SaleItem() {
    }

    public Integer getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(Integer saleItemId) {
        this.saleItemId = saleItemId;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public void setPriceAtSale(BigDecimal priceAtSale) {
        this.priceAtSale = priceAtSale;
    }

    public String getCompatibilityOverrideReason() {
        return compatibilityOverrideReason;
    }

    public void setCompatibilityOverrideReason(String compatibilityOverrideReason) {
        this.compatibilityOverrideReason = compatibilityOverrideReason;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }
}