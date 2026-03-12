package com.example.menu_pos.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A menu category (e.g. Bilao Specials, Silog, Chill Zone).
 */
public class MenuCategory {
    private final String id;
    private final String name;
    private final String subtitle;
    private final List<MenuItem> items;
    private final int iconResId;

    private MenuCategory(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.subtitle = b.subtitle;
        this.items = b.items != null ? b.items : new ArrayList<>();
        this.iconResId = b.iconResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSubtitle() { return subtitle; }
    public List<MenuItem> getItems() { return items; }
    public int getIconResId() { return iconResId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String name;
        private String subtitle;
        private List<MenuItem> items;
        private int iconResId;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder subtitle(String subtitle) { this.subtitle = subtitle; return this; }
        public Builder items(List<MenuItem> items) { this.items = items; return this; }
        public Builder iconResId(int resId) { this.iconResId = resId; return this; }
        public MenuCategory build() { return new MenuCategory(this); }
    }
}
