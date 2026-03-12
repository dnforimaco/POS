package com.example.menu_pos.ui.inventory;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.InventoryCategoryEntity;
import com.example.menu_pos.data.InventoryItemEntity;
import com.example.menu_pos.data.InventoryMovementEntity;
import com.example.menu_pos.databinding.FragmentInventoryBinding;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InventoryFragment extends Fragment {

    private FragmentInventoryBinding binding;
    private InventoryViewModel vm;

    private ArrayAdapter<CategoryRow> categorySpinnerAdapter;
    private ItemsAdapter itemsAdapter;

    private HistoryAdapter historyAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(InventoryViewModel.class);

        binding.inventoryBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        setupTabs();
        setupItemsPanel();
        setupHistoryPanel();
    }

    private void setupTabs() {
        TabLayout tabs = binding.inventoryTabs;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText(R.string.inventory_tab_items));
        tabs.addTab(tabs.newTab().setText(R.string.inventory_tab_history));

        showPanel(0);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showPanel(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showPanel(int index) {
        binding.panelItems.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        binding.panelHistory.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (index == 1) vm.refreshHistory();
    }

    private void setupItemsPanel() {
        categorySpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                CategoryRow row = getItem(position);
                if (t != null && row != null) t.setText(row.label);
                return v;
            }
            @NonNull
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                CategoryRow row = getItem(position);
                if (t != null && row != null) t.setText(row.label);
                return v;
            }
        };
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inventoryCategorySpinner.setAdapter(categorySpinnerAdapter);

        binding.inventoryCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CategoryRow row = (CategoryRow) parent.getItemAtPosition(position);
                vm.setSelectedCategoryId(row != null ? row.id : null);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        itemsAdapter = new ItemsAdapter(new ArrayList<>(),
                this::onRestockItem,
                this::onEditItem,
                this::onDeleteItem,
                vm);
        binding.inventoryItemsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.inventoryItemsList.setAdapter(itemsAdapter);

        vm.getCategoryRows().observe(getViewLifecycleOwner(), rows -> {
            categorySpinnerAdapter.clear();
            if (rows != null) categorySpinnerAdapter.addAll(rows);
            categorySpinnerAdapter.notifyDataSetChanged();
        });

        vm.getItems().observe(getViewLifecycleOwner(), items -> itemsAdapter.submitList(items != null ? new ArrayList<>(items) : new ArrayList<>()));

        binding.inventoryAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        binding.inventoryAddItem.setOnClickListener(v -> showItemDialog(null));
        binding.inventoryExportReport.setOnClickListener(v -> exportInventoryReport());
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.inventory_category));
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.inventory_add_category)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.inventory_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    vm.addCategory(name);
                    Toast.makeText(requireContext(), R.string.inventory_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupHistoryPanel() {
        historyAdapter = new HistoryAdapter(new ArrayList<>(), vm);
        binding.inventoryHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.inventoryHistoryList.setAdapter(historyAdapter);

        vm.getHistory().observe(getViewLifecycleOwner(), list -> historyAdapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>()));

        boolean manager = vm != null && vm.isManager();
        binding.inventoryClearHistory.setVisibility(manager ? View.VISIBLE : View.GONE);
        binding.inventoryClearHistory.setOnClickListener(v -> {
            if (vm == null || !vm.isManager()) {
                Toast.makeText(requireContext(), R.string.inventory_manager_only, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.inventory_clear_history)
                    .setMessage(R.string.inventory_clear_history_confirm)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        vm.clearHistory();
                        Toast.makeText(requireContext(), R.string.inventory_clear_history_done, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void showItemDialog(@Nullable InventoryItemEntity existing) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_inventory_item, null);
        Spinner catSpinner = dialogView.findViewById(R.id.dialog_inv_category_spinner);
        EditText nameEt = dialogView.findViewById(R.id.dialog_inv_item_name);
        EditText stockEt = dialogView.findViewById(R.id.dialog_inv_stock_qty);
        EditText minEt = dialogView.findViewById(R.id.dialog_inv_min_stock_qty);

        ArrayAdapter<InventoryCategoryEntity> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                InventoryCategoryEntity c = getItem(position);
                if (t != null && c != null) t.setText(c.name);
                return v;
            }
            @NonNull
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                InventoryCategoryEntity c = getItem(position);
                if (t != null && c != null) t.setText(c.name);
                return v;
            }
        };
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        catSpinner.setAdapter(catAdapter);

        List<InventoryCategoryEntity> cats = vm.getInventoryCategoriesNow();
        if (cats != null) {
            for (InventoryCategoryEntity c : cats) {
                if ("inv_general".equals(c.id)) continue;
                catAdapter.add(c);
            }
        }
        catAdapter.notifyDataSetChanged();

        boolean isEdit = existing != null;
        if (isEdit) {
            nameEt.setText(existing.name);
            stockEt.setText(String.valueOf(existing.stockQty));
            minEt.setText(String.valueOf(existing.minStockQty));
            for (int i = 0; i < catAdapter.getCount(); i++) {
                InventoryCategoryEntity c = catAdapter.getItem(i);
                if (c != null && c.id.equals(existing.categoryId)) {
                    catSpinner.setSelection(i);
                    break;
                }
            }
        } else {
            stockEt.setText("0");
            minEt.setText("0");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? R.string.inventory_edit_item : R.string.inventory_add_item)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    InventoryCategoryEntity selectedCat = (InventoryCategoryEntity) catSpinner.getSelectedItem();
                    String categoryId = selectedCat != null ? selectedCat.id : "inv_general";

                    String name = nameEt.getText() != null ? nameEt.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.inventory_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int stock = parseIntSafe(stockEt.getText() != null ? stockEt.getText().toString() : "0");
                    int min = parseIntSafe(minEt.getText() != null ? minEt.getText().toString() : "0");

                    if (isEdit) {
                        vm.updateItem(existing.id, categoryId, name, stock, min);
                    } else {
                        vm.addItem(categoryId, name, stock, min);
                    }
                    Toast.makeText(requireContext(), R.string.inventory_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onEditItem(InventoryItemEntity item) {
        showItemDialog(item);
    }

    private void onDeleteItem(InventoryItemEntity item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.inventory_delete_item)
                .setMessage(item.name)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    vm.deleteItem(item.id);
                    Toast.makeText(requireContext(), R.string.inventory_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onRestockItem(InventoryItemEntity item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_inventory_restock, null);
        EditText qtyEt = dialogView.findViewById(R.id.dialog_restock_qty);
        qtyEt.setText("");

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.inventory_restock) + ": " + item.name)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    int qty = parseIntSafe(qtyEt.getText() != null ? qtyEt.getText().toString() : "0");
                    if (qty <= 0) {
                        Toast.makeText(requireContext(), R.string.inventory_qty_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    vm.restock(item.id, qty);
                    Toast.makeText(requireContext(), R.string.inventory_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void exportInventoryReport() {
        List<InventoryItemEntity> items = vm != null ? vm.getAllInventoryItemsNow() : null;
        if (items == null) items = new ArrayList<>();

        File cacheDir = requireContext().getCacheDir();
        File pdfFile = new File(cacheDir, "inventory_report.pdf");
        try {
            generateInventoryPdf(pdfFile, items);
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dirknashorimaco1@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Report");
            intent.putExtra(Intent.EXTRA_TEXT, "Attached is the latest inventory report from the POS system.");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, getString(R.string.inventory_export_report)));
            Toast.makeText(requireContext(), R.string.inventory_export_report, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), R.string.inventory_export_error, Toast.LENGTH_LONG).show();
        }
    }

    private void generateInventoryPdf(File outFile, List<InventoryItemEntity> items) throws IOException {
        final int pageWidth = 595;
        final int pageHeight = 842;
        final int margin = 40;
        final int rowHeight = 25;
        final float col1 = margin + 10;
        final float col2 = margin + 180;
        final float col3 = margin + 330;
        final float col4 = margin + 440;

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(10);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        Paint headerPaint = new Paint(textPaint);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xFF888888);

        // Status Colors
        int redColor = ContextCompat.getColor(requireContext(), R.color.price_red);
        int orangeColor = ContextCompat.getColor(requireContext(), R.color.inventory_stock_low_orange);
        int greenColor = ContextCompat.getColor(requireContext(), R.color.inventory_stock_in_green);

        PdfDocument doc = new PdfDocument();
        int y = margin + 20;
        int pageNum = 0;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Header row
        canvas.drawText("Item Name", col1, y, headerPaint);
        canvas.drawText("Category", col2, y, headerPaint);
        canvas.drawText("Status", col3, y, headerPaint);
        canvas.drawText("Stock Quantity", col4, y, headerPaint);
        y += 12; // Gap below header text baseline
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
        y += 25; // Large gap after line to first item baseline

        for (InventoryItemEntity item : items) {
            if (y > pageHeight - margin - rowHeight) {
                doc.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum + 1).create();
                page = doc.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin + 20;
            }
            String name = item.name != null ? item.name : "";
            String category = vm.getCategoryName(item.categoryId);
            if (name.length() > 28) name = name.substring(0, 25) + "...";
            if (category.length() > 18) category = category.substring(0, 15) + "...";

            // Status Logic
            int qty = item.stockQty;
            String statusText;
            int statusColor;
            if (qty == 0) {
                statusText = "OUT OF STOCK";
                statusColor = redColor;
            } else if (qty >= 1 && qty <= 5) {
                statusText = "LOW";
                statusColor = orangeColor;
            } else {
                statusText = "IN STOCK";
                statusColor = greenColor;
            }

            canvas.drawText(name, col1, y, textPaint);
            canvas.drawText(category, col2, y, textPaint);

            // Draw status with color
            Paint statusPaint = new Paint(textPaint);
            statusPaint.setColor(statusColor);
            statusPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(statusText, col3, y, statusPaint);

            canvas.drawText(String.valueOf(qty), col4, y, textPaint);
            y += rowHeight;
        }

        doc.finishPage(page);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            doc.writeTo(fos);
        }
        doc.close();
    }

    private static int parseIntSafe(String s) {
        try {
            String t = s != null ? s.trim() : "";
            if (t.isEmpty()) return 0;
            return Integer.parseInt(t);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class CategoryRow {
        public final String id;
        public final String label;
        public CategoryRow(String id, String label) { this.id = id; this.label = label; }
    }

    private static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.Holder> {
        private List<InventoryItemEntity> list;
        interface ItemAction { void run(InventoryItemEntity item); }
        private final ItemAction onRestock;
        private final ItemAction onEdit;
        private final ItemAction onDelete;
        private final InventoryViewModel vm;

        ItemsAdapter(List<InventoryItemEntity> list, ItemAction onRestock, ItemAction onEdit, ItemAction onDelete, InventoryViewModel vm) {
            this.list = list;
            this.onRestock = onRestock;
            this.onEdit = onEdit;
            this.onDelete = onDelete;
            this.vm = vm;
        }

        void submitList(List<InventoryItemEntity> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_item, parent, false);
            return new Holder(v);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            InventoryItemEntity item = list.get(position);
            h.name.setText(item.name);
            h.category.setText(vm.getCategoryName(item.categoryId));
            h.btnRestock.setOnClickListener(v -> onRestock.run(item));

            int qty = item.stockQty;
            String statusText;
            int statusColorResId;
            if (qty == 0) {
                statusText = "OUT OF STOCK";
                statusColorResId = R.color.price_red;
            } else if (qty >= 1 && qty <= 5) {
                statusText = "LOW";
                statusColorResId = R.color.inventory_stock_low_orange;
            } else {
                statusText = "IN STOCK";
                statusColorResId = R.color.inventory_stock_in_green;
            }
            h.stockStatus.setText(statusText);
            h.stockStatus.setTextColor(h.itemView.getContext().getResources().getColor(statusColorResId));

            h.stockQty.setText(String.valueOf(qty));
            h.btnMore.setOnClickListener(v -> {
                String[] actions = new String[] {
                        v.getContext().getString(R.string.manage_edit),
                        v.getContext().getString(R.string.manage_delete)
                };
                new AlertDialog.Builder(v.getContext())
                        .setTitle(item.name)
                        .setItems(actions, (d, which) -> {
                            if (which == 0) onEdit.run(item);
                            if (which == 1) onDelete.run(item);
                        })
                        .show();
            });
        }

        @Override public int getItemCount() { return list != null ? list.size() : 0; }

        static class Holder extends RecyclerView.ViewHolder {
            final android.widget.TextView name, category, stockStatus, stockQty;
            final com.google.android.material.button.MaterialButton btnRestock, btnMore;
            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.inv_item_name);
                category = v.findViewById(R.id.inv_item_category);
                stockStatus = v.findViewById(R.id.inv_item_stock_status);
                stockQty = v.findViewById(R.id.inv_item_stock_qty);
                btnRestock = v.findViewById(R.id.inv_btn_restock);
                btnMore = v.findViewById(R.id.inv_btn_more);
            }
        }
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        private static final String DATE_FORMAT = "MMM dd, HH:mm";
        private List<InventoryMovementEntity> list;
        private final InventoryViewModel vm;

        HistoryAdapter(List<InventoryMovementEntity> list, InventoryViewModel vm) {
            this.list = list;
            this.vm = vm;
        }

        void submitList(List<InventoryMovementEntity> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_history, parent, false);
            return new Holder(v);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            InventoryMovementEntity e = list.get(position);
            String itemName = vm.getInventoryItemName(e.itemId);
            String title = e.type + " " + (e.deltaQty >= 0 ? "+" + e.deltaQty : String.valueOf(e.deltaQty)) + " · " + itemName;
            h.title.setText(title);
            SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            String when = df.format(new Date(e.timestampMillis));
            String subtitle = when
                    + (e.actor != null && !e.actor.isEmpty() ? " · " + e.actor : "")
                    + (e.relatedOrderId != null ? " · Order #" + e.relatedOrderId : "")
                    + (e.note != null && !e.note.isEmpty() ? " · " + e.note : "");
            h.subtitle.setText(subtitle);
        }

        @Override public int getItemCount() { return list != null ? list.size() : 0; }

        static class Holder extends RecyclerView.ViewHolder {
            final android.widget.TextView title, subtitle;
            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.hist_title);
                subtitle = v.findViewById(R.id.hist_subtitle);
            }
        }
    }
}
