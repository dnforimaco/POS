package com.example.menu_pos.ui.orders;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrdersViewModel extends AndroidViewModel {

    private final PaidOrderRepository repo;
    private final MutableLiveData<List<PaidOrderEntity>> orders = new MutableLiveData<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public OrdersViewModel(@NonNull Application application) {
        super(application);
        repo = new PaidOrderRepository(application);
        refresh();
    }

    public LiveData<List<PaidOrderEntity>> getOrders() {
        return orders;
    }

    public void refresh() {
        ioExecutor.execute(() -> orders.postValue(repo.getAllOrdersDesc()));
    }

    @Override
    protected void onCleared() {
        ioExecutor.shutdown();
        super.onCleared();
    }
}

