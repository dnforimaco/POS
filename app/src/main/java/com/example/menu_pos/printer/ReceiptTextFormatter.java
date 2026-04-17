package com.example.menu_pos.printer;

import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 58mm paper: max 32 chars per line. */
public final class ReceiptTextFormatter {
    public static final int WIDTH = 32;

    /** Column widths for item table header and alignment (qty | item | price). */
    private static final int COL_QTY = 4;
    private static final int COL_PRICE = 8;
    private static final int COL_ITEM = WIDTH - COL_QTY - COL_PRICE;

    private ReceiptTextFormatter() {}

    public static String buildReceipt(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        StringBuilder out = new StringBuilder();

        out.append(center("NAOMIE'S FAMOUS")).append('\n');
        out.append(center("LOMI, BULALO, GOTONG")).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        String orderType = safe(order.orderType);
        if (orderType.isEmpty()) orderType = "DINE IN";
        out.append(center("*** " + orderType.toUpperCase(Locale.getDefault()) + " ***")).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        String timeStr = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
                .format(new Date(order.timestampMillis));
        out.append(fitLeftRight("ORDER #" + order.id, timeStr)).append('\n');

        String table = safe(order.tableNumber);
        if (!table.isEmpty()) out.append(fitLeftRight("TABLE", table)).append('\n');

        out.append(repeat('-', WIDTH)).append('\n');
        out.append(receiptTableHeaderLine()).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        int itemCount = 0;
        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                itemCount += Math.max(0, l.quantity);

                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.trim().isEmpty()) {
                    name += " (" + l.variantLabel.trim() + ")";
                }
                int lineTotalPesos = (l.getLineTotalCents() / 100);

                // Line 1: "2x Item name..."
                String prefix = l.quantity + "x ";
                List<String> wrapped = wrap(name, WIDTH - prefix.length());
                if (wrapped.isEmpty()) wrapped = new ArrayList<>();
                if (wrapped.isEmpty()) wrapped.add("");
                out.append(truncate(prefix + wrapped.get(0), WIDTH)).append('\n');
                for (int i = 1; i < wrapped.size(); i++) {
                    out.append(truncate(spaces(prefix.length()) + wrapped.get(i), WIDTH)).append('\n');
                }

                if (l.hasLineDiscount()) {
                    int discPesos = l.getDiscountAmountCents() / 100;
                    out.append(fitLeftRight("  -10% disc", "PHP " + discPesos)).append('\n');
                }

