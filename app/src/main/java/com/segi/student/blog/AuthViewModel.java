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
    // ADD A NEW LIVEDATA FOR THE PASSWORD CHANGE STATE
    private final MutableLiveData<AuthState> _passwordChangeState = new MutableLiveData<>(AuthState.IDLE);
    public final LiveData<AuthState> passwordChangeState = _passwordChangeState;
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
    // ADD THIS NEW METHOD
    public void changePassword(String oldPassword, String newPassword) {
        _passwordChangeState.setValue(AuthState.LOADING);
        authRepository.changePassword(oldPassword, newPassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                _passwordChangeState.setValue(AuthState.SUCCESS);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
                _passwordChangeState.setValue(AuthState.ERROR);
            }
        });
    }

    // ADD A METHOD TO RESET THE STATE
    public void resetPasswordChangeState() {
        _passwordChangeState.setValue(AuthState.IDLE);
    }
    // Resets the state to IDLE, typically after an error has been shown.
    public void resetState() {
        _authState.setValue(AuthState.IDLE);
    }
}