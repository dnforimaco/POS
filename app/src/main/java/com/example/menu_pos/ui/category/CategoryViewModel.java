package com.example.menu_pos.ui.category;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuRepository;

public class CategoryViewModel extends AndroidViewModel {

    private final MenuRepository menuRepo;
    private final MutableLiveData<MenuCategory> category = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        menuRepo = new MenuRepository(application);
    }

    public void loadCategory(String categoryId) {
        category.setValue(menuRepo.getCategoryById(categoryId));
    }

    public LiveData<MenuCategory> getCategory() {
        return category;
    }
}
