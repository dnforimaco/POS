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

    /** Width reserved for quantity (header and values right-aligned in this band). */
    private static final float QTY_COL_WIDTH = 36f;
    /** Width reserved for line amount on the right. */
    private static final float PRICE_COL_WIDTH = 72f;
    /** Gap between qty column and item text (must match {@link #drawItemRow}). */
    private static final float QTY_ITEM_GAP = 4f;

    private ReceiptPrinter() {}

    public static byte[] buildReceiptPdfBytes(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) throws Exception {
        PdfDocument doc = new PdfDocument();
        // 57mm thermal paper (about 48mm printable) -> keep width narrow.
        int pageWidth = 300;
        int pageHeight = 900;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint title = new Paint();
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(14);
        title.setTextAlign(Paint.Align.LEFT);

        Paint normal = new Paint();
        normal.setTextSize(10);
        normal.setTypeface(Typeface.MONOSPACE);
        normal.setTextAlign(Paint.Align.LEFT);

        Paint small = new Paint(normal);
        small.setTextSize(9);

        float margin = 12f;
        float contentWidth = pageWidth - (margin * 2);
        float centerX = pageWidth / 2f;
        float y = 24;

        // Header - centered
        drawCenteredText(page, "NAOMIE'S FAMOUS", y, title, centerX);
        y += 18;
        drawCenteredText(page, "LOMI, BULALO, GOTONG BATANGAS", y, normal, centerX);
        y += 18;

        // Order meta row
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(order.timestampMillis));
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(order.timestampMillis));
        String orderLabel = "ORDER #" + order.id;
        String dateTimeLabel = dateStr + " | " + timeStr;
        String orderTypeDisplay = safe(order.orderType).toUpperCase(Locale.getDefault());
        if (orderTypeDisplay.isEmpty()) orderTypeDisplay = "DINE IN";

        // Restaurant header separator
        y += 4;
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        // Order type section (requested format)
        drawLeftRightText(page, "ORDER TYPE:", orderTypeDisplay, y, normal, margin, margin + contentWidth);
        y += 14;

        // Order number + date/time section
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        drawLeftRightText(page, orderLabel, dateTimeLabel, y, normal, margin, margin + contentWidth);
        y += 14;

        String tableDisplay = safe(order.tableNumber);
        if (!tableDisplay.isEmpty()) {
            drawLeftRightText(page, "TABLE #:", tableDisplay, y, normal, margin, margin + contentWidth);
            y += 14;
        }
        y += 4;

        // Separator
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        // Column headers
        drawColumnsHeader(page, y, normal, margin, contentWidth);
        y += 14;
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        int itemCount = 0;

        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.isEmpty()) {
                    name += " (" + l.variantLabel + ")";
                }
                int qty = l.quantity;
                int lineTotal = l.getLineTotalCents() / 100;

                y = drawItemRow(page, qty, name, lineTotal, y, normal, small, margin, contentWidth);
                itemCount += qty;
                if (l.hasLineDiscount()) {
                    y += 2;
                    drawLeftRightText(page, "    -10% disc", "₱" + (l.getDiscountAmountCents() / 100), y, small, margin, margin + contentWidth);
                    y += 12;
                }

                if (y > pageHeight - 80) break;
            }
        }

        y += 4;
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 14;

        // Summary section
        drawLeftRightText(page, "ITEM COUNT:", String.valueOf(itemCount), y, normal, margin, margin + contentWidth);
        y += 14;
        drawLeftRightText(page, "TOTAL:", "₱" + (order.totalCents / 100), y, title, margin, margin + contentWidth);
        y += 16;
        drawLeftRightText(page, "AMOUNT PAID:", "₱" + (order.cashReceivedCents / 100), y, normal, margin, margin + contentWidth);
        y += 14;
        drawLeftRightText(page, "CHANGE:", "₱" + (order.changeCents / 100), y, normal, margin, margin + contentWidth);
        y += 20;

        // Payment info
        String payType = safe(order.paymentType).toUpperCase(Locale.getDefault());
        drawLeftRightText(page, "PAYMENT:", payType, y, normal, margin, margin + contentWidth);
        y += 14;
        if (order.paymentRefLast4 != null && !order.paymentRefLast4.isEmpty()) {
            drawLeftRightText(page, "REFERENCE NO.:", "****" + safe(order.paymentRefLast4), y, normal, margin, margin + contentWidth);
            y += 18;
        } else {
            y += 10;
        }

        y += 10;
        drawCenteredText(page, "ENJOY YOUR MEAL!", y, normal, centerX);

        doc.finishPage(page);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.writeTo(bos);
        doc.close();
        return bos.toByteArray();
    }

    public static byte[] buildBillOutReceiptPdfBytes(Context context, PaidOrderEntity order, List<PaidOrderLineEntity> lines) throws Exception {
        PdfDocument doc = new PdfDocument();
        int pageWidth = 300;
        int pageHeight = 900;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint title = new Paint();
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(14);
        title.setTextAlign(Paint.Align.LEFT);

        Paint normal = new Paint();
        normal.setTextSize(10);
        normal.setTypeface(Typeface.MONOSPACE);
        normal.setTextAlign(Paint.Align.LEFT);

        Paint small = new Paint(normal);
        small.setTextSize(9);

        float margin = 12f;
        float contentWidth = pageWidth - (margin * 2);
        float centerX = pageWidth / 2f;
        float y = 24;

        drawCenteredText(page, "NAOMIE'S FAMOUS", y, title, centerX);
        y += 18;
        drawCenteredText(page, "LOMI, BULALO, GOTONG BATANGAS", y, normal, centerX);
        y += 18;
        drawCenteredText(page, "BILL OUT RECEIPT", y, title, centerX);
        y += 18;
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(order.timestampMillis));
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(order.timestampMillis));
        drawLeftRightText(page, "ORDER #" + order.id, dateStr + " | " + timeStr, y, normal, margin, margin + contentWidth);
        y += 14;

        String orderTypeDisplay = safe(order.orderType).toUpperCase(Locale.getDefault());
        if (!orderTypeDisplay.isEmpty()) {
            drawLeftRightText(page, "ORDER TYPE:", orderTypeDisplay, y, normal, margin, margin + contentWidth);
            y += 14;
        }
        String tableDisplay = safe(order.tableNumber);
        if (!tableDisplay.isEmpty()) {
            drawLeftRightText(page, "TABLE #:", tableDisplay, y, normal, margin, margin + contentWidth);
            y += 14;
        }

        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        drawColumnsHeader(page, y, normal, margin, contentWidth);
        y += 14;
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 12;

        int itemCount = 0;
        int discountTotalPesos = 0;
        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.isEmpty()) {
                    name += " (" + l.variantLabel + ")";
                }
                int qty = l.quantity;
                int lineTotal = l.getLineTotalCents() / 100;
                itemCount += Math.max(0, qty);
                discountTotalPesos += Math.max(0, l.getDiscountAmountCents() / 100);
                y = drawItemRow(page, qty, name, lineTotal, y, normal, small, margin, contentWidth);
                if (l.hasLineDiscount()) {
                    y += 2;
                    drawLeftRightText(page, "    DISCOUNT", "₱" + (l.getDiscountAmountCents() / 100), y, small, margin, margin + contentWidth);
                    y += 12;
                }
                if (y > pageHeight - 80) break;
            }
        }

        y += 4;
        drawSeparatorLine(page, y, margin, margin + contentWidth, normal);
        y += 14;
        drawLeftRightText(page, "ITEM COUNT:", String.valueOf(itemCount), y, normal, margin, margin + contentWidth);
        y += 14;
        drawLeftRightText(page, "TOTAL:", "₱" + (order.totalCents / 100), y, title, margin, margin + contentWidth);
        y += 14;
        if (discountTotalPesos > 0) {
            drawLeftRightText(page, "DISCOUNTS:", "₱" + discountTotalPesos, y, normal, margin, margin + contentWidth);
            y += 14;
        }
        y += 8;
        drawCenteredText(page, "HAVE A NICE DAY!", y, normal, centerX);

        doc.finishPage(page);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        doc.writeTo(bos);
        doc.close();
        return bos.toByteArray();
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static void drawCenteredText(PdfDocument.Page page, String text, float y, Paint paint, float centerX) {
        float w = paint.measureText(text);
        float x = centerX - (w / 2f);
        page.getCanvas().drawText(text, x, y, paint);
    }

    private static void drawLeftRightText(PdfDocument.Page page, String left, String right, float y, Paint paint, float leftX, float rightEdgeX) {
        page.getCanvas().drawText(left, leftX, y, paint);
        float rightWidth = paint.measureText(right);
        float rightX = rightEdgeX - rightWidth;
        page.getCanvas().drawText(right, rightX, y, paint);
    }

    private static void drawSeparatorLine(PdfDocument.Page page, float y, float startX, float endX, Paint paint) {
        page.getCanvas().drawLine(startX, y, endX, y, paint);
    }

    private static void drawColumnsHeader(PdfDocument.Page page, float y, Paint paint, float margin, float contentWidth) {
        float amountRightX = margin + contentWidth;
        float qtyColRight = margin + QTY_COL_WIDTH;
        float itemStartX = margin + QTY_COL_WIDTH + QTY_ITEM_GAP;

        float qtyHeaderW = paint.measureText("QTY");
        page.getCanvas().drawText("QTY", qtyColRight - qtyHeaderW, y, paint);

        page.getCanvas().drawText("ITEM", itemStartX, y, paint);

        float priceTextWidth = paint.measureText("PRICE");
        page.getCanvas().drawText("PRICE", amountRightX - priceTextWidth, y, paint);
    }

    private static float drawItemRow(PdfDocument.Page page,
                                     int qty,
                                     String itemName,
                                     int lineTotalPesos,
                                     float startY,
                                     Paint mainPaint,
                                     Paint smallPaint,
                                     float margin,
                                     float contentWidth) {
        float amountRightX = margin + contentWidth;
        float qtyColRight = margin + QTY_COL_WIDTH;
        float itemStartX = margin + QTY_COL_WIDTH + QTY_ITEM_GAP;
        float itemMaxWidth = contentWidth - QTY_COL_WIDTH - QTY_ITEM_GAP - PRICE_COL_WIDTH;

        float y = startY;

        String qtyText = String.valueOf(qty);
        float qtyTextW = mainPaint.measureText(qtyText);
        page.getCanvas().drawText(qtyText, qtyColRight - qtyTextW, y, mainPaint);

        String amountText = "₱" + lineTotalPesos;
        float amountWidth = mainPaint.measureText(amountText);
        float amountX = amountRightX - amountWidth;
        page.getCanvas().drawText(amountText, amountX, y, mainPaint);

        y = drawWrappedText(page, itemName, itemStartX, y, itemMaxWidth, smallPaint);
        y += 8;
        return y;
    }

    private static float drawWrappedText(PdfDocument.Page page,
                                         String text,
                                         float startX,
                                         float startY,
                                         float maxWidth,
                                         Paint paint) {
        if (text == null || text.isEmpty()) {
            return startY;
        }

        float y = startY;
        int start = 0;
        int len = text.length();
        float lineHeight = paint.getTextSize() + 2;

        while (start < len) {
            int count = paint.breakText(text, start, len, true, maxWidth, null);
            if (count <= 0) break;
            String line = text.substring(start, start + count);
            page.getCanvas().drawText(line, startX, y, paint);
            start += count;
            if (start < len) {
                y += lineHeight;
            }
        }
        return y;
    }
}

