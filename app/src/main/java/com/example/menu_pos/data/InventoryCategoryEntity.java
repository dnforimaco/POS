package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inventory_categories",
        indices = {@Index(value = "id", unique = true)}
)
public class InventoryCategoryEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public int sortOrder;

    public InventoryCategoryEntity() {
        this.id = "";
    }

    @Ignore
    public InventoryCategoryEntity(@NonNull String id, String name, int sortOrder) {
        this.id = id;
        this.name = name != null ? name : "";
        this.sortOrder = sortOrder;
    }
}

