package com.example.menu_pos.ui.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared cart state (use with activityViewModels() so cart survives navigation).
 */
public class CartViewModel extends ViewModel {

    private final MutableLiveData<List<CartItem>> items = new MutableLiveData<>(new ArrayList<>());
    private final List<CartItem> cartState = new ArrayList<>();
    private final Object lock = new Object();
    /** Non-null when cart is linked to a saved unpaid order row. */
    private final MutableLiveData<Long> activePendingOrderId = new MutableLiveData<>(null);
    private final MutableLiveData<String> orderNotes = new MutableLiveData<>("");
    private final MutableLiveData<String> pendingOrderType = new MutableLiveData<>("");
    private final MutableLiveData<String> pendingTableNumber = new MutableLiveData<>("");

    public LiveData<List<CartItem>> getItems() {
        return items;
    }

    public LiveData<Long> getActivePendingOrderId() {
        return activePendingOrderId;
    }

    public LiveData<String> getOrderNotes() {
        return orderNotes;
    }

    public LiveData<String> getPendingOrderType() {
        return pendingOrderType;
    }

    public LiveData<String> getPendingTableNumber() {
        return pendingTableNumber;
    }

    public Long getActivePendingOrderIdValue() {
        return activePendingOrderId.getValue();
    }

    public String getOrderNotesValue() {
        String n = orderNotes.getValue();
        return n != null ? n : "";
    }

    public String getPendingOrderTypeValue() {
        String t = pendingOrderType.getValue();
        return t != null ? t : "";
    }

    public String getPendingTableNumberValue() {
        String t = pendingTableNumber.getValue();
        return t != null ? t : "";
    }

    public void setOrderNotes(String notes) {
        orderNotes.setValue(notes != null ? notes : "");
    }

    public void setPendingOrderType(String orderType) {
        pendingOrderType.setValue(orderType != null ? orderType : "");
    }

    public void setPendingTableNumber(String tableNumber) {
        pendingTableNumber.setValue(tableNumber != null ? tableNumber : "");
    }

    public void setActivePendingOrderId(Long id) {
        activePendingOrderId.setValue(id);
    }

    public void loadPendingOrder(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        List<CartItem> list = new ArrayList<>();
        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                String id = (l.sourceItemId != null && !l.sourceItemId.isEmpty())
                        ? l.sourceItemId
                        : ("open:" + order.id + ":" + l.id);
                boolean disc = l.hasLineDiscount();
                list.add(new CartItem(id, l.itemName, l.variantLabel, l.unitPriceCents, l.quantity, disc));
            }
        }
        synchronized (lock) {
            cartState.clear();
            cartState.addAll(list);
        }
        publishSnapshot();
        activePendingOrderId.setValue(order.id);
        orderNotes.setValue(order.orderNotes != null ? order.orderNotes : "");
        pendingOrderType.setValue(order.orderType != null ? order.orderType : "");
        pendingTableNumber.setValue(order.tableNumber != null ? order.tableNumber : "");
    }

    public void addItem(CartItem newItem) {
        if (newItem == null) return;
        synchronized (lock) {
            for (int i = 0; i < cartState.size(); i++) {
                CartItem existing = cartState.get(i);
                if (isSameLine(existing, newItem)) {
                    cartState.set(i, new CartItem(existing.getItemId(), existing.getItemName(), existing.getVariantLabel(),
                            existing.getUnitPriceCents(), existing.getQuantity() + newItem.getQuantity(),
                            existing.isApplyTenPercentDiscount()));
                    publishSnapshot();
                    return;
                }
            }
            cartState.add(newItem);
        }
        publishSnapshot();
    }

    public void setQuantity(CartItem item, int quantity) {
        if (quantity <= 0) {
            removeItem(item);
            return;
        }
        synchronized (lock) {
            for (int i = 0; i < cartState.size(); i++) {
                CartItem existing = cartState.get(i);
                if (isSameLine(existing, item)) {
                    cartState.set(i, new CartItem(existing.getItemId(), existing.getItemName(), existing.getVariantLabel(),
                            existing.getUnitPriceCents(), quantity, existing.isApplyTenPercentDiscount()));
                    publishSnapshot();
                    return;
                }
            }
        }
    }

    public void setLineDiscountAt(int index, boolean applyTenPercentDiscount) {
        synchronized (lock) {
            if (index < 0 || index >= cartState.size()) return;
            CartItem existing = cartState.get(index);
            cartState.set(index, new CartItem(existing.getItemId(), existing.getItemName(), existing.getVariantLabel(),
                    existing.getUnitPriceCents(), existing.getQuantity(), applyTenPercentDiscount));
        }
        publishSnapshot();
    }

    public void removeItem(CartItem item) {
        synchronized (lock) {
            cartState.removeIf(c -> isSameLine(c, item));
        }
        publishSnapshot();
    }

    public void clear() {
        synchronized (lock) {
            cartState.clear();
        }
        publishSnapshot();
        activePendingOrderId.setValue(null);
        orderNotes.setValue("");
        pendingOrderType.setValue("");
        pendingTableNumber.setValue("");
    }

    public int getTotalPesos() {
        int total = 0;
        synchronized (lock) {
            for (CartItem c : cartState) total += c.getTotalCents();
        }
        return total;
    }

    public int getItemCount() {
        int n = 0;
        synchronized (lock) {
            for (CartItem c : cartState) n += c.getQuantity();
        }
        return n;
    }

    private void publishSnapshot() {
        List<CartItem> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(cartState);
        }
        items.setValue(snapshot);
    }

    private static boolean isSameLine(CartItem a, CartItem b) {
        if (a == null || b == null) return false;
        String aId = a.getItemId() != null ? a.getItemId() : "";
        String bId = b.getItemId() != null ? b.getItemId() : "";
        String aVariant = a.getVariantLabel() != null ? a.getVariantLabel() : "";
        String bVariant = b.getVariantLabel() != null ? b.getVariantLabel() : "";
        return aId.equals(bId)
                && aVariant.equals(bVariant)
                && a.isApplyTenPercentDiscount() == b.isApplyTenPercentDiscount();
    }
}
