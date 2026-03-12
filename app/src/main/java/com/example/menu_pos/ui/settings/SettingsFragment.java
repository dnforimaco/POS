package com.example.menu_pos.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.menu_pos.R;
import com.example.menu_pos.data.UserRepository;
import com.example.menu_pos.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private UserRepository userRepo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepo = new UserRepository(requireContext().getApplicationContext());

        binding.settingsBack.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Restaurant info is read-only (fixed in layout strings)

        // Manager-only sections
        boolean isManager = "Manager".equals(userRepo.getLoggedInUser());
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
                    if (userRepo.changePassword("Manager", cur, newP)) {
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
        binding = null;
    }
}
