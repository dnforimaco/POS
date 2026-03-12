package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MenuItemIngredientDao {
    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId ORDER BY id ASC")
    List<MenuItemIngredientEntity> getByMenuItemId(String menuItemId);

    @Insert
    void insert(MenuItemIngredientEntity e);

    @Query("DELETE FROM menu_item_ingredients WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    void deleteByMenuItemId(String menuItemId);
}

