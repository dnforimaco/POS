package com.example.menu_pos.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        User.class,
        CategoryEntity.class, MenuItemEntity.class, MenuItemVariantEntity.class,
        PaidOrderEntity.class, PaidOrderLineEntity.class,
        InventoryCategoryEntity.class, InventoryItemEntity.class,
        InventoryMovementEntity.class,
        MenuItemIngredientEntity.class
}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract MenuItemDao menuItemDao();
    public abstract MenuItemVariantDao menuItemVariantDao();
    public abstract PaidOrderDao paidOrderDao();
    public abstract PaidOrderLineDao paidOrderLineDao();

    public abstract InventoryCategoryDao inventoryCategoryDao();
    public abstract InventoryItemDao inventoryItemDao();
    public abstract InventoryMovementDao inventoryMovementDao();
    public abstract MenuItemIngredientDao menuItemIngredientDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "menu_pos_db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
