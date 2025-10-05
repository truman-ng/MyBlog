package com.segi.student.blog.ui.my;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.WriteBlogActivity;
import com.segi.student.blog.databinding.FragmentDraftsBinding;
import com.segi.student.blog.ui.blogs.BlogsAdapter;
import com.segi.student.blog.ui.blogs.BlogsViewModel;

import java.util.ArrayList;

public class DraftsFragment extends Fragment {

    private FragmentDraftsBinding binding;
    private DraftsViewModel draftsViewModel;
    private BlogsAdapter draftsAdapter; // 复用 BlogsAdapter

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDraftsBinding.inflate(inflater, container, false);
        draftsViewModel = new ViewModelProvider(this).get(DraftsViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次返回都刷新草稿列表
        draftsViewModel.loadDrafts();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupRecyclerView() {
        draftsAdapter = new BlogsAdapter(); // 复用现有的 Adapter
        binding.draftsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.draftsRecyclerView.setAdapter(draftsAdapter);

        // 设置点击事件
        draftsAdapter.setOnItemClickListener(post -> {
            Intent intent = new Intent(getActivity(), WriteBlogActivity.class);
            // 传入 postId，以便 WriteBlogActivity 加载草稿
            intent.putExtra(WriteBlogActivity.EXTRA_POST_ID, post.getPostId());
            startActivity(intent);
        });
        // --- NEW: Handle long clicks ---
        draftsAdapter.setOnItemLongClickListener(post -> {
            showDeleteConfirmationDialog(post);
        });
    }

    // --- NEW: Method to show the confirmation dialog ---
    private void showDeleteConfirmationDialog(BlogsViewModel.Post post) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Draft")
                .setMessage("Are you sure you want to permanently delete this draft?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Call the ViewModel to delete the post
                    draftsViewModel.deleteDraft(post.getPostId());
                })
                .show();
    }
    private void setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> draftsViewModel.loadDrafts());
    }

    private void observeViewModel() {
        draftsViewModel.drafts.observe(getViewLifecycleOwner(), drafts -> {
            draftsAdapter.submitList(new ArrayList<>(drafts));
            binding.emptyView.setVisibility(drafts.isEmpty() && !draftsViewModel.isLoading.getValue() ? View.VISIBLE : View.GONE);
        });

        draftsViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading && draftsAdapter.getItemCount() == 0) {
                binding.progressIndicator.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
                binding.progressIndicator.setVisibility(View.GONE);
            }
            if (!isLoading && binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        draftsViewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
