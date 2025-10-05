package com.segi.student.blog.ui.detail;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.HashMap;
import java.util.Map;

public class BlogDetailViewModel extends ViewModel {

    // Data Models
    public static class Post {
        public String title, contentMd, coverUrl, status, authorId; // Added authorId
        public long updatedAt;
    }

    public static class Author {
        public String displayName, avatarUrl, bio;
    }

    public static class InteractionState {
        public boolean isLiked = false;
        public boolean isFollowing = false;
        public boolean isBookmarked = false;
        public boolean isOwnPost = false;
    }

    // LiveData
    private final MutableLiveData<Post> _post = new MutableLiveData<>();
    public final LiveData<Post> post = _post;

    private final MutableLiveData<Author> _author = new MutableLiveData<>();
    public final LiveData<Author> author = _author;

    private final MutableLiveData<InteractionState> _interactionState = new MutableLiveData<>(new InteractionState());
    public final LiveData<InteractionState> interactionState = _interactionState;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final DatabaseReference dbRef;
    private final String currentUserId;
    private String authorId;
    private String postId;

    public BlogDetailViewModel() {
        this.dbRef = FirebaseDatabase.getInstance("https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadPost(String postId) {
        this.postId = postId;
        _isLoading.setValue(true);

        // Fetch post from /feeds/global (it's a public post)
        dbRef.child("feeds/global").child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Post fetchedPost = snapshot.getValue(Post.class);
                if (fetchedPost != null) {
                    _post.setValue(fetchedPost);
                    authorId = fetchedPost.authorId;
                    if (authorId != null) {
                        fetchAuthorDetails();
                        checkInteractionStates();
                        // --- NEW: Automatically log to reading history ---
                        logReadingHistory();
                    }
                } else {
                    _error.setValue("Post not found.");
                }
                _isLoading.setValue(false);
            }
            @Override public void onCancelled(DatabaseError error) {
                _error.setValue(error.getMessage());
                _isLoading.setValue(false);
            }
        });
    }
    // --- NEW: Add this method to the ViewModel ---
    private void logReadingHistory() {
        if (currentUserId == null || postId == null) {
            return; // Don't log if user isn't signed in or post is invalid
        }
        // Set the value to a timestamp. This allows sorting by recently viewed.
        // If the entry already exists, its timestamp will simply be updated.
        dbRef.child("users").child(currentUserId).child("readingHistory").child(postId)
                .setValue(ServerValue.TIMESTAMP);
    }
    private void fetchAuthorDetails() {
        // 在路径末尾添加 .child("profile") 来获取嵌套的用户信息
        dbRef.child("users").child(authorId).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // 现在 snapshot 指向 "profile" 节点，这里的代码可以正确解析Author对象
                _author.setValue(snapshot.getValue(Author.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 可以选择在这里处理错误，例如记录日志
                Log.e("BlogDetailViewModel", "获取作者详情失败: " + error.getMessage());
            }
        });
        // --- 修改结束 ---
    }

    private void checkInteractionStates() {
        InteractionState newState = new InteractionState();
        newState.isOwnPost = currentUserId != null && currentUserId.equals(authorId);

        if (currentUserId == null) {
            _interactionState.setValue(newState);
            return;
        }

        // Check Following
        dbRef.child("users").child(currentUserId).child("following").child(authorId).get().addOnCompleteListener(task -> {
            newState.isFollowing = task.isSuccessful() && task.getResult().exists();
            _interactionState.setValue(newState);
        });

        // Check Liked
        dbRef.child("users").child(currentUserId).child("likedPosts").child(postId).get().addOnCompleteListener(task -> {
            newState.isLiked = task.isSuccessful() && task.getResult().exists();
            _interactionState.setValue(newState);
        });

        // Check Reading List
        dbRef.child("users").child(currentUserId).child("bookmarks").child(postId).get().addOnCompleteListener(task -> {
            newState.isBookmarked = task.isSuccessful() && task.getResult().exists();
            _interactionState.setValue(newState);
        });
    }

    public void toggleLike() {
        boolean isCurrentlyLiked = _interactionState.getValue().isLiked;
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + currentUserId + "/likedPosts/" + postId, !isCurrentlyLiked ? ServerValue.TIMESTAMP : null);
        dbRef.updateChildren(updates);
    }

    public void toggleFollow() {
        // Make sure we have the required IDs
        if (currentUserId == null || authorId == null) {
            return;
        }

        // Get the current state *before* making changes
        boolean isCurrentlyFollowing = _interactionState.getValue() != null && _interactionState.getValue().isFollowing;

        Map<String, Object> updates = new HashMap<>();

        // CORE LOGIC:
        // If NOT currently following, add the IDs.
        // If IS currently following, set to null to delete the data.
        Object valueToSet = !isCurrentlyFollowing ? true : null;

        // Path 1: Add the author to the current user's "following" list
        updates.put("/users/" + currentUserId + "/following/" + authorId, valueToSet);

        // Path 2: Add the current user to the author's "followers" list
        updates.put("/users/" + authorId + "/followers/" + currentUserId, valueToSet);

        dbRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Manually update the local UI state immediately for responsiveness
                InteractionState newState = _interactionState.getValue();
                if (newState != null) {
                    newState.isFollowing = !isCurrentlyFollowing;
                    _interactionState.setValue(newState);
                }
            } else {
                // Optional: Show an error if the follow/unfollow failed
            }
        });
    }

    public void toggleBookmark() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + currentUserId + "/bookmarks/" + postId, ServerValue.TIMESTAMP); // Always update timestamp
        dbRef.updateChildren(updates);
    }
}
