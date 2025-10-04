package com.segi.student.blog;

import android.util.Patterns;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Repository for handling authentication using Firebase.
 * <p>
 * This class uses FirebaseAuth to perform login and sign-up operations.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public void login(String email, String password, AuthCallback callback) {
        // Basic validation before calling Firebase
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            callback.onError("Invalid email format");
            return;
        }
        if (password == null || password.isEmpty()) {
            callback.onError("Password cannot be empty");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        // Provide a more generic error to avoid account enumeration attacks
                        callback.onError("Authentication failed. Please check your credentials.");
                    }
                });
    }

    public void signUp(String email, String password, AuthCallback callback) {
        // Basic validation before calling Firebase
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            callback.onError("Invalid email format");
            return;
        }
        if (password == null || password.length() < 8) {
            callback.onError("Password must be at least 8 characters");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        // Firebase provides specific error messages (e.g., email already in use)
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Sign up failed. Please try again.";
                        callback.onError(errorMessage);
                    }
                });
    }

    // Optional: Add a method to check if a user is already signed in
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Optional: Add a sign-out method
    public void signOut() {
        firebaseAuth.signOut();
    }
}
