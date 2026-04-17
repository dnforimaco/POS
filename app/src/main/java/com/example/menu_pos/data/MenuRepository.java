package com.example.menu_pos.data;

import android.content.Context;

import com.example.menu_pos.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Single source of menu data. Reads/writes categories and items from Room DB.
 * Seeds from MenuData on first run.
 */
public class MenuRepository {

    private final CategoryDao categoryDao;
    private final MenuItemDao menuItemDao;
    private final MenuItemVariantDao variantDao;

    public MenuRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        categoryDao = db.categoryDao();
        menuItemDao = db.menuItemDao();
        variantDao = db.menuItemVariantDao();
        repairCatalogIfNeeded();
        seedIfEmpty();
        applyImageMappings();
        applySeedUpdates();
    }

    /**
     * Repairs catalog rows when storage was partially cleared and only some tables survived.
     */
    private void repairCatalogIfNeeded() {
        try {
            if (categoryDao.getCount() == 0 || menuItemDao.getCount() == 0) {
                seedIfEmpty();
                syncMenuDataToDb();
                syncCategoryOrderFromMenuData();
            }
        } catch (Exception ignored) {
        }
    }

    private void seedIfEmpty() {
        if (categoryDao.getCount() > 0) return;
        List<MenuCategory> seed = MenuData.getCategories();
        int order = 0;
        for (MenuCategory c : seed) {
            categoryDao.insert(new CategoryEntity(c.getId(), c.getName(), c.getSubtitle(),
                    c.getIconResId(), order++));
            int itemOrder = 0;
            for (MenuItem item : c.getItems()) {
                int img = imageResForItemId(item.getId(), item.getImageResId());
                menuItemDao.insert(new MenuItemEntity(item.getId(), c.getId(), item.getName(),
                        item.getDescription(), img, item.isBestSeller(), item.isSpicy(), itemOrder++));
                for (MenuItemVariant v : item.getVariants()) {
                    variantDao.insert(new MenuItemVariantEntity(item.getId(), v.getLabel(), v.getPriceCents()));
                }
            }
        }
    }

    private void applyImageMappings() {
        // Update existing seeded DB rows to include newly added drawables.
        applyImageIfPresent("bilao_miki_bihon", R.drawable.bilao_miki_bihon);
        applyImageIfPresent("bilao_pancit_bihon", R.drawable.bilao_pancit_bihon);
        applyImageIfPresent("bilao_lumpiang_shanghai", R.drawable.bilao_lumpiang_shanghai);
        applyImageIfPresent("bilao_canton", R.drawable.bilao_canton_guisado);
        applyImageIfPresent("bilao_chami", R.drawable.bilao_chami);
    }

    private void applySeedUpdates() {
        // Seed only runs on first install; this keeps existing installs in sync for key data fixes.
        applyChillZoneVariantsIfPresent();
        applyOthersCategoryAndItemsIfMissingOrOutdated();
        syncMenuDataToDb();
        syncCategoryOrderFromMenuData();
    }

    /**
     * Inserts any categories and items from MenuData that are not yet in the DB,
     * so new categories (e.g. Drinks, Appetizers, Longganisa) and new items appear after an app update.
     */
    private void syncMenuDataToDb() {
        try {
            List<MenuCategory> seed = MenuData.getCategories();
            for (MenuCategory c : seed) {
                CategoryEntity existingCat = categoryDao.getById(c.getId());
                if (existingCat == null) {
                    int order = categoryDao.getCount();
                    categoryDao.insert(new CategoryEntity(c.getId(), c.getName(), c.getSubtitle(),
                            c.getIconResId(), order));
                    int itemOrder = 0;
                    for (MenuItem item : c.getItems()) {
                        if (menuItemDao.getById(item.getId()) != null) continue;
                        int img = imageResForItemId(item.getId(), item.getImageResId());
                        menuItemDao.insert(new MenuItemEntity(item.getId(), c.getId(), item.getName(),
                                item.getDescription(), img, item.isBestSeller(), item.isSpicy(), itemOrder++));
                        for (MenuItemVariant v : item.getVariants()) {
                            variantDao.insert(new MenuItemVariantEntity(item.getId(), v.getLabel(), v.getPriceCents()));
                        }
                    }
                } else {
                    int itemOrder = menuItemDao.getByCategoryId(c.getId()).size();
                    for (MenuItem item : c.getItems()) {
                        MenuItemEntity existingItem = menuItemDao.getById(item.getId());
                        if (existingItem == null) {
                            int img = imageResForItemId(item.getId(), item.getImageResId());
                            menuItemDao.insert(new MenuItemEntity(item.getId(), c.getId(), item.getName(),
                                    item.getDescription(), img, item.isBestSeller(), item.isSpicy(), itemOrder++));
                            for (MenuItemVariant v : item.getVariants()) {
                                variantDao.insert(new MenuItemVariantEntity(item.getId(), v.getLabel(), v.getPriceCents()));
                            }
                            itemOrder++;
                        } else if (isUserManagedItem(existingItem)) {
                            // Manager added/edited this row — do not overwrite from bundled MenuData.
                            continue;
                        } else {
                            // Sync existing item: name, description, flags, and variants (prices) from MenuData
                            boolean changed = false;
                            if (item.getName() != null && !item.getName().equals(existingItem.name)) {
                                existingItem.name = item.getName();
                                changed = true;
                            }
                            String desc = item.getDescription() != null ? item.getDescription() : "";
                            if (!desc.equals(existingItem.description)) {
                                existingItem.description = desc;
                                changed = true;
                            }
                            if (existingItem.bestSeller != item.isBestSeller()) {
                                existingItem.bestSeller = item.isBestSeller();
                                changed = true;
                            }
                            if (existingItem.spicy != item.isSpicy()) {
                                existingItem.spicy = item.isSpicy();
                                changed = true;
                            }
                            int newImg = imageResForItemId(item.getId(), item.getImageResId());
                            if (existingItem.imageResId != newImg) {
                                existingItem.imageResId = newImg;
                                changed = true;
                            }
                            if (changed) menuItemDao.update(existingItem);
                            variantDao.deleteByMenuItemId(item.getId());
                            for (MenuItemVariant v : item.getVariants()) {
                                variantDao.insert(new MenuItemVariantEntity(item.getId(), v.getLabel(), v.getPriceCents()));
                            }
                        }
                    }
                }
            }
            removeItemsNotInMenuData(seed);
        } catch (Exception ignored) {
        }
    }

    /**
     * Removes from the DB any menu items that are no longer in MenuData for each category,
     * so that deleted items (e.g. Bagnet Kare-Kare, Coke/Drinks from Others) disappear after an update.
     */
    private void removeItemsNotInMenuData(List<MenuCategory> seed) {
        try {
            Set<String> seedItemIds = new HashSet<>();
            for (MenuCategory c : seed) {
                seedItemIds.clear();
                for (MenuItem item : c.getItems()) {
                    seedItemIds.add(item.getId());
                }
                List<MenuItemEntity> dbItems = menuItemDao.getByCategoryId(c.getId());
                for (MenuItemEntity dbItem : dbItems) {
                    if (isUserManagedItem(dbItem)) continue;
                    if (!seedItemIds.contains(dbItem.id)) {
                        variantDao.deleteByMenuItemId(dbItem.id);
                        menuItemDao.deleteById(dbItem.id);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyChillZoneVariantsIfPresent() {
        try {
            MenuCategory chill = MenuData.getCategoryById("chill");
            if (chill == null) return;
            for (MenuItem seedItem : chill.getItems()) {
                // Only update rows that already exist in DB (seeded earlier).
                MenuItemEntity existing = menuItemDao.getById(seedItem.getId());
                if (existing == null) continue;
                if (isUserManagedItem(existing)) continue;

                // Keep display name in sync (you updated some names like "Sundae's Best").
                if (seedItem.getName() != null && !seedItem.getName().equals(existing.name)) {
                    existing.name = seedItem.getName();
                    menuItemDao.update(existing);
                }

                // Overwrite variants so the UI shows the picker dialog.
                variantDao.deleteByMenuItemId(seedItem.getId());
                for (MenuItemVariant v : seedItem.getVariants()) {
                    variantDao.insert(new MenuItemVariantEntity(seedItem.getId(), v.getLabel(), v.getPriceCents()));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyOthersCategoryAndItemsIfMissingOrOutdated() {
        try {
            MenuCategory seed = MenuData.getCategoryById("others");
            if (seed == null) return;

            CategoryEntity existingCat = categoryDao.getById(seed.getId());
            if (existingCat == null) {
                // Add category at the end.
                categoryDao.insert(new CategoryEntity(
                        seed.getId(),
                        seed.getName(),
                        seed.getSubtitle(),
                        seed.getIconResId(),
                        categoryDao.getCount()
                ));
            } else {
                boolean catChanged = false;
                if (seed.getName() != null && !seed.getName().equals(existingCat.name)) {
                    existingCat.name = seed.getName();
                    catChanged = true;
                }
                if (seed.getSubtitle() != null && !seed.getSubtitle().equals(existingCat.subtitle)) {
                    existingCat.subtitle = seed.getSubtitle();
                    catChanged = true;
                }
                if (catChanged) categoryDao.update(existingCat);
            }

            // Upsert items (and variants) so existing installs get them too.
            int order = 0;
            for (MenuItem seedItem : seed.getItems()) {
                MenuItemEntity existingItem = menuItemDao.getById(seedItem.getId());
                if (existingItem == null) {
                    menuItemDao.insert(new MenuItemEntity(
                            seedItem.getId(),
                            seed.getId(),
                            seedItem.getName(),
                            seedItem.getDescription(),
                            MenuItem.NO_IMAGE,
                            seedItem.isBestSeller(),
                            seedItem.isSpicy(),
                            order
                    ));
                } else if (isUserManagedItem(existingItem)) {
                    order++;
                    continue;
                } else {
                    boolean changed = false;
                    if (seedItem.getName() != null && !seedItem.getName().equals(existingItem.name)) {
                        existingItem.name = seedItem.getName();
                        changed = true;
                    }
                    String desc = seedItem.getDescription() != null ? seedItem.getDescription() : "";
                    if (!desc.equals(existingItem.description)) {
                        existingItem.description = desc;
                        changed = true;
                    }
                    if (existingItem.categoryId == null || !seed.getId().equals(existingItem.categoryId)) {
                        existingItem.categoryId = seed.getId();
                        changed = true;
                    }
                    if (existingItem.bestSeller != seedItem.isBestSeller()) {
                        existingItem.bestSeller = seedItem.isBestSeller();
                        changed = true;
                    }
                    if (existingItem.spicy != seedItem.isSpicy()) {
                        existingItem.spicy = seedItem.isSpicy();
                        changed = true;
                    }
                    if (changed) menuItemDao.update(existingItem);
                }

                // Keep variants in sync.
                variantDao.deleteByMenuItemId(seedItem.getId());
                for (MenuItemVariant v : seedItem.getVariants()) {
                    variantDao.insert(new MenuItemVariantEntity(seedItem.getId(), v.getLabel(), v.getPriceCents()));
                }

                order++;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Syncs the order of all categories from MenuData to the database.
     * This ensures category order updates are reflected when MenuData changes.
     */
    private void syncCategoryOrderFromMenuData() {
        try {
            List<MenuCategory> seed = MenuData.getCategories();
            for (int i = 0; i < seed.size(); i++) {
                MenuCategory c = seed.get(i);
                CategoryEntity existing = categoryDao.getById(c.getId());
                if (existing != null && existing.sortOrder != i) {
                    existing.sortOrder = i;
                    categoryDao.update(existing);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void applyImageIfPresent(String itemId, int resId) {
        try {
            MenuItemEntity e = menuItemDao.getById(itemId);
            if (e == null) return;
            if (e.imageResId != resId) menuItemDao.updateImageResId(itemId, resId);
        } catch (Exception ignored) {
        }
    }

    private int imageResForItemId(String itemId, int fallbackResId) {
        if ("bilao_miki_bihon".equals(itemId)) return R.drawable.bilao_miki_bihon;
        if ("bilao_pancit_bihon".equals(itemId)) return R.drawable.bilao_pancit_bihon;
        if ("bilao_lumpiang_shanghai".equals(itemId)) return R.drawable.bilao_lumpiang_shanghai;
        if ("bilao_canton".equals(itemId)) return R.drawable.bilao_canton_guisado;
        if ("bilao_chami".equals(itemId)) return R.drawable.bilao_chami;
        return fallbackResId;
    }

    public List<MenuCategory> getCategories() {
        repairCatalogIfNeeded();
        List<CategoryEntity> cats = categoryDao.getAll();
        if (cats.isEmpty()) {
            seedIfEmpty();
            cats = categoryDao.getAll();
        }
        List<MenuCategory> result = new ArrayList<>();
        for (CategoryEntity e : cats) {
            List<MenuItem> items = getItemsForCategory(e.id);
            result.add(MenuCategory.builder()
                    .id(e.id)
                    .name(e.name)
                    .subtitle(e.subtitle)
                    .items(items)
                    .iconResId(e.iconResId)
                    .build());
        }
        return result;
    }

    private List<MenuItem> getItemsForCategory(String categoryId) {
        List<MenuItemEntity> entities = menuItemDao.getByCategoryId(categoryId);
        List<MenuItem> result = new ArrayList<>();
        for (MenuItemEntity e : entities) {
            List<MenuItemVariantEntity> vars = variantDao.getByMenuItemId(e.id);
            MenuItem.Builder b = MenuItem.builder().id(e.id).name(e.name).description(e.description)
                    .imageResId(e.imageResId).bestSeller(e.bestSeller).spicy(e.spicy);
            for (MenuItemVariantEntity v : vars) {
                b.variant(v.label, v.priceCents);
            }
            result.add(b.build());
        }
        result.sort((a, b) -> {
            boolean aCustom = "combo_custom".equals(a.getId()) || "others_custom".equals(a.getId());
            boolean bCustom = "combo_custom".equals(b.getId()) || "others_custom".equals(b.getId());
            if (aCustom && !bCustom) return 1;
            if (!aCustom && bCustom) return -1;
            String aName = a.getName() != null ? a.getName() : "";
            String bName = b.getName() != null ? b.getName() : "";
            return String.CASE_INSENSITIVE_ORDER.compare(aName, bName);
        });
        return result;
    }

    @androidx.annotation.Nullable
    public MenuCategory getCategoryById(String categoryId) {
        CategoryEntity e = categoryDao.getById(categoryId);
        if (e == null) return null;
        List<MenuItem> items = getItemsForCategory(e.id);
        return MenuCategory.builder().id(e.id).name(e.name).subtitle(e.subtitle)
                .items(items).iconResId(e.iconResId).build();
    }

    public String getCategoryNameForItemId(String itemId) {
        MenuItemEntity item = menuItemDao.getById(itemId);
        if (item == null) return "Other";
        CategoryEntity cat = categoryDao.getById(item.categoryId);
        return cat != null ? cat.name : "Other";
    }

    // --- Category CRUD ---

    public String addCategory(String name, String subtitle) {
        String id = "cat_" + UUID.randomUUID().toString().substring(0, 8);
        categoryDao.insert(new CategoryEntity(id, name, subtitle != null ? subtitle : "", R.drawable.ic_food_placeholder, categoryDao.getCount()));
        return id;
    }

    public void updateCategory(String id, String name, String subtitle) {
        CategoryEntity e = categoryDao.getById(id);
        if (e == null) return;
        e.name = name;
        e.subtitle = subtitle != null ? subtitle : "";
        categoryDao.update(e);
    }

    public void deleteCategory(String id) {
        menuItemDao.getByCategoryId(id).forEach(item -> {
            variantDao.deleteByMenuItemId(item.id);
        });
        menuItemDao.deleteByCategoryId(id);
        categoryDao.deleteById(id);
    }

    // --- Menu item CRUD ---

    public String addMenuItem(String categoryId, String name, String description,
                              List<MenuItemVariant> variants, boolean bestSeller, boolean spicy) {
        String id = "item_" + UUID.randomUUID().toString().substring(0, 8);
        int sortOrder = menuItemDao.getByCategoryId(categoryId).size();
        menuItemDao.insert(new MenuItemEntity(id, categoryId, name, description != null ? description : "",
                MenuItem.NO_IMAGE, bestSeller, spicy, sortOrder, true));
        if (variants != null) {
            for (MenuItemVariant v : variants) {
                variantDao.insert(new MenuItemVariantEntity(id, v.getLabel(), v.getPriceCents()));
            }
        }
        return id;
    }

    public void updateMenuItem(String id, String name, String description,
                               List<MenuItemVariant> variants, boolean bestSeller, boolean spicy) {
        MenuItemEntity e = menuItemDao.getById(id);
        if (e == null) return;
        e.name = name;
        e.description = description != null ? description : "";
        e.bestSeller = bestSeller;
        e.spicy = spicy;
        e.userManaged = true;
        menuItemDao.update(e);
        variantDao.deleteByMenuItemId(id);
        if (variants != null) {
            for (MenuItemVariant v : variants) {
                variantDao.insert(new MenuItemVariantEntity(id, v.getLabel(), v.getPriceCents()));
            }
        }
    }

    public void deleteMenuItem(String id) {
        variantDao.deleteByMenuItemId(id);
        menuItemDao.deleteById(id);
    }

    @androidx.annotation.Nullable
    public MenuItem getMenuItemById(String id) {
        MenuItemEntity e = menuItemDao.getById(id);
        if (e == null) return null;
        List<MenuItemVariantEntity> vars = variantDao.getByMenuItemId(e.id);
        MenuItem.Builder b = MenuItem.builder().id(e.id).name(e.name).description(e.description)
                .imageResId(e.imageResId).bestSeller(e.bestSeller).spicy(e.spicy);
        for (MenuItemVariantEntity v : vars) {
            b.variant(v.label, v.priceCents);
        }
        return b.build();
    }

    /** Items created in Manage Menu or edited by a manager must not be overwritten or deleted by seed sync. */
    private static boolean isUserManagedItem(MenuItemEntity e) {
        return e != null && (e.userManaged || (e.id != null && e.id.startsWith("item_")));
    }
}
