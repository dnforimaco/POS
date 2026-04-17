package com.example.menu_pos.ui.pos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PosViewModel extends AndroidViewModel {

    private final MenuRepository menuRepo;
    private List<MenuCategory> categories = Collections.emptyList();
    private List<List<PosGridRow>> categoryRows = Collections.emptyList();
    private final MutableLiveData<Integer> selectedIndex = new MutableLiveData<>(0);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<List<PosGridRow>> filtered = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> globalSearchActive = new MutableLiveData<>(false);

    public PosViewModel(@NonNull Application application) {
        super(application);
        menuRepo = new MenuRepository(application);
        refreshCategories();
    }

    public void refreshCategories() {
        categories = menuRepo.getCategories();
        rebuildCategoryRows();
        recompute();
    }

    public List<MenuCategory> getCategories() {
        return categories != null ? categories : Collections.emptyList();
    }

    public LiveData<Integer> getSelectedIndex() {
        return selectedIndex;
    }

    public LiveData<List<PosGridRow>> getFilteredRows() {
        return filtered;
    }

    /** True when the search box has text and results include all categories. */
    public LiveData<Boolean> getGlobalSearchActive() {
        return globalSearchActive;
    }

    public void setSelectedIndex(int idx) {
        if (categories.isEmpty() || idx < 0 || idx >= categories.size()) return;
        Integer current = selectedIndex.getValue();
        if (current != null && current == idx) return;
        selectedIndex.setValue(idx);
        recompute();
    }

    public void setQuery(String q) {
        String normalized = q != null ? q : "";
        String current = query.getValue();
        if (normalized.equals(current != null ? current : "")) return;
        query.setValue(normalized);
        recompute();
    }

    private void rebuildCategoryRows() {
        if (categories == null || categories.isEmpty()) {
            categoryRows = Collections.emptyList();
            return;
        }
        List<List<PosGridRow>> rowsByCategory = new ArrayList<>(categories.size());
        for (MenuCategory category : categories) {
            List<MenuItem> items = category.getItems();
            List<PosGridRow> rows = new ArrayList<>(items.size());
            for (MenuItem item : items) rows.add(new PosGridRow(item, null));
            rowsByCategory.add(rows);
        }
        categoryRows = rowsByCategory;
    }

    private void recompute() {
        int idx = selectedIndex.getValue() != null ? selectedIndex.getValue() : 0;
        String q = query.getValue() != null ? query.getValue() : "";
        q = q.trim().toLowerCase(Locale.ROOT);
        globalSearchActive.setValue(!q.isEmpty());

        if (categories.isEmpty()) {
            filtered.setValue(Collections.emptyList());
            return;
        }

        if (q.isEmpty()) {
            int safeIdx = Math.min(idx, categoryRows.size() - 1);
            filtered.setValue(safeIdx >= 0 ? categoryRows.get(safeIdx) : Collections.emptyList());
            return;
        }

        List<PosGridRow> out = new ArrayList<>();
        for (MenuCategory cat : categories) {
            String catName = cat.getName();
            for (MenuItem i : cat.getItems()) {
                if (matchesQuery(i, q)) {
                    out.add(new PosGridRow(i, catName));
                }
            }
        }
        filtered.setValue(out);
    }

    private static boolean matchesQuery(MenuItem i, String q) {
        return i.getName() != null && i.getName().toLowerCase(Locale.ROOT).contains(q);
    }
}
