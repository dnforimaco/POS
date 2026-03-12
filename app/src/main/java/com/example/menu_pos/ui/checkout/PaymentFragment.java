package com.example.menu_pos.ui.checkout;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.menu_pos.R;
import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.data.InventoryRepository;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentPaymentBinding;
import com.example.menu_pos.ui.cart.CartViewModel;

import java.util.ArrayList;
import java.util.List;

public class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding;
    private CartViewModel cartVm;

    private int totalCents = 0;
    private int cashReceivedCents = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaymentBinding.inflate(inflater, container, false);
        cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.paymentBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        computeTotalFromCart();
        binding.paymentTotalValue.setText(formatPesos(totalCents));
        binding.paymentChangeValue.setText(formatPesos(0));

        binding.paymentCashInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                cashReceivedCents = parseCashToCents(s != null ? s.toString() : "");
                int change = Math.max(0, cashReceivedCents - totalCents);
                binding.paymentChangeValue.setText(formatPesos(change));
            }
        });

        binding.paymentConfirm.setOnClickListener(v -> confirmPayment());
    }

    private void computeTotalFromCart() {
        List<CartItem> items = cartVm.getItems().getValue();
        totalCents = 0;
        if (items != null) {
            for (CartItem c : items) totalCents += c.getTotalCents();
        }
    }

    private void confirmPayment() {
        List<CartItem> items = cartVm.getItems().getValue();
        if (items == null || items.isEmpty()) {
            Toast.makeText(requireContext(), R.string.cart_empty_checkout, Toast.LENGTH_SHORT).show();
            return;
        }

        computeTotalFromCart();
        if (cashReceivedCents < totalCents) {
            Toast.makeText(requireContext(), R.string.payment_invalid_cash, Toast.LENGTH_SHORT).show();
            return;
        }

        int change = cashReceivedCents - totalCents;
        String cashier = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if (cashier == null) cashier = "";

        long now = System.currentTimeMillis();
        PaidOrderEntity order = new PaidOrderEntity(
                now,
                totalCents,
                getString(R.string.payment_type_cash),
                cashier,
                cashReceivedCents,
                change
        );

        List<PaidOrderLineEntity> lines = new ArrayList<>();
        for (CartItem c : items) {
            lines.add(new PaidOrderLineEntity(
                    0,
                    c.getItemName(),
                    c.getVariantLabel(),
                    c.getUnitPriceCents(),
                    c.getQuantity()
            ));
        }

        PaidOrderRepository repo = new PaidOrderRepository(requireContext().getApplicationContext());
        long orderId = repo.savePaidOrder(order, lines);

        // Deduct inventory for completed order (if recipe mappings are configured).
        try {
            InventoryRepository invRepo = new InventoryRepository(requireContext().getApplicationContext());
            List<InventoryRepository.LowStockWarning> warnings = invRepo.deductForSale(items, orderId, cashier);
            if (warnings != null && !warnings.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Low stock: ");
                for (int i = 0; i < warnings.size(); i++) {
                    InventoryRepository.LowStockWarning w = warnings.get(i);
                    if (w == null) continue;
                    if (i > 0) sb.append(", ");
                    sb.append(w.itemName).append(" (").append(w.stockQty).append(")");
                }
                Toast.makeText(requireContext(), sb.toString(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ignored) {
            // Keep checkout flow working even if inventory is not configured.
        }

        cartVm.clear();

        Bundle args = new Bundle();
        args.putLong("orderId", orderId);
        args.putInt("totalCents", totalCents);
        args.putInt("cashCents", cashReceivedCents);
        args.putInt("changeCents", change);

        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_payment, true)
                .build();
        Navigation.findNavController(requireView()).navigate(R.id.nav_payment_confirmation, args, navOptions);
    }

    private static String formatPesos(int cents) {
        return "₱" + (cents / 100);
    }

    private static int parseCashToCents(String input) {
        try {
            String s = input != null ? input.trim() : "";
            if (s.isEmpty()) return 0;
            // pesos only (integer). store as cents
            int pesos = Integer.parseInt(s);
            return Math.max(0, pesos) * 100;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

