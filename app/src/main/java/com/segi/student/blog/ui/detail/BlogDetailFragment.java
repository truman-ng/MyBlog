package com.segi.student.blog.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.segi.student.blog.R;
import com.segi.student.blog.databinding.FragmentBlogDetailBinding;
import io.noties.markwon.Markwon;

public class BlogDetailFragment extends Fragment {

    private FragmentBlogDetailBinding binding;
    private BlogDetailViewModel viewModel;
    private Markwon markwon;
    private String postId;

    public static Bundle createArgs(String postId) {
        Bundle args = new Bundle();
        args.putString("postId", postId);
        return args;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }
        markwon = Markwon.create(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlogDetailBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(BlogDetailViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        observeViewModel();
        setupClickListeners();

        if (postId != null) {
            viewModel.loadPost(postId);
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupClickListeners() {
        binding.likeButton.setOnClickListener(v -> viewModel.toggleLike());
        binding.followButton.setOnClickListener(v -> viewModel.toggleFollow());
        binding.bookmarkButton.setOnClickListener(v -> viewModel.toggleBookmark());
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.post.observe(getViewLifecycleOwner(), post -> {
            binding.collapsingToolbar.setTitle(post.title);
            binding.titleText.setText(post.title);
            markwon.setMarkdown(binding.contentText, post.contentMd);
            Glide.with(this).load(post.coverUrl).into(binding.coverImage);
        });

        viewModel.author.observe(getViewLifecycleOwner(), author -> {
            binding.authorName.setText(author.displayName);
            binding.authorBio.setText(author.bio);
            Glide.with(this).load(author.avatarUrl).circleCrop().into(binding.authorAvatar);
        });

        viewModel.interactionState.observe(getViewLifecycleOwner(), state -> {
            // Update Follow Button
            if (state.isOwnPost) {
                binding.followButton.setVisibility(View.GONE);
            } else {
                binding.followButton.setVisibility(View.VISIBLE);
                binding.followButton.setText(state.isFollowing ? "Following" : "Follow");
            }

            // Update Like Button
            int likeIcon = state.isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline;
            binding.likeButton.setImageResource(likeIcon);

            // Update Reading List Button
            int bookmarkIcon = state.isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_add;
            binding.bookmarkButton.setImageResource(bookmarkIcon);
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            // Handle error, e.g., show a Snackbar
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
