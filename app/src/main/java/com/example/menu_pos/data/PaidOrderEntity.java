package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "paid_orders")
public class PaidOrderEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMillis;
    public int totalCents;
    public String paymentType;   // e.g. "Cash"
    public String cashierName;   // logged-in username
    public int cashReceivedCents;
    public int changeCents;

    public PaidOrderEntity() {}

    @Ignore
    public PaidOrderEntity(long timestampMillis, int totalCents, String paymentType, String cashierName,
                           int cashReceivedCents, int changeCents) {
        this.timestampMillis = timestampMillis;
        this.totalCents = totalCents;
        this.paymentType = paymentType != null ? paymentType : "";
        this.cashierName = cashierName != null ? cashierName : "";
        this.cashReceivedCents = cashReceivedCents;
        this.changeCents = changeCents;
    }
}
