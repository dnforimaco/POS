package com.example.menu_pos.ui.pos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.example.menu_pos.databinding.FragmentPosBinding;
import com.example.menu_pos.databinding.ItemCartBinding;
import com.example.menu_pos.databinding.ItemPosProductBinding;
import com.example.menu_pos.ui.cart.CartViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class PosFragment extends Fragment {

    private FragmentPosBinding binding;
    private PosViewModel posVm;
    private CartViewModel cartVm;
    private boolean cartCollapsed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPosBinding.inflate(inflater, container, false);
        posVm = new ViewModelProvider(this).get(PosViewModel.class);
        cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRail();
        setupTabs();
        setupSearch();
        setupProducts();
        setupCart();
        setupCartToggle();
    }

    private void setupRail() {
        binding.navRail.setSelectedItemId(R.id.pos_home);
        binding.navRail.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.pos_orders) {
                Navigation.findNavController(requireView()).navigate(R.id.nav_orders);
                return true;
            }
            if (id == R.id.pos_reports) {
                handleReportsClick();
                return true;
            }
            if (id == R.id.pos_inventory) {
                handleInventoryClick();
                return true;
            }
            if (item.getItemId() == R.id.pos_settings) {
                Navigation.findNavController(requireView()).navigate(R.id.nav_settings);
                return true;
            }
            if (item.getItemId() == R.id.pos_home) {
                return true;
            }
            if (item.getItemId() == R.id.pos_logout) {
                new UserRepository(requireContext().getApplicationContext()).logout();
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_pos, true)
                        .build();
                Navigation.findNavController(requireView()).navigate(R.id.nav_login, null, options);
                return true;
            }
            Toast.makeText(requireContext(), item.getTitle() + " (soon)", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void handleReportsClick() {
        String currentUser = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if ("Manager".equals(currentUser)) {
            Navigation.findNavController(requireView()).navigate(R.id.nav_reports);
        } else {
            Toast.makeText(requireContext(), R.string.reports_manager_only, Toast.LENGTH_SHORT).show();
            binding.navRail.setSelectedItemId(R.id.pos_home);
        }
    }

    private void handleInventoryClick() {
        String currentUser = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if ("Manager".equals(currentUser) || "Employee".equals(currentUser)) {
            Navigation.findNavController(requireView()).navigate(R.id.nav_inventory);
            return;
        }
        Toast.makeText(requireContext(), R.string.inventory_manager_only, Toast.LENGTH_SHORT).show();
        binding.navRail.setSelectedItemId(R.id.pos_home);
    }

    private void setupTabs() {
        TabLayout tabs = binding.tabsCategories;
        tabs.removeAllTabs();
        List<MenuCategory> cats = posVm.getCategories();
        for (MenuCategory c : cats) {
            tabs.addTab(tabs.newTab().setText(c.getName()));
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { posVm.setSelectedIndex(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        binding.searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                posVm.setQuery(s != null ? s.toString() : "");
            }
        });
    }

    private void setupProducts() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        int span;
        if (widthDp >= 1200) span = 4;
        else if (widthDp >= 900) span = 3;
        else span = 2;

        GridLayoutManager glm = new GridLayoutManager(requireContext(), span);
        binding.recyclerProducts.setLayoutManager(glm);
        ProductAdapter adapter = new ProductAdapter(this::onProductClick);
        binding.recyclerProducts.setAdapter(adapter);

        posVm.getFilteredItems().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    private void onProductClick(MenuItem item) {
        List<MenuItemVariant> vars = item.getVariants();
        if (vars == null || vars.isEmpty()) return;
        if (vars.size() == 1) {
            MenuItemVariant v = vars.get(0);
            cartVm.addItem(new CartItem(item.getId(), item.getName(), v.getLabel(), v.getPriceCents(), 1));
            Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
            return;
        }
        showVariantDialog(item, vars);
    }

    private void showVariantDialog(MenuItem item, List<MenuItemVariant> variants) {
        String[] labels = new String[variants.size()];
        for (int i = 0; i < variants.size(); i++) {
            MenuItemVariant v = variants.get(i);
            String priceStr = getString(R.string.price_format, v.getPriceCents() / 100);
            labels[i] = v.getLabel().isEmpty() ? priceStr : v.getLabel() + " — " + priceStr;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(labels, (dialog, which) -> {
                    MenuItemVariant v = variants.get(which);
                    cartVm.addItem(new CartItem(item.getId(), item.getName(), v.getLabel(), v.getPriceCents(), 1));
                    Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setupCart() {
        CartAdapter adapter = new CartAdapter(cartVm);
        binding.recyclerCart.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        binding.recyclerCart.setAdapter(adapter);

        cartVm.getItems().observe(getViewLifecycleOwner(), items -> {
            List<CartItem> list = items != null ? new ArrayList<>(items) : new ArrayList<>();
            adapter.submitList(list);

            boolean empty = list.isEmpty();
            if (!cartCollapsed) {
                binding.cartEmptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.recyclerCart.setVisibility(empty ? View.GONE : View.VISIBLE);
            } else {
                binding.cartEmptyMessage.setVisibility(View.GONE);
                binding.recyclerCart.setVisibility(View.GONE);
            }

            int totalCents = 0;
            for (CartItem c : list) totalCents += c.getTotalCents();
            binding.cartTotal.setText(getString(R.string.cart_total, totalCents / 100));
        });

        binding.btnClear.setOnClickListener(v -> cartVm.clear());
        binding.btnCheckout.setOnClickListener(v -> {
            List<CartItem> items = cartVm.getItems().getValue();
            if (items == null || items.isEmpty()) {
                Toast.makeText(requireContext(), R.string.cart_empty_checkout, Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(requireView()).navigate(R.id.nav_payment);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int prevIdx = posVm.getSelectedIndex().getValue() != null ? posVm.getSelectedIndex().getValue() : 0;
        posVm.refreshCategories();
        binding.tabsCategories.removeAllTabs();
        List<MenuCategory> cats = posVm.getCategories();
        for (MenuCategory c : cats) {
            binding.tabsCategories.addTab(binding.tabsCategories.newTab().setText(c.getName()));
        }
        if (!cats.isEmpty() && prevIdx < cats.size()) {
            if (binding.tabsCategories.getTabAt(prevIdx) != null) {
                binding.tabsCategories.getTabAt(prevIdx).select();
            }
            posVm.setSelectedIndex(prevIdx);
        }
    }

    private void setupCartToggle() {
        binding.btnCartToggle.setOnClickListener(v -> setCartCollapsed(!cartCollapsed));
        setCartCollapsed(false);
    }

    private void setCartCollapsed(boolean collapsed) {
        cartCollapsed = collapsed;
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) binding.guidelineCart.getLayoutParams();
        lp.guideEnd = getResources().getDimensionPixelSize(collapsed ? R.dimen.pos_cart_width_collapsed : R.dimen.pos_cart_width);
        binding.guidelineCart.setLayoutParams(lp);

        // Header visuals: avoid vertical "Your order" text in collapsed mode.
        binding.cartHeader.setVisibility(collapsed ? View.GONE : View.VISIBLE);

        binding.recyclerCart.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        binding.cartFooter.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        if (collapsed) {
            binding.cartEmptyMessage.setVisibility(View.GONE);
        }

        // Use the new custom round icons
        int iconRes = collapsed ? R.drawable.less_than_round_icon : R.drawable.greater_than_round_icon;
        binding.btnCartToggle.setIcon(ContextCompat.getDrawable(requireContext(), iconRes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.Holder> {
        private final OnProductClick listener;
        private List<MenuItem> list = new ArrayList<>();

        ProductAdapter(OnProductClick listener) { this.listener = listener; }

        void submitList(List<MenuItem> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPosProductBinding b = ItemPosProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            MenuItem item = list.get(position);
            h.name.setText(item.getName());

            // show min price for multi-variant items
            int min = Integer.MAX_VALUE;
            for (MenuItemVariant v : item.getVariants()) min = Math.min(min, v.getPriceCents());
            if (min == Integer.MAX_VALUE) min = 0;
            h.price.setText(h.itemView.getContext().getString(R.string.price_format, min / 100));

            h.image.setImageResource(item.getImageResId() != 0 ? item.getImageResId() : R.drawable.ic_food_placeholder);
            h.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name, price;
            Holder(ItemPosProductBinding b) {
                super(b.getRoot());
                image = b.productImage;
                name = b.productName;
                price = b.productPrice;
            }
        }

        interface OnProductClick { void onClick(MenuItem item); }
    }

    private static class CartAdapter extends RecyclerView.Adapter<CartAdapter.Holder> {
        private final CartViewModel vm;
        private List<CartItem> list = new ArrayList<>();

        CartAdapter(CartViewModel vm) { this.vm = vm; }

        void submitList(List<CartItem> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCartBinding b = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            CartItem item = list.get(position);
            h.line.setText(item.getDisplayLine());
            h.price.setText(h.itemView.getContext().getString(R.string.price_format, item.getTotalCents() / 100));
            h.qty.setText(String.valueOf(item.getQuantity()));
            h.btnMinus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() - 1));
            h.btnPlus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() + 1));
            h.btnRemove.setOnClickListener(v -> vm.removeItem(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView line, price, qty;
            final com.google.android.material.button.MaterialButton btnMinus, btnPlus;
            final android.widget.ImageButton btnRemove;
            Holder(ItemCartBinding b) {
                super(b.getRoot());
                line = b.cartItemLine;
                price = b.cartItemPrice;
                qty = b.cartItemQty;
                btnMinus = b.btnMinus;
                btnPlus = b.btnPlus;
                btnRemove = b.btnRemove;
            }
        }
    }
}
