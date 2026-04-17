package com.example.menu_pos.ui.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.example.menu_pos.databinding.FragmentCategoryBinding;
import com.example.menu_pos.databinding.ItemMenuItemBinding;
import com.example.menu_pos.ui.VariantPickerDialogHelper;
import com.example.menu_pos.ui.cart.CartViewModel;

import java.util.ArrayList;
import java.util.List;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private CartViewModel cartVm;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String categoryId = getArguments() != null ? getArguments().getString("categoryId", "") : "";
        CategoryViewModel vm = new ViewModelProvider(this).get(CategoryViewModel.class);
        vm.loadCategory(categoryId);

        binding.toolbarCategory.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        ItemAdapter adapter = new ItemAdapter((item, variant) -> {
            cartVm.addItem(new CartItem(item.getId(), item.getName(),
                    variant.getLabel(), variant.getPriceCents(), 1));
            Toast.makeText(requireContext(), getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show();
        }, (item, variants) -> showVariantDialog(item, variants));
        binding.recyclerItems.setAdapter(adapter);

        vm.getCategory().observe(getViewLifecycleOwner(), cat -> {
            if (cat != null) {
                binding.toolbarCategory.setTitle(cat.getName());
                adapter.submitList(cat.getItems());
            }
        });
    }

    private void showVariantDialog(MenuItem item, List<MenuItemVariant> variants) {
        VariantPickerDialogHelper.show(requireContext(), item, variants, v -> {
            cartVm.addItem(new CartItem(item.getId(), item.getName(), v.getLabel(), v.getPriceCents(), 1));
            Toast.makeText(requireContext(), getString(R.string.added_to_cart), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Holder> {
        private final OnAddOne addOne;
        private final OnPickVariant pickVariant;
        private List<MenuItem> list = new ArrayList<>();

        ItemAdapter(OnAddOne addOne, OnPickVariant pickVariant) {
            this.addOne = addOne;
            this.pickVariant = pickVariant;
        }

        void submitList(List<MenuItem> list) {
            List<MenuItem> newList = list != null ? list : new ArrayList<>();
            List<MenuItem> oldList = this.list;
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return oldList.size(); }
                @Override public int getNewListSize() { return newList.size(); }
                @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                    return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
                }
                @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                    MenuItem o = oldList.get(oldPos);
                    MenuItem n = newList.get(newPos);
                    if (!java.util.Objects.equals(o.getName(), n.getName())) return false;
                    if (o.getImageResId() != n.getImageResId()) return false;
                    if (o.isBestSeller() != n.isBestSeller()) return false;
                    if (o.isSpicy() != n.isSpicy()) return false;
                    if (o.getCardDisplayPriceCents() != n.getCardDisplayPriceCents()) return false;
                    return variantsEqual(o.getVariants(), n.getVariants());
                }
            });
            this.list = newList;
            diff.dispatchUpdatesTo(this);
        }

        private static boolean variantsEqual(List<MenuItemVariant> a, List<MenuItemVariant> b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) {
                MenuItemVariant x = a.get(i);
                MenuItemVariant y = b.get(i);
                String lx = x.getLabel() != null ? x.getLabel() : "";
                String ly = y.getLabel() != null ? y.getLabel() : "";
                if (!lx.equals(ly) || x.getPriceCents() != y.getPriceCents()) return false;
            }
            return true;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMenuItemBinding b = ItemMenuItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            MenuItem item = list.get(position);
            h.name.setText(item.getName());
            List<MenuItemVariant> vars = item.getVariants();
            if (vars.isEmpty()) {
                h.variants.setText("");
            } else if (vars.size() == 1) {
                int p = vars.get(0).getPriceCents();
                h.variants.setText(h.itemView.getContext().getString(R.string.price_format, p / 100));
            } else {
                StringBuilder sb = new StringBuilder();
                for (MenuItemVariant v : vars) {
                    if (sb.length() > 0) sb.append(" · ");
                    sb.append(v.getLabel()).append(" ₱").append(v.getPriceCents() / 100);
                }
                h.variants.setText(sb.toString());
            }
            if (item.getImageResId() != 0) {
                h.image.setImageResource(item.getImageResId());
                h.image.setVisibility(View.VISIBLE);
            } else {
                h.image.setImageResource(R.drawable.ic_food_placeholder);
                h.image.setVisibility(View.VISIBLE);
            }
            h.badgeBestSeller.setVisibility(item.isBestSeller() ? View.VISIBLE : View.GONE);
            h.badgeSpicy.setVisibility(item.isSpicy() ? View.VISIBLE : View.GONE);
            h.btnAdd.setOnClickListener(v -> {
                if (vars.size() == 1) {
                    addOne.add(item, vars.get(0));
                } else if (vars.size() > 1) {
                    pickVariant.pick(item, vars);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name, variants, badgeBestSeller, badgeSpicy;
            final com.google.android.material.button.MaterialButton btnAdd;

            Holder(ItemMenuItemBinding b) {
                super(b.getRoot());
                image = b.itemImage;
                name = b.itemName;
                variants = b.itemVariants;
                badgeBestSeller = b.badgeBestSeller;
                badgeSpicy = b.badgeSpicy;
                btnAdd = b.btnAdd;
            }
        }

        interface OnAddOne {
            void add(MenuItem item, MenuItemVariant variant);
        }
        interface OnPickVariant {
            void pick(MenuItem item, List<MenuItemVariant> variants);
        }
    }
}
