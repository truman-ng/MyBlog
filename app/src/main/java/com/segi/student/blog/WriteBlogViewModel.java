package com.segi.student.blog;import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
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
        public String postId = null;
        public boolean hasUnsavedChanges = false;
        public String initialContentMd = "";

        // --- START: Add Missing Fields for Firebase Deserialization ---
        public String coverUrl;
        public long createdAt;
        public String status;
        public long updatedAt;
        // --- END: Add Missing Fields ---
        public FormState() {
        }
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
    private final FirebaseDatabase database; // <-- 获取 Database 实例
    private final StorageReference userCoversRef;
    private final String currentUserId;

    // --- START: 新增加载草稿的方法 ---
    public void loadDraft(String postId) {
        if (currentUserId == null || postId == null) {
            _error.setValue("Cannot load draft. User or Post ID is missing.");
            return;
        }

        _isLoading.setValue(true);
        DatabaseReference draftRef = database.getReference("posts").child(currentUserId).child(postId);

        draftRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FormState draftState = snapshot.getValue(FormState.class);
                    if (draftState != null) {
                        // --- START: 核心修复 ---
                        // 将从Firebase加载的 coverUrl 赋值给用于UI显示的 remoteCoverUrl
                        draftState.remoteCoverUrl = draftState.coverUrl;
                        // --- END: 核心修复 ---

                        draftState.postId = postId; // 确保 postId 被设置
                        draftState.hasUnsavedChanges = false; // 初始加载，无改动
                        draftState.initialContentMd = draftState.contentMd; // 设置初始内容基线
                        _formState.setValue(draftState);
                    }
                } else {
                    _error.setValue("Draft not found.");
                }
                _isLoading.setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                _error.setValue("Failed to load draft: " + error.getMessage());
                _isLoading.setValue(false);
            }
        });
    }
    // --- END: 新增加载草稿的方法 ---

    public WriteBlogViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String dbUrl = "https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/";
        database = FirebaseDatabase.getInstance(dbUrl); // <-- 初始化 Database

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            userCoversRef = FirebaseStorage.getInstance().getReference("covers").child(currentUserId);
        } else {
            currentUserId = null;
            userCoversRef = null;
            _error.setValue("User not signed in. Cannot save post.");
        }
    }

    // --- Public Actions from UI (保持不变) ---
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
        if (currentState.remoteCoverUrl != null && userCoversRef != null) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(currentState.remoteCoverUrl).delete();
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
        currentState.localCoverUri = null;
        currentState.remoteCoverUrl = null;
        currentState.hasUnsavedChanges = true;
        _formState.setValue(currentState);
    }

    public void saveDraft() {
        if (!validate(false)) return;
        savePost(PostStatus.DRAFT);
    }

    public void publish() {
        if (!validate(true)) return;
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
            currentState.hasUnsavedChanges = false;
            _formState.setValue(currentState);
        }
        _backEvent.setValue(BackEvent.FINISH_IMMEDIATELY);
    }

    public void clearEventMessages() {
        _error.setValue(null);
        _successMessage.setValue(null);
        _backEvent.setValue(null);
    }


    // --- Internal Logic (核心修改区域) ---

    private void savePost(PostStatus status) {
        if (currentUserId == null || userCoversRef == null) {
            _error.setValue("Cannot save post. User not signed in.");
            return;
        }

        _isLoading.setValue(true);
        FormState currentState = Objects.requireNonNull(_formState.getValue());

        if (currentState.localCoverUri != null) {
            uploadCoverImage(currentState, status);
        } else {
            writePostToDatabase(currentState.remoteCoverUrl, currentState, status);
        }
    }

    private void uploadCoverImage(FormState state, PostStatus status) {
        final StorageReference fileRef = userCoversRef.child(UUID.randomUUID().toString() + ".png");
        fileRef.putFile(state.localCoverUri)
                .onSuccessTask(taskSnapshot -> fileRef.getDownloadUrl())
                .addOnSuccessListener(downloadUri -> {
                    writePostToDatabase(downloadUri.toString(), state, status);
                })
                .addOnFailureListener(e -> {
                    _error.setValue("Image upload failed: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    /**
     * 这是核心修改方法
     */
    private void writePostToDatabase(String coverUrl, FormState state, PostStatus status) {
        String postId = state.postId;
        if (postId == null) {
            // 为新帖子生成一个唯一的 ID
            postId = database.getReference("posts").child(currentUserId).push().getKey();
        }
        if (postId == null) {
            _error.setValue("Failed to create post ID.");
            _isLoading.setValue(false);
            return;
        }

        // 1. 创建帖子数据对象
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", state.title.trim());
        postData.put("contentMd", state.contentMd.trim());
        postData.put("coverUrl", coverUrl);
        postData.put("status", status.name().toLowerCase());
        postData.put("updatedAt", ServerValue.TIMESTAMP);


        // --- START: THIS IS THE REQUIRED CHANGE ---
        // Add the author's ID to the post data.
        postData.put("authorId", currentUserId);
        // --- END: THIS IS THE REQUIRED CHANGE ---

        if (state.postId == null) { // 仅在第一次保存时设置 createdAt
            postData.put("createdAt", ServerValue.TIMESTAMP);
        }

        // 2. 构建多路径更新 Map
        Map<String, Object> fanOutData = new HashMap<>();

        // 路径 1: 写入到用户的个人帖子列表
        String userPostPath = "/posts/" + currentUserId + "/" + postId;
        fanOutData.put(userPostPath, postData);

        // 路径 2: 如果是发布状态，则同步到全局 feed
        String globalFeedPath = "/feeds/global/" + postId;
        if (status == PostStatus.PUBLISHED) {
            fanOutData.put(globalFeedPath, postData);
        } else {
            // 如果是保存为草稿，或者从发布状态改为草稿，要确保全局 feed 中没有它
            fanOutData.put(globalFeedPath, null); // 传入 null 会删除该路径的数据
        }

        final String finalPostId = postId;
        // 3. 执行一次原子性的更新操作
        database.getReference().updateChildren(fanOutData)
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
            currentState.initialContentMd = savedContent;
            _formState.setValue(currentState);
        }
        _isLoading.setValue(false);
        _successMessage.setValue(status == PostStatus.PUBLISHED ? "Post published successfully!" : "Draft saved successfully.");
    }

    private boolean validate(boolean isPublishing) {
        _error.setValue(null);
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
