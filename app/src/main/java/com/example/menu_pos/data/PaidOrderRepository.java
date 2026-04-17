package com.example.menu_pos.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaidOrderRepository {

    private final PaidOrderDao orderDao;
    private final PaidOrderLineDao lineDao;

    public PaidOrderRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        orderDao = db.paidOrderDao();
        lineDao = db.paidOrderLineDao();
    }

    public long savePaidOrder(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        if (order.orderStatus != OrderStatus.PENDING && order.orderStatus != OrderStatus.PAID) {
            order.orderStatus = OrderStatus.PAID;
        }
        long orderId = orderDao.insert(order);
        attachLines(orderId, lines);
        return orderId;
    }

    /**
     * Creates a kitchen / unpaid order. Does not deduct inventory.
     */
    public long createPendingOrderFromCart(List<CartItem> items, String orderNotes, String cashierName, String orderType, String tableNumber) {
        if (items == null || items.isEmpty()) return -1;
        long now = System.currentTimeMillis();
        int total = 0;
        for (CartItem c : items) total += c.getTotalCents();
        PaidOrderEntity o = new PaidOrderEntity();
        o.timestampMillis = now;
        o.totalCents = total;
        o.orderType = orderType != null ? orderType : "";
        o.paymentType = "";
        o.paymentRefLast4 = "";
        o.cashierName = cashierName != null ? cashierName : "";
        o.tableNumber = tableNumber != null ? tableNumber : "";
        o.cashReceivedCents = 0;
        o.changeCents = 0;
        o.orderStatus = OrderStatus.PENDING;
        o.orderNotes = orderNotes != null ? orderNotes : "";
        long orderId = orderDao.insert(o);
        attachLines(orderId, linesFromCart(items));
        return orderId;
    }

    /**
     * Updates line items and totals for an existing pending order.
     */
    public void updatePendingOrderFromCart(long orderId, List<CartItem> items, String orderNotes, String cashierName, String orderType, String tableNumber) {
        if (items == null || items.isEmpty()) return;
        PaidOrderEntity existing = orderDao.getById(orderId);
        if (existing == null || existing.orderStatus != OrderStatus.PENDING) return;
        int total = 0;
        for (CartItem c : items) total += c.getTotalCents();
        existing.totalCents = total;
        existing.orderNotes = orderNotes != null ? orderNotes : "";
        existing.orderType = orderType != null ? orderType : "";
        existing.tableNumber = tableNumber != null ? tableNumber : "";
        if (cashierName != null) existing.cashierName = cashierName;
        orderDao.update(existing);
        lineDao.deleteByOrderId(orderId);
        attachLines(orderId, linesFromCart(items));
    }

    /**
     * Replaces lines and header fields when completing payment on a pending order.
     */
    public void finalizePendingOrderAsPaid(long orderId, PaidOrderEntity paidHeader, List<PaidOrderLineEntity> lines) {
        paidHeader.id = orderId;
        paidHeader.orderStatus = OrderStatus.PAID;
        orderDao.update(paidHeader);
        lineDao.deleteByOrderId(orderId);
        attachLines(orderId, lines);
    }

    private void attachLines(long orderId, List<PaidOrderLineEntity> lines) {
        if (lines == null || lines.isEmpty()) return;
        List<PaidOrderLineEntity> toInsert = new ArrayList<>();
        for (PaidOrderLineEntity l : lines) {
            l.orderId = orderId;
            toInsert.add(l);
        }
        lineDao.insertAll(toInsert);
    }

    private static List<PaidOrderLineEntity> linesFromCart(List<CartItem> items) {
        List<PaidOrderLineEntity> lines = new ArrayList<>();
        for (CartItem c : items) {
            PaidOrderLineEntity l = new PaidOrderLineEntity();
            l.itemName = c.getItemName() != null ? c.getItemName() : "";
            l.variantLabel = c.getVariantLabel() != null ? c.getVariantLabel() : "";
            l.unitPriceCents = c.getUnitPriceCents();
            l.quantity = c.getQuantity();
            l.sourceItemId = c.getItemId() != null ? c.getItemId() : "";
            l.discountAmountCents = c.getDiscountCents();
            l.lineTotalCents = c.getLineTotalCents();
            lines.add(l);
        }
        return lines;
    }

    public static List<PaidOrderLineEntity> buildLinesFromCart(List<CartItem> items) {
        return linesFromCart(items != null ? items : new ArrayList<>());
    }

    public List<PaidOrderEntity> getAllOrdersDesc() {
        return orderDao.getAllDesc();
    }

    public PaidOrderEntity getOrderById(long id) {
        return orderDao.getById(id);
    }

    public List<PaidOrderLineEntity> getLinesForOrder(long orderId) {
        return lineDao.getByOrderId(orderId);
    }

    public Map<Long, Integer> getItemCountsForOrders(List<Long> orderIds) {
        Map<Long, Integer> counts = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) return counts;
        List<OrderItemCountRow> rows = lineDao.getItemCountsForOrders(orderIds);
        if (rows == null) return counts;
        for (OrderItemCountRow row : rows) {
            counts.put(row.orderId, Math.max(0, row.itemCount));
        }
        return counts;
    }

    public void deleteOrder(long orderId) {
        lineDao.deleteByOrderId(orderId);
        orderDao.deleteById(orderId);
    }

    public void deleteOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return;
        for (Long id : orderIds) {
            if (id != null) lineDao.deleteByOrderId(id);
        }
        orderDao.deleteByIds(orderIds);
    }
}

