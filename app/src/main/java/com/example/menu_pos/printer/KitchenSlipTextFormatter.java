package com.example.menu_pos.printer;

import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Narrow kitchen ticket: item, qty, unit price, line total — no payment block. */
public final class KitchenSlipTextFormatter {
    public static final int WIDTH = ReceiptTextFormatter.WIDTH;

    private KitchenSlipTextFormatter() {}

    public static String buildKitchenSlip(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        return buildKitchenSlip(order, lines, "KITCHEN ORDER", true);
    }

    public static String buildAdditionalItemsSlip(PaidOrderEntity order, List<PaidOrderLineEntity> lines) {
        return buildKitchenSlip(order, lines, "Additional for Order #" + order.id, false);
    }

    private static String buildKitchenSlip(PaidOrderEntity order,
                                           List<PaidOrderLineEntity> lines,
                                           String title,
                                           boolean includeGrandTotal) {
        StringBuilder out = new StringBuilder();
        out.append(center(title)).append('\n');
        String orderType = safe(order.orderType);
        if (orderType.isEmpty()) orderType = "DINE IN";
        else orderType = orderType.toUpperCase(Locale.getDefault());
        // Normalize order type so "TakeOut", "TAKE_OUT", etc. print consistently.
        orderType = orderType.replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
        if (orderType.contains("DINE")) orderType = "DINE IN";
        else if (orderType.contains("TAKE")) orderType = "TAKE OUT";

        out.append(repeat('-', WIDTH)).append('\n');
        out.append(center("*** " + orderType + " ***")).append('\n');
        out.append(repeat('-', WIDTH)).append('\n');

        String timeStr = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new Date(order.timestampMillis));
        out.append(fitLeftRight("ORDER #" + order.id, timeStr)).append('\n');
        String table = safe(order.tableNumber);
        if (!table.isEmpty()) {
            out.append(fitLeftRight("TABLE", table)).append('\n');
        }
        if (order.orderNotes != null && !order.orderNotes.trim().isEmpty()) {
            // Include a "Notes:" prefix to match PDF output.
            for (String noteLine : wrap("Notes: " + order.orderNotes.trim(), WIDTH)) {
                out.append(truncate(noteLine, WIDTH)).append('\n');
            }
        }
        out.append(repeat('-', WIDTH)).append('\n');

        if (lines != null) {
            for (PaidOrderLineEntity l : lines) {
                if (l == null) continue;
                String name = safe(l.itemName);
                if (l.variantLabel != null && !l.variantLabel.trim().isEmpty()) {
                    name += " (" + l.variantLabel.trim() + ")";
                }
                int linePesos = l.getLineSubtotalCents() / 100;
                String prefix = l.quantity + "x ";
                List<String> wrapped = wrap(name, WIDTH - prefix.length());
                if (wrapped.isEmpty()) wrapped.add("");
                out.append(truncate(prefix + wrapped.get(0), WIDTH)).append('\n');
                for (int i = 1; i < wrapped.size(); i++) {
                    out.append(truncate(spaces(prefix.length()) + wrapped.get(i), WIDTH)).append('\n');
                }
                out.append(fitLeftRight("", "PHP " + linePesos)).append('\n');
            }
        }

        out.append(repeat('-', WIDTH)).append('\n');
        if (includeGrandTotal) {
            out.append(fitLeftRight("TOTAL", "PHP " + (order.totalCents / 100))).append('\n');
        } else {
            out.append(fitLeftRight("ADDED", "PHP " + (sumLineSubtotalPesos(lines)))).append('\n');
        }
        out.append('\n');
        return out.toString();
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

    private static String center(String text) {
        String s = truncate(text != null ? text : "", WIDTH);
        int leftPad = Math.max(0, (WIDTH - s.length()) / 2);
        // Avoid trailing spaces; some thermal printers behave oddly if the cursor
        // isn't advanced to column 0 correctly.
        return spaces(leftPad) + s;
    }

    private static String fitLeftRight(String left, String right) {
        String l = left != null ? left : "";
        String r = right != null ? right : "";
        if (l.length() + r.length() > WIDTH) {
            int maxLeft = Math.max(0, WIDTH - r.length() - 1);
            l = truncate(l, maxLeft);
        }
        int sp = Math.max(1, WIDTH - l.length() - r.length());
        return l + spaces(sp) + r;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String s = text.trim();
        if (s.isEmpty() || width <= 0) {
            if (s.isEmpty()) out.add("");
            else out.add(truncate(s, width));
            return out;
        }
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(s.length(), i + width);
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
