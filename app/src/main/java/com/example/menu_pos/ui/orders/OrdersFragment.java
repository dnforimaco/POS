package com.example.menu_pos.ui.orders;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.menu_pos.R;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentOrdersBinding;
import com.example.menu_pos.databinding.ItemOrderBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrdersViewModel viewModel;
    private OrdersAdapter adapter;
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
        String user = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        isManager = "Manager".equals(user);

        binding.ordersBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        adapter = new OrdersAdapter(new ArrayList<>(), isManager, this::showOrderDetails, this::confirmDeleteOne, this::onSelectionChanged);
        binding.ordersList.setLayoutManager(new LinearLayoutManager(requireContext()));
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
            adapter.submitList(safe);
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

    private void showOrderDetails(PaidOrderEntity order) {
        if (order == null) return;
        PaidOrderRepository repo = new PaidOrderRepository(requireContext().getApplicationContext());
        List<PaidOrderLineEntity> lines = repo.getLinesForOrder(order.id);
        StringBuilder msg = new StringBuilder();
        for (PaidOrderLineEntity line : lines) {
            msg.append("• ").append(line.itemName);
            if (line.variantLabel != null && !line.variantLabel.isEmpty()) {
                msg.append(" (").append(line.variantLabel).append(")");
            }
            msg.append(" x").append(line.quantity);
            msg.append(" — ₱").append(line.getLineTotalCents() / 100);
            msg.append("\n");
        }
        msg.append("\n").append(getString(R.string.orders_total, order.totalCents / 100));
        msg.append("\n").append("Paid: ₱").append(order.cashReceivedCents / 100);
        msg.append("\n").append("Change: ₱").append(order.changeCents / 100);
        msg.append("\n").append("Cashier: ").append(order.cashierName != null ? order.cashierName : "");
        msg.append("\n").append("Payment: ").append(order.paymentType != null ? order.paymentType : "");

        String idStr = String.valueOf(order.id);
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.orders_details_title, idStr))
                .setMessage(msg.toString().trim())
                .setPositiveButton(R.string.orders_details_close, null)
                .show();
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
                    new PaidOrderRepository(requireContext().getApplicationContext()).deleteOrder(order.id);
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
                    new PaidOrderRepository(requireContext().getApplicationContext()).deleteOrders(selected);
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
        private List<PaidOrderEntity> list = new ArrayList<>();
        private boolean selectionMode = false;
        private final Set<Long> selectedIds = new HashSet<>();

        interface OnOrderClick { void run(PaidOrderEntity order); }
        interface OnSelectionChanged { void run(int selectedCount); }

        OrdersAdapter(List<PaidOrderEntity> list, boolean isManager, OnOrderClick onClick, OnOrderClick onDeleteOne, OnSelectionChanged onSelectionChanged) {
            this.list = list;
            this.isManager = isManager;
            this.onClick = onClick;
            this.onDeleteOne = onDeleteOne;
            this.onSelectionChanged = onSelectionChanged;
        }

        void submitList(List<PaidOrderEntity> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
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

            int itemCount = 0;
            PaidOrderRepository repo = new PaidOrderRepository(h.itemView.getContext().getApplicationContext());
            List<PaidOrderLineEntity> lines = repo.getLinesForOrder(o.id);
            for (PaidOrderLineEntity l : lines) itemCount += Math.max(0, l.quantity);
            String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(o.timestampMillis));
            h.binding.orderSubtitle.setText(date + " • " + h.itemView.getContext().getString(R.string.orders_items_count, itemCount));

            h.binding.orderTotal.setText(h.itemView.getContext().getString(R.string.orders_total, o.totalCents / 100));
            h.binding.orderPaid.setText("Paid: ₱" + (o.cashReceivedCents / 100));
            h.binding.orderChange.setText("Change: ₱" + (o.changeCents / 100));

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

        static class Holder extends RecyclerView.ViewHolder {
            final ItemOrderBinding binding;
            Holder(ItemOrderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
