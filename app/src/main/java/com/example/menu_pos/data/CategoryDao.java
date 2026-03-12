package com.example.menu_pos.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    List<CategoryEntity> getAll();

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    CategoryEntity getById(String id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Query("DELETE FROM categories WHERE id = :id")
    void deleteById(String id);
}
