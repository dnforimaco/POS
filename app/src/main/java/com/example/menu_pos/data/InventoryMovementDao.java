package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InventoryMovementDao {
    @Insert
    void insert(InventoryMovementEntity e);

    @Query("SELECT * FROM inventory_movements ORDER BY timestampMillis DESC, id DESC LIMIT :limit")
    List<InventoryMovementEntity> getRecent(int limit);

    @Query("SELECT * FROM inventory_movements WHERE itemId = :itemId ORDER BY timestampMillis DESC, id DESC LIMIT :limit")
    List<InventoryMovementEntity> getRecentForItem(String itemId, int limit);

    @Query("DELETE FROM inventory_movements")
    void deleteAll();
}

