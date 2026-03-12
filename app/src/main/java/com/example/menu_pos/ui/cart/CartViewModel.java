package com.example.menu_pos.ui.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.menu_pos.data.CartItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared cart state (use with activityViewModels() so cart survives navigation).
 */
public class CartViewModel extends ViewModel {

    private final MutableLiveData<List<CartItem>> items = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<CartItem>> getItems() {
        return items;
    }

    public void addItem(CartItem newItem) {
        List<CartItem> list = new ArrayList<>(items.getValue() != null ? items.getValue() : new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            CartItem existing = list.get(i);
            if (existing.getItemId().equals(newItem.getItemId()) && existing.getVariantLabel().equals(newItem.getVariantLabel())) {
                list.set(i, new CartItem(existing.getItemId(), existing.getItemName(), existing.getVariantLabel(), existing.getUnitPriceCents(), existing.getQuantity() + newItem.getQuantity()));
                items.setValue(list);
                return;
            }
        }
        list.add(newItem);
        items.setValue(list);
    }

    public void setQuantity(CartItem item, int quantity) {
        if (quantity <= 0) {
            removeItem(item);
            return;
        }
        List<CartItem> list = new ArrayList<>(items.getValue() != null ? items.getValue() : new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            CartItem existing = list.get(i);
            if (existing.getItemId().equals(item.getItemId()) && existing.getVariantLabel().equals(item.getVariantLabel())) {
                list.set(i, new CartItem(existing.getItemId(), existing.getItemName(), existing.getVariantLabel(), existing.getUnitPriceCents(), quantity));
                items.setValue(list);
                return;
            }
        }
    }

    public void removeItem(CartItem item) {
        List<CartItem> list = new ArrayList<>(items.getValue() != null ? items.getValue() : new ArrayList<>());
        list.removeIf(c -> c.getItemId().equals(item.getItemId()) && c.getVariantLabel().equals(item.getVariantLabel()));
        items.setValue(list);
    }

    public void clear() {
        items.setValue(new ArrayList<>());
    }

    public int getTotalPesos() {
        List<CartItem> list = items.getValue();
        if (list == null) return 0;
        int total = 0;
        for (CartItem c : list) total += c.getTotalCents();
        return total;
    }

    public int getItemCount() {
        List<CartItem> list = items.getValue();
        if (list == null) return 0;
        int n = 0;
        for (CartItem c : list) n += c.getQuantity();
        return n;
    }
}
