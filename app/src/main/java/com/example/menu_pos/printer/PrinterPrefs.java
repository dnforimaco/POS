package com.example.menu_pos.printer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class PrinterPrefs {
    private static final String PREFS = "printer_prefs";

    public static final String KEY_PRINTER_MAC = "printer_mac";
    public static final String KEY_PRINTER_NAME = "printer_name";
    public static final String KEY_AUTO_CONNECT = "auto_connect";
    public static final String KEY_AUTO_PRINT = "auto_print";

    private PrinterPrefs() {}

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isAutoConnectEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_CONNECT, true);
    }

    public static boolean isAutoPrintEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_PRINT, true);
    }

    @Nullable
    public static String getPrinterMac(Context context) {
        String mac = prefs(context).getString(KEY_PRINTER_MAC, "");
        return mac != null && !mac.trim().isEmpty() ? mac.trim() : null;
    }

    public static String getPrinterName(Context context) {
        String name = prefs(context).getString(KEY_PRINTER_NAME, "");
        return name != null && !name.trim().isEmpty() ? name.trim() : "";
    }

    public static void setSelectedPrinter(Context context, String name, String mac) {
        prefs(context).edit()
                .putString(KEY_PRINTER_NAME, name != null ? name : "")
                .putString(KEY_PRINTER_MAC, mac != null ? mac : "")
                .apply();
    }

    public static void setAutoConnect(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply();
    }

    public static void setAutoPrint(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_PRINT, enabled).apply();
    }
}

