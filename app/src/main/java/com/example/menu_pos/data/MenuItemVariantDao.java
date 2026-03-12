package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MenuItemVariantDao {

    @Query("SELECT * FROM menu_item_variants WHERE menuItemId = :menuItemId ORDER BY id ASC")
    List<MenuItemVariantEntity> getByMenuItemId(String menuItemId);

    @Query("SELECT * FROM menu_item_variants WHERE id = :id LIMIT 1")
    MenuItemVariantEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MenuItemVariantEntity variant);

    @Update
    void update(MenuItemVariantEntity variant);

    @Query("DELETE FROM menu_item_variants WHERE menuItemId = :menuItemId")
    void deleteByMenuItemId(String menuItemId);

    @Query("DELETE FROM menu_item_variants WHERE id = :id")
    void deleteById(long id);
}
