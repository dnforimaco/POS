package com.example.menu_pos.printer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintManager;
import android.widget.Toast;

import com.example.menu_pos.R;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.ui.checkout.KitchenSlipPrinter;
import com.example.menu_pos.ui.checkout.PdfBytesPrintAdapter;

import java.util.List;

/** Sends kitchen slip to Bluetooth thermal printer or system PDF print. */
public final class KitchenSlipPrint {

    private KitchenSlipPrint() {}

    public static void print(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        printInternal(context, order, lines, false);
    }

    public static void printAdditionalItems(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        printInternal(context, order, lines, true);
    }

    private static void printInternal(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines, boolean additionalOnly) {
        if (order == null || context == null) return;
        final Context appCtx = context.getApplicationContext();
        try {
            String mac = PrinterPrefs.getPrinterMac(context);
            if (mac != null && !mac.isEmpty()) {
                String text = additionalOnly
                        ? KitchenSlipTextFormatter.buildAdditionalItemsSlip(order, lines)
                        : KitchenSlipTextFormatter.buildKitchenSlip(order, lines);
                PrinterManager pm = new PrinterManager(appCtx);
                pm.connectIfNeededAndPrint(mac, text, success -> new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(appCtx,
                                success ? appCtx.getString(R.string.kitchen_slip_print_sent)
                                        : appCtx.getString(R.string.printer_not_connected_retry),
                                Toast.LENGTH_SHORT).show()));
                return;
            }
            byte[] pdf = additionalOnly
                    ? KitchenSlipPrinter.buildAdditionalItemsSlipPdfBytes(appCtx, order, lines)
                    : KitchenSlipPrinter.buildKitchenSlipPdfBytes(appCtx, order, lines);
            PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
            if (printManager == null) return;
            String jobName = (additionalOnly ? "Kitchen_Additional_" : "Kitchen_") + order.id;
            printManager.print(jobName, new PdfBytesPrintAdapter(jobName + ".pdf", pdf), null);
        } catch (Exception ignored) {
        }
    }
}
