package com.example.menu_pos.ui.checkout;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Generates a simple receipt PDF for Android printing. */
public final class ReceiptPrinter {

    private ReceiptPrinter() {}

    public static byte[] buildReceiptPdfBytes(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) throws Exception {
        PdfDocument doc = new PdfDocument();
        int pageWidth = 300;   // narrow for receipt printers (points-ish)
        int pageHeight = 800;  // will truncate if too long; keep simple
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint title = new Paint();
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(14);

        Paint normal = new Paint();
        normal.setTextSize(10);

        float x = 12;
        float y = 24;

        page.getCanvas().drawText("NAOMIE'S", x, y, title);
        y += 16;
        page.getCanvas().drawText("Famous Lomi, Bulalo, Gotong", x, y, normal);
        y += 14;
        page.getCanvas().drawText("59 General Ordoñez St, Marikina", x, y, normal);
        y += 14;
        page.getCanvas().drawText("0956 667 4823 | 0994 865 7461", x, y, normal);
        y += 18;

        String dt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(order.timestampMillis));
        page.getCanvas().drawText("Order #" + order.id, x, y, normal);
        y += 12;
        page.getCanvas().drawText("Date: " + dt, x, y, normal);
        y += 12;
        page.getCanvas().drawText("Cashier: " + safe(order.cashierName), x, y, normal);
        y += 12;
        page.getCanvas().drawText("Payment: " + safe(order.paymentType), x, y, normal);
        y += 16;

        page.getCanvas().drawLine(x, y, pageWidth - 12, y, normal);
        y += 14;

        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.isEmpty()) {
                    name += " (" + l.variantLabel + ")";
                }
                page.getCanvas().drawText(name, x, y, normal);
                y += 12;
                page.getCanvas().drawText("  x" + l.quantity + "  ₱" + (l.getLineTotalCents() / 100), x, y, normal);
                y += 14;
                if (y > pageHeight - 80) break;
            }
        }

        y += 6;
        page.getCanvas().drawLine(x, y, pageWidth - 12, y, normal);
        y += 14;

        page.getCanvas().drawText("TOTAL: ₱" + (order.totalCents / 100), x, y, title);
        y += 14;
        page.getCanvas().drawText("CASH: ₱" + (order.cashReceivedCents / 100), x, y, normal);
        y += 12;
        page.getCanvas().drawText("CHANGE: ₱" + (order.changeCents / 100), x, y, normal);
        y += 18;
        page.getCanvas().drawText("Thank you!", x, y, title);

        doc.finishPage(page);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.writeTo(bos);
        doc.close();
        return bos.toByteArray();
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}

