package com.example.menu_pos.ui.menu_management;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.menu_pos.databinding.FragmentManageCategoriesBinding;
import com.example.menu_pos.databinding.ItemManageCategoryBinding;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesFragment extends Fragment {

    private FragmentManageCategoriesBinding binding;
    private ManageCategoriesViewModel viewModel;
    private CategoriesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ManageCategoriesViewModel.class);

        binding.manageCategoriesBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        adapter = new CategoriesAdapter(new ArrayList<>(), this::onEditCategory, this::onDeleteCategory);
        binding.manageCategoriesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.manageCategoriesList.setAdapter(adapter);

        viewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
        });

        binding.manageCategoriesFab.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void showAddCategoryDialog() {
        showCategoryDialog(null, null, null);
    }

    private void onEditCategory(MenuCategory c) {
        showCategoryDialog(c.getId(), c.getName(), c.getSubtitle());
    }

    private void showCategoryDialog(@Nullable String id, @Nullable String name, @Nullable String subtitle) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category, null);
        EditText nameEt = dialogView.findViewById(R.id.dialog_category_name);
        EditText subtitleEt = dialogView.findViewById(R.id.dialog_category_subtitle);
        if (name != null) nameEt.setText(name);
        if (subtitle != null) subtitleEt.setText(subtitle);

        boolean isEdit = id != null;
        new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? R.string.manage_edit : R.string.manage_add_category)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String nameStr = nameEt.getText() != null ? nameEt.getText().toString().trim() : "";
                    if (nameStr.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.manage_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String subStr = subtitleEt.getText() != null ? subtitleEt.getText().toString().trim() : "";
                    if (isEdit) {
                        viewModel.updateCategory(id, nameStr, subStr);
                    } else {
                        viewModel.addCategory(nameStr, subStr);
                    }
                    Toast.makeText(requireContext(), R.string.manage_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onDeleteCategory(MenuCategory c) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_delete)
                .setMessage(R.string.manage_confirm_delete_category)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.deleteCategory(c.getId());
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

    private static class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.Holder> {
        private List<MenuCategory> list = new ArrayList<>();
        private final OnCategoryAction onEdit;
        private final OnCategoryAction onDelete;

        interface OnCategoryAction { void run(MenuCategory c); }

        CategoriesAdapter(List<MenuCategory> list, OnCategoryAction onEdit, OnCategoryAction onDelete) {
            this.list = list;
            this.onEdit = onEdit;
            this.onDelete = onDelete;
        }

        void submitList(List<MenuCategory> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemManageCategoryBinding b = ItemManageCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            MenuCategory c = list.get(position);
            h.binding.itemCategoryName.setText(c.getName());
            h.binding.itemCategorySubtitle.setText(c.getSubtitle());
            h.binding.itemCategoryEdit.setOnClickListener(v -> onEdit.run(c));
            h.binding.itemCategoryDelete.setOnClickListener(v -> onDelete.run(c));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemManageCategoryBinding binding;
            Holder(ItemManageCategoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
