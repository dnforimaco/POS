package com.example.menu_pos.data;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Inventory CRUD + deduction/restock + movement history.
 *
 * Note: DB is configured with allowMainThreadQueries() in AppDatabase for this app.
 */
public class InventoryRepository {

    /** Default minimum stock warning threshold. */
    public static final int DEFAULT_MIN_STOCK_QTY = 5;

    private final AppDatabase db;
    private final InventoryCategoryDao categoryDao;
    private final InventoryItemDao itemDao;
    private final InventoryMovementDao movementDao;
    private final MenuItemIngredientDao ingredientDao;
    private final MenuRepository menuRepo;

    public InventoryRepository(Context context) {
        db = AppDatabase.getInstance(context.getApplicationContext());
        categoryDao = db.inventoryCategoryDao();
        itemDao = db.inventoryItemDao();
        movementDao = db.inventoryMovementDao();
        ingredientDao = db.menuItemIngredientDao();
        menuRepo = new MenuRepository(context.getApplicationContext());
        seedIfEmpty();
        syncFromMenu();
    }

    private void seedIfEmpty() {
        if (categoryDao.getCount() > 0) return;
        // Seed a hidden catch‑all category used internally for orphaned items.
        categoryDao.insert(new InventoryCategoryEntity("inv_general", "", 0));
    }

