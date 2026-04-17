package com.example.menu_pos.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {
        User.class,
        CategoryEntity.class, MenuItemEntity.class, MenuItemVariantEntity.class,
        PaidOrderEntity.class, PaidOrderLineEntity.class,
        InventoryCategoryEntity.class, InventoryItemEntity.class,
        InventoryMovementEntity.class,
        MenuItemIngredientEntity.class
}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE menu_items ADD COLUMN userManaged INTEGER NOT NULL DEFAULT 0");
            // Manager-created ids are item_<uuid>; mark existing rows so seed sync stops deleting them.
            db.execSQL("UPDATE menu_items SET userManaged = 1 WHERE id GLOB 'item_*'");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE paid_orders ADD COLUMN tableNumber TEXT NOT NULL DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE paid_orders ADD COLUMN orderStatus INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE paid_orders ADD COLUMN orderNotes TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE paid_order_lines ADD COLUMN sourceItemId TEXT NOT NULL DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE paid_order_lines ADD COLUMN discount_amount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE paid_order_lines ADD COLUMN line_total INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE paid_order_lines SET line_total = unitPriceCents * quantity WHERE line_total = 0");
        }
    };

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
                                    .addMigrations(MIGRATION_6_7)
                                    .addMigrations(MIGRATION_7_8)
                                    .addMigrations(MIGRATION_8_9)
                                    .addMigrations(MIGRATION_9_10)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
