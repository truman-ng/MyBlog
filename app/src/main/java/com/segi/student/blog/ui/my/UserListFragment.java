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

import com.segi.student.blog.databinding.FragmentPostListBinding; // Reusing layout
import com.segi.student.blog.ui.my.UserListViewModel.ListMode;

public class UserListFragment extends Fragment {

    private FragmentPostListBinding binding; // Reusing the same binding
    private UserListViewModel viewModel;
    private UserListAdapter adapter;
    private ListMode currentMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // --- START: FIX ---
            // 1. Get the argument as a String. This avoids the ClassCastException.
            String listModeName = getArguments().getString("listMode");

            if (listModeName != null) {
                // 2. Safely convert the String name back into the actual Enum object.
                currentMode = UserListViewModel.ListMode.valueOf(listModeName);
            }
            // --- END: FIX ---
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostListBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(UserListViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupRecyclerView();
        observeViewModel();

        if (currentMode != null) {
            viewModel.loadUsers(currentMode);
        }
    }

    private void setupToolbar() {
        String title = (currentMode == ListMode.FOLLOWING) ? "Following" : "Followers";
        binding.toolbar.setTitle(title);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new UserListAdapter();
        // The ID is post_list_recycler_view because we are reusing the layout
        binding.postListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.postListRecyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.users.observe(getViewLifecycleOwner(), users -> {
            adapter.submitList(users);
            String emptyText = (currentMode == ListMode.FOLLOWING) ? "You are not following anyone yet." : "You have no followers yet.";
            binding.emptyText.setText(emptyText);
            binding.emptyText.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            // Handle error
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
