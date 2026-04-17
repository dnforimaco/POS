package com.example.menu_pos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.menu_pos.R;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Material dialog: tappable variant rows + explicit Cancel (clearer than a plain list dialog). */
public final class VariantPickerDialogHelper {

    public interface OnVariantSelected {
        void onSelect(@NonNull MenuItemVariant variant);
    }

    private VariantPickerDialogHelper() {}

    public static void show(
            @NonNull Context context,
            @NonNull MenuItem item,
            @NonNull List<MenuItemVariant> variants,
            @NonNull OnVariantSelected listener) {
        if (variants.isEmpty()) return;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_variant_picker, null, false);
        TextView titleTv = content.findViewById(R.id.variant_dialog_title);
        titleTv.setText(item.getName());
        ViewGroup container = content.findViewById(R.id.variant_options_container);

        builder.setView(content).setNegativeButton(R.string.pos_variant_cancel, null);
        AlertDialog dialog = builder.create();

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < variants.size(); i++) {
            final MenuItemVariant v = variants.get(i);
            View row = inflater.inflate(R.layout.item_variant_option_row, container, false);
            TextView labelTv = row.findViewById(R.id.variant_option_label);
            TextView priceTv = row.findViewById(R.id.variant_option_price);
            String label = v.getLabel().isEmpty()
                    ? context.getString(R.string.pos_variant_default_label) : v.getLabel();
            labelTv.setText(label);
            priceTv.setText(context.getString(R.string.price_format, v.getPriceCents() / 100));
            row.setOnClickListener(click -> {
                listener.onSelect(v);
                dialog.dismiss();
            });
            container.addView(row);
        }

        dialog.show();
    }
}
