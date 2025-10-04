package com.segi.student.blog.ui.my;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.AuthViewModel;
import com.segi.student.blog.databinding.FragmentAccountSecurityBinding;

public class AccountSecurityFragment extends Fragment {

    private FragmentAccountSecurityBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountSecurityBinding.inflate(inflater, container, false);
        // Use the activity as the scope to get a shared ViewModel instance
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupClickListeners() {
        binding.updatePasswordButton.setOnClickListener(v -> {
            String oldPassword = binding.currentPasswordEditText.getText().toString();
            String newPassword = binding.newPasswordEditText.getText().toString();
            authViewModel.changePassword(oldPassword, newPassword);
        });
    }

    private void observeViewModel() {
        authViewModel.passwordChangeState.observe(getViewLifecycleOwner(), state -> {
            setLoading(state == AuthViewModel.AuthState.LOADING);

            if (state == AuthViewModel.AuthState.SUCCESS) {
                Snackbar.make(binding.getRoot(), "Password updated successfully!", Snackbar.LENGTH_SHORT).show();
                authViewModel.resetPasswordChangeState();
                NavHostFragment.findNavController(this).navigateUp();
            } else if (state == AuthViewModel.AuthState.ERROR) {
                String error = authViewModel.errorMessage.getValue();
                Snackbar.make(binding.getRoot(), error != null ? error : "An unknown error occurred.", Snackbar.LENGTH_LONG).show();
                authViewModel.resetPasswordChangeState();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.updatePasswordButton.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.currentPasswordEditText.setEnabled(!isLoading);
        binding.newPasswordEditText.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
