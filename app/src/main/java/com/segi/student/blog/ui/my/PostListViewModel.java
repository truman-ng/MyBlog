package com.segi.student.blog.ui.my;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.segi.student.blog.ui.blogs.BlogsViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostListViewModel extends ViewModel {

    // Enum to define which list we are fetching
    public enum ListMode {
        READING_HISTORY,
        LIKES,
        BOOKMARKS // <-- NEW}
    }

    private final MutableLiveData<List<BlogsViewModel.Post>> _posts = new MutableLiveData<>();
    public final LiveData<List<BlogsViewModel.Post>> posts = _posts;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final DatabaseReference dbRef;
    private final String currentUserId;

    public PostListViewModel() {
        this.dbRef = FirebaseDatabase.getInstance("https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadPosts(ListMode mode) {
        if (currentUserId == null) {
            _error.setValue("User not signed in.");
            return;
        }
        _isLoading.setValue(true);

        // Determine the path to the list of Post IDs based on the mode
        String listPath = "";
        if (mode == ListMode.READING_HISTORY) {
            listPath = "users/" + currentUserId + "/readingList";
        } else if (mode == ListMode.LIKES) {
            listPath = "users/" + currentUserId + "/likedPosts";
        } else if (mode == ListMode.BOOKMARKS) { // <-- NEW
            listPath = "users/" + currentUserId + "/bookmarks";
        }else {
            _isLoading.setValue(false);
            return;
        }

        // 1. First, get the list of Post IDs from the user's profile
        dbRef.child(listPath).orderByValue().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    _posts.setValue(new ArrayList<>());
                    _isLoading.setValue(false);
                    return;
                }

                List<Task<DataSnapshot>> tasks = new ArrayList<>();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String postId = postSnapshot.getKey();
                    if (postId != null) {
                        // 2. For each Post ID, create a task to fetch the full post data from /feeds/global
                        Task<DataSnapshot> task = dbRef.child("feeds/global").child(postId).get();
                        tasks.add(task);
                    }
                }

                // 3. When all fetch tasks are complete, process the results
                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    List<BlogsViewModel.Post> resultList = new ArrayList<>();
                    for (Object result : results) {
                        DataSnapshot postDataSnapshot = (DataSnapshot) result;
                        BlogsViewModel.Post post = postDataSnapshot.getValue(BlogsViewModel.Post.class);
                        if (post != null) {
                            post.setPostId(postDataSnapshot.getKey());
                            resultList.add(post);
                        }
                    }
                    // The posts are fetched in ascending timestamp order, so we reverse to show newest first.
                    Collections.reverse(resultList);
                    _posts.setValue(resultList);
                    _isLoading.setValue(false);
                }).addOnFailureListener(e -> {
                    _error.setValue("Failed to fetch posts: " + e.getMessage());
                    _isLoading.setValue(false);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                _error.setValue(error.getMessage());
                _isLoading.setValue(false);
            }
        });
    }
}
