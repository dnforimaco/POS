package com.example.menu_pos.data;

import android.content.Context;

import com.example.menu_pos.R;

import java.util.ArrayList;
import java.util.List;
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
        seedIfEmpty();
        applyImageMappings();
        applySeedUpdates();
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
        applyImageIfPresent("bilao_miki", R.drawable.bilao_miki);
        applyImageIfPresent("bilao_pancit", R.drawable.bilao_pancit);
        applyImageIfPresent("bilao_lumpia", R.drawable.bilao_shanghai);
        applyImageIfPresent("bilao_canton", R.drawable.bilao_canton);
        applyImageIfPresent("bilao_chami", R.drawable.bilao_chami);
    }

    private void applySeedUpdates() {
        // Seed only runs on first install; this keeps existing installs in sync for key data fixes.
        applyChillZoneVariantsIfPresent();
    }

    private void applyChillZoneVariantsIfPresent() {
        try {
            MenuCategory chill = MenuData.getCategoryById("chill");
            if (chill == null) return;
            for (MenuItem seedItem : chill.getItems()) {
                // Only update rows that already exist in DB (seeded earlier).
                MenuItemEntity existing = menuItemDao.getById(seedItem.getId());
                if (existing == null) continue;

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

    private void applyImageIfPresent(String itemId, int resId) {
        try {
            MenuItemEntity e = menuItemDao.getById(itemId);
            if (e == null) return;
            if (e.imageResId != resId) menuItemDao.updateImageResId(itemId, resId);
        } catch (Exception ignored) {
        }
    }

    private int imageResForItemId(String itemId, int fallbackResId) {
        if ("bilao_miki".equals(itemId)) return R.drawable.bilao_miki;
        if ("bilao_pancit".equals(itemId)) return R.drawable.bilao_pancit;
        if ("bilao_lumpia".equals(itemId)) return R.drawable.bilao_shanghai;
        if ("bilao_canton".equals(itemId)) return R.drawable.bilao_canton;
        if ("bilao_chami".equals(itemId)) return R.drawable.bilao_chami;
        return fallbackResId;
    }

    public List<MenuCategory> getCategories() {
        List<CategoryEntity> cats = categoryDao.getAll();
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
                MenuItem.NO_IMAGE, bestSeller, spicy, sortOrder));
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
}
