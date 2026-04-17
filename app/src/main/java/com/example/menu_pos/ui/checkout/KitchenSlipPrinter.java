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

/** Minimal PDF for kitchen slip when no Bluetooth printer is configured. */
public final class KitchenSlipPrinter {

    private static final float QTY_COL_WIDTH = 36f;
    private static final float PRICE_COL_WIDTH = 72f;
    private static final float QTY_ITEM_GAP = 4f;

    private KitchenSlipPrinter() {}

    public static byte[] buildKitchenSlipPdfBytes(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) throws Exception {
        return buildKitchenSlipPdfBytes(context, order, lines, "KITCHEN ORDER", true);
    }

    public static byte[] buildAdditionalItemsSlipPdfBytes(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) throws Exception {
        return buildKitchenSlipPdfBytes(context, order, lines, "Additional for Order #" + order.id, false);
    }

    private static byte[] buildKitchenSlipPdfBytes(Context context,
                                                   PaidOrderEntity order,
                                                   List<PaidOrderLineEntity> lines,
                                                   String titleText,
                                                   boolean includeGrandTotal) throws Exception {
        PdfDocument doc = new PdfDocument();
        int pageWidth = 300;
        int pageHeight = 800;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint title = new Paint();
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(14);

        Paint emphasis = new Paint();
        emphasis.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        emphasis.setTextSize(14);

        Paint normal = new Paint();
        normal.setTextSize(10);
        normal.setTypeface(Typeface.MONOSPACE);

        float margin = 12f;
        float contentWidth = pageWidth - (margin * 2);
        float rightEdge = margin + contentWidth;
        float y = 24f;

        drawCentered(page, titleText, y, title, pageWidth / 2f);
        y += 22f;

        String orderType = safe(order.orderType).trim();
        if (orderType.isEmpty()) orderType = "DINE IN";
        y += 4f;
        drawLine(page, y, margin, rightEdge, normal);
        y += 8f;
        drawCentered(page, "*** " + orderType.toUpperCase(Locale.getDefault()) + " ***", y, emphasis, pageWidth / 2f);
        y += 14f;
        drawLine(page, y, margin, rightEdge, normal);
        y += 10f;

        String timeStr = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new Date(order.timestampMillis));
        drawLeftRight(page, "ORDER #" + order.id, timeStr, y, normal, margin, rightEdge);
        y += 16f;
        String table = safe(order.tableNumber).trim();
        if (!table.isEmpty()) {
            drawLeftRight(page, "TABLE", table, y, normal, margin, rightEdge);
            y += 16f;
        }

        if (order.orderNotes != null && !order.orderNotes.trim().isEmpty()) {
            y = drawWrapped(page, "Notes: " + order.orderNotes.trim(), margin, y, contentWidth, normal);
            y += 8f;
        }

        y += 4f;
        drawLine(page, y, margin, rightEdge, normal);
        y += 14f;

        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.isEmpty()) {
                    name += " (" + l.variantLabel + ")";
                }
                int linePesos = l.getLineSubtotalCents() / 100;
                y = drawItemRow(page, l.quantity, name, linePesos, y, normal, margin, contentWidth);
                if (y > pageHeight - 60) break;
            }
        }

        y += 4f;
        drawLine(page, y, margin, rightEdge, normal);
        y += 14f;
        if (includeGrandTotal) {
            drawLeftRight(page, "TOTAL", "PHP " + (order.totalCents / 100), y, emphasis, margin, rightEdge);
        } else {
            drawLeftRight(page, "ADDED", "PHP " + sumLineSubtotalPesos(lines), y, emphasis, margin, rightEdge);
        }

        doc.finishPage(page);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.writeTo(bos);
        doc.close();
        return bos.toByteArray();
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static int sumLineSubtotalPesos(List<PaidOrderLineEntity> lines) {
        if (lines == null) return 0;
        int sum = 0;
        for (PaidOrderLineEntity l : lines) {
            if (l == null) continue;
            sum += Math.max(0, l.getLineSubtotalCents() / 100);
        }
        return sum;
    }

    private static void drawCentered(PdfDocument.Page page, String text, float y, Paint paint, float cx) {
        float w = paint.measureText(text);
        page.getCanvas().drawText(text, cx - w / 2f, y, paint);
    }

    private static void drawLeftRight(PdfDocument.Page page, String left, String right, float y, Paint paint, float leftX, float rightEdge) {
        page.getCanvas().drawText(left, leftX, y, paint);
        float rw = paint.measureText(right);
        page.getCanvas().drawText(right, rightEdge - rw, y, paint);
    }

    private static void drawLine(PdfDocument.Page page, float y, float x0, float x1, Paint paint) {
        page.getCanvas().drawLine(x0, y, x1, y, paint);
    }

    private static void drawColumnsHeader(PdfDocument.Page page, float y, Paint paint, float margin, float contentWidth) {
        float amountRightX = margin + contentWidth;
        float qtyColRight = margin + QTY_COL_WIDTH;
        float itemStartX = margin + QTY_COL_WIDTH + QTY_ITEM_GAP;

        float qtyHeaderW = paint.measureText("QTY");
        page.getCanvas().drawText("QTY", qtyColRight - qtyHeaderW, y, paint);

        page.getCanvas().drawText("ITEM", itemStartX, y, paint);

        float amountTextWidth = paint.measureText("AMOUNT");
        page.getCanvas().drawText("AMOUNT", amountRightX - amountTextWidth, y, paint);
    }

    private static float drawItemRow(PdfDocument.Page page,
                                     int qty,
                                     String itemName,
                                     int lineTotalPesos,
                                     float startY,
                                     Paint paint,
                                     float margin,
                                     float contentWidth) {
        float amountRightX = margin + contentWidth;
        float qtyColRight = margin + QTY_COL_WIDTH;
        float itemStartX = margin + QTY_COL_WIDTH + QTY_ITEM_GAP;
        float itemMaxWidth = contentWidth - QTY_COL_WIDTH - QTY_ITEM_GAP - PRICE_COL_WIDTH;

        float y = startY;

        String qtyText = String.valueOf(qty);
        float qtyW = paint.measureText(qtyText);
        page.getCanvas().drawText(qtyText, qtyColRight - qtyW, y, paint);

        String amountText = "₱" + lineTotalPesos;
        float amountWidth = paint.measureText(amountText);
        float amountX = amountRightX - amountWidth;
        page.getCanvas().drawText(amountText, amountX, y, paint);

        y = drawWrapped(page, itemName, itemStartX, y, itemMaxWidth, paint);
        y += 8f;
        return y;
    }

    private static float drawWrapped(PdfDocument.Page page, String text, float x, float y, float maxWidth, Paint paint) {
        if (text == null || text.isEmpty()) return y;
        int start = 0;
        int len = text.length();
        float lineHeight = paint.getTextSize() + 3;
        while (start < len) {
            int count = paint.breakText(text, start, len, true, maxWidth, null);
            if (count <= 0) break;
            String line = text.substring(start, start + count);
            page.getCanvas().drawText(line, x, y, paint);
            start += count;
            y += lineHeight;
        }
        return y;
    }
}
