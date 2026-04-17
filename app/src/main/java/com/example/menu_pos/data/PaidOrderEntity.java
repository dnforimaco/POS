package com.example.menu_pos.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "paid_orders")
public class PaidOrderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMillis;
    public int totalCents;
    /** e.g. "Dine In" or "Take Out" */
    public String orderType;
    public String paymentType;   // e.g. "Cash"
    public String paymentRefLast4; // for GCash: last 4 digits
    public String cashierName;   // logged-in username
    
    /** e.g. "7" or "A3" for dine-in seating */
    @NonNull
    @ColumnInfo(defaultValue = "")
    public String tableNumber = "";

    public int cashReceivedCents;
    public int changeCents;

    /** {@link OrderStatus#PENDING} or {@link OrderStatus#PAID}. */
    @ColumnInfo(defaultValue = "1")
    public int orderStatus = OrderStatus.PAID;

    @NonNull
    @ColumnInfo(defaultValue = "")
    public String orderNotes = "";

    public PaidOrderEntity() {}

    @Ignore
    public PaidOrderEntity(long timestampMillis, int totalCents, String orderType, String paymentType, String cashierName,
                           String tableNumber, int cashReceivedCents, int changeCents, String paymentRefLast4) {
        this.timestampMillis = timestampMillis;
        this.totalCents = totalCents;
        this.orderType = orderType != null ? orderType : "";
        this.paymentType = paymentType != null ? paymentType : "";
        this.cashierName = cashierName != null ? cashierName : "";
        this.tableNumber = tableNumber != null ? tableNumber : "";
        this.cashReceivedCents = cashReceivedCents;
        this.changeCents = changeCents;
        this.paymentRefLast4 = paymentRefLast4 != null ? paymentRefLast4 : "";
        this.orderStatus = OrderStatus.PAID;
        this.orderNotes = "";
    }
}