                // Line total right-aligned (after discount)
                out.append(fitLeftRight("", "PHP " + lineTotalPesos)).append('\n');
            }
        }

        out.append(repeat('-', WIDTH)).append('\n');
        out.append(fitLeftRight("ITEMS", String.valueOf(itemCount))).append('\n');
        out.append(fitLeftRight("TOTAL", "PHP " + (order.totalCents / 100))).append('\n');
        out.append(fitLeftRight("PAID", "PHP " + (order.cashReceivedCents / 100))).append('\n');
        out.append(fitLeftRight("CHANGE", "PHP " + (order.changeCents / 100))).append('\n');

        String payType = safe(order.paymentType);
        if (!payType.isEmpty()) out.append(fitLeftRight("PAYMENT", payType)).append('\n');
        if (order.paymentRefLast4 != null && !order.paymentRefLast4.trim().isEmpty()) {
            out.append(fitLeftRight("REF", "****" + order.paymentRefLast4.trim())).append('\n');
        }
        String cashier = safe(order.cashierName);
        if (!cashier.isEmpty()) out.append(fitLeftRight("CASHIER", cashier)).append('\n');

        out.append('\n');
        out.append(center("ENJOY YOUR MEAL!")).append('\n');
        out.append("\n");
        return out.toString();
    }

    public static String buildBillOutReceipt(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        StringBuilder out = new StringBuilder();
        out.append(center("NAOMIE'S FAMOUS")).append('\n');
        out.append(center("LOMI, BULALO, GOTONG")).append('\n');
        out.append(center("BILL OUT RECEIPT")).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        String timeStr = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
                .format(new Date(order.timestampMillis));
        out.append(fitLeftRight("ORDER #" + order.id, timeStr)).append('\n');

        String orderType = safe(order.orderType);
        if (!orderType.isEmpty()) out.append(fitLeftRight("TYPE", orderType)).append('\n');

        out.append(repeat('-', WIDTH)).append('\n');
        out.append(receiptTableHeaderLine()).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        int itemCount = 0;
        int discountTotalPesos = 0;
        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                itemCount += Math.max(0, l.quantity);
                discountTotalPesos += Math.max(0, l.getDiscountAmountCents() / 100);

                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.trim().isEmpty()) {
                    name += " (" + l.variantLabel.trim() + ")";
                }
                int lineTotalPesos = l.getLineTotalCents() / 100;

                String prefix = l.quantity + "x ";
                List<String> wrapped = wrap(name, WIDTH - prefix.length());
                if (wrapped.isEmpty()) wrapped.add("");
                out.append(truncate(prefix + wrapped.get(0), WIDTH)).append('\n');
                for (int i = 1; i < wrapped.size(); i++) {
                    out.append(truncate(spaces(prefix.length()) + wrapped.get(i), WIDTH)).append('\n');
                }
                if (l.hasLineDiscount()) {
                    out.append(fitLeftRight("  DISCOUNT", "PHP " + (l.getDiscountAmountCents() / 100))).append('\n');
                }
                out.append(fitLeftRight("", "PHP " + lineTotalPesos)).append('\n');
            }
        }

        out.append(repeat('-', WIDTH)).append('\n');
        out.append(fitLeftRight("ITEMS", String.valueOf(itemCount))).append('\n');
        out.append(fitLeftRight("TOTAL", "PHP " + (order.totalCents / 100))).append('\n');
        if (discountTotalPesos > 0) {
            out.append(fitLeftRight("DISCOUNTS", "PHP " + discountTotalPesos)).append('\n');
        }
        String table = safe(order.tableNumber);
        if (!table.isEmpty()) out.append(fitLeftRight("TABLE", table)).append('\n');
        out.append('\n');
        out.append(center("HAVE A NICE DAY!")).append('\n');
        out.append("\n");
        return out.toString();
    }

    public static String buildTestPrint(String printerName) {
        StringBuilder out = new StringBuilder();
        out.append(center("TEST PRINT")).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');
        out.append(fitLeftRight("PRINTER", safe(printerName))).append('\n');
        out.append(fitLeftRight("MODEL", "VOZY P50")).append('\n');
        out.append(fitLeftRight("STATUS", "OK")).append('\n');
        out.append("\n\n\n");
        return out.toString();
    }

    private static String safe(String s) {
        return s != null ? s.trim() : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String spaces(int n) {
        return repeat(' ', Math.max(0, n));
    }

    /** One 32-char row: QTY (left), ITEM (middle), PRICE (right-aligned). */
    private static String receiptTableHeaderLine() {
        return padRightTruncate("QTY", COL_QTY)
                + padRightTruncate("ITEM", COL_ITEM)
                + padLeftTruncate("PRICE", COL_PRICE);
    }

    private static String padRightTruncate(String s, int w) {
        String t = truncate(s != null ? s : "", w);
        return t + spaces(w - t.length());
    }

    private static String padLeftTruncate(String s, int w) {
        String t = truncate(s != null ? s : "", w);
        return spaces(w - t.length()) + t;
    }

    private static String center(String text) {
        String s = truncate(text != null ? text : "", WIDTH);
        int pad = Math.max(0, (WIDTH - s.length()) / 2);
        return spaces(pad) + s;
    }

    private static String fitLeftRight(String left, String right) {
        String l = left != null ? left : "";
        String r = right != null ? right : "";
        if (l.length() + r.length() > WIDTH) {
            int maxLeft = Math.max(0, WIDTH - r.length() - 1);
            l = truncate(l, maxLeft);
        }
        int spaces = Math.max(1, WIDTH - l.length() - r.length());
        return l + spaces(spaces) + r;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String s = text.trim();
        if (s.isEmpty() || width <= 0) {
            out.add("");
            return out;
        }
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(s.length(), i + width);
            // Try to break on space
            int space = s.lastIndexOf(' ', end);
            if (space <= i) space = end;
            String line = s.substring(i, space).trim();
            if (line.isEmpty()) line = s.substring(i, end).trim();
            out.add(truncate(line, width));
            i = space;
            while (i < s.length() && s.charAt(i) == ' ') i++;
        }
        return out;
    }
}

