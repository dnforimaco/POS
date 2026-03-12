package com.example.menu_pos.ui.menu_management;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.example.menu_pos.databinding.FragmentManageItemsBinding;
import com.example.menu_pos.databinding.ItemManageItemBinding;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class ManageMenuItemsFragment extends Fragment {

    private FragmentManageItemsBinding binding;
    private ManageMenuItemsViewModel viewModel;
    private ItemsAdapter adapter;
    private ArrayAdapter<MenuCategory> spinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageItemsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ManageMenuItemsViewModel.class);

        binding.manageItemsBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        spinnerAdapter = new ArrayAdapter<MenuCategory>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<MenuCategory>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                if (getItem(position) != null) t.setText(getItem(position).getName());
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                android.widget.TextView t = v.findViewById(android.R.id.text1);
                if (getItem(position) != null) t.setText(getItem(position).getName());
                return v;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.manageItemsCategorySpinner.setAdapter(spinnerAdapter);

        viewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            spinnerAdapter.clear();
            if (list != null) spinnerAdapter.addAll(list);
            spinnerAdapter.notifyDataSetChanged();
            String selectedId = viewModel.getSelectedCategoryId();
            if (selectedId != null && list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getId().equals(selectedId)) {
                        binding.manageItemsCategorySpinner.setSelection(i);
                        break;
                    }
                }
            }
        });

        binding.manageItemsCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MenuCategory c = (MenuCategory) parent.getItemAtPosition(position);
                if (c != null) viewModel.setSelectedCategoryId(c.getId());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        adapter = new ItemsAdapter(new ArrayList<>(), this::onEditItem, this::onDeleteItem);
        binding.manageItemsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.manageItemsList.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
        });

        binding.manageItemsFab.setOnClickListener(v -> {
            if (viewModel.getSelectedCategoryId() == null) {
                Toast.makeText(requireContext(), R.string.manage_select_category, Toast.LENGTH_SHORT).show();
                return;
            }
            showItemDialog(null, viewModel.getSelectedCategoryId());
        });
    }

    private void showItemDialog(@Nullable MenuItem existing, @Nullable String categoryIdForAdd) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_menu_item, null);
        EditText nameEt = dialogView.findViewById(R.id.dialog_item_name);
        EditText descEt = dialogView.findViewById(R.id.dialog_item_description);
        MaterialCheckBox bestSellerCb = dialogView.findViewById(R.id.dialog_item_best_seller);
        MaterialCheckBox spicyCb = dialogView.findViewById(R.id.dialog_item_spicy);
        LinearLayout variantsContainer = dialogView.findViewById(R.id.dialog_variants_container);
        View addVariantBtn = dialogView.findViewById(R.id.dialog_add_variant);

        boolean isEdit = existing != null;
        if (isEdit) {
            nameEt.setText(existing.getName());
            descEt.setText(existing.getDescription());
            bestSellerCb.setChecked(existing.isBestSeller());
            spicyCb.setChecked(existing.isSpicy());
            for (MenuItemVariant v : existing.getVariants()) {
                addVariantRow(variantsContainer, v.getLabel(), v.getPriceCents() / 100);
            }
        } else {
            addVariantRow(variantsContainer, "", 0);
        }

        addVariantBtn.setOnClickListener(v -> addVariantRow(variantsContainer, "", 0));

        new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? R.string.manage_edit : R.string.manage_add_item)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String nameStr = nameEt.getText() != null ? nameEt.getText().toString().trim() : "";
                    if (nameStr.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.manage_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<MenuItemVariant> variants = collectVariants(variantsContainer);
                    if (variants.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.manage_one_variant_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String descStr = descEt.getText() != null ? descEt.getText().toString().trim() : "";
                    boolean bestSeller = bestSellerCb.isChecked();
                    boolean spicy = spicyCb.isChecked();
                    if (isEdit) {
                        viewModel.updateItem(existing.getId(), nameStr, descStr, variants, bestSeller, spicy);
                    } else {
                        viewModel.addItem(categoryIdForAdd, nameStr, descStr, variants, bestSeller, spicy);
                    }
                    Toast.makeText(requireContext(), R.string.manage_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addVariantRow(LinearLayout container, String label, int pricePesos) {
        View row = getLayoutInflater().inflate(R.layout.row_variant, container, false);
        EditText labelEt = row.findViewById(R.id.row_variant_label);
        EditText priceEt = row.findViewById(R.id.row_variant_price);
        labelEt.setText(label);
        priceEt.setText(pricePesos > 0 ? String.valueOf(pricePesos) : "");
        row.findViewById(R.id.row_variant_remove).setOnClickListener(v -> container.removeView(row));
        container.addView(row);
    }

    private List<MenuItemVariant> collectVariants(LinearLayout container) {
        List<MenuItemVariant> list = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View row = container.getChildAt(i);
            EditText labelEt = row.findViewById(R.id.row_variant_label);
            EditText priceEt = row.findViewById(R.id.row_variant_price);
            String label = labelEt.getText() != null ? labelEt.getText().toString().trim() : "";
            int pricePesos = 0;
            try {
                String p = priceEt.getText() != null ? priceEt.getText().toString().trim() : "";
                if (!p.isEmpty()) pricePesos = Integer.parseInt(p);
            } catch (NumberFormatException ignored) {}
            if (pricePesos >= 0) list.add(new MenuItemVariant(label, pricePesos * 100));
        }
        return list;
    }

    private void onEditItem(MenuItem item) {
        showItemDialog(item, null);
    }

    private void onDeleteItem(MenuItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_delete)
                .setMessage(R.string.manage_confirm_delete_item)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.deleteItem(item.getId());
                    Toast.makeText(requireContext(), R.string.manage_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.Holder> {
        private List<MenuItem> list = new ArrayList<>();
        private final OnItemAction onEdit;
        private final OnItemAction onDelete;

        interface OnItemAction { void run(MenuItem item); }

        ItemsAdapter(List<MenuItem> list, OnItemAction onEdit, OnItemAction onDelete) {
            this.list = list;
            this.onEdit = onEdit;
            this.onDelete = onDelete;
        }

        void submitList(List<MenuItem> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemManageItemBinding b = ItemManageItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            MenuItem item = list.get(position);
            h.binding.itemMenuName.setText(item.getName());
            h.binding.itemMenuEdit.setOnClickListener(v -> onEdit.run(item));
            h.binding.itemMenuDelete.setOnClickListener(v -> onDelete.run(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemManageItemBinding binding;
            Holder(ItemManageItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
