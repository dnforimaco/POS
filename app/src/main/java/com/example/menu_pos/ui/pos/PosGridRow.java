package com.example.menu_pos.ui.pos;

import androidx.annotation.Nullable;

import com.example.menu_pos.data.MenuItem;

/**
 * One cell in the POS product grid: item plus optional category label (global search).
 */
public final class PosGridRow {
    private final MenuItem item;
    @Nullable private final String categoryName;

    public PosGridRow(MenuItem item, @Nullable String categoryName) {
        this.item = item;
        this.categoryName = categoryName;
    }

    public MenuItem getItem() {
        return item;
    }

    @Nullable
    public String getCategoryName() {
        return categoryName;
    }
}
