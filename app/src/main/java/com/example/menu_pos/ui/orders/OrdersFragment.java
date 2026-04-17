package com.example.menu_pos.ui.orders;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.print.PrintManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.OrderStatus;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentOrdersBinding;
import com.example.menu_pos.printer.PrinterManager;
import com.example.menu_pos.printer.PrinterPrefs;
import com.example.menu_pos.printer.ReceiptTextFormatter;
import com.example.menu_pos.printer.KitchenSlipPrint;
import com.example.menu_pos.ui.checkout.PdfBytesPrintAdapter;
import com.example.menu_pos.ui.checkout.ReceiptPrinter;
import com.example.menu_pos.ui.cart.CartViewModel;
import com.example.menu_pos.databinding.ItemOrderBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrdersAdapter adapter;
    private PaidOrderRepository paidOrderRepo;
    private boolean isManager = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        paidOrderRepo = new PaidOrderRepository(requireContext().getApplicationContext());
        String user = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        isManager = "manager".equalsIgnoreCase(user);

        binding.ordersBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        adapter = new OrdersAdapter(
                new ArrayList<>(),
                isManager,
                this::onOrderRowClick,
                this::confirmDeleteOne,
                this::onSelectionChanged,
                this::payOrder,
                this::printBillOutReceipt,
                this::openPendingInPos,
                this::showOrderDetails,
                this::reprintKitchenSlip
        );
        binding.ordersList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.ordersList.setHasFixedSize(true);
        binding.ordersList.setAdapter(adapter);

        binding.ordersDelete.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.ordersDelete.setOnClickListener(v -> confirmDeleteSelected());

        binding.ordersSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (adapter != null) {
                adapter.selectAll(isChecked);
            }
        });

        viewModel.getOrders().observe(getViewLifecycleOwner(), list -> {
            List<PaidOrderEntity> safe = list != null ? new ArrayList<>(list) : new ArrayList<>();
            List<Long> ids = new ArrayList<>(safe.size());
            for (PaidOrderEntity order : safe) ids.add(order.id);
            Map<Long, Integer> itemCounts = ids.isEmpty()
                    ? Collections.emptyMap()
                    : paidOrderRepo.getItemCountsForOrders(ids);
            adapter.submitList(safe, itemCounts);
            boolean empty = safe.isEmpty();
            binding.ordersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.ordersList.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }

    private void onOrderRowClick(PaidOrderEntity order) {
        if (order == null) return;
        // Always open details directly (including unpaid orders).
        showOrderDetails(order);
    }

    private void showPendingOrderActions(PaidOrderEntity order) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_order_actions, null, false);
        View openPosButton = dialogView.findViewById(R.id.actionOpenPos);
        View detailsButton = dialogView.findViewById(R.id.actionViewDetails);
        View reprintButton = dialogView.findViewById(R.id.actionReprintKitchen);
        View billOutButton = dialogView.findViewById(R.id.actionBillOutReceipt);
        TextView titleText = dialogView.findViewById(R.id.orderActionsTitle);
        TextView cancelText = dialogView.findViewById(R.id.actionCancel);
        titleText.setText(getString(R.string.orders_pending_actions_title, String.valueOf(order.id)));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        openPosButton.setOnClickListener(v -> {
            dialog.dismiss();
            openPendingInPos(order);
        });
        detailsButton.setOnClickListener(v -> {
            dialog.dismiss();
            showOrderDetails(order);
        });
        reprintButton.setOnClickListener(v -> {
            dialog.dismiss();
            reprintKitchenSlip(order);
        });
        billOutButton.setOnClickListener(v -> {
            dialog.dismiss();
            printBillOutReceipt(order);
        });
        cancelText.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void openPendingInPos(PaidOrderEntity order) {
        List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(order.id);
        CartViewModel cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        cartVm.loadPendingOrder(order, lines);
        Navigation.findNavController(requireView()).navigate(R.id.nav_pos);
    }

    private void reprintKitchenSlip(PaidOrderEntity order) {
        if (order == null || order.orderStatus != OrderStatus.PENDING) {
            Toast.makeText(requireContext(), R.string.kitchen_reprint_pending_only, Toast.LENGTH_SHORT).show();
            return;
        }
        List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(order.id);
        KitchenSlipPrint.print(requireContext(), order, lines);
    }

    private void printBillOutReceipt(PaidOrderEntity order) {
        if (order == null) return;
        List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(order.id);
        Toast.makeText(requireContext(), R.string.orders_bill_out_printing, Toast.LENGTH_SHORT).show();
        try {
            String mac = PrinterPrefs.getPrinterMac(requireContext());
            if (mac != null && !mac.isEmpty()) {
                String receipt = ReceiptTextFormatter.buildBillOutReceipt(order, lines);
                PrinterManager pm = new PrinterManager(requireContext().getApplicationContext());
                pm.connectIfNeededAndPrint(mac, receipt, success -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    success ? "Bill out receipt print sent." : getString(R.string.printer_not_connected_retry),
                                    Toast.LENGTH_SHORT).show());
                });
                return;
            }
            byte[] pdf = ReceiptPrinter.buildBillOutReceiptPdfBytes(requireContext(), order, lines);
            PrintManager printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);
            if (printManager == null) return;
            String jobName = "BillOut_" + order.id;
            printManager.print(jobName, new PdfBytesPrintAdapter(jobName + ".pdf", pdf), null);
        } catch (Exception ignored) {
        }
    }

    private void showOrderDetails(PaidOrderEntity order) {
        if (order == null) return;
        List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(order.id);
        StringBuilder itemsText = new StringBuilder();
        int discountTotal = 0;
        for (PaidOrderLineEntity line : lines) {
            itemsText.append("• ").append(line.itemName);
            if (line.variantLabel != null && !line.variantLabel.isEmpty()) {
                itemsText.append(" (").append(line.variantLabel).append(")");
            }
            itemsText.append(" x").append(line.quantity);
            itemsText.append("  —  ₱").append(line.getLineTotalCents() / 100);
            if (line.hasLineDiscount()) {
                int disc = line.getDiscountAmountCents() / 100;
                discountTotal += Math.max(0, disc);
                itemsText.append("  (−₱").append(disc).append(" disc)");
            }
            itemsText.append("\n");
        }
        if (itemsText.length() == 0) itemsText.append("No items");

        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.orders_total, order.totalCents / 100));
        if (order.orderStatus == OrderStatus.PENDING) {
            summary.append("\n").append(getString(R.string.orders_unpaid_hint));
        } else {
            summary.append("\n").append("Paid: ₱").append(order.cashReceivedCents / 100);
            summary.append("\n").append("Change: ₱").append(order.changeCents / 100);
        }
        if (discountTotal > 0) {
            summary.append("\n").append("Discounts: ₱").append(discountTotal);
        }
        if (order.orderNotes != null && !order.orderNotes.trim().isEmpty()) {
            summary.append("\n").append("Notes: ").append(order.orderNotes.trim());
        }
        if (order.orderType != null && !order.orderType.trim().isEmpty()) {
            summary.append("\n").append("Order type: ").append(order.orderType.trim());
        }
        if (order.tableNumber != null && !order.tableNumber.trim().isEmpty()) {
            summary.append("\n").append("Table #: ").append(order.tableNumber.trim());
        }
        summary.append("\n").append("Cashier: ").append(order.cashierName != null ? order.cashierName : "");
        if (order.orderStatus == OrderStatus.PAID) {
            summary.append("\n").append("Payment: ").append(order.paymentType != null ? order.paymentType : "");
            if (order.paymentRefLast4 != null && !order.paymentRefLast4.isEmpty()) {
                summary.append("\n").append("Ref: ****").append(order.paymentRefLast4);
            }
        }

        String dateTime = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new Date(order.timestampMillis));

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_order_details, null, false);
        TextView title = view.findViewById(R.id.orderDetailsTitle);
        TextView meta = view.findViewById(R.id.orderDetailsMeta);
        TextView items = view.findViewById(R.id.orderDetailsItems);
        TextView summaryView = view.findViewById(R.id.orderDetailsSummary);
        View billOutBtn = view.findViewById(R.id.orderDetailsBillOut);
        View payBtn = view.findViewById(R.id.orderDetailsPay);
        View closeBtn = view.findViewById(R.id.orderDetailsClose);

        title.setText(getString(R.string.orders_details_title, String.valueOf(order.id)));
        meta.setText(TextUtils.concat(
                dateTime,
                "  •  ",
                (order.orderStatus == OrderStatus.PENDING
                        ? getString(R.string.orders_status_pending)
                        : getString(R.string.orders_status_paid))
        ));
        items.setText(itemsText.toString().trim());
        summaryView.setText(summary.toString().trim());
        billOutBtn.setVisibility(order.orderStatus == OrderStatus.PENDING ? View.VISIBLE : View.GONE);
        payBtn.setVisibility(order.orderStatus == OrderStatus.PENDING ? View.VISIBLE : View.GONE);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
        billOutBtn.setOnClickListener(v -> {
            dialog.dismiss();
            printBillOutReceipt(order);
        });
        payBtn.setOnClickListener(v -> {
            dialog.dismiss();
            payOrder(order);
        });
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void payOrder(PaidOrderEntity order) {
        if (order == null) return;
        if (order.orderStatus != OrderStatus.PENDING) return;

        List<PaidOrderLineEntity> lines = paidOrderRepo.getLinesForOrder(order.id);
        CartViewModel cartVm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        cartVm.loadPendingOrder(order, lines);
        Navigation.findNavController(requireView()).navigate(R.id.nav_payment);
    }

    private void confirmDeleteOne(PaidOrderEntity order) {
        if (!isManager) {
            android.widget.Toast.makeText(requireContext(), R.string.orders_manager_only_delete, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (order == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.orders_delete)
                .setMessage(R.string.orders_delete_one)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    paidOrderRepo.deleteOrder(order.id);
                    if (viewModel != null) viewModel.refresh();
                    android.widget.Toast.makeText(requireContext(), R.string.orders_deleted, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmDeleteSelected() {
        if (!isManager) return;
        List<Long> selected = adapter.getSelectedOrderIds();
        if (selected.isEmpty() && !adapter.isSelectionMode()) {
            // Enter selection mode if none selected and not already in selection mode
            adapter.setSelectionMode(true);
            return;
        }
        if (selected.isEmpty()) {
            // If already in selection mode but nothing selected, maybe just exit selection mode or show tip
            adapter.setSelectionMode(false);
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.orders_delete)
                .setMessage(R.string.orders_delete_selected)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    paidOrderRepo.deleteOrders(selected);
                    adapter.clearSelection();
                    if (viewModel != null) viewModel.refresh();
                    android.widget.Toast.makeText(requireContext(), R.string.orders_deleted, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void onSelectionChanged(int selectedCount) {
        if (!isManager) return;
        boolean inSelectionMode = adapter.isSelectionMode();
        binding.ordersDelete.setAlpha(selectedCount > 0 ? 1f : 0.6f);
        binding.ordersSelectAll.setVisibility(inSelectionMode ? View.VISIBLE : View.GONE);
        if (!inSelectionMode) {
            binding.ordersSelectAll.setChecked(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.Holder> {
        private final OnOrderClick onClick;
        private final boolean isManager;
        private final OnOrderClick onDeleteOne;
        private final OnSelectionChanged onSelectionChanged;
        private final OnPayOrder onPayOrder;
        private final OnBillOut onBillOut;
        private final OnOpenPos onOpenPos;
        private final OnViewDetails onViewDetails;
        private final OnReprintKitchen onReprintKitchen;
        private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        private List<PaidOrderEntity> list = new ArrayList<>();
        private Map<Long, Integer> orderItemCounts = new HashMap<>();
        private boolean selectionMode = false;
        private final Set<Long> selectedIds = new HashSet<>();

        interface OnOrderClick { void run(PaidOrderEntity order); }
        interface OnPayOrder { void run(PaidOrderEntity order); }
        interface OnBillOut { void run(PaidOrderEntity order); }
        interface OnOpenPos { void run(PaidOrderEntity order); }
        interface OnViewDetails { void run(PaidOrderEntity order); }
        interface OnReprintKitchen { void run(PaidOrderEntity order); }
        interface OnSelectionChanged { void run(int selectedCount); }

        OrdersAdapter(
                List<PaidOrderEntity> list,
                boolean isManager,
                OnOrderClick onClick,
                OnOrderClick onDeleteOne,
                OnSelectionChanged onSelectionChanged,
                OnPayOrder onPayOrder,
                OnBillOut onBillOut,
                OnOpenPos onOpenPos,
                OnViewDetails onViewDetails,
                OnReprintKitchen onReprintKitchen
        ) {
            this.list = list;
            this.isManager = isManager;
            this.onClick = onClick;
            this.onDeleteOne = onDeleteOne;
            this.onSelectionChanged = onSelectionChanged;
            this.onPayOrder = onPayOrder;
            this.onBillOut = onBillOut;
            this.onOpenPos = onOpenPos;
            this.onViewDetails = onViewDetails;
            this.onReprintKitchen = onReprintKitchen;
            setHasStableIds(true);
        }

        void submitList(List<PaidOrderEntity> list, Map<Long, Integer> orderItemCounts) {
            List<PaidOrderEntity> next = list != null ? list : new ArrayList<>();
            List<PaidOrderEntity> old = this.list != null ? this.list : new ArrayList<>();
            Map<Long, Integer> nextCounts = orderItemCounts != null ? new HashMap<>(orderItemCounts) : new HashMap<>();
            Map<Long, Integer> oldCounts = this.orderItemCounts != null ? this.orderItemCounts : new HashMap<>();
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return old.size();
                }

                @Override
                public int getNewListSize() {
                    return next.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return old.get(oldItemPosition).id == next.get(newItemPosition).id;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    PaidOrderEntity a = old.get(oldItemPosition);
                    PaidOrderEntity b = next.get(newItemPosition);
                    Integer aCount = oldCounts.get(a.id);
                    Integer bCount = nextCounts.get(b.id);
                    int ai = aCount != null ? aCount : 0;
                    int bi = bCount != null ? bCount : 0;
                    String aCashier = a.cashierName != null ? a.cashierName : "";
                    String bCashier = b.cashierName != null ? b.cashierName : "";
                    String aType = a.paymentType != null ? a.paymentType : "";
                    String bType = b.paymentType != null ? b.paymentType : "";
                    return a.timestampMillis == b.timestampMillis
                            && a.totalCents == b.totalCents
                            && a.cashReceivedCents == b.cashReceivedCents
                            && a.changeCents == b.changeCents
                            && a.orderStatus == b.orderStatus
                            && aCashier.equals(bCashier)
                            && aType.equals(bType)
                            && ai == bi;
                }
            });
            this.list = next;
            this.orderItemCounts = nextCounts;
            diff.dispatchUpdatesTo(this);
        }

        boolean isSelectionMode() {
            return selectionMode;
        }

        void setSelectionMode(boolean enabled) {
            if (!isManager) return;
            selectionMode = enabled;
            if (!selectionMode) selectedIds.clear();
            notifyDataSetChanged();
            if (onSelectionChanged != null) onSelectionChanged.run(selectedIds.size());
        }

        void selectAll(boolean select) {
            if (!isManager || !selectionMode) return;
            if (select) {
                for (PaidOrderEntity o : list) selectedIds.add(o.id);
            } else {
                selectedIds.clear();
            }
            notifyDataSetChanged();
            if (onSelectionChanged != null) onSelectionChanged.run(selectedIds.size());
        }

        List<Long> getSelectedOrderIds() {
            return new ArrayList<>(selectedIds);
        }

        void clearSelection() {
            selectionMode = false;
            selectedIds.clear();
            notifyDataSetChanged();
            if (onSelectionChanged != null) onSelectionChanged.run(0);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemOrderBinding b = ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            PaidOrderEntity o = list.get(position);
            String idStr = String.valueOf(o.id);
            h.binding.orderTitle.setText("Order #" + idStr);

            Integer cachedCount = orderItemCounts.get(o.id);
            int itemCount = cachedCount != null ? cachedCount : 0;
            String date = dateFormatter.format(new Date(o.timestampMillis));
            h.binding.orderSubtitle.setText(date + " • " + h.itemView.getContext().getString(R.string.orders_items_count, itemCount));

            h.binding.orderTotal.setText(h.itemView.getContext().getString(R.string.orders_total, o.totalCents / 100));
            boolean pending = o.orderStatus == OrderStatus.PENDING;
            h.binding.orderStatus.setText(pending
                    ? h.itemView.getContext().getString(R.string.orders_status_pending)
                    : h.itemView.getContext().getString(R.string.orders_status_paid));
            h.binding.orderStatus.setTextColor(androidx.core.content.ContextCompat.getColor(
                    h.itemView.getContext(),
                    pending ? R.color.price_red : R.color.inventory_stock_in_green
            ));
            if (pending) {
                h.binding.orderPaid.setText(R.string.orders_unpaid_hint);
                h.binding.orderChange.setVisibility(View.GONE);
                h.binding.orderActionsContainer.setVisibility(View.VISIBLE);

                h.binding.orderOpenPos.setOnClickListener(v -> {
                    if (onOpenPos != null) onOpenPos.run(o);
                });
                h.binding.orderViewDetails.setOnClickListener(v -> {
                    if (onViewDetails != null) onViewDetails.run(o);
                });
                h.binding.orderReprintKitchen.setOnClickListener(v -> {
                    if (onReprintKitchen != null) onReprintKitchen.run(o);
                });
                h.binding.orderBillOut.setOnClickListener(v -> {
                    if (onBillOut != null) onBillOut.run(o);
                });
                h.binding.orderPay.setOnClickListener(v -> {
                    if (onPayOrder != null) onPayOrder.run(o);
                });
            } else {
                h.binding.orderPaid.setText("Paid: ₱" + (o.cashReceivedCents / 100));
                h.binding.orderChange.setVisibility(View.VISIBLE);
                h.binding.orderChange.setText("Change: ₱" + (o.changeCents / 100));
                h.binding.orderActionsContainer.setVisibility(View.GONE);

                h.binding.orderOpenPos.setOnClickListener(null);
                h.binding.orderViewDetails.setOnClickListener(null);
                h.binding.orderReprintKitchen.setOnClickListener(null);
                h.binding.orderBillOut.setOnClickListener(null);
                h.binding.orderPay.setOnClickListener(null);
            }

            // Manager-only delete/select controls
            h.binding.orderDelete.setVisibility(isManager ? View.VISIBLE : View.GONE);
            h.binding.orderSelect.setVisibility(isManager && selectionMode ? View.VISIBLE : View.GONE);
            h.binding.orderSelect.setOnCheckedChangeListener(null);
            h.binding.orderSelect.setChecked(selectedIds.contains(o.id));
            h.binding.orderSelect.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) selectedIds.add(o.id);
                else selectedIds.remove(o.id);
                if (onSelectionChanged != null) onSelectionChanged.run(selectedIds.size());
            });

            h.binding.orderDelete.setOnClickListener(v -> onDeleteOne.run(o));

            h.itemView.setOnLongClickListener(v -> {
                if (!isManager) return false;
                selectionMode = true;
                selectedIds.add(o.id);
                notifyDataSetChanged();
                if (onSelectionChanged != null) onSelectionChanged.run(selectedIds.size());
                return true;
            });

            h.itemView.setOnClickListener(v -> {
                if (isManager && selectionMode) {
                    boolean currently = selectedIds.contains(o.id);
                    if (currently) selectedIds.remove(o.id);
                    else selectedIds.add(o.id);
                    notifyItemChanged(position);
                    if (onSelectionChanged != null) onSelectionChanged.run(selectedIds.size());
                    return;
                }
                onClick.run(o);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public long getItemId(int position) {
            if (position < 0 || position >= list.size()) return RecyclerView.NO_ID;
            return list.get(position).id;
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemOrderBinding binding;
            Holder(ItemOrderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
