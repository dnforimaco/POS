package com.example.menu_pos.data;

import androidx.annotation.Nullable;

/**
 * An item in the cart: menu item + selected variant + quantity + optional 10% line discount.
 */
public class CartItem {
    private final String itemId;
    private final String itemName;
    private final String variantLabel;
    private final int unitPriceCents;
    private final int quantity;
    private final boolean applyTenPercentDiscount;

    public CartItem(String itemId, String itemName, String variantLabel, int unitPriceCents, int quantity) {
        this(itemId, itemName, variantLabel, unitPriceCents, quantity, false);
    }

    public CartItem(String itemId, String itemName, String variantLabel, int unitPriceCents, int quantity,
                    boolean applyTenPercentDiscount) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.variantLabel = variantLabel;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
        this.applyTenPercentDiscount = applyTenPercentDiscount;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getVariantLabel() { return variantLabel; }
    public int getUnitPriceCents() { return unitPriceCents; }
    public int getQuantity() { return quantity; }
    public boolean isApplyTenPercentDiscount() { return applyTenPercentDiscount; }

    /** Unit price × quantity (before discount). */
    public int getSubtotalCents() {
        return unitPriceCents * quantity;
    }

    /**
     * 10% of line subtotal ({@link #unitPriceCents} × {@link #quantity}), in centavos,
     * rounded half-up to the nearest whole centavo.
     */
    public int getDiscountCents() {
        if (!applyTenPercentDiscount) return 0;
        int sub = getSubtotalCents();
        return (sub * 10 + 50) / 100;
    }

    /** Line amount after discount (what the customer pays for this line). */
    public int getLineTotalCents() {
        return Math.max(0, getSubtotalCents() - getDiscountCents());
    }

    /** Alias for order totals: sum of line totals. */
    public int getTotalCents() {
        return getLineTotalCents();
    }

    /** Display line: e.g. "Tocino Silog · 1" or "Miki Bihon · 10 PAX x2" */
    public String getDisplayLine() {
        if (variantLabel != null && !variantLabel.isEmpty()) {
            return itemName + " · " + variantLabel + (quantity > 1 ? " x" + quantity : "");
        }
        return itemName + (quantity > 1 ? " x" + quantity : "");
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CartItem)) return false;
        CartItem o = (CartItem) obj;
        return itemId.equals(o.itemId)
                && variantLabel.equals(o.variantLabel)
                && applyTenPercentDiscount == o.applyTenPercentDiscount;
    }

    @Override
    public int hashCode() {
        int h = 31 * itemId.hashCode() + variantLabel.hashCode();
        return 31 * h + (applyTenPercentDiscount ? 1 : 0);
    }
}
