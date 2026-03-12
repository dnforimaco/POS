package com.example.menu_pos.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists and loads completed orders (SharedPreferences + JSON).
 */
public class OrderRepository {

    private static final String PREFS_NAME = "pos_orders";
    private static final String KEY_ORDERS = "orders";

    private final SharedPreferences prefs;

    public OrderRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveOrder(OrderRecord order) {
        List<OrderRecord> list = new ArrayList<>(getOrders());
        list.add(order);
        saveAll(list);
    }

    public List<OrderRecord> getOrders() {
        List<OrderRecord> out = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_ORDERS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                OrderRecord r = parseOrder(arr.getJSONObject(i));
                if (r != null) out.add(r);
            }
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    public List<OrderRecord> getOrdersBetween(long startMillis, long endMillis) {
        List<OrderRecord> out = new ArrayList<>();
        for (OrderRecord r : getOrders()) {
            if (r.getTimestampMillis() >= startMillis && r.getTimestampMillis() <= endMillis)
                out.add(r);
        }
        return out;
    }

    private void saveAll(List<OrderRecord> orders) {
        try {
            JSONArray arr = new JSONArray();
            for (OrderRecord r : orders) {
                arr.put(toJson(r));
            }
            prefs.edit().putString(KEY_ORDERS, arr.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private static JSONObject toJson(OrderRecord r) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", r.getId());
        o.put("timestamp", r.getTimestampMillis());
        o.put("totalCents", r.getTotalCents());
        JSONArray lines = new JSONArray();
        for (OrderLineItem line : r.getLines()) {
            JSONObject l = new JSONObject();
            l.put("itemId", line.getItemId());
            l.put("itemName", line.getItemName());
            l.put("variantLabel", line.getVariantLabel());
            l.put("unitPriceCents", line.getUnitPriceCents());
            l.put("quantity", line.getQuantity());
            lines.put(l);
        }
        o.put("lines", lines);
        return o;
    }

    private static OrderRecord parseOrder(JSONObject o) {
        try {
            long id = o.getLong("id");
            long timestamp = o.getLong("timestamp");
            int totalCents = o.getInt("totalCents");
            List<OrderLineItem> lines = new ArrayList<>();
            JSONArray linesArr = o.getJSONArray("lines");
            for (int i = 0; i < linesArr.length(); i++) {
                JSONObject l = linesArr.getJSONObject(i);
                lines.add(new OrderLineItem(
                        l.optString("itemId", ""),
                        l.optString("itemName", ""),
                        l.optString("variantLabel", ""),
                        l.optInt("unitPriceCents", 0),
                        l.optInt("quantity", 0)
                ));
            }
            return new OrderRecord(id, timestamp, lines, totalCents);
        } catch (Exception e) {
            return null;
        }
    }
}
