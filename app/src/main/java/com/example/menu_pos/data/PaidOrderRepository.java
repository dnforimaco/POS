package com.example.menu_pos.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class PaidOrderRepository {

    private final PaidOrderDao orderDao;
    private final PaidOrderLineDao lineDao;

    public PaidOrderRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        orderDao = db.paidOrderDao();
        lineDao = db.paidOrderLineDao();
    }

    public long savePaidOrder(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        long orderId = orderDao.insert(order);
        List<PaidOrderLineEntity> toInsert = new ArrayList<>();
        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                l.orderId = orderId;
                toInsert.add(l);
            }
        }
        if (!toInsert.isEmpty()) lineDao.insertAll(toInsert);
        return orderId;
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

