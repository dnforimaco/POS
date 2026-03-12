package com.example.menu_pos.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inventory_movements",
        indices = {@Index("itemId"), @Index("timestampMillis")}
)
public class InventoryMovementEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMillis;
    public String itemId;

    /** + for restock, - for deductions. */
    public int deltaQty;

    /** e.g. "RESTOCK", "SALE", "ADJUST" */
    public String type;

    /** Optional human-readable note (menu item name, etc). */
    public String note;

    /** Nullable. When the movement is caused by a completed order. */
    public Long relatedOrderId;

    /** User who performed the action (Manager) or cashier on sale. */
    public String actor;

    public InventoryMovementEntity() {}

    @Ignore
    public InventoryMovementEntity(long timestampMillis, String itemId, int deltaQty, String type,
                                   String note, Long relatedOrderId, String actor) {
        this.timestampMillis = timestampMillis;
        this.itemId = itemId;
        this.deltaQty = deltaQty;
        this.type = type != null ? type : "";
        this.note = note != null ? note : "";
        this.relatedOrderId = relatedOrderId;
        this.actor = actor != null ? actor : "";
    }
}

