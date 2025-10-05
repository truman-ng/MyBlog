package com.segi.student.blog;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class WriteBlogViewModel extends ViewModel {

    public static class FormState {
        public String title = "";
        public String contentMd = "";
        public Uri localCoverUri = null;
        public String remoteCoverUrl = null;
        public String postId = null; // Becomes non-null after first save
        public boolean hasUnsavedChanges = false;
        public String initialContentMd = ""; // To track changes
    }

    public enum PostStatus { DRAFT, PUBLISHED }
    public enum BackEvent { PROMPT_USER, FINISH_IMMEDIATELY }

    // State LiveData
    private final MutableLiveData<FormState> _formState = new MutableLiveData<>(new FormState());
    public final LiveData<FormState> formState = _formState;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();
    public final LiveData<String> successMessage = _successMessage;

    private final MutableLiveData<BackEvent> _backEvent = new MutableLiveData<>();
    public final LiveData<BackEvent> backEvent = _backEvent;

    // Firebase
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference userPostsRef;
    private final StorageReference userCoversRef;

    public WriteBlogViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String dbUrl = "https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/";
            userPostsRef = FirebaseDatabase.getInstance(dbUrl).getReference("posts").child(uid);
            userCoversRef = FirebaseStorage.getInstance().getReference("covers").child(uid);
        } else {
            userPostsRef = null;
            userCoversRef = null;
            _error.setValue("User not signed in. Cannot save post.");
        }
    }

    // --- Public Actions from UI ---

    public void setTitle(String title) {
        FormState currentState = _formState.getValue();
        if (currentState != null && !currentState.title.equals(title)) {
            currentState.title = title;
            currentState.hasUnsavedChanges = true;
            _formState.setValue(currentState);
        }
    }

    public void setContent(String content) {
        FormState currentState = _formState.getValue();
        if (currentState != null && !currentState.contentMd.equals(content)) {
            currentState.contentMd = content;
            currentState.hasUnsavedChanges = !content.equals(currentState.initialContentMd);
            _formState.setValue(currentState);
        }
    }

    public void setCoverLocalUri(Uri uri) {
        FormState currentState = _formState.getValue();
        if (currentState != null) {
            currentState.localCoverUri = uri;
            currentState.hasUnsavedChanges = true;
            _formState.setValue(currentState);
        }
    }

    public void removeCover() {
        FormState currentState = _formState.getValue();
        if (currentState == null) return;

        // If there was a remote URL, delete it from storage
        if (currentState.remoteCoverUrl != null && userCoversRef != null) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(currentState.remoteCoverUrl).delete();
            } catch (IllegalArgumentException e) {
                // Ignore if URL is invalid or object is already deleted
            }
        }
        currentState.localCoverUri = null;
        currentState.remoteCoverUrl = null;
        currentState.hasUnsavedChanges = true;
        _formState.setValue(currentState);
    }

    public void saveDraft() {
        if (!validate(false)) return; // No validation for draft
        savePost(PostStatus.DRAFT);
    }

    public void publish() {
        if (!validate(true)) return; // Strict validation for publish
        savePost(PostStatus.PUBLISHED);
    }

    public void onBackAttempt() {
        FormState currentState = _formState.getValue();
        if (currentState != null && currentState.hasUnsavedChanges) {
            _backEvent.setValue(BackEvent.PROMPT_USER);
        } else {
            _backEvent.setValue(BackEvent.FINISH_IMMEDIATELY);
        }
    }

    public void discardAndExit() {
        FormState currentState = _formState.getValue();
        if (currentState != null) {
            currentState.hasUnsavedChanges = false; // Mark as handled
            _formState.setValue(currentState);
        }
        _backEvent.setValue(BackEvent.FINISH_IMMEDIATELY);
    }

    public void clearEventMessages() {
        _error.setValue(null);
        _successMessage.setValue(null);
        _backEvent.setValue(null);
    }

    // --- Internal Logic ---

    private void savePost(PostStatus status) {
        if (userPostsRef == null || userCoversRef == null) return;

        _isLoading.setValue(true);
        FormState currentState = Objects.requireNonNull(_formState.getValue());

        // Step 1: Upload cover image if a new one is selected
        if (currentState.localCoverUri != null) {
            uploadCoverImage(currentState, status);
        } else {
            // Step 2: Proceed to save post data directly
            writePostToDatabase(currentState.remoteCoverUrl, currentState, status);
        }
    }

    private void uploadCoverImage(FormState state, PostStatus status) {
        final StorageReference fileRef = userCoversRef.child(UUID.randomUUID().toString() + ".png");
        fileRef.putFile(state.localCoverUri)
                .onSuccessTask(taskSnapshot -> fileRef.getDownloadUrl())
                .addOnSuccessListener(downloadUri -> {
                    // Step 2: Save post data with the new image URL
                    writePostToDatabase(downloadUri.toString(), state, status);
                })
                .addOnFailureListener(e -> {
                    _error.setValue("Image upload failed: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    private void writePostToDatabase(String coverUrl, FormState state, PostStatus status) {
        String postId = state.postId;
        if (postId == null) {
            postId = userPostsRef.push().getKey();
        }
        if (postId == null) {
            _error.setValue("Failed to create post ID.");
            _isLoading.setValue(false);
            return;
        }

        Map<String, Object> postData = new HashMap<>();
        postData.put("title", state.title.trim());
        postData.put("contentMd", state.contentMd.trim());
        postData.put("coverUrl", coverUrl);
        postData.put("status", status.name().toLowerCase());
        postData.put("updatedAt", ServerValue.TIMESTAMP);

        if (state.postId == null) { // Only set createdAt on first save
            postData.put("createdAt", ServerValue.TIMESTAMP);
        }

        final String finalPostId = postId;
        userPostsRef.child(postId).updateChildren(postData)
                .addOnSuccessListener(aVoid -> onSaveSuccess(finalPostId, state.contentMd.trim(), status))
                .addOnFailureListener(e -> {
                    _error.setValue("Failed to save post: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    private void onSaveSuccess(String finalPostId, String savedContent, PostStatus status) {
        FormState currentState = _formState.getValue();
        if (currentState != null) {
            currentState.postId = finalPostId;
            currentState.hasUnsavedChanges = false;
            currentState.initialContentMd = savedContent; // Update baseline for changes
            _formState.setValue(currentState);
        }
        _isLoading.setValue(false);
        _successMessage.setValue(status == PostStatus.PUBLISHED ? "Post published successfully!" : "Draft saved successfully.");
    }

    private boolean validate(boolean isPublishing) {
        _error.setValue(null); // Clear previous errors
        FormState state = Objects.requireNonNull(_formState.getValue());

        if (isPublishing) {
            if (state.title.trim().isEmpty()) {
                _error.setValue("Title cannot be empty.");
                return false;
            }
            if (state.contentMd.trim().length() < 10) {
                _error.setValue("Content must be at least 10 characters.");
                return false;
            }
        }
        return true;
    }
}
