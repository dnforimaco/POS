package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "menu_items", indices = {@Index(value = "id", unique = true), @Index("categoryId")})
public class MenuItemEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String categoryId;
    public String name;
    public String description;
    public int imageResId;
    public boolean bestSeller;
    public boolean spicy;
    public int sortOrder;
    /**
     * When true, seed sync must not overwrite this row (manager added or edited the item).
     */
    public boolean userManaged;

    public MenuItemEntity() {
        this.id = "";
    }

    @Ignore
    public MenuItemEntity(@NonNull String id, String categoryId, String name, String description,
                          int imageResId, boolean bestSeller, boolean spicy, int sortOrder) {
        this(id, categoryId, name, description, imageResId, bestSeller, spicy, sortOrder, false);
    }

    @Ignore
    public MenuItemEntity(@NonNull String id, String categoryId, String name, String description,
                          int imageResId, boolean bestSeller, boolean spicy, int sortOrder,
                          boolean userManaged) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description != null ? description : "";
        this.imageResId = imageResId;
        this.bestSeller = bestSeller;
        this.spicy = spicy;
        this.sortOrder = sortOrder;
        this.userManaged = userManaged;
    }
}
