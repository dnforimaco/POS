package com.example.menu_pos.data;

import androidx.annotation.Nullable;

/**
 * An item in the cart: menu item + selected variant + quantity.
 */
public class CartItem {
    private final String itemId;
    private final String itemName;
    private final String variantLabel;
    private final int unitPriceCents;
    private final int quantity;

    public CartItem(String itemId, String itemName, String variantLabel, int unitPriceCents, int quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.variantLabel = variantLabel;
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getVariantLabel() { return variantLabel; }
    public int getUnitPriceCents() { return unitPriceCents; }
    public int getQuantity() { return quantity; }
    public int getTotalCents() { return unitPriceCents * quantity; }

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
        return itemId.equals(o.itemId) && variantLabel.equals(o.variantLabel);
    }

    @Override
    public int hashCode() {
        return 31 * itemId.hashCode() + variantLabel.hashCode();
    }
}
