package com.example.menu_pos.ui.orders;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderRepository;

import java.util.List;

public class OrdersViewModel extends AndroidViewModel {

    private final PaidOrderRepository repo;
    private final MutableLiveData<List<PaidOrderEntity>> orders = new MutableLiveData<>();

    public OrdersViewModel(@NonNull Application application) {
        super(application);
        repo = new PaidOrderRepository(application);
        refresh();
    }

    public LiveData<List<PaidOrderEntity>> getOrders() {
        return orders;
    }

    public void refresh() {
        orders.setValue(repo.getAllOrdersDesc());
    }
}

