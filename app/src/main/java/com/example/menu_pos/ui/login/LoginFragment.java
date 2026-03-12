package com.example.menu_pos.ui.login;

import android.os.Bundle;
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
import com.example.menu_pos.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        viewModel.getLoginResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result instanceof LoginViewModel.LoginResult.Success) {
                String username = ((LoginViewModel.LoginResult.Success) result).username;
                Toast.makeText(requireContext(), getString(R.string.login_welcome, username), Toast.LENGTH_SHORT).show();
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login, true)
                        .build();
                Navigation.findNavController(requireView()).navigate(R.id.nav_pos, null, options);
            } else {
                Toast.makeText(requireContext(), R.string.login_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptLogin() {
        String username = binding.loginUsername.getText() != null ? binding.loginUsername.getText().toString().trim() : "";
        String password = binding.loginPassword.getText() != null ? binding.loginPassword.getText().toString() : "";
        if (username.isEmpty()) {
            binding.loginUsernameLayout.setError(getString(R.string.login_username_hint));
            return;
        }
        binding.loginUsernameLayout.setError(null);
        binding.loginPasswordLayout.setError(null);
        viewModel.login(username, password);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
