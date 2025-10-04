package com.segi.student.blog.ui.my;

import android.net.Uri;import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import androidx.annotation.NonNull;

public class MyViewModel extends ViewModel {

    public static class Profile {
        public String avatarUrl;
        public String displayName;
        public String email;
        public String bio;
    }

    public enum UiState { IDLE, LOADING, ERROR, SUCCESS }

    private final MutableLiveData<Profile> _profile = new MutableLiveData<>();
    public final LiveData<Profile> profile = _profile;

    private final MutableLiveData<UiState> _profileLoadState = new MutableLiveData<>(UiState.IDLE);
    public final LiveData<UiState> profileLoadState = _profileLoadState;

    private final MutableLiveData<UiState> _avatarUploadState = new MutableLiveData<>(UiState.IDLE);
    public final LiveData<UiState> avatarUploadState = _avatarUploadState;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference userProfileRef;
    private final StorageReference avatarStorageRef;

    // ADD A NEW LIVEDATA FOR PROFILE UPDATE STATE
    private final MutableLiveData<UiState> _profileUpdateState = new MutableLiveData<>(UiState.IDLE);
    public final LiveData<UiState> profileUpdateState = _profileUpdateState;

    public MyViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String dbUrl = "https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/";
            userProfileRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(uid).child("profile");
            avatarStorageRef = FirebaseStorage.getInstance().getReference("avatars").child(uid);
            loadProfile();
        } else {
            userProfileRef = null;
            avatarStorageRef = null;
            _profileLoadState.setValue(UiState.ERROR);
            _errorMessage.setValue("User not signed in.");
        }
    }

    public void updateProfile(String newNickname, String newBio) {
        if (userProfileRef == null) {
            _errorMessage.setValue("Cannot update profile. User not signed in.");
            _profileUpdateState.setValue(UiState.ERROR);
            return;
        }

        if (newNickname == null || newNickname.trim().isEmpty()) {
            _errorMessage.setValue("Nickname cannot be empty.");
            _profileUpdateState.setValue(UiState.ERROR);
            return;
        }
        _profileUpdateState.setValue(UiState.LOADING);

        Map<String, Object> profileUpdates = new HashMap<>();
        profileUpdates.put("displayName", newNickname.trim());
        profileUpdates.put("bio", newBio != null ? newBio.trim() : "");

        userProfileRef.updateChildren(profileUpdates)
                .addOnSuccessListener(aVoid -> _profileUpdateState.setValue(UiState.SUCCESS))
                .addOnFailureListener(e -> {
                    _errorMessage.setValue(e.getMessage());
                    _profileUpdateState.setValue(UiState.ERROR);
                });
    }

    // ADD A METHOD TO RESET THE UPDATE STATE
    public void resetProfileUpdateState() {
        _profileUpdateState.setValue(UiState.IDLE);
    }

    private void loadProfile() {
        _profileLoadState.setValue(UiState.LOADING);
        if (userProfileRef == null) return;

        userProfileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Profile userProfile = snapshot.getValue(Profile.class);
                if (userProfile != null) {
                    _profile.setValue(userProfile);
                    _profileLoadState.setValue(UiState.SUCCESS);
                } else {
                    // If no profile exists, create a default one
                    createDefaultProfile();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                _errorMessage.setValue(error.getMessage());
                _profileLoadState.setValue(UiState.ERROR);
            }
        });
    }

    private void createDefaultProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || userProfileRef == null) return;

        Profile defaultProfile = new Profile();
        defaultProfile.email = user.getEmail();
        defaultProfile.displayName = "User"; // Default name
        defaultProfile.avatarUrl = null;
        defaultProfile.bio = "";

        userProfileRef.setValue(defaultProfile); // This will trigger onDataChange again
    }

    public void updateAvatar(Uri imageUri) {
        if (imageUri == null || avatarStorageRef == null) return;
        _avatarUploadState.setValue(UiState.LOADING);

        final StorageReference fileRef = avatarStorageRef.child(UUID.randomUUID().toString() + ".png");
        fileRef.putFile(imageUri)
                .onSuccessTask(taskSnapshot -> fileRef.getDownloadUrl())
                .addOnSuccessListener(downloadUri -> {
                    userProfileRef.child("avatarUrl").setValue(downloadUri.toString());
                    _avatarUploadState.setValue(UiState.SUCCESS);
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue(e.getMessage());
                    _avatarUploadState.setValue(UiState.ERROR);
                });
    }

    public void resetAvatarUploadState() {
        _avatarUploadState.setValue(UiState.IDLE);
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}