    /**
     * Syncs inventory with the current menu: adds new items, moves existing items to the correct
     * category, and removes inventory items that are no longer on the menu for each menu-derived category.
     */
    private void syncFromMenu() {
        try {
            List<MenuCategory> menuCats = menuRepo.getCategories();
            if (menuCats == null) return;

            int nextSort = categoryDao.getCount();
            // For each menu-derived inv category, track the set of item names that belong there.
            Map<String, Set<String>> expectedNamesByInvCategoryId = new HashMap<>();

            for (MenuCategory mc : menuCats) {
                if (mc == null) continue;
                String catName = mc.getName() != null ? mc.getName().trim() : "";
                if (catName.isEmpty()) catName = "General";

                InventoryCategoryEntity invCat = categoryDao.getByName(catName);
                if (invCat == null) {
                    String newId = "inv_cat_" + UUID.randomUUID().toString().substring(0, 8);
                    invCat = new InventoryCategoryEntity(newId, catName, nextSort++);
                    categoryDao.insert(invCat);
                }

                Set<String> expectedNames = new HashSet<>();
                expectedNamesByInvCategoryId.put(invCat.id, expectedNames);

                List<MenuItem> menuItems = mc.getItems();
                if (menuItems == null) continue;

                for (MenuItem mi : menuItems) {
                    if (mi == null) continue;
                    String baseName = mi.getName() != null ? mi.getName().trim() : "";
                    if (baseName.isEmpty()) continue;

                    List<MenuItemVariant> vars = mi.getVariants();
                    if (vars == null || vars.isEmpty()) {
                        ensureInventoryItemInCategory(invCat.id, baseName, expectedNames);
                        continue;
                    }

                    for (MenuItemVariant v : vars) {
                        String label = v != null && v.getLabel() != null ? v.getLabel().trim() : "";
                        String invName = label.isEmpty() ? baseName : (baseName + " " + label);
                        ensureInventoryItemInCategory(invCat.id, invName, expectedNames);
                    }
                }
            }

            // Remove inventory items that are no longer on the menu for each menu-derived category.
            for (Map.Entry<String, Set<String>> e : expectedNamesByInvCategoryId.entrySet()) {
                String invCatId = e.getKey();
                Set<String> expected = e.getValue();
                if (expected == null) continue;
                for (InventoryItemEntity item : itemDao.getByCategory(invCatId)) {
                    if (item == null || item.name == null) continue;
                    String name = item.name.trim();
                    if (!expected.contains(name)) {
                        itemDao.deleteById(item.id);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Ensures an inventory item with this name exists in the given category.
     * If it already exists (by name), moves it to this category. Preserves stock.
     */
    private void ensureInventoryItemInCategory(String categoryId, String name, Set<String> expectedNames) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        expectedNames.add(trimmed);
        InventoryItemEntity existing = itemDao.getByName(trimmed);
        if (existing != null) {
            if (existing.name != null && !existing.name.trim().isEmpty()) {
                expectedNames.add(existing.name.trim());
            }
            if (!categoryId.equals(existing.categoryId)) {
                existing.categoryId = categoryId;
                itemDao.update(existing);
            }
            return;
        }
        String id = "inv_item_" + UUID.randomUUID().toString().substring(0, 8);
        itemDao.insert(new InventoryItemEntity(id, categoryId, trimmed, 0, DEFAULT_MIN_STOCK_QTY));
    }

    // --- Categories ---

    public List<InventoryCategoryEntity> getCategories() {
        return categoryDao.getAll();
    }

    public String addCategory(String name) {
        String id = "inv_cat_" + UUID.randomUUID().toString().substring(0, 8);
        int sort = categoryDao.getCount();
        categoryDao.insert(new InventoryCategoryEntity(id, name, sort));
        return id;
    }

    public void updateCategory(String id, String name) {
        InventoryCategoryEntity c = categoryDao.getById(id);
        if (c == null) return;
        c.name = name != null ? name : "";
        categoryDao.update(c);
    }

    public void deleteCategory(String id) {
        // Keep items; categoryId would point to deleted id. For now, move them to General.
        for (InventoryItemEntity item : itemDao.getByCategory(id)) {
            item.categoryId = "inv_general";
            itemDao.update(item);
        }
        categoryDao.deleteById(id);
    }

    // --- Items ---

    public List<InventoryItemEntity> getItems(@Nullable String categoryIdOrNull) {
        if (categoryIdOrNull == null || categoryIdOrNull.trim().isEmpty()) return itemDao.getAll();
        return itemDao.getByCategory(categoryIdOrNull);
    }

    public String addItem(String categoryId, String name, int stockQty, int minStockQty) {
        String id = "inv_item_" + UUID.randomUUID().toString().substring(0, 8);
        itemDao.insert(new InventoryItemEntity(id, categoryId, name, Math.max(0, stockQty), Math.max(0, minStockQty)));
        return id;
    }

    public void updateItem(String id, String categoryId, String name, int stockQty, int minStockQty) {
        InventoryItemEntity item = itemDao.getById(id);
        if (item == null) return;
        item.categoryId = categoryId;
        item.name = name != null ? name : "";
        item.stockQty = Math.max(0, stockQty);
        item.minStockQty = Math.max(0, minStockQty);
        itemDao.update(item);
    }

    public void deleteItem(String id) {
        itemDao.deleteById(id);
    }

    public void restock(String itemId, int qty, @Nullable String actor, @Nullable String note) {
        if (qty <= 0) return;
        long ts = System.currentTimeMillis();
        db.runInTransaction(() -> {
            itemDao.addStock(itemId, qty);
            movementDao.insert(new InventoryMovementEntity(ts, itemId, qty, "RESTOCK", note, null, actor));
        });
    }

    // --- Recipes / Ingredients ---

    public List<MenuItemIngredientEntity> getIngredientsForMenuItem(String menuItemId) {
        return ingredientDao.getByMenuItemId(menuItemId);
    }

    public void addIngredient(String menuItemId, String inventoryItemId, int qtyPerUnit) {
        if (qtyPerUnit <= 0) return;
        ingredientDao.insert(new MenuItemIngredientEntity(menuItemId, inventoryItemId, qtyPerUnit));
    }

    public void deleteIngredient(long ingredientId) {
        ingredientDao.deleteById(ingredientId);
    }

    // --- History ---

    public List<InventoryMovementEntity> getRecentHistory(int limit) {
        return movementDao.getRecent(limit);
    }

    public void clearHistory() {
        movementDao.deleteAll();
    }

    // --- Sale deduction ---

    public static final class LowStockWarning {
        public final String itemName;
        public final int stockQty;
        public final int minStockQty;

        LowStockWarning(String itemName, int stockQty, int minStockQty) {
            this.itemName = itemName;
            this.stockQty = stockQty;
            this.minStockQty = minStockQty;
        }
    }

    /**
     * Deduct inventory for a completed order.
     *
     * This project uses a simple 1:1 mapping: when a menu item is sold, deduct an inventory item
     * with the same name (case-insensitive) by the sold quantity.
     * Returns low-stock warnings (items that are now <= minStockQty).
     */
    public List<LowStockWarning> deductForSale(List<CartItem> cartItems, long orderId, @Nullable String cashier) {
        if (cartItems == null || cartItems.isEmpty()) return new ArrayList<>();

        // Aggregate required deductions across the whole cart by inventory item id.
        Map<String, Integer> requiredByInventoryItemId = new HashMap<>();
        Map<String, String> noteByInventoryItemId = new HashMap<>();

        for (CartItem c : cartItems) {
            if (c == null) continue;
            String baseName = c.getItemName();
            if (baseName == null || baseName.trim().isEmpty()) continue;
            String label = c.getVariantLabel() != null ? c.getVariantLabel().trim() : "";
            String menuName = label.isEmpty() ? baseName.trim() : (baseName.trim() + " " + label);

            InventoryItemEntity invItem = itemDao.getByName(menuName.trim());
            if (invItem == null || invItem.id == null || invItem.id.trim().isEmpty()) continue;

            int qty = Math.max(0, c.getQuantity());
            if (qty <= 0) continue;

            int prev = requiredByInventoryItemId.containsKey(invItem.id) ? requiredByInventoryItemId.get(invItem.id) : 0;
            requiredByInventoryItemId.put(invItem.id, prev + qty);
            if (!noteByInventoryItemId.containsKey(invItem.id)) {
                noteByInventoryItemId.put(invItem.id, "Sale: " + menuName);
            }
        }

        if (requiredByInventoryItemId.isEmpty()) return new ArrayList<>(); // nothing matched by name

        long ts = System.currentTimeMillis();
        List<LowStockWarning> warnings = new ArrayList<>();

        db.runInTransaction(() -> {
            for (Map.Entry<String, Integer> e : requiredByInventoryItemId.entrySet()) {
                String invItemId = e.getKey();
                int deductQty = e.getValue() != null ? e.getValue() : 0;
                if (deductQty <= 0) continue;

                // Clamp to zero (don’t go negative).
                itemDao.addStockClampZero(invItemId, -deductQty);

                movementDao.insert(new InventoryMovementEntity(
                        ts,
                        invItemId,
                        -deductQty,
                        "SALE",
                        noteByInventoryItemId.get(invItemId),
                        orderId,
                        cashier
                ));

                InventoryItemEntity updated = itemDao.getById(invItemId);
                if (updated != null && updated.minStockQty > 0 && updated.stockQty <= updated.minStockQty) {
                    warnings.add(new LowStockWarning(updated.name, updated.stockQty, updated.minStockQty));
                }
            }
        });

        return warnings;
    }
}

