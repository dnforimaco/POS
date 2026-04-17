package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
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

    /** Menu {@link com.example.menu_pos.data.MenuItem} id when known; empty for legacy rows. */
    @NonNull
    @ColumnInfo(defaultValue = "")
    public String sourceItemId = "";

    /** Stored discount in centavos (10% of line subtotal when applied). */
    @ColumnInfo(name = "discount_amount", defaultValue = "0")
    public int discountAmountCents;

    /** Line amount after discount (centavos). */
    @ColumnInfo(name = "line_total", defaultValue = "0")
    public int lineTotalCents;

    public PaidOrderLineEntity() {}

    @Ignore
    public PaidOrderLineEntity(long orderId, String itemName, String variantLabel, int unitPriceCents, int quantity) {
        this.orderId = orderId;
        this.itemName = itemName != null ? itemName : "";
        this.variantLabel = variantLabel != null ? variantLabel : "";
        this.unitPriceCents = unitPriceCents;
        this.quantity = quantity;
        this.sourceItemId = "";
        this.discountAmountCents = 0;
        this.lineTotalCents = unitPriceCents * quantity;
    }

    /** Unit price × quantity (kitchen slip uses this — no discounts). */
    public int getLineSubtotalCents() {
        return unitPriceCents * quantity;
    }

    /** Amount after discount; matches persisted {@link #lineTotalCents} when set. */
    public int getLineTotalCents() {
        if (lineTotalCents != 0 || discountAmountCents != 0) {
            return lineTotalCents;
        }
        return getLineSubtotalCents();
    }

    public int getDiscountAmountCents() {
        return discountAmountCents;
    }

    public boolean hasLineDiscount() {
        return discountAmountCents > 0;
    }
}
