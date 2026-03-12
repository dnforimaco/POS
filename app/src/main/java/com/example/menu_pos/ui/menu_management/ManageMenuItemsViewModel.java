package com.example.menu_pos.ui.menu_management;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.example.menu_pos.data.MenuRepository;

import java.util.ArrayList;
import java.util.List;

public class ManageMenuItemsViewModel extends AndroidViewModel {

    private final MenuRepository menuRepo;
    private final MutableLiveData<List<MenuCategory>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<MenuItem>> items = new MutableLiveData<>(new ArrayList<>());
    private String selectedCategoryId;

    public ManageMenuItemsViewModel(@NonNull Application application) {
        super(application);
        menuRepo = new MenuRepository(application);
        categories.setValue(menuRepo.getCategories());
        List<MenuCategory> cats = menuRepo.getCategories();
        if (!cats.isEmpty()) {
            selectedCategoryId = cats.get(0).getId();
            loadItems();
        }
    }

    public LiveData<List<MenuCategory>> getCategories() {
        return categories;
    }

    public LiveData<List<MenuItem>> getItems() {
        return items;
    }

    public void setSelectedCategoryId(String categoryId) {
        this.selectedCategoryId = categoryId;
        loadItems();
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void loadItems() {
        if (selectedCategoryId == null) {
            items.setValue(new ArrayList<>());
            return;
        }
        MenuCategory cat = menuRepo.getCategoryById(selectedCategoryId);
        items.setValue(cat != null ? new ArrayList<>(cat.getItems()) : new ArrayList<>());
    }

    public void loadCategories() {
        categories.setValue(menuRepo.getCategories());
    }

    public String addItem(String categoryId, String name, String description,
                         List<MenuItemVariant> variants, boolean bestSeller, boolean spicy) {
        String id = menuRepo.addMenuItem(categoryId, name, description, variants, bestSeller, spicy);
        loadItems();
        return id;
    }

    public void updateItem(String id, String name, String description,
                           List<MenuItemVariant> variants, boolean bestSeller, boolean spicy) {
        menuRepo.updateMenuItem(id, name, description, variants, bestSeller, spicy);
        loadItems();
    }

    public void deleteItem(String id) {
        menuRepo.deleteMenuItem(id);
        loadItems();
    }

    public MenuItem getItemById(String id) {
        return menuRepo.getMenuItemById(id);
    }
}
