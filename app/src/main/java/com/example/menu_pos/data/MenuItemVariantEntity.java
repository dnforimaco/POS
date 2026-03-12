package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "menu_item_variants", indices = {@Index("menuItemId")})
public class MenuItemVariantEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String menuItemId;
    public String label;
    public int priceCents;

    public MenuItemVariantEntity() {}

    public MenuItemVariantEntity(String menuItemId, String label, int priceCents) {
        this.menuItemId = menuItemId;
        this.label = label != null ? label : "";
        this.priceCents = priceCents;
    }
}
