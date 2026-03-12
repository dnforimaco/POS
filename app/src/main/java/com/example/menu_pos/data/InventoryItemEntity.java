package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inventory_items",
        indices = {@Index(value = "id", unique = true), @Index("categoryId")}
)
public class InventoryItemEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String categoryId;
    public String name;
    public int stockQty;
    public int minStockQty;

    public InventoryItemEntity() {
        this.id = "";
    }

    @Ignore
    public InventoryItemEntity(@NonNull String id, String categoryId, String name, int stockQty, int minStockQty) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name != null ? name : "";
        this.stockQty = stockQty;
        this.minStockQty = Math.max(0, minStockQty);
    }
}

