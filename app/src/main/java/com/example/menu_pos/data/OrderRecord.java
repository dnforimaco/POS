package com.example.menu_pos.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A completed order (saved when user checks out).
 */
public class OrderRecord {
    private final long id;
    private final long timestampMillis;
    private final List<OrderLineItem> lines;
    private final int totalCents;

    public OrderRecord(long id, long timestampMillis, List<OrderLineItem> lines, int totalCents) {
        this.id = id;
        this.timestampMillis = timestampMillis;
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
        this.totalCents = totalCents;
    }

    public long getId() { return id; }
    public long getTimestampMillis() { return timestampMillis; }
    public List<OrderLineItem> getLines() { return lines; }
    public int getTotalCents() { return totalCents; }
}
