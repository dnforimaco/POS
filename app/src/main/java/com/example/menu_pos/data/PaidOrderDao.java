package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PaidOrderDao {

    @Insert
    long insert(PaidOrderEntity order);

    @Query("SELECT * FROM paid_orders ORDER BY id DESC")
    List<PaidOrderEntity> getAllDesc();

    @Query("SELECT * FROM paid_orders WHERE id = :id LIMIT 1")
    PaidOrderEntity getById(long id);

    @Query("DELETE FROM paid_orders WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM paid_orders WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);
}

