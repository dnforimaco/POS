package com.example.menu_pos.ui.pos;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.menu_pos.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

/** Dialog helper for collecting kitchen send details before final confirmation. */
public final class KitchenOrderDialogHelper {
    private static final List<String> TABLE_OPTIONS = Arrays.asList(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

    public interface OnConfirmListener {
        void onConfirm(@NonNull String orderType, @NonNull String tableNumber, @NonNull String orderNotes);
    }

    private KitchenOrderDialogHelper() {}

    public static void show(
            @NonNull Context context,
            @Nullable String initialOrderType,
            @Nullable String initialTableNumber,
            @Nullable String initialOrderNotes,
            @NonNull OnConfirmListener listener) {

        View content = LayoutInflater.from(context).inflate(R.layout.dialog_kitchen_order_form, null, false);
        TextInputLayout tableLayout = content.findViewById(R.id.pos_table_layout);
        AutoCompleteTextView tableInput = content.findViewById(R.id.pos_table_input);
        TextInputEditText notesInput = content.findViewById(R.id.order_notes_input);
        com.google.android.material.button.MaterialButtonToggleGroup toggle =
                content.findViewById(R.id.pos_order_type_toggle);
        tableInput.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, TABLE_OPTIONS));
        tableInput.setOnClickListener(v -> tableInput.showDropDown());

        String normalizedType = normalizeOrderType(context, initialOrderType);
        toggle.check(isDineIn(context, normalizedType) ? R.id.pos_order_dine_in : R.id.pos_order_take_out);

        String table = initialTableNumber != null ? initialTableNumber.trim() : "";
        if (TABLE_OPTIONS.contains(table)) {
            tableInput.setText(table, false);
        } else {
            tableInput.setText("", false);
        }
        String notes = initialOrderNotes != null ? initialOrderNotes : "";
        notesInput.setText(notes);
        applyTableVisibility(context, tableLayout, tableInput, normalizedType);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(content)
                .create();

        content.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            String type = checkedId == R.id.pos_order_take_out
                    ? context.getString(R.string.payment_order_type_take_out)
                    : context.getString(R.string.payment_order_type_dine_in);
            applyTableVisibility(context, tableLayout, tableInput, type);
        });

        content.findViewById(R.id.btn_confirm_send).setOnClickListener(v -> {
            int checked = toggle.getCheckedButtonId();
            String type = checked == R.id.pos_order_take_out
                    ? context.getString(R.string.payment_order_type_take_out)
                    : context.getString(R.string.payment_order_type_dine_in);
            String selectedTable = readTrimmed(tableInput);
            if (isDineIn(context, type) && selectedTable.isEmpty()) {
                tableLayout.setError(context.getString(R.string.pos_table_required));
                tableInput.requestFocus();
                return;
            }
            tableLayout.setError(null);
            listener.onConfirm(type, selectedTable, readTrimmed(notesInput));
            dialog.dismiss();
        });

        dialog.show();
    }

    @NonNull
    private static String readTrimmed(@Nullable TextView input) {
        if (input == null) return "";
        CharSequence text = input.getText();
        return text != null ? text.toString().trim() : "";
    }

    private static void applyTableVisibility(
            @NonNull Context context,
            @NonNull TextInputLayout tableLayout,
            @NonNull AutoCompleteTextView tableInput,
            @NonNull String orderType) {
        boolean dineIn = isDineIn(context, orderType);
        tableLayout.setVisibility(dineIn ? View.VISIBLE : View.GONE);
        tableLayout.setError(null);
        if (!dineIn) {
            tableInput.setText("", false);
        }
    }

    @NonNull
    private static String normalizeOrderType(@NonNull Context context, @Nullable String rawType) {
        String normalized = rawType != null ? rawType.trim().toLowerCase() : "";
        if (normalized.equals("take out") || normalized.equals("takeout")) {
            return context.getString(R.string.payment_order_type_take_out);
        }
        return context.getString(R.string.payment_order_type_dine_in);
    }

    private static boolean isDineIn(@NonNull Context context, @NonNull String orderType) {
        return context.getString(R.string.payment_order_type_dine_in).equals(orderType);
    }
}
