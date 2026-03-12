package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InventoryItemDao {
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    List<InventoryItemEntity> getAll();

    @Query("SELECT * FROM inventory_items WHERE categoryId = :categoryId ORDER BY name ASC")
    List<InventoryItemEntity> getByCategory(String categoryId);

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    InventoryItemEntity getById(String id);

    @Query("SELECT * FROM inventory_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    InventoryItemEntity getByName(String name);

    @Insert
    void insert(InventoryItemEntity item);

    @Update
    void update(InventoryItemEntity item);

    @Query("DELETE FROM inventory_items WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE inventory_items SET stockQty = stockQty + :delta WHERE id = :id")
    void addStock(String id, int delta);

    @Query("UPDATE inventory_items SET stockQty = CASE WHEN stockQty + :delta < 0 THEN 0 ELSE stockQty + :delta END WHERE id = :id")
    void addStockClampZero(String id, int delta);
}

