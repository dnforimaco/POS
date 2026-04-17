package com.example.menu_pos.data;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.List;

/**
 * A single menu item (e.g. Miki Bihon, Tocino Silog). May have multiple variants (sizes/portions).
 */
public class MenuItem {
    public static final int NO_IMAGE = 0;

    private final String id;
    private final String name;
    private final String description;
    private final List<MenuItemVariant> variants;
    @DrawableRes private final int imageResId;
    private final boolean bestSeller;
    private final boolean spicy;

    private MenuItem(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.description = b.description;
        this.variants = b.variants != null ? b.variants : new ArrayList<>();
        this.imageResId = b.imageResId;
        this.bestSeller = b.bestSeller;
        this.spicy = b.spicy;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<MenuItemVariant> getVariants() { return variants; }
    @DrawableRes public int getImageResId() { return imageResId; }
    public boolean isBestSeller() { return bestSeller; }
    public boolean isSpicy() { return spicy; }

    /** Single price in cents if only one variant; otherwise 0. */
    public int getSinglePriceCents() {
        if (variants.size() == 1) return variants.get(0).getPriceCents();
        return 0;
    }

    /**
     * Price for product-grid cards. When both with-rice and à la carte exist, shows the with-rice
     * price instead of the cheaper option. Otherwise matches the former “lowest variant” behavior.
     */
    public int getCardDisplayPriceCents() {
        if (variants.isEmpty()) return 0;
        if (variants.size() == 1) return variants.get(0).getPriceCents();
        for (MenuItemVariant v : variants) {
            if ("W/RICE".equals(v.getLabel())) return v.getPriceCents();
        }
        for (MenuItemVariant v : variants) {
            if ("Solo (with rice)".equals(v.getLabel())) return v.getPriceCents();
        }
        for (MenuItemVariant v : variants) {
            if ("Solo".equals(v.getLabel())) return v.getPriceCents();
        }
        int min = Integer.MAX_VALUE;
        for (MenuItemVariant v : variants) min = Math.min(min, v.getPriceCents());
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<MenuItemVariant> variants;
        private int imageResId;
        private boolean bestSeller;
        private boolean spicy;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder variants(List<MenuItemVariant> variants) { this.variants = variants; return this; }
        public Builder variant(String label, int priceCents) {
            if (this.variants == null) this.variants = new ArrayList<>();
            this.variants.add(new MenuItemVariant(label, priceCents));
            return this;
        }
        public Builder imageResId(int resId) { this.imageResId = resId; return this; }
        public Builder bestSeller(boolean v) { this.bestSeller = v; return this; }
        public Builder spicy(boolean v) { this.spicy = v; return this; }
        public MenuItem build() { return new MenuItem(this); }
    }
}
