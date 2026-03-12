package com.example.menu_pos.ui.checkout;

import android.os.Bundle;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.menu_pos.R;
import com.example.menu_pos.data.PaidOrderEntity;
import com.example.menu_pos.data.PaidOrderLineEntity;
import com.example.menu_pos.data.PaidOrderRepository;
import com.example.menu_pos.databinding.FragmentPaymentConfirmationBinding;

import java.util.List;

public class PaymentConfirmationFragment extends Fragment {

    private FragmentPaymentConfirmationBinding binding;

    private long orderId;
    private int totalCents;
    private int cashCents;
    private int changeCents;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPaymentConfirmationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            orderId = args.getLong("orderId", 0);
            totalCents = args.getInt("totalCents", 0);
            cashCents = args.getInt("cashCents", 0);
            changeCents = args.getInt("changeCents", 0);
        }

        binding.confirmBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        binding.confirmOrderId.setText("#" + orderId);
        binding.confirmTotalValue.setText(formatPesos(totalCents));
        binding.confirmCashValue.setText(formatPesos(cashCents));
        binding.confirmChangeValue.setText(formatPesos(changeCents));

        binding.confirmPrint.setOnClickListener(v -> printReceipt());
        binding.confirmReprint.setOnClickListener(v -> printReceipt());
        binding.confirmNewOrder.setOnClickListener(v -> {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_pos, false)
                    .build();
            Navigation.findNavController(requireView()).navigate(R.id.nav_pos, null, options);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Auto-print after successful payment
        printReceipt();
    }

    private void printReceipt() {
        try {
            Toast.makeText(requireContext(), R.string.confirm_printing, Toast.LENGTH_SHORT).show();
            PaidOrderRepository repo = new PaidOrderRepository(requireContext().getApplicationContext());
            PaidOrderEntity order = repo.getOrderById(orderId);
            List<PaidOrderLineEntity> lines = repo.getLinesForOrder(orderId);
            if (order == null) return;

            byte[] pdf = ReceiptPrinter.buildReceiptPdfBytes(requireContext(), order, lines);

            PrintManager printManager = (PrintManager) requireContext().getSystemService(android.content.Context.PRINT_SERVICE);
            if (printManager == null) return;
            String jobName = "Receipt_" + orderId;
            printManager.print(jobName, new PdfBytesPrintAdapter(jobName + ".pdf", pdf), null);
        } catch (Exception ignored) {
            // If printing fails, user can still use Orders tab and try again later.
        }
    }

    private static String formatPesos(int cents) {
        return "₱" + (cents / 100);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

