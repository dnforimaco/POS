package com.example.menu_pos.ui.menu_management;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuRepository;

import java.util.List;

public class ManageCategoriesViewModel extends AndroidViewModel {

    private final MenuRepository menuRepo;
    private final MutableLiveData<List<MenuCategory>> categories = new MutableLiveData<>();

    public ManageCategoriesViewModel(@NonNull Application application) {
        super(application);
        menuRepo = new MenuRepository(application);
        load();
    }

    public LiveData<List<MenuCategory>> getCategories() {
        return categories;
    }

    public void load() {
        categories.setValue(menuRepo.getCategories());
    }

    public String addCategory(String name, String subtitle) {
        String id = menuRepo.addCategory(name, subtitle);
        load();
        return id;
    }

    public void updateCategory(String id, String name, String subtitle) {
        menuRepo.updateCategory(id, name, subtitle);
        load();
    }

    public void deleteCategory(String id) {
        menuRepo.deleteCategory(id);
        load();
    }
}
