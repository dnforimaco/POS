package com.example.menu_pos.ui.inventory;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.InventoryCategoryEntity;
import com.example.menu_pos.data.InventoryItemEntity;
import com.example.menu_pos.data.InventoryMovementEntity;
import com.example.menu_pos.data.InventoryRepository;
import com.example.menu_pos.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryViewModel extends AndroidViewModel {

    private final InventoryRepository invRepo;
    private final UserRepository userRepo;

    private final MutableLiveData<List<InventoryFragment.CategoryRow>> categoryRows = new MutableLiveData<>();
    private final MutableLiveData<List<InventoryItemEntity>> items = new MutableLiveData<>();
    private final MutableLiveData<List<InventoryMovementEntity>> history = new MutableLiveData<>();

    private final Map<String, String> categoryNameCache = new HashMap<>();
    private final Map<String, String> inventoryItemNameCache = new HashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private String selectedCategoryId = ""; // empty => All
    public InventoryViewModel(@NonNull Application application) {
        super(application);
        invRepo = new InventoryRepository(application);
        userRepo = new UserRepository(application);

        refreshCategories();
        refreshItems();
        refreshHistory();
    }

    public LiveData<List<InventoryFragment.CategoryRow>> getCategoryRows() { return categoryRows; }
    public LiveData<List<InventoryItemEntity>> getItems() { return items; }
    public LiveData<List<InventoryMovementEntity>> getHistory() { return history; }

    public void setSelectedCategoryId(String idOrNull) {
        selectedCategoryId = idOrNull != null ? idOrNull : "";
        refreshItems();
    }

    public List<InventoryCategoryEntity> getInventoryCategoriesNow() {
        return invRepo.getCategories();
    }

    public List<InventoryItemEntity> getAllInventoryItemsNow() {
        return invRepo.getItems(null);
    }

    public String getCategoryName(String categoryId) {
        if (categoryId == null || "inv_general".equals(categoryId)) return "";
        String cached;
        synchronized (categoryNameCache) {
            cached = categoryNameCache.get(categoryId);
        }
        if (cached != null) return cached;
        // If cache is stale, rebuild it quickly.
        refreshCategoryNameCache();
        synchronized (categoryNameCache) {
            cached = categoryNameCache.get(categoryId);
        }
        if (cached == null && "inv_general".equals(categoryId)) return "";
        return cached != null ? cached : "";
    }

    public String getInventoryItemName(String itemId) {
        if (itemId == null) return "";
        String cached;
        synchronized (inventoryItemNameCache) {
            cached = inventoryItemNameCache.get(itemId);
        }
        if (cached != null) return cached;
        refreshInventoryItemNameCache();
        synchronized (inventoryItemNameCache) {
            cached = inventoryItemNameCache.get(itemId);
        }
        return cached != null ? cached : "";
    }

    public void addItem(String categoryId, String name, int stockQty, int minStockQty) {
        ioExecutor.execute(() -> {
            invRepo.addItem(categoryId, name, stockQty, minStockQty);
            refreshItemsInternal();
        });
    }

    public void addCategory(String name) {
        ioExecutor.execute(() -> {
            invRepo.addCategory(name);
            refreshCategoriesInternal();
        });
    }

    public void updateItem(String id, String categoryId, String name, int stockQty, int minStockQty) {
        ioExecutor.execute(() -> {
            invRepo.updateItem(id, categoryId, name, stockQty, minStockQty);
            refreshItemsInternal();
        });
    }

    public void deleteItem(String id) {
        ioExecutor.execute(() -> {
            invRepo.deleteItem(id);
            refreshItemsInternal();
        });
    }

    public void restock(String itemId, int qty) {
        String actor = userRepo.getLoggedInUser();
        ioExecutor.execute(() -> {
            invRepo.restock(itemId, qty, actor, null);
            refreshItemsInternal();
            history.postValue(invRepo.getRecentHistory(200));
        });
    }

    public void refreshHistory() {
        ioExecutor.execute(() -> history.postValue(invRepo.getRecentHistory(200)));
    }

    public boolean isManager() {
        String user = userRepo.getLoggedInUser();
        return "manager".equalsIgnoreCase(user);
    }

    public void clearHistory() {
        ioExecutor.execute(() -> {
            invRepo.clearHistory();
            history.postValue(invRepo.getRecentHistory(200));
        });
    }

    private void refreshCategories() {
        ioExecutor.execute(this::refreshCategoriesInternal);
    }

    private void refreshCategoriesInternal() {
        List<InventoryCategoryEntity> cats = invRepo.getCategories();
        List<InventoryFragment.CategoryRow> rows = new ArrayList<>();
        rows.add(new InventoryFragment.CategoryRow("", "All categories"));
        if (cats != null) {
            for (InventoryCategoryEntity c : cats) {
                // Do not expose the internal fallback category.
                if ("inv_general".equals(c.id)) continue;
                rows.add(new InventoryFragment.CategoryRow(c.id, c.name));
            }
        }
        categoryRows.postValue(rows);
        refreshCategoryNameCache(cats);
    }

    private void refreshItems() {
        ioExecutor.execute(this::refreshItemsInternal);
    }

    private void refreshItemsInternal() {
        String cat = selectedCategoryId != null ? selectedCategoryId : "";
        List<InventoryItemEntity> list = invRepo.getItems(cat.isEmpty() ? null : cat);
        items.postValue(list);
        refreshInventoryItemNameCache(list);
    }

    private void refreshCategoryNameCache(List<InventoryCategoryEntity> cats) {
        synchronized (categoryNameCache) {
            categoryNameCache.clear();
            if (cats != null) {
                for (InventoryCategoryEntity c : cats) categoryNameCache.put(c.id, c.name);
            }
        }
    }

    private void refreshInventoryItemNameCache(List<InventoryItemEntity> list) {
        synchronized (inventoryItemNameCache) {
            inventoryItemNameCache.clear();
            if (list != null) {
                for (InventoryItemEntity it : list) inventoryItemNameCache.put(it.id, it.name);
            }
        }
    }

    @Override
    protected void onCleared() {
        ioExecutor.shutdown();
        super.onCleared();
    }

    private void refreshCategoryNameCache() {
        List<InventoryCategoryEntity> cats = invRepo.getCategories();
        if (cats != null) {
            synchronized (categoryNameCache) {
                categoryNameCache.clear();
                for (InventoryCategoryEntity c : cats) categoryNameCache.put(c.id, c.name);
            }
        }
    }

    private void refreshInventoryItemNameCache() {
        List<InventoryItemEntity> list = invRepo.getItems(null);
        synchronized (inventoryItemNameCache) {
            inventoryItemNameCache.clear();
            if (list != null) {
                for (InventoryItemEntity it : list) inventoryItemNameCache.put(it.id, it.name);
            }
        }
    }
}

