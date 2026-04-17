package com.example.menu_pos.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import com.example.menu_pos.R;
import com.example.menu_pos.data.CartItem;
import com.example.menu_pos.databinding.FragmentCartBinding;
import com.example.menu_pos.databinding.ItemCartBinding;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CartViewModel vm = new ViewModelProvider(requireActivity()).get(CartViewModel.class);
        CartAdapter adapter = new CartAdapter(vm);
        binding.recyclerCart.setAdapter(adapter);
        binding.recyclerCart.setItemAnimator(null);

        vm.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items != null ? new ArrayList<>(items) : new ArrayList<>());
            boolean empty = items == null || items.isEmpty();
            binding.cartEmptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerCart.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.cartFooter.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (!empty) {
                int total = 0;
                for (CartItem c : items) total += c.getTotalCents();
                binding.cartTotal.setText(getString(R.string.cart_total, total / 100));
            }
        });

        binding.btnClear.setOnClickListener(v -> {
            vm.clear();
            Toast.makeText(requireContext(), R.string.cart_clear, Toast.LENGTH_SHORT).show();
        });
        binding.btnCheckout.setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.cart_checkout, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCartBinding b = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new Holder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
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
            h.discountCb.setOnCheckedChangeListener((btn, checked) -> {
                int pos = h.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                vm.setLineDiscountAt(pos, checked);
            });
            h.btnMinus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() - 1));
            h.btnPlus.setOnClickListener(v -> vm.setQuantity(item, item.getQuantity() + 1));
            h.btnRemove.setOnClickListener(v -> vm.removeItem(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView line, price, qty;
            final MaterialCheckBox discountCb;
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
