package com.segi.student.blog.ui.my;

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

import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.R;
import com.segi.student.blog.databinding.FragmentPostListBinding;
import com.segi.student.blog.ui.blogs.BlogsAdapter;
import com.segi.student.blog.ui.detail.BlogDetailFragment;

import java.util.ArrayList;

public class PostListFragment extends Fragment {

    private FragmentPostListBinding binding;
    private PostListViewModel viewModel;
    private BlogsAdapter adapter; // Reuse the existing adapter
    private PostListViewModel.ListMode currentMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // --- START: FIX ---
            // 1. Get the argument as a String, which is how defaultValue stores enums.
            String listModeName = getArguments().getString("listMode");

            if (listModeName != null) {
                // 2. Convert the String name back into the actual Enum object.
                currentMode = PostListViewModel.ListMode.valueOf(listModeName);
            }
            // --- END: FIX ---
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostListBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(PostListViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        observeViewModel();
        setupListeners();

        // Load data for the first time
        if (currentMode != null) {
            viewModel.loadPosts(currentMode);
        }
    }

    private void setupToolbar() {
        if (currentMode == PostListViewModel.ListMode.READING_HISTORY) {
            binding.toolbar.setTitle("Reading History");
            binding.emptyText.setText("Your reading history is empty");
        } else if (currentMode == PostListViewModel.ListMode.LIKES) {
            binding.toolbar.setTitle("My Likes");
            binding.emptyText.setText("You haven't liked any posts yet");
        } else if (currentMode == PostListViewModel.ListMode.BOOKMARKS) { // <-- NEW
            binding.toolbar.setTitle("My Bookmarks");
            binding.emptyText.setText("You haven't bookmarked any posts yet");
        }
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new BlogsAdapter();
        binding.postListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.postListRecyclerView.setAdapter(adapter);

        // Handle item clicks to navigate to the detail page
        adapter.setOnItemClickListener(post -> {
            NavHostFragment.findNavController(this)
                    .navigate(
                            R.id.action_global_to_blogDetail, // A new global action
                            BlogDetailFragment.createArgs(post.getPostId())
                    );
        });
    }

    private void setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentMode != null) {
                viewModel.loadPosts(currentMode);
            }
        });
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // Show swipe refresh indicator only if the list is not empty
            if (!isLoading) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            // Show centered progress indicator only on initial load
            binding.progressIndicator.setVisibility(isLoading && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });

        viewModel.posts.observe(getViewLifecycleOwner(), posts -> {
            adapter.submitList(new ArrayList<>(posts));
            // Show empty text only when not loading and the list is empty
            boolean showEmptyView = (posts == null || posts.isEmpty()) && (viewModel.isLoading.getValue() != null && !viewModel.isLoading.getValue());
            binding.emptyText.setVisibility(showEmptyView ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
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
