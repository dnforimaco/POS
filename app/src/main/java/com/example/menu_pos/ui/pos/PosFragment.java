package com.example.menu_pos.ui.pos;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.data.MenuCategory;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.data.MenuItem;
import com.example.menu_pos.data.MenuItemVariant;
import com.example.menu_pos.databinding.FragmentPosBinding;
import com.example.menu_pos.databinding.ItemCartBinding;
import com.example.menu_pos.databinding.ItemPosProductBinding;
import com.example.menu_pos.printer.KitchenSlipPrint;
import com.example.menu_pos.ui.VariantPickerDialogHelper;
import com.example.menu_pos.ui.cart.CartViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PosFragment extends Fragment {

    private static final String COMBO_CUSTOM_ITEM_ID = "combo_custom";
    private static final String OTHERS_CUSTOM_ITEM_ID = "others_custom";

    private FragmentPosBinding binding;
    private PosViewModel posVm;
    private CartViewModel cartVm;
    private boolean cartCollapsed = false;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ProductImageLoader productImageLoader = new ProductImageLoader();
    private String selectedKitchenOrderType = "";
    private String selectedKitchenTableNumber = "";

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
        posVm.getGlobalSearchActive().observe(getViewLifecycleOwner(), active ->
                binding.searchScopeBanner.setVisibility(Boolean.TRUE.equals(active) ? View.VISIBLE : View.GONE));
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
        if ("manager".equalsIgnoreCase(currentUser)) {
            Navigation.findNavController(requireView()).navigate(R.id.nav_reports);
        } else {
            Toast.makeText(requireContext(), R.string.reports_manager_only, Toast.LENGTH_SHORT).show();
            binding.navRail.setSelectedItemId(R.id.pos_home);
        }
    }

    private void handleInventoryClick() {
        String currentUser = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if ("manager".equalsIgnoreCase(currentUser) || "employee".equalsIgnoreCase(currentUser)) {
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
        prewarmCategoryImages(0);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                posVm.setSelectedIndex(pos);
                prewarmCategoryImages(pos);
            }
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
        binding.recyclerProducts.setHasFixedSize(true);
        binding.recyclerProducts.setItemAnimator(null);
        ProductAdapter adapter = new ProductAdapter(this::onProductClick, productImageLoader);
        binding.recyclerProducts.setAdapter(adapter);

        posVm.getFilteredRows().observe(getViewLifecycleOwner(),
                rows -> adapter.submitList(rows != null ? rows : Collections.emptyList()));
    }

    private void prewarmCategoryImages(int centerCategoryIndex) {
        if (!isAdded()) return;
        List<MenuCategory> cats = posVm.getCategories();
        if (cats == null || cats.isEmpty()) return;
        int safeIdx = Math.max(0, Math.min(centerCategoryIndex, cats.size() - 1));
        List<Integer> ids = new ArrayList<>();
        collectCategoryImageResIds(cats.get(safeIdx), ids);
        if (safeIdx - 1 >= 0) collectCategoryImageResIds(cats.get(safeIdx - 1), ids);
        if (safeIdx + 1 < cats.size()) collectCategoryImageResIds(cats.get(safeIdx + 1), ids);
        productImageLoader.preload(requireContext().getApplicationContext(), ids, R.drawable.ic_food_placeholder);
    }

    private static void collectCategoryImageResIds(@Nullable MenuCategory category, List<Integer> out) {
        if (category == null || out == null) return;
        List<MenuItem> items = category.getItems();
        if (items == null) return;
        for (MenuItem item : items) {
            if (item == null) continue;
            int res = item.getImageResId();
            out.add(res != 0 ? res : R.drawable.ic_food_placeholder);
        }
    }

    private void onProductClick(MenuItem item) {
        if (COMBO_CUSTOM_ITEM_ID.equals(item.getId())) {
            showCustomComboDialog(item);
            return;
        }
        if (OTHERS_CUSTOM_ITEM_ID.equals(item.getId())) {
            showCustomOthersDialog(item);
            return;
        }
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

    /** Combo Sizz: user-entered pairing at ₱179; text is stored as the cart line variant label. */
    private void showCustomComboDialog(MenuItem item) {
        if (!isAdded()) return;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_combo_custom, null, false);
        TextView priceTv = content.findViewById(R.id.combo_custom_price);
        TextInputEditText input = content.findViewById(R.id.combo_custom_input);
        List<MenuItemVariant> vars = item.getVariants();
        final int priceCents = (vars != null && !vars.isEmpty())
                ? vars.get(0).getPriceCents()
                : 17900;
        priceTv.setText(getString(R.string.price_format, priceCents / 100));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.combo_custom_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.pos_variant_tap_to_add, null)
                .setNegativeButton(R.string.pos_variant_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                CharSequence cs = input.getText();
                String text = cs != null ? cs.toString().trim() : "";
                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.combo_custom_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                cartVm.addItem(new CartItem(item.getId(), item.getName(), text, priceCents, 1));
                Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            input.post(() -> {
                input.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            });
        });
        dialog.show();
    }

    /** Others: user-entered line item with custom price in pesos. */
    private void showCustomOthersDialog(MenuItem item) {
        if (!isAdded()) return;
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_others_custom, null, false);
        TextInputEditText nameInput = content.findViewById(R.id.others_custom_name);
        TextInputEditText priceInput = content.findViewById(R.id.others_custom_price);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.others_custom_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.pos_variant_tap_to_add, null)
                .setNegativeButton(R.string.pos_variant_cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String priceStr = priceInput.getText() != null ? priceInput.getText().toString().trim() : "";
                int pesos = -1;
                try { pesos = Integer.parseInt(priceStr); } catch (Exception ignored) {}
                if (name.isEmpty() || pesos <= 0) {
                    Toast.makeText(requireContext(), R.string.others_custom_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                int priceCents = pesos * 100;
                // Use a unique id per custom line so different custom items don't merge.
                String lineId = OTHERS_CUSTOM_ITEM_ID + ":" + System.currentTimeMillis();
                cartVm.addItem(new CartItem(lineId, name, "", priceCents, 1));
                Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            nameInput.post(() -> {
                nameInput.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
            });
        });
        dialog.show();
    }

    private void showVariantDialog(MenuItem item, List<MenuItemVariant> variants) {
        VariantPickerDialogHelper.show(requireContext(), item, variants, v -> {
            cartVm.addItem(new CartItem(item.getId(), item.getName(), v.getLabel(), v.getPriceCents(), 1));
            Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCart() {
        CartAdapter adapter = new CartAdapter(cartVm);
        binding.recyclerCart.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        binding.recyclerCart.setItemAnimator(null);
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

        cartVm.getActivePendingOrderId().observe(getViewLifecycleOwner(), id -> {
            if (id != null && id > 0) {
                binding.cartPendingBanner.setVisibility(View.VISIBLE);
                binding.cartPendingBanner.setText(getString(R.string.pos_editing_order, id.intValue()));
                binding.btnSendKitchen.setText(R.string.pos_save_order);
            } else {
                binding.cartPendingBanner.setVisibility(View.GONE);
                binding.btnSendKitchen.setText(R.string.pos_send_to_kitchen);
            }
        });

        applyKitchenOrderTypeSelection(cartVm.getPendingOrderTypeValue());
        cartVm.getPendingOrderType().observe(getViewLifecycleOwner(), this::applyKitchenOrderTypeSelection);
        cartVm.getPendingTableNumber().observe(getViewLifecycleOwner(), this::applyKitchenTableNumberSelection);
        applyKitchenTableNumberSelection(cartVm.getPendingTableNumberValue());

        binding.btnSendKitchen.setOnClickListener(v -> openKitchenOrderDialog());

        binding.btnClear.setOnClickListener(v -> cartVm.clear());
    }

    private void openKitchenOrderDialog() {
        if (!isAdded()) return;
        KitchenOrderDialogHelper.show(
                requireContext(),
                selectedKitchenOrderType,
                selectedKitchenTableNumber,
                cartVm.getOrderNotesValue(),
                (orderType, tableNumber, orderNotes) -> {
                    selectedKitchenOrderType = orderType;
                    selectedKitchenTableNumber = tableNumber;
                    cartVm.setPendingOrderType(orderType);
                    cartVm.setPendingTableNumber(tableNumber);
                    cartVm.setOrderNotes(orderNotes);
                    sendToKitchen();
                }
        );
    }

    /**
     * Persists the cart as an unpaid order, then prints the kitchen slip (when auto-print is on).
     */
    private void sendToKitchen() {
        List<CartItem> items = cartVm.getItems().getValue();
        if (items == null || items.isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_empty_checkout, Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = cartVm.getOrderNotesValue();
        String cashier = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        String orderType = selectedKitchenOrderType != null ? selectedKitchenOrderType.trim() : "";
        if (orderType.isEmpty()) {
            Toast.makeText(requireContext(), R.string.pos_select_order_type, Toast.LENGTH_SHORT).show();
            return;
        }
        String tableNumber = selectedKitchenTableNumber != null ? selectedKitchenTableNumber.trim() : "";
        if (isKitchenDineInSelected() && tableNumber.isEmpty()) {
            Toast.makeText(requireContext(), R.string.pos_table_required, Toast.LENGTH_SHORT).show();
            return;
        }
        Long pid = cartVm.getActivePendingOrderIdValue();
        binding.btnSendKitchen.setEnabled(false);
        List<CartItem> safeItems = new ArrayList<>(items);
        String safeOrderType = orderType;
        String safeTableNumber = tableNumber;
        ioExecutor.execute(() -> {
            try {
                PaidOrderRepository repo = new PaidOrderRepository(requireContext().getApplicationContext());
                long orderId;
                if (pid != null && pid > 0) {
                    List<PaidOrderLineEntity> previousLines = repo.getLinesForOrder(pid);
                    repo.updatePendingOrderFromCart(pid, safeItems, notes, cashier, safeOrderType, safeTableNumber);
                    orderId = pid;
                    List<PaidOrderLineEntity> additionalLines = computeAdditionalLines(previousLines, safeItems);
                    PaidOrderEntity order = repo.getOrderById(orderId);
                    mainHandler.post(() -> {
                        if (!isAdded() || binding == null) return;
                        binding.btnSendKitchen.setEnabled(true);
                        if (order == null) return;
                        if (pid == null || pid <= 0) cartVm.setActivePendingOrderId(orderId);
                        if (!additionalLines.isEmpty()) {
                            KitchenSlipPrint.printAdditionalItems(requireContext(), order, additionalLines);
                            Toast.makeText(requireContext(), R.string.kitchen_additional_items_printed, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), R.string.kitchen_order_saved_no_additional_items, Toast.LENGTH_SHORT).show();
                        }
                        cartVm.clear();
                    });
                    return;
                } else {
                    orderId = repo.createPendingOrderFromCart(safeItems, notes, cashier, safeOrderType, safeTableNumber);
                    if (orderId <= 0) {
                        mainHandler.post(() -> {
                            if (binding != null) binding.btnSendKitchen.setEnabled(true);
                        });
                        return;
                    }
                }
                PaidOrderEntity order = repo.getOrderById(orderId);
                List<PaidOrderLineEntity> lines = repo.getLinesForOrder(orderId);
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    binding.btnSendKitchen.setEnabled(true);
                    if (order == null) return;
                    if (pid == null || pid <= 0) cartVm.setActivePendingOrderId(orderId);
                    KitchenSlipPrint.print(requireContext(), order, lines);
                    cartVm.clear();
                    Toast.makeText(requireContext(), R.string.kitchen_slip_saved, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    binding.btnSendKitchen.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.kitchen_slip_save_failed, Toast.LENGTH_SHORT).show();
                });
            }
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

    private static List<PaidOrderLineEntity> computeAdditionalLines(List<PaidOrderLineEntity> previousLines, List<CartItem> currentItems) {
        Map<String, Integer> oldQtyByKey = new HashMap<>();
        if (previousLines != null) {
            for (PaidOrderLineEntity l : previousLines) {
                if (l == null) continue;
                String key = lineKey(l.itemName, l.variantLabel, l.unitPriceCents);
                int qty = oldQtyByKey.containsKey(key) ? oldQtyByKey.get(key) : 0;
                oldQtyByKey.put(key, qty + Math.max(0, l.quantity));
            }
        }
        List<PaidOrderLineEntity> additions = new ArrayList<>();
        if (currentItems == null) return additions;
        for (CartItem c : currentItems) {
            if (c == null) continue;
            String key = lineKey(c.getItemName(), c.getVariantLabel(), c.getUnitPriceCents());
            int oldQty = oldQtyByKey.containsKey(key) ? oldQtyByKey.get(key) : 0;
            int newQty = Math.max(0, c.getQuantity());
            int addQty = newQty - oldQty;
            if (addQty <= 0) continue;

            PaidOrderLineEntity l = new PaidOrderLineEntity();
            l.itemName = c.getItemName() != null ? c.getItemName() : "";
            l.variantLabel = c.getVariantLabel() != null ? c.getVariantLabel() : "";
            l.unitPriceCents = c.getUnitPriceCents();
            l.quantity = addQty;
            l.sourceItemId = c.getItemId() != null ? c.getItemId() : "";
            l.discountAmountCents = 0;
            l.lineTotalCents = addQty * c.getUnitPriceCents();
            additions.add(l);
        }
        return additions;
    }

    private static String lineKey(String itemName, String variantLabel, int unitPriceCents) {
        String n = itemName != null ? itemName.trim() : "";
        String v = variantLabel != null ? variantLabel.trim() : "";
        return n + "|" + v + "|" + unitPriceCents;
    }

    private void applyKitchenOrderTypeSelection(@Nullable String rawType) {
        String type = rawType != null ? rawType.trim() : "";
        String normalized = type.toLowerCase(Locale.ROOT);
        if (normalized.equals("take out") || normalized.equals("takeout")) {
            selectedKitchenOrderType = getString(R.string.payment_order_type_take_out);
            return;
        }
        // Default to Dine In for new orders or unknown values.
        selectedKitchenOrderType = getString(R.string.payment_order_type_dine_in);
    }

    private void applyKitchenTableNumberSelection(@Nullable String tableNumberRaw) {
        String table = tableNumberRaw != null ? tableNumberRaw.trim() : "";
        selectedKitchenTableNumber = table;
    }

    private boolean isKitchenDineInSelected() {
        return getString(R.string.payment_order_type_dine_in).equals(selectedKitchenOrderType);
    }

    private void setCartCollapsed(boolean collapsed) {
        if (binding != null) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(170);
            TransitionManager.beginDelayedTransition(binding.getRoot(), transition);
        }
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

    @Override
    public void onDestroy() {
        productImageLoader.shutdown();
        ioExecutor.shutdown();
        super.onDestroy();
    }

    private static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.Holder> {
        private final OnProductClick listener;
        private final ProductImageLoader imageLoader;
        private List<PosGridRow> list = Collections.emptyList();

        ProductAdapter(OnProductClick listener, ProductImageLoader imageLoader) {
            this.listener = listener;
            this.imageLoader = imageLoader;
            setHasStableIds(true);
        }

        void submitList(List<PosGridRow> rows) {
            this.list = rows != null ? rows : Collections.emptyList();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPosProductBinding b = ItemPosProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            PosGridRow row = list.get(position);
            MenuItem item = row.getItem();
            h.name.setText(item.getName());
            String cat = row.getCategoryName();
            if (cat != null && !cat.isEmpty()) {
                h.category.setText(cat);
                h.category.setVisibility(View.VISIBLE);
            } else {
                h.category.setVisibility(View.GONE);
            }

            int displayCents = item.getCardDisplayPriceCents();
            h.price.setText(h.itemView.getContext().getString(R.string.price_format, displayCents / 100));

            int imageRes = item.getImageResId();
            if (imageRes == 0) {
                imageRes = R.drawable.ic_food_placeholder;
            }
            imageLoader.load(h.image, imageRes, R.drawable.ic_food_placeholder);
            h.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public long getItemId(int position) {
            if (position < 0 || position >= list.size()) return RecyclerView.NO_ID;
            PosGridRow row = list.get(position);
            String itemId = row.getItem() != null && row.getItem().getId() != null ? row.getItem().getId() : "";
            String cat = row.getCategoryName() != null ? row.getCategoryName() : "";
            return (itemId + "|" + cat).hashCode();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name, category, price;
            Holder(ItemPosProductBinding b) {
                super(b.getRoot());
                image = b.productImage;
                name = b.productName;
                category = b.productCategory;
                price = b.productPrice;
            }
        }

        interface OnProductClick { void onClick(MenuItem item); }
    }

    /**
     * Small async image loader for product cards.
     * Converts drawables to bitmaps once and reuses them to avoid repeated rasterization on tab switches.
     */
    private static class ProductImageLoader {
        private final LruCache<Integer, Bitmap> bitmapCache;
        private final ExecutorService decodeExecutor = Executors.newFixedThreadPool(2);
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        ProductImageLoader() {
            final int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
            final int cacheKb = Math.max(2 * 1024, maxKb / 16);
            bitmapCache = new LruCache<Integer, Bitmap>(cacheKb) {
                @Override
                protected int sizeOf(@NonNull Integer key, @NonNull Bitmap value) {
                    return value.getByteCount() / 1024;
                }
            };
        }

        void load(ImageView target, int resId, int fallbackResId) {
            int safeRes = resId != 0 ? resId : fallbackResId;
            target.setTag(safeRes);

            Bitmap cached = bitmapCache.get(safeRes);
            if (cached != null && !cached.isRecycled()) {
                target.setImageBitmap(cached);
                return;
            }

            target.setImageResource(fallbackResId);
            final android.content.Context appContext = target.getContext().getApplicationContext();
            final int sizePx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    96,
                    target.getResources().getDisplayMetrics()
            );

            decodeExecutor.execute(() -> {
                Bitmap rendered = renderDrawableBitmap(appContext, safeRes, sizePx, sizePx);
                if (rendered == null) return;
                bitmapCache.put(safeRes, rendered);
                mainHandler.post(() -> {
                    Object tag = target.getTag();
                    if (!(tag instanceof Integer) || ((Integer) tag) != safeRes) return;
                    target.setImageBitmap(rendered);
                });
            });
        }

        void preload(android.content.Context context, List<Integer> resIds, int fallbackResId) {
            if (context == null || resIds == null || resIds.isEmpty()) return;
            final android.content.Context appContext = context.getApplicationContext();
            final int sizePx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    96,
                    appContext.getResources().getDisplayMetrics()
            );
            for (Integer id : resIds) {
                final int safeRes = (id != null && id != 0) ? id : fallbackResId;
                Bitmap cached = bitmapCache.get(safeRes);
                if (cached != null && !cached.isRecycled()) continue;
                decodeExecutor.execute(() -> {
                    Bitmap rendered = renderDrawableBitmap(appContext, safeRes, sizePx, sizePx);
                    if (rendered == null) return;
                    bitmapCache.put(safeRes, rendered);
                });
            }
        }

        @Nullable
        private static Bitmap renderDrawableBitmap(android.content.Context context, int resId, int w, int h) {
            try {
                Drawable drawable = ContextCompat.getDrawable(context, resId);
                if (drawable == null) return null;
                Bitmap bitmap = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            } catch (Exception ignored) {
                return null;
            }
        }

        void shutdown() {
            decodeExecutor.shutdown();
            bitmapCache.evictAll();
        }
    }

    private static class CartAdapter extends RecyclerView.Adapter<CartAdapter.Holder> {
        private final CartViewModel vm;
        private List<CartItem> list = new ArrayList<>();

        CartAdapter(CartViewModel vm) {
            this.vm = vm;
        }

        void submitList(List<CartItem> list) {
            this.list = list != null ? new ArrayList<>(list) : new ArrayList<>();
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
            android.content.Context ctx = h.itemView.getContext();
            if (item.isApplyTenPercentDiscount()) {
                String cur = ctx.getString(R.string.price_format, item.getLineTotalCents() / 100);
                String was = ctx.getString(R.string.price_format, item.getSubtotalCents() / 100);
                h.price.setText(ctx.getString(R.string.cart_line_with_discount, cur, was));
            } else {
                h.price.setText(ctx.getString(R.string.price_format, item.getTotalCents() / 100));
            }
            h.qty.setText(String.valueOf(item.getQuantity()));
            h.discountCb.setOnCheckedChangeListener(null);
            h.discountCb.setChecked(item.isApplyTenPercentDiscount());
            h.discountCb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = h.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                vm.setLineDiscountAt(pos, isChecked);
            });
            h.btnMinus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() - 1));
            h.btnPlus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() + 1));
            h.btnRemove.setOnClickListener(v -> vm.removeItem(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView line, price, qty;
            final com.google.android.material.checkbox.MaterialCheckBox discountCb;
            final com.google.android.material.button.MaterialButton btnMinus, btnPlus;
            final android.widget.ImageButton btnRemove;
            Holder(ItemCartBinding b) {
                super(b.getRoot());
                line = b.cartItemLine;
                price = b.cartItemPrice;
                qty = b.cartItemQty;
                discountCb = b.cartItemDiscount;
                btnMinus = b.btnMinus;
                btnPlus = b.btnPlus;
                btnRemove = b.btnRemove;
            }
        }
    }
}
