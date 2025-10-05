package com.segi.student.blog.ui.blogs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // 保留日志
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.R;
import com.segi.student.blog.WriteBlogActivity;
import com.segi.student.blog.databinding.FragmentBlogsBinding;
import com.segi.student.blog.ui.detail.BlogDetailFragment;

import java.util.ArrayList;

public class BlogsFragment extends Fragment {

    private static final String TAG = "BlogsFragment"; // 保留日志 TAG

    private FragmentBlogsBinding binding;
    private BlogsViewModel blogsViewModel;
    private BlogsAdapter blogsAdapter;

    // --- START: 新增用于控制刷新的变量 ---
    // 定义一个刷新冷却时间，例如 60 秒 (60 * 1000 毫秒)
    private static final long REFRESH_COOLDOWN_MS = 60 * 1000;
    // 记录上次成功加载数据的时间戳
    private long lastRefreshTimestamp = 0;
    // --- END: 新增用于控制刷新的变量 ---

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBlogsBinding.inflate(inflater, container, false);
        blogsViewModel = new ViewModelProvider(this).get(BlogsViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        // 首次创建时，无条件加载数据
        blogsViewModel.loadInitialPosts();
        // 记录加载时间
        lastRefreshTimestamp = System.currentTimeMillis();
    }

    // --- START: 新增 onResume() 方法 ---
    /**
     * 当 Fragment 变为活动状态时被调用。
     * 这是处理“返回时刷新”逻辑的最佳位置。
     */
    @Override
    public void onResume() {
        super.onResume();
        // 检查自上次刷新以来是否已超过冷却时间
        if (System.currentTimeMillis() - lastRefreshTimestamp > REFRESH_COOLDOWN_MS) {
            Log.d(TAG, "返回到列表页面，并且超过了刷新冷却时间，开始自动刷新...");
            // 显示下拉刷新的加载动画，提供即时反馈
            if (binding != null && !binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(true);
            }
            // 调用加载方法
            blogsViewModel.loadInitialPosts();
        } else {
            Log.d(TAG, "返回到列表页面，但仍在刷新冷却时间内，本次不自动刷新。");
        }
    }
    // --- END: 新增 onResume() 方法 ---

    private void setupRecyclerView() {
        // ... 此方法内容保持不变 ...
        blogsAdapter = new BlogsAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.blogsRecyclerView.setLayoutManager(layoutManager);
        binding.blogsRecyclerView.setAdapter(blogsAdapter);
        binding.blogsRecyclerView.setHasFixedSize(true);

        // ...
        blogsAdapter.setOnItemClickListener(post -> {
            // Use NavController to navigate to the detail screen with the postId
            NavHostFragment.findNavController(this)
                    .navigate(
                            R.id.action_blogs_to_blogDetail,
                            BlogDetailFragment.createArgs(post.getPostId())
                    );
        });

        binding.blogsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (blogsViewModel.hasMore.getValue() != null && blogsViewModel.hasMore.getValue() &&
                        blogsViewModel.isLoading.getValue() != null && !blogsViewModel.isLoading.getValue()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        blogsViewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupListeners() {
        // ... 此方法内容保持不变 ...
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "用户手动下拉刷新。");
            blogsViewModel.loadInitialPosts();
        });
        binding.writeFirstPostButton.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), WriteBlogActivity.class))
        );
    }

    private void observeViewModel() {
        blogsViewModel.posts.observe(getViewLifecycleOwner(), posts -> {
            Log.d(TAG, "获取到新的博客列表数据。 数量: " + posts.size());
            blogsAdapter.submitList(new ArrayList<>(posts));
            binding.emptyView.setVisibility(posts.isEmpty() && (blogsViewModel.isLoading.getValue() != null && !blogsViewModel.isLoading.getValue()) ? View.VISIBLE : View.GONE);

            // --- START: 刷新成功后更新时间戳 ---
            // 只有在非加载状态下更新列表，才认为是“成功加载”
            if (blogsViewModel.isLoading.getValue() != null && !blogsViewModel.isLoading.getValue()) {
                lastRefreshTimestamp = System.currentTimeMillis();
                Log.d(TAG, "列表更新完成，刷新时间戳。");
            }
            // --- END: 刷新成功后更新时间戳 ---
        });

        blogsViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // ... 此方法内容保持不变 ...
            if (isLoading && blogsAdapter.getItemCount() == 0) {
                binding.progressIndicator.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.progressIndicator.setVisibility(View.GONE);
            }
            if (!isLoading && binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        blogsViewModel.error.observe(getViewLifecycleOwner(), error -> {
            // ... 此方法内容保持不变 ...
            if (error != null) {
                Log.e(TAG, "加载数据时发生错误: " + error);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry, v -> blogsViewModel.loadInitialPosts())
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
