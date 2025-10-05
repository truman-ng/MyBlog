package com.segi.student.blog.ui.my;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.segi.student.blog.ui.blogs.BlogsViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DraftsViewModel extends ViewModel {

    private final MutableLiveData<List<BlogsViewModel.Post>> _drafts = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<BlogsViewModel.Post>> drafts = _drafts;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private DatabaseReference userPostsRef;
    private final String currentUserId; // Store the user ID for deletion

    public DraftsViewModel() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.currentUserId = currentUser.getUid();
            String uid = currentUser.getUid();
            String dbUrl = "https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/";
            userPostsRef = FirebaseDatabase.getInstance(dbUrl).getReference("posts").child(uid);
        } else {
            this.currentUserId = null; // <-- Handle case where user is null
        }
    }
    // --- NEW: Method to delete a draft ---
    public void deleteDraft(String postId) {
        if (currentUserId == null || postId == null) {
            _error.setValue("Cannot delete. User or Post ID is invalid.");
            return;
        }

        // Use a multi-path update to delete the draft from both locations
        // to ensure data consistency.
        Map<String, Object> fanOutData = new HashMap<>();
        fanOutData.put("/posts/" + currentUserId + "/" + postId, null);
        fanOutData.put("/feeds/global/" + postId, null); // Also remove from any potential global feed entry

        DatabaseReference rootRef = FirebaseDatabase.getInstance("https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();

        rootRef.updateChildren(fanOutData)
                .addOnSuccessListener(aVoid -> {
                    // Success! Now, remove the item locally from the list to update the UI instantly.
                    List<BlogsViewModel.Post> currentList = new ArrayList<>(_drafts.getValue());
                    currentList.removeIf(p -> p.getPostId().equals(postId));
                    _drafts.setValue(currentList);
                })
                .addOnFailureListener(e -> {
                    _error.setValue("Failed to delete draft: " + e.getMessage());
                });
    }
    public void loadDrafts() {
        if (userPostsRef == null) {
            _error.setValue("User not signed in.");
            return;
        }

        _isLoading.setValue(true);
        // 查询 status 字段等于 "draft" 的帖子
        Query draftsQuery = userPostsRef.orderByChild("status").equalTo("draft");

        draftsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    _drafts.setValue(new ArrayList<>());
                } else {
                    List<BlogsViewModel.Post> resultList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        BlogsViewModel.Post post = snapshot.getValue(BlogsViewModel.Post.class);
                        if (post != null) {
                            post.setPostId(snapshot.getKey());
                            resultList.add(post);
                        }
                    }
                    // 按更新时间降序排序
                    Collections.sort(resultList, (p1, p2) -> Long.compare(p2.getUpdatedAt(), p1.getUpdatedAt()));
                    _drafts.setValue(resultList);
                }
                _isLoading.setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                _error.setValue(databaseError.getMessage());
                _isLoading.setValue(false);
            }
        });
    }
}
