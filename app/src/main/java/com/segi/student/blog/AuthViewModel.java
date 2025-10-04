package com.segi.student.blog;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {

    // Enum to represent the current state of an authentication operation.
    public enum AuthState {
        IDLE,       // Not performing any operation
        LOADING,    // An operation is in progress
        SUCCESS,    // The operation completed successfully
        ERROR       // The operation failed
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<AuthState> _authState = new MutableLiveData<>(AuthState.IDLE);
    public final LiveData<AuthState> authState = _authState;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
    }

    public void login(String email, String password) {
        _authState.setValue(AuthState.LOADING);
        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                _authState.setValue(AuthState.SUCCESS);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
                _authState.setValue(AuthState.ERROR);
            }
        });
    }

    public void signUp(String email, String password) {
        _authState.setValue(AuthState.LOADING);
        authRepository.signUp(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                _authState.setValue(AuthState.SUCCESS);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
                _authState.setValue(AuthState.ERROR);
            }
        });
    }

    // Resets the state to IDLE, typically after an error has been shown.
    public void resetState() {
        _authState.setValue(AuthState.IDLE);
    }
}