package com.example.menu_pos.ui.reports;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuRepository;
import com.example.menu_pos.data.OrderStatus;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsViewModel extends AndroidViewModel {

    private final PaidOrderRepository paidOrderRepo;
    private final MutableLiveData<ReportData> reportData = new MutableLiveData<>();
    private final MenuRepository menuRepo;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        paidOrderRepo = new PaidOrderRepository(application);
        menuRepo = new MenuRepository(application);
        refresh();
    }

    public LiveData<ReportData> getReportData() {
        return reportData;
    }

    public void refresh() {
        ioExecutor.execute(() -> reportData.postValue(computeReport()));
    }

    private ReportData computeReport() {
        List<PaidOrderEntity> orders = paidOrderRepo.getAllOrdersDesc();
        Calendar cal = Calendar.getInstance(Locale.getDefault());

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();

        // "Last 7 days" includes today + previous 6 days.
        long weekStart = todayStart - 6L * 24 * 60 * 60 * 1000;
        // "Last 30 days" includes today + previous 29 days.
        long monthStart = todayStart - 29L * 24 * 60 * 60 * 1000;

        int dailyCents = 0, dailyCashCents = 0, dailyGcashCents = 0, weeklyCents = 0, monthlyCents = 0;
        Map<String, Integer> itemQuantities = new HashMap<>();
        Map<String, Integer> categoryCents = new HashMap<>();
        Map<Integer, Integer> hourCounts = new HashMap<>();
        String mostPopularName = null;
        int mostPopularQty = 0;

        // Best-effort category mapping: paid order lines store itemName (not itemId),
        // so map current menu item names -> category.
        Map<String, String> itemNameToCategory = new HashMap<>();
        try {
            List<MenuCategory> cats = menuRepo.getCategories();
            for (MenuCategory cat : cats) {
                if (cat == null) continue;
                String catName = cat.getName() != null && !cat.getName().isEmpty() ? cat.getName() : "Other";
                List<MenuItem> items = cat.getItems();
                if (items == null) continue;
                for (MenuItem item : items) {
                    if (item == null) continue;
                    String name = item.getName();
                    if (name == null || name.isEmpty()) continue;
                    itemNameToCategory.put(name.trim().toLowerCase(Locale.getDefault()), catName);
                }
            }
        } catch (Exception ignored) {
        }

        for (PaidOrderEntity o : orders) {
            if (o.orderStatus != OrderStatus.PAID) continue;
            long t = o.timestampMillis;
            int total = o.totalCents;
            if (t >= todayStart) {
                dailyCents += total;
                String pt = o.paymentType != null ? o.paymentType.trim().toLowerCase(Locale.getDefault()) : "";
                if ("gcash".equals(pt)) dailyGcashCents += total;
                else dailyCashCents += total; // default to cash for legacy/unknown
            }
            if (t >= weekStart) weeklyCents += total;
            if (t >= monthStart) monthlyCents += total;

            Calendar orderCal = Calendar.getInstance(Locale.getDefault());
            orderCal.setTimeInMillis(t);
            int hour = orderCal.get(Calendar.HOUR_OF_DAY);
            hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);

            // Only include items from the last 30 days in the Top Selling and Category Sales lists
            if (t >= monthStart) {
                List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(o.id);
                for (PaidOrderLineEntity line : lines) {
                    String baseName = line.itemName != null ? line.itemName : "";
                    String name = baseName;
                    if (line.variantLabel != null && !line.variantLabel.isEmpty()) {
                        name = name + " (" + line.variantLabel + ")";
                    }

                    int qty = Math.max(0, line.quantity);
                    itemQuantities.put(name, itemQuantities.getOrDefault(name, 0) + qty);
                    if (itemQuantities.get(name) > mostPopularQty) {
                        mostPopularQty = itemQuantities.get(name);
                        mostPopularName = name;
                    }

                    String key = baseName.trim().toLowerCase(Locale.getDefault());
                    String cat = itemNameToCategory.getOrDefault(key, "Other");
                    categoryCents.put(cat, categoryCents.getOrDefault(cat, 0) + line.getLineTotalCents());
                }
            }
        }

        List<ReportData.TopItem> topItems = new ArrayList<>();
        itemQuantities.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> topItems.add(new ReportData.TopItem(e.getKey(), e.getValue())));

        List<ReportData.CategorySale> byCategory = new ArrayList<>();
        categoryCents.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> byCategory.add(new ReportData.CategorySale(e.getKey(), e.getValue())));

        int peakHour = 0;
        int peakCount = 0;
        for (Map.Entry<Integer, Integer> e : hourCounts.entrySet()) {
            if (e.getValue() > peakCount) {
                peakCount = e.getValue();
                peakHour = e.getKey();
            }
        }
        String peakHourStr = peakCount > 0 ? String.format(Locale.getDefault(), "%d:00 - %d:59", peakHour, peakHour) : "—";

        int ordersInMonth = 0;
        int paidOrdersCount = 0;
        for (PaidOrderEntity o : orders) {
            if (o.orderStatus != OrderStatus.PAID) continue;
            paidOrdersCount++;
            if (o.timestampMillis >= monthStart) ordersInMonth++;
        }
        int totalOrders = paidOrdersCount;
        int avgCents = ordersInMonth > 0 ? monthlyCents / ordersInMonth : 0;

        return new ReportData(
                dailyCents, dailyCashCents, dailyGcashCents, weeklyCents, monthlyCents,
                topItems, byCategory,
                totalOrders, avgCents,
                peakHourStr, mostPopularName != null ? mostPopularName : "—", 0
        );
    }

    @Override
    protected void onCleared() {
        ioExecutor.shutdown();
        super.onCleared();
    }
}
