package com.example.menu_pos.data;

/**
 * A single line in a completed order (for persistence).
 */
public class OrderLineItem {
    private final String itemId;
    private final String itemName;
    private final String variantLabel;
    private final int unitPriceCents;
    private final int quantity;

    public OrderLineItem(String itemId, String itemName, String variantLabel, int unitPriceCents, int quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.variantLabel = variantLabel != null ? variantLabel : "";
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getVariantLabel() { return variantLabel; }
    public int getUnitPriceCents() { return unitPriceCents; }
    public int getQuantity() { return quantity; }
    public int getTotalCents() { return unitPriceCents * quantity; }
}
