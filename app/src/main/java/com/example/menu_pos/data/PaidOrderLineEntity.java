package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "paid_order_lines", indices = {@Index("orderId")})
public class PaidOrderLineEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long orderId;
    public String itemName;
    public String variantLabel;
    public int unitPriceCents;
    public int quantity;

    public PaidOrderLineEntity() {}

    @Ignore
    public PaidOrderLineEntity(long orderId, String itemName, String variantLabel, int unitPriceCents, int quantity) {
        this.orderId = orderId;
        this.itemName = itemName != null ? itemName : "";
        this.variantLabel = variantLabel != null ? variantLabel : "";
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
    }

    public int getLineTotalCents() {
        return unitPriceCents * quantity;
    }
}
