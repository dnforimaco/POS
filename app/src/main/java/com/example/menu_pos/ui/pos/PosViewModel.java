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

public class PosViewModel extends AndroidViewModel {

    private final MenuRepository menuRepo;
    private List<MenuCategory> categories = Collections.emptyList();
    private final MutableLiveData<Integer> selectedIndex = new MutableLiveData<>(0);
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<List<MenuItem>> filtered = new MutableLiveData<>(Collections.emptyList());

    public PosViewModel(@NonNull Application application) {
        super(application);
        menuRepo = new MenuRepository(application);
        refreshCategories();
    }

    public void refreshCategories() {
        categories = menuRepo.getCategories();
        recompute();
    }

    public List<MenuCategory> getCategories() {
        return categories != null ? categories : Collections.emptyList();
    }

    public LiveData<Integer> getSelectedIndex() {
        return selectedIndex;
    }

    public LiveData<List<MenuItem>> getFilteredItems() {
        return filtered;
    }

    public void setSelectedIndex(int idx) {
        if (categories.isEmpty() || idx < 0 || idx >= categories.size()) return;
        selectedIndex.setValue(idx);
        recompute();
    }

    public void setQuery(String q) {
        query.setValue(q != null ? q : "");
        recompute();
    }

    private void recompute() {
        int idx = selectedIndex.getValue() != null ? selectedIndex.getValue() : 0;
        String q = query.getValue() != null ? query.getValue() : "";
        q = q.trim().toLowerCase();

        if (categories.isEmpty()) {
            filtered.setValue(Collections.emptyList());
            return;
        }
        List<MenuItem> items = categories.get(Math.min(idx, categories.size() - 1)).getItems();
        if (q.isEmpty()) {
            filtered.setValue(new ArrayList<>(items));
            return;
        }
        List<MenuItem> out = new ArrayList<>();
        for (MenuItem i : items) {
            if (i.getName() != null && i.getName().toLowerCase().contains(q)) out.add(i);
        }
        filtered.setValue(out);
    }
}
