package com.segi.student.blog.ui.blogs;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BlogsViewModel extends ViewModel {

    private static final int PAGE_SIZE = 10;
    private final DatabaseReference feedsRef; // 指向 /feeds/global

    // LiveData 和分页状态
    private final MutableLiveData<List<Post>> _posts = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Post>> posts = _posts;
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;
    private final MutableLiveData<Boolean> _hasMore = new MutableLiveData<>(true);
    public final LiveData<Boolean> hasMore = _hasMore;
    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;
    private long lastPostTimestamp = -1;
    private boolean isFetching = false;

    // 数据模型 (保持简单，不需要 author)
    public static class Post {
        private String postId;
        private String title;
        private String coverUrl;
        private long updatedAt;

        public Post() {}

        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
        public String getTitle() { return title; }
        public String getCoverUrl() { return coverUrl; }
        public long getUpdatedAt() { return updatedAt; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Post post = (Post) o;
            return updatedAt == post.updatedAt && postId.equals(post.postId) && Objects.equals(title, post.title) && Objects.equals(coverUrl, post.coverUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, title, coverUrl, updatedAt);
        }
    }

    public BlogsViewModel() {
        String dbUrl = "https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/";
        feedsRef = FirebaseDatabase.getInstance(dbUrl).getReference("feeds/global");
    }

    public void loadInitialPosts() {
        if (isFetching) return;
        isFetching = true;
        _isLoading.setValue(true);
        _hasMore.setValue(true);
        lastPostTimestamp = -1;
        Query query = feedsRef.orderByChild("updatedAt").limitToLast(PAGE_SIZE);
        fetchPosts(query, true);
    }

    public void loadNextPage() {
        if (isFetching || !_hasMore.getValue()) return;
        isFetching = true;
        _isLoading.setValue(true);
        Query query = feedsRef.orderByChild("updatedAt").endAt(lastPostTimestamp - 1).limitToLast(PAGE_SIZE);
        fetchPosts(query, false);
    }

    private void fetchPosts(Query query, boolean isInitialLoad) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    _hasMore.setValue(false);
                    if (isInitialLoad) {
                        _posts.setValue(new ArrayList<>());
                    }
                    finishLoading();
                    return;
                }

                List<Post> newPosts = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snapshot.getKey());
                        newPosts.add(post);
                    }
                }

                Collections.reverse(newPosts);

                if (!newPosts.isEmpty()) {
                    lastPostTimestamp = newPosts.get(newPosts.size() - 1).getUpdatedAt();
                }

                _hasMore.setValue(newPosts.size() == PAGE_SIZE);

                if (isInitialLoad) {
                    _posts.setValue(newPosts);
                } else {
                    List<Post> currentPosts = new ArrayList<>(_posts.getValue());
                    currentPosts.addAll(newPosts);
                    _posts.setValue(currentPosts);
                }
                finishLoading();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                _error.setValue(databaseError.getMessage());
                finishLoading();
            }
        });
    }

    private void finishLoading() {
        isFetching = false;
        _isLoading.setValue(false);
    }
}
