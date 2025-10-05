package com.segi.student.blog.ui.my;

import android.util.Log;

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
import java.util.ArrayList;
import java.util.List;

public class UserListViewModel extends ViewModel {

    public enum ListMode {
        FOLLOWING,
        FOLLOWERS
    }
    // 2. 定义一个TAG，方便在Logcat中过滤
    private static final String TAG = "UserListViewModel_DEBUG";
    private final MutableLiveData<List<UserItem>> _users = new MutableLiveData<>();
    public final LiveData<List<UserItem>> users = _users;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final DatabaseReference dbRef;
    private final String currentUserId;

    public UserListViewModel() {
        this.dbRef = FirebaseDatabase.getInstance("https://studentblog-bd92d-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void loadUsers(ListMode mode) {
        if (currentUserId == null) {
            _error.setValue("User not signed in.");
            return;
        }
        Log.d(TAG, "--- 开始加载用户列表 ---");
        Log.d(TAG, "当前用户ID: " + currentUserId);
        Log.d(TAG, "加载模式: " + mode.toString());
        _isLoading.setValue(true);

        String listPath = (mode == ListMode.FOLLOWING)
                ? "users/" + currentUserId + "/following"
                : "users/" + currentUserId + "/followers";
        // 3. 打印将要查询的Firebase路径
        Log.d(TAG, "正在查询Firebase路径: " + listPath);
        // 1. Get the list of User IDs from the path (e.g., /users/{uid}/following)
        dbRef.child(listPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "路径 " + listPath + " 下没有找到任何数据。列表将为空。");
                    _users.setValue(new ArrayList<>());
                    _isLoading.setValue(false);
                    return;
                }
                Log.d(TAG, "成功获取到用户ID列表! 共有 " + snapshot.getChildrenCount() + " 个用户。");
                // 2. For each User ID found, create a task to fetch that user's full profile
                List<Task<DataSnapshot>> tasks = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (userId != null) {
                        Log.d(TAG, "准备获取用户详情, 用户ID: " + userId);
                        // --- 这是需要修改的地方 ---
                        // 告诉 Firebase 去 users/{userId}/profile 节点获取数据
                        tasks.add(dbRef.child("users").child(userId).child("profile").get());
                        // --- 修改结束 ---
                    }
                }

                // 3. When all user profile fetches are complete, process them
                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    Log.d(TAG, "所有用户详情已成功获取，开始处理数据...");
                    List<UserItem> resultList = new ArrayList<>();
                    for (Object result : results) {
                        DataSnapshot userDataSnapshot = (DataSnapshot) result;
                        UserItem user = userDataSnapshot.getValue(UserItem.class);
                        if (user != null) {
                            // --- 这里也需要修改 ---
                            // 现在 userDataSnapshot 是 "profile" 节点，
                            // 我们需要从它的父节点获取 UID
                            String uid = userDataSnapshot.getRef().getParent().getKey();
                            user.setUid(uid); // 手动设置 UID
                            // --- 修改结束 ---

                            Log.d(TAG, "成功解析用户: " + user.getDisplayName() + " (UID: " + user.getUid() + ")");
                            resultList.add(user);
                            // --- 修改结束 ---
                        } else {
                            Log.w(TAG, "解析用户失败, snapshot: " + userDataSnapshot.toString());
                        }
                    }

                    _users.setValue(resultList);
                    _isLoading.setValue(false);
                    Log.d(TAG, "--- 加载完成，最终列表包含 " + resultList.toString() + " 用户 ---");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "获取部分或全部用户详情时失败。", e);
                    _error.setValue("Failed to fetch user profiles: " + e.getMessage());
                    _isLoading.setValue(false);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "数据库查询被取消或失败。", databaseError.toException());
                _error.setValue(databaseError.getMessage());
                _isLoading.setValue(false);
            }
        });
    }
}
