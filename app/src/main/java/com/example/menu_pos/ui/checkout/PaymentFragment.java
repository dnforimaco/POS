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
import com.example.menu_pos.data.OrderStatus;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.data.InventoryRepository;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentPaymentBinding;
import com.example.menu_pos.ui.cart.CartViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentFragment extends Fragment {

    private FragmentPaymentBinding binding;
    private CartViewModel cartVm;

    private int totalCents = 0;
    private int cashReceivedCents = 0;
    private String selectedOrderType = "";
    private String selectedPaymentType = "";
    private String gcashRefLast4 = "";
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

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

        // Checkout no longer asks order type here; reuse what was already set on POS flow.
        selectedOrderType = resolveCheckoutOrderType(cartVm.getPendingOrderTypeValue());
        cartVm.setPendingOrderType(selectedOrderType);
        binding.paymentConfirm.setEnabled(true);

        // Default to Cash.
        binding.paymentMethodToggle.check(R.id.payment_method_cash);
        selectedPaymentType = getString(R.string.payment_type_cash);
        applyPaymentMethodUi();

        binding.paymentMethodToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.payment_method_cash) {
                selectedPaymentType = getString(R.string.payment_type_cash);
            } else if (checkedId == R.id.payment_method_gcash) {
                selectedPaymentType = getString(R.string.payment_type_gcash);
            }
            applyPaymentMethodUi();
        });

        binding.paymentCashInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isCashSelected()) return;
                cashReceivedCents = parseCashToCents(s != null ? s.toString() : "");
                int change = Math.max(0, cashReceivedCents - totalCents);
                binding.paymentChangeValue.setText(formatPesos(change));
            }
        });

        binding.paymentGcashRefInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isCashSelected()) return;
                gcashRefLast4 = s != null ? s.toString().trim() : "";
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
        int change;
        if (isCashSelected()) {
            if (cashReceivedCents < totalCents) {
                Toast.makeText(requireContext(), R.string.payment_invalid_cash, Toast.LENGTH_SHORT).show();
                return;
            }
            change = cashReceivedCents - totalCents;
        } else {
            // Validate GCash reference digits.
            if (!isValidGcashRef(gcashRefLast4)) {
                Toast.makeText(requireContext(), R.string.payment_gcash_ref_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            // Non-cash: treat as exact payment.
            cashReceivedCents = totalCents;
            change = 0;
        }

        String cashier = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if (cashier == null) cashier = "";

        long now = System.currentTimeMillis();
        String notes = cartVm.getOrderNotesValue();

        binding.paymentConfirm.setEnabled(false);
        List<CartItem> safeItems = new ArrayList<>(items);
        String safeCashier = cashier;
        int safeTotalCents = totalCents;
        int safeCashReceivedCents = cashReceivedCents;
        String safeOrderType = selectedOrderType;
        String safePaymentType = selectedPaymentType;
        String safeGcashRef = gcashRefLast4;
        ioExecutor.execute(() -> {
            long orderId = 0;
            String lowStockMessage = null;
            try {
                List<PaidOrderLineEntity> lines = PaidOrderRepository.buildLinesFromCart(safeItems);
                PaidOrderRepository repo = new PaidOrderRepository(requireContext().getApplicationContext());
                Long pendingId = cartVm.getActivePendingOrderIdValue();

                if (pendingId != null && pendingId > 0) {
                    PaidOrderEntity existing = repo.getOrderById(pendingId);
                    if (existing != null && existing.orderStatus == OrderStatus.PENDING) {
                        PaidOrderEntity order = new PaidOrderEntity(
                                now,
                                safeTotalCents,
                                safeOrderType,
                                safePaymentType,
                                safeCashier,
                                existing.tableNumber != null ? existing.tableNumber : "",
                                safeCashReceivedCents,
                                change,
                                safeGcashRef
                        );
                        order.orderNotes = notes;
                        repo.finalizePendingOrderAsPaid(pendingId, order, lines);
                        orderId = pendingId;
                    } else {
                        PaidOrderEntity order = new PaidOrderEntity(
                                now,
                                safeTotalCents,
                                safeOrderType,
                                safePaymentType,
                                safeCashier,
                                "",
                                safeCashReceivedCents,
                                change,
                                safeGcashRef
                        );
                        order.orderNotes = notes;
                        order.orderStatus = OrderStatus.PAID;
                        orderId = repo.savePaidOrder(order, lines);
                    }
                } else {
                    PaidOrderEntity order = new PaidOrderEntity(
                            now,
                            safeTotalCents,
                            safeOrderType,
                            safePaymentType,
                            safeCashier,
                            "",
                            safeCashReceivedCents,
                            change,
                            safeGcashRef
                    );
                    order.orderNotes = notes;
                    order.orderStatus = OrderStatus.PAID;
                    orderId = repo.savePaidOrder(order, lines);
                }

                try {
                    InventoryRepository invRepo = new InventoryRepository(requireContext().getApplicationContext());
                    List<InventoryRepository.LowStockWarning> warnings = invRepo.deductForSale(safeItems, orderId, safeCashier);
                    if (warnings != null && !warnings.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Low stock: ");
                        for (int i = 0; i < warnings.size(); i++) {
                            InventoryRepository.LowStockWarning w = warnings.get(i);
                            if (w == null) continue;
                            if (i > 0) sb.append(", ");
                            sb.append(w.itemName).append(" (").append(w.stockQty).append(")");
                        }
                        lowStockMessage = sb.toString();
                    }
                } catch (Exception ignored) {
                    // Keep checkout flow working even if inventory is not configured.
                }

                long finalOrderId = orderId;
                String finalLowStockMessage = lowStockMessage;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || binding == null) return;
                        binding.paymentConfirm.setEnabled(true);
                        if (finalOrderId <= 0) return;
                        cartVm.clear();
                        if (finalLowStockMessage != null && !finalLowStockMessage.isEmpty()) {
                            Toast.makeText(requireContext(), finalLowStockMessage, Toast.LENGTH_LONG).show();
                        }
                        Bundle args = new Bundle();
                        args.putLong("orderId", finalOrderId);
                        args.putInt("totalCents", safeTotalCents);
                        args.putInt("cashCents", safeCashReceivedCents);
                        args.putInt("changeCents", change);
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_payment, true)
                                .build();
                        Navigation.findNavController(requireView()).navigate(R.id.nav_payment_confirmation, args, navOptions);
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || binding == null) return;
                        binding.paymentConfirm.setEnabled(true);
                        Toast.makeText(requireContext(), R.string.payment_process_failed, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
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

    private boolean isCashSelected() {
        return getString(R.string.payment_type_cash).equals(selectedPaymentType);
    }

    private String resolveCheckoutOrderType(@Nullable String rawType) {
        String type = rawType != null ? rawType.trim() : "";
        if (type.equalsIgnoreCase(getString(R.string.payment_order_type_take_out))
                || type.equalsIgnoreCase("takeout")) {
            return getString(R.string.payment_order_type_take_out);
        }
        return getString(R.string.payment_order_type_dine_in);
    }

    private void applyPaymentMethodUi() {
        if (binding == null) return;
        boolean cash = isCashSelected();
        binding.paymentCashLayout.setVisibility(cash ? View.VISIBLE : View.GONE);
        if (!cash) {
            // Clear cash input when not needed; keep change at 0.
            binding.paymentCashInput.setText("");
            cashReceivedCents = totalCents;
            binding.paymentChangeValue.setText(formatPesos(0));
        }
        binding.paymentGcashLayout.setVisibility(cash ? View.GONE : View.VISIBLE);
        if (cash) {
            binding.paymentGcashRefInput.setText("");
            gcashRefLast4 = "";
        }
    }

    private boolean isValidGcashRef(String ref) {
        if (ref == null) return false;
        String s = ref.trim();
        if (s.length() != 4) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        ioExecutor.shutdown();
        super.onDestroy();
    }
}

