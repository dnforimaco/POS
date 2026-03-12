package com.example.menu_pos.data;

/**
 * A size/portion option for a menu item (e.g. "3-5 PAX", "Regular", "45 PCS").
 */
public class MenuItemVariant {
    private final String label;
    private final int priceCents;

    public MenuItemVariant(String label, int priceCents) {
        this.label = label;
        this.priceCents = priceCents;
    }

    public String getLabel() { return label; }
    public int getPriceCents() { return priceCents; }
}
