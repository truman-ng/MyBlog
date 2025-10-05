package com.segi.student.blog.ui.detail;

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
        public boolean isInReadingList = false;
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

    private void fetchAuthorDetails() {
        dbRef.child("users").child(authorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                _author.setValue(snapshot.getValue(Author.class));
            }
            @Override public void onCancelled(DatabaseError error) { /* Ignore */ }
        });
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
        dbRef.child("users").child(currentUserId).child("readingList").child(postId).get().addOnCompleteListener(task -> {
            newState.isInReadingList = task.isSuccessful() && task.getResult().exists();
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
        boolean isCurrentlyFollowing = _interactionState.getValue().isFollowing;
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + currentUserId + "/following/" + authorId, !isCurrentlyFollowing ? true : null);
        updates.put("/users/" + authorId + "/followers/" + currentUserId, !isCurrentlyFollowing ? true : null);
        dbRef.updateChildren(updates);
    }

    public void toggleReadingList() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + currentUserId + "/readingList/" + postId, ServerValue.TIMESTAMP); // Always update timestamp
        dbRef.updateChildren(updates);
    }
}
