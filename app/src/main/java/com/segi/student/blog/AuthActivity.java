package com.segi.student.blog;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.databinding.ActivityAuthBinding; // Make sure this matches your package

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupUI();
        observeViewModel();
    }

    private void setupUI() {
        // Set default selection
        binding.toggleButtonGroup.check(binding.buttonLoginToggle.getId());

        // Toggle between Login and Sign Up
        binding.toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isLoginMode = checkedId == binding.buttonLoginToggle.getId();
                updateFormUI();
            }
        });

        // Main action button click
        binding.buttonAction.setOnClickListener(v -> performAuthentication());

        // Handle "Done" on keyboard
        binding.passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performAuthentication();
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        authViewModel.authState.observe(this, state -> {
            switch (state) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    // Navigate to your main activity or show success message
                    Snackbar.make(binding.getRoot(), isLoginMode ? "Login successful!" : "Sign up successful!", Snackbar.LENGTH_LONG).show();
                    // Example: finish();
                    break;
                case ERROR:
                    setLoading(false);
                    break; // Error message is handled by its own observer
                case IDLE:
                    setLoading(false);
                    break;
            }
        });

        authViewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                authViewModel.resetState(); // Reset after showing message
            }
        });
    }

    private void performAuthentication() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString();

        // Basic validation
        binding.emailLayout.setError(null);
        if (email.isEmpty()) {
            binding.emailLayout.setError("Email cannot be empty");
            return;
        }

        if (isLoginMode) {
            authViewModel.login(email, password);
        } else {
            authViewModel.signUp(email, password);
        }
    }

    private void updateFormUI() {
        binding.buttonAction.setText(isLoginMode ? "Login" : "Sign Up");
        // Clear errors when toggling
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
    }

    private void setLoading(boolean isLoading) {
        binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonAction.setText(isLoading ? "" : (isLoginMode ? "Login" : "Sign Up"));
        binding.buttonAction.setEnabled(!isLoading);
        binding.emailEditText.setEnabled(!isLoading);
        binding.passwordEditText.setEnabled(!isLoading);
        binding.toggleButtonGroup.setEnabled(!isLoading);
    }
}