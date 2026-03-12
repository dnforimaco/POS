package com.example.menu_pos.ui.reports;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.menu_pos.R;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentReportsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private ReportsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String currentUser = new UserRepository(requireContext().getApplicationContext()).getLoggedInUser();
        if (!"Manager".equals(currentUser)) {
            Toast.makeText(requireContext(), R.string.reports_manager_only, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        binding.reportsBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        viewModel.getReportData().observe(getViewLifecycleOwner(), this::bindReport);

        binding.reportsExportPdfBtn.setOnClickListener(v -> exportToPdf());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }

    private void bindReport(ReportData data) {
        if (data == null) return;
        binding.reportsDailyValue.setText(formatPesos(data.getDailySalesCents()));
        binding.reportsWeeklyValue.setText(formatPesos(data.getWeeklySalesCents()));
        binding.reportsMonthlyValue.setText(formatPesos(data.getMonthlySalesCents()));

        StringBuilder top = new StringBuilder();
        for (ReportData.TopItem t : data.getTopSellingItems()) {
            if (top.length() > 0) top.append("\n");
            top.append(t.itemName).append(" (").append(t.quantity).append(")");
        }
        binding.reportsTopSellingContent.setText(top.length() > 0 ? top.toString() : getString(R.string.reports_no_data));

        StringBuilder cat = new StringBuilder();
        for (ReportData.CategorySale c : data.getSalesByCategory()) {
            if (cat.length() > 0) cat.append("\n");
            cat.append(c.categoryName).append(": ").append(formatPesos(c.totalCents));
        }
        binding.reportsSalesByCategoryContent.setText(cat.length() > 0 ? cat.toString() : getString(R.string.reports_no_data));

        binding.reportsTotalOrdersValue.setText(String.valueOf(data.getTotalOrders()));
        binding.reportsAvgOrderValue.setText(formatPesos(data.getAverageOrderValueCents()));
        binding.reportsPeakHoursValue.setText(data.getPeakHour());
        binding.reportsPopularItemValue.setText(data.getMostPopularItem());
    }

    private String formatPesos(int cents) {
        return getString(R.string.price_format, (cents / 100));
    }

    private void exportToPdf() {
        ReportData data = viewModel.getReportData().getValue();
        if (data == null) data = new ReportData(0, 0, 0, null, null, 0, 0, "—", "—", 0);

        PdfDocument doc = new PdfDocument();
        int pageWidth = 595; // A4 pt
        int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint titlePaint = new Paint();
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(18);
        float y = 40;

        page.getCanvas().drawText("NAOMIE'S LOMI HOUSE — Sales Report", 40, y, titlePaint);
        y += 24;

        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        Paint subPaint = new Paint();
        subPaint.setTextSize(10);
        page.getCanvas().drawText("Generated: " + dateStr, 40, y, subPaint);
        y += 28;

        Paint labelPaint = new Paint();
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextSize(12);
        Paint valuePaint = new Paint();
        valuePaint.setTextSize(11);

        drawLine(page, "Daily Sales (today):", formatPesos(data.getDailySalesCents()), 40, y, labelPaint, valuePaint);
        y += 18;
        drawLine(page, "Weekly Sales (last 7 days):", formatPesos(data.getWeeklySalesCents()), 40, y, labelPaint, valuePaint);
        y += 18;
        drawLine(page, "Monthly Sales (last 30 days):", formatPesos(data.getMonthlySalesCents()), 40, y, labelPaint, valuePaint);
        y += 22;

        page.getCanvas().drawText("Top selling items", 40, y, labelPaint);
        y += 16;
        for (ReportData.TopItem t : data.getTopSellingItems()) {
            page.getCanvas().drawText("  • " + t.itemName + " (" + t.quantity + ")", 40, y, valuePaint);
            y += 14;
        }
        if (data.getTopSellingItems().isEmpty()) {
            page.getCanvas().drawText("  —", 40, y, valuePaint);
            y += 14;
        }
        y += 8;

        page.getCanvas().drawText("Sales by category", 40, y, labelPaint);
        y += 16;
        for (ReportData.CategorySale c : data.getSalesByCategory()) {
            page.getCanvas().drawText("  • " + c.categoryName + ": " + formatPesos(c.totalCents), 40, y, valuePaint);
            y += 14;
        }
        if (data.getSalesByCategory().isEmpty()) {
            page.getCanvas().drawText("  —", 40, y, valuePaint);
            y += 14;
        }
        y += 16;

        drawLine(page, "Total orders:", String.valueOf(data.getTotalOrders()), 40, y, labelPaint, valuePaint);
        y += 18;
        drawLine(page, "Average order value:", formatPesos(data.getAverageOrderValueCents()), 40, y, labelPaint, valuePaint);
        y += 18;
        drawLine(page, "Peak hour:", data.getPeakHour(), 40, y, labelPaint, valuePaint);
        y += 18;
        drawLine(page, "Most popular item:", data.getMostPopularItem(), 40, y, labelPaint, valuePaint);

        doc.finishPage(page);

        File dir = requireContext().getCacheDir();
        String fileName = "sales_report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File file = new File(dir, fileName);
        try {
            try (FileOutputStream out = new FileOutputStream(file)) {
                doc.writeTo(out);
            }
            doc.close();
        } catch (IOException e) {
            doc.close();
            Toast.makeText(requireContext(), R.string.reports_export_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_EMAIL, new String[]{"dirknashorimaco1@gmail.com"});
        share.putExtra(Intent.EXTRA_SUBJECT, "Sales Report");
        share.putExtra(Intent.EXTRA_TEXT, "Attached is the latest sales report from the POS system.");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.reports_export_share)));
        Toast.makeText(requireContext(), R.string.reports_export_success, Toast.LENGTH_SHORT).show();
    }

    private static void drawLine(PdfDocument.Page page, String label, String value, float x, float y, Paint labelPaint, Paint valuePaint) {
        page.getCanvas().drawText(label, x, y, labelPaint);
        page.getCanvas().drawText(value, x + 220, y, valuePaint);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
