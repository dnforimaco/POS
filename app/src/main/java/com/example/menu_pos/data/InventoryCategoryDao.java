package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface InventoryCategoryDao {
    @Query("SELECT * FROM inventory_categories ORDER BY sortOrder ASC, name ASC")
    List<InventoryCategoryEntity> getAll();

    @Query("SELECT COUNT(*) FROM inventory_categories")
    int getCount();

    @Query("SELECT * FROM inventory_categories WHERE id = :id LIMIT 1")
    InventoryCategoryEntity getById(String id);

    @Query("SELECT * FROM inventory_categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    InventoryCategoryEntity getByName(String name);

    @Insert
    void insert(InventoryCategoryEntity c);

    @Update
    void update(InventoryCategoryEntity c);

    @Query("DELETE FROM inventory_categories WHERE id = :id")
    void deleteById(String id);
}

