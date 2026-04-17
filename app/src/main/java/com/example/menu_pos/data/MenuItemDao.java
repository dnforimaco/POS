package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MenuItemDao {

    @Query("SELECT * FROM menu_items WHERE categoryId = :categoryId ORDER BY sortOrder ASC, id ASC")
    List<MenuItemEntity> getByCategoryId(String categoryId);

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    MenuItemEntity getById(String id);

    @Query("SELECT COUNT(*) FROM menu_items")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MenuItemEntity item);

    @Update
    void update(MenuItemEntity item);

    @Query("DELETE FROM menu_items WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM menu_items WHERE categoryId = :categoryId")
    void deleteByCategoryId(String categoryId);

    @Query("UPDATE menu_items SET imageResId = :imageResId WHERE id = :id")
    void updateImageResId(String id, int imageResId);
}
