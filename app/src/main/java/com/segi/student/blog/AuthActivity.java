package com.segi.student.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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

        // Set the initial UI state
        updateUiForMode();

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Listener for the new text-based toggle
        binding.toggleText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode; // Switch the mode
            updateUiForMode();
        });

        // Listener for the main action button (Login/Sign Up)
        binding.buttonAction.setOnClickListener(v -> performAuthentication());

        // Listener for the "Done" button on the keyboard
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
                    // When authentication is successful, send the user to MainActivity
                    sendUserToMainActivity();
                    break;
                case ERROR:
                    setLoading(false);
                    // Error message is handled by its own observer
                    break;
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

        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        // Basic validation
        if (email.isEmpty()) {
            binding.emailLayout.setError("Email cannot be empty");
            return;
        }
        if (password.isEmpty()){
            binding.passwordLayout.setError("Password cannot be empty");
            return;
        }

        if (isLoginMode) {
            authViewModel.login(email, password);
        } else {
            authViewModel.signUp(email, password);
        }
    }

    /**
     * Navigates the user to MainActivity after a successful login/sign-up.
     */
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(AuthActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish(); // Finish AuthActivity
    }

    /**
     * Updates all UI text based on whether the user is logging in or signing up.
     */
    private void updateUiForMode() {
        if (isLoginMode) {
            binding.subtitleText.setText("Welcome back! Please log in.");
            binding.buttonAction.setText("Login");
            binding.toggleText.setText("Don't have an account? Sign Up");
        } else {
            binding.subtitleText.setText("Create a new account to get started.");
            binding.buttonAction.setText("Sign Up");
            binding.toggleText.setText("Already have an account? Login");
        }
        // Clear any previous errors
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
    }

    /**
     * Manages the UI state when a network request is in progress.
     */
    private void setLoading(boolean isLoading) {
        binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // Hide the button text while loading
        binding.buttonAction.setText(isLoading ? "" : (isLoginMode ? "Login" : "Sign Up"));
        binding.buttonAction.setEnabled(!isLoading);
        binding.emailEditText.setEnabled(!isLoading);
        binding.passwordEditText.setEnabled(!isLoading);
        binding.toggleText.setEnabled(!isLoading); // Disable the toggle text as well
    }
}
