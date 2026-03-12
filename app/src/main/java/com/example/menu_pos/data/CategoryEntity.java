package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories", indices = {@Index(value = "id", unique = true)})
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String subtitle;
    public int iconResId;
    public int sortOrder;

    public CategoryEntity() {
        this.id = "";
    }

    @Ignore
    public CategoryEntity(@NonNull String id, String name, String subtitle, int iconResId, int sortOrder) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle != null ? subtitle : "";
        this.iconResId = iconResId;
        this.sortOrder = sortOrder;
    }
}
