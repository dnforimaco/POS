package com.example.menu_pos.ui.settings;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.menu_pos.R;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentSettingsBinding;
import com.example.menu_pos.printer.BluetoothPrinterManager;
import com.example.menu_pos.printer.PrinterManager;
import com.example.menu_pos.printer.PrinterPrefs;
import com.example.menu_pos.printer.ReceiptTextFormatter;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private UserRepository userRepo;
    private BluetoothPrinterManager bt;
    private PrinterManager printerManager;

    private static final int REQ_BT_PERMS = 501;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepo = new UserRepository(requireContext().getApplicationContext());
        bt = BluetoothPrinterManager.getInstance(requireContext().getApplicationContext());
        printerManager = new PrinterManager(requireContext().getApplicationContext());

        binding.settingsBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Restaurant info is read-only (fixed in layout strings)

        // Manager-only sections
        boolean isManager = "manager".equalsIgnoreCase(userRepo.getLoggedInUser());
        binding.settingsCardMenu.setVisibility(isManager ? View.VISIBLE : View.GONE);
        binding.settingsCardUsers.setVisibility(isManager ? View.VISIBLE : View.GONE);

        if (isManager) {
            binding.settingsMenuAddEdit.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_manage_items));
            binding.settingsMenuPrices.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_manage_items));
            binding.settingsMenuCategories.setOnClickListener(v ->
                    Navigation.findNavController(requireView()).navigate(R.id.nav_manage_categories));
            binding.settingsChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // Printer guideline is static text in layout
        setupPrinterSettings();
    }

    private void setupPrinterSettings() {
        if (binding == null) return;

        // Switches
        binding.settingsPrinterAutoConnect.setChecked(PrinterPrefs.isAutoConnectEnabled(requireContext()));
        binding.settingsPrinterAutoPrint.setChecked(PrinterPrefs.isAutoPrintEnabled(requireContext()));
        binding.settingsPrinterAutoConnect.setOnCheckedChangeListener((btn, checked) -> {
            PrinterPrefs.setAutoConnect(requireContext(), checked);
            if (checked) bt.autoConnectIfEnabled();
            updatePrinterUi();
        });
        binding.settingsPrinterAutoPrint.setOnCheckedChangeListener((btn, checked) ->
                PrinterPrefs.setAutoPrint(requireContext(), checked));

        binding.settingsPrinterSelect.setOnClickListener(v -> {
            if (!ensureBluetoothPermissions()) {
                Toast.makeText(requireContext(), R.string.settings_printer_permission_needed, Toast.LENGTH_SHORT).show();
                return;
            }
            showPairedPrintersDialog();
        });

        binding.settingsPrinterReconnect.setOnClickListener(v -> {
            String mac = PrinterPrefs.getPrinterMac(requireContext());
            if (mac == null) return;
            bt.connect(mac);
            Toast.makeText(requireContext(), "Connecting…", Toast.LENGTH_SHORT).show();
            updatePrinterUi();
        });

        binding.settingsPrinterTestPrint.setOnClickListener(v -> {
            String mac = PrinterPrefs.getPrinterMac(requireContext());
            String name = PrinterPrefs.getPrinterName(requireContext());
            if (mac == null) {
                Toast.makeText(requireContext(), R.string.settings_printer_none_selected, Toast.LENGTH_SHORT).show();
                return;
            }
            String test = ReceiptTextFormatter.buildTestPrint(name);
            printerManager.connectIfNeededAndPrint(mac, test, success ->
                    requireActivity().runOnUiThread(() -> {
                        updatePrinterUi();
                        Toast.makeText(requireContext(),
                                success ? "Test print sent." : getString(R.string.printer_not_connected_retry),
                                Toast.LENGTH_SHORT).show();
                    }));
        });

        bt.setListener(connected -> {
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(this::updatePrinterUi);
        });

        updatePrinterUi();
    }

    private void updatePrinterUi() {
        if (binding == null) return;
        String name = PrinterPrefs.getPrinterName(requireContext());
        binding.settingsPrinterSelectedName.setText(name.isEmpty() ? getString(R.string.settings_printer_none_selected) : name);

        boolean connected = bt != null && bt.isConnected();
        binding.settingsPrinterStatus.setText(getString(connected
                ? R.string.settings_printer_status_connected
                : R.string.settings_printer_status_disconnected));
        int dotColor = connected ? 0xFF2E7D32 : 0xFFB71C1C; // green/red
        binding.settingsPrinterStatusDot.setTextColor(dotColor);
    }

    private boolean ensureBluetoothPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        boolean connectOk = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
        boolean scanOk = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
        if (connectOk && scanOk) return true;
        requestPermissions(new String[]{
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
        }, REQ_BT_PERMS);
        return false;
    }

    private void showPairedPrintersDialog() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(requireContext(), R.string.settings_printer_no_paired_devices, Toast.LENGTH_SHORT).show();
            return;
        }
        List<BluetoothDevice> bonded = new ArrayList<>(adapter.getBondedDevices());
        if (bonded.isEmpty()) {
            Toast.makeText(requireContext(), R.string.settings_printer_no_paired_devices, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[bonded.size()];
        for (int i = 0; i < bonded.size(); i++) {
            BluetoothDevice d = bonded.get(i);
            String dn = d.getName() != null ? d.getName() : "Unknown";
            labels[i] = dn + "\n" + d.getAddress();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.settings_printer_select))
                .setItems(labels, (dialog, which) -> {
                    BluetoothDevice d = bonded.get(which);
                    String dn = d.getName() != null ? d.getName() : "Bluetooth Printer";
                    String mac = d.getAddress();
                    PrinterPrefs.setSelectedPrinter(requireContext(), dn, mac);
                    bt.connect(mac);
                    // Auto test print
                    String test = ReceiptTextFormatter.buildTestPrint(dn);
                    printerManager.connectIfNeededAndPrint(mac, test, success ->
                            requireActivity().runOnUiThread(() -> {
                                updatePrinterUi();
                                Toast.makeText(requireContext(),
                                        success ? "Printer selected. Test print sent." : getString(R.string.printer_not_connected_retry),
                                        Toast.LENGTH_SHORT).show();
                            }));
                    updatePrinterUi();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BT_PERMS) {
            updatePrinterUi();
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText current = dialogView.findViewById(R.id.dialog_current_password);
        EditText newPass = dialogView.findViewById(R.id.dialog_new_password);
        EditText confirm = dialogView.findViewById(R.id.dialog_confirm_password);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_change_password_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String cur = current.getText() != null ? current.getText().toString() : "";
                    String newP = newPass.getText() != null ? newPass.getText().toString() : "";
                    String conf = confirm.getText() != null ? confirm.getText().toString() : "";
                    if (!newP.equals(conf)) {
                        Toast.makeText(requireContext(), R.string.settings_password_mismatch, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String loggedIn = userRepo.getLoggedInUser();
                    if (loggedIn != null && userRepo.changePassword(loggedIn, cur, newP)) {
                        Toast.makeText(requireContext(), R.string.settings_password_changed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), R.string.settings_password_wrong, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bt != null) bt.setListener(null);
        binding = null;
    }
}
