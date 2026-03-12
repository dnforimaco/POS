package com.example.menu_pos.ui.reports;

import java.util.ArrayList;
import java.util.List;

/** Computed report values for the Reports tab. */
public final class ReportData {
    private final int dailySalesCents;
    private final int weeklySalesCents;
    private final int monthlySalesCents;
    private final List<TopItem> topSellingItems;
    private final List<CategorySale> salesByCategory;
    private final int totalOrders;
    private final int averageOrderValueCents;
    private final String peakHour;
    private final String mostPopularItem;
    private final int cancelledOrders;

    public ReportData(int dailySalesCents, int weeklySalesCents, int monthlySalesCents,
                      List<TopItem> topSellingItems, List<CategorySale> salesByCategory,
                      int totalOrders, int averageOrderValueCents, String peakHour,
                      String mostPopularItem, int cancelledOrders) {
        this.dailySalesCents = dailySalesCents;
        this.weeklySalesCents = weeklySalesCents;
        this.monthlySalesCents = monthlySalesCents;
        this.topSellingItems = topSellingItems != null ? new ArrayList<>(topSellingItems) : new ArrayList<>();
        this.salesByCategory = salesByCategory != null ? new ArrayList<>(salesByCategory) : new ArrayList<>();
        this.totalOrders = totalOrders;
        this.averageOrderValueCents = averageOrderValueCents;
        this.peakHour = peakHour != null ? peakHour : "—";
        this.mostPopularItem = mostPopularItem != null ? mostPopularItem : "—";
        this.cancelledOrders = cancelledOrders;
    }

    public int getDailySalesCents() { return dailySalesCents; }
    public int getWeeklySalesCents() { return weeklySalesCents; }
    public int getMonthlySalesCents() { return monthlySalesCents; }
    public List<TopItem> getTopSellingItems() { return topSellingItems; }
    public List<CategorySale> getSalesByCategory() { return salesByCategory; }
    public int getTotalOrders() { return totalOrders; }
    public int getAverageOrderValueCents() { return averageOrderValueCents; }
    public String getPeakHour() { return peakHour; }
    public String getMostPopularItem() { return mostPopularItem; }
    public int getCancelledOrders() { return cancelledOrders; }

    public static final class TopItem {
        public final String itemName;
        public final int quantity;

        public TopItem(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    public static final class CategorySale {
        public final String categoryName;
        public final int totalCents;

        public CategorySale(String categoryName, int totalCents) {
            this.categoryName = categoryName;
            this.totalCents = totalCents;
        }
    }
}
