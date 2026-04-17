package com.example.menu_pos.printer;

import android.content.Context;

import androidx.annotation.Nullable;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrinterManager {
    private final Context appContext;
    private final BluetoothPrinterManager bt;
    private final ExecutorService printExecutor = Executors.newSingleThreadExecutor();

    public interface PrintCallback {
        void onDone(boolean success);
    }

    public PrinterManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.bt = BluetoothPrinterManager.getInstance(appContext);
    }

    public void printReceipt(String receiptText, @Nullable PrintCallback cb) {
        printExecutor.execute(() -> {
            boolean ok = printReceiptBlocking(receiptText);
            if (cb != null) cb.onDone(ok);
        });
    }

    public boolean printReceiptBlocking(String receiptText) {
        try {
            OutputStream os = bt.getOutputStream();
            if (os == null || !bt.isConnected()) return false;

            // ESC/POS init
            os.write(new byte[]{0x1B, 0x40});
            // Vozy P50: stick to a safe codepage to avoid garbled symbols.
            // ESC t n = select character code table. 0 is commonly CP437.
            os.write(new byte[]{0x1B, 0x74, 0x00});

            String safeText = sanitizeForThermal(receiptText != null ? receiptText : "");
            // Many ESC/POS printers require carriage return in addition to line feed.
            // Without CR, the next line can start at the previous horizontal cursor position.
            safeText = safeText.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
            byte[] bytes = safeText.getBytes(Charset.forName("CP437"));
            os.write(bytes);
            // Portable printers like Vozy P50 usually have no cutter; just feed lines.
            os.write(new byte[]{0x0A, 0x0A, 0x0A});
            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Many portable ESC/POS printers don't support UTF-8 well (₱, Ñ, etc.).
     * Keep the output ASCII-ish to print consistently on Vozy P50.
     */
    private static String sanitizeForThermal(String s) {
        if (s == null || s.isEmpty()) return "";
        return s
                .replace("₱", "PHP ")
                .replace("Ñ", "N")
                .replace("ñ", "n")
                .replace("–", "-")
                .replace("—", "-")
                .replace("•", "*");
    }

    public void connectIfNeededAndPrint(String mac, String receiptText, @Nullable PrintCallback cb) {
        if (mac == null || mac.trim().isEmpty()) {
            if (cb != null) cb.onDone(false);
            return;
        }
        printExecutor.execute(() -> {
            if (!bt.isConnected()) {
                boolean connected = bt.connectBlockingWithRetry(mac);
                if (!connected) {
                    if (cb != null) cb.onDone(false);
                    return;
                }
            }
            boolean ok = printReceiptBlocking(receiptText);
            if (cb != null) cb.onDone(ok);
        });
    }
}

