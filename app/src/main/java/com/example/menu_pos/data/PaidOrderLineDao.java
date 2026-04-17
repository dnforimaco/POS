package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PaidOrderLineDao {

    @Insert
    void insertAll(List<PaidOrderLineEntity> lines);

    @Query("SELECT * FROM paid_order_lines WHERE orderId = :orderId ORDER BY id ASC")
    List<PaidOrderLineEntity> getByOrderId(long orderId);

    @Query("SELECT orderId, COALESCE(SUM(CASE WHEN quantity > 0 THEN quantity ELSE 0 END), 0) AS itemCount " +
            "FROM paid_order_lines WHERE orderId IN (:orderIds) GROUP BY orderId")
    List<OrderItemCountRow> getItemCountsForOrders(List<Long> orderIds);

    @Query("DELETE FROM paid_order_lines WHERE orderId = :orderId")
    void deleteByOrderId(long orderId);
}

