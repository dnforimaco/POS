package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "menu_item_ingredients",
        indices = {@Index("menuItemId"), @Index("inventoryItemId")}
)
public class MenuItemIngredientEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String menuItemId;
    public String inventoryItemId;

    /** Quantity to deduct from inventory for 1 sold unit of this menu item. */
    public int qtyPerUnit;

    public MenuItemIngredientEntity() {}

    @Ignore
    public MenuItemIngredientEntity(String menuItemId, String inventoryItemId, int qtyPerUnit) {
        this.menuItemId = menuItemId;
        this.inventoryItemId = inventoryItemId;
        this.qtyPerUnit = Math.max(0, qtyPerUnit);
    }
}

