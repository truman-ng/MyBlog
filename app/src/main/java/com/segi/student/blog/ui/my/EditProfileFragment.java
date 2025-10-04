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
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.segi.student.blog.R;
import com.segi.student.blog.databinding.FragmentEditProfileBinding;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private MyViewModel myViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        // Use the activity as the scope to get a ViewModel shared with MyFragment
        myViewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        observeViewModel();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    private void setupClickListeners() {
        binding.saveButton.setOnClickListener(v -> {
            String nickname = binding.nicknameEditText.getText().toString();
            String bio = binding.bioEditText.getText().toString();
            myViewModel.updateProfile(nickname, bio);
        });
    }

    private void observeViewModel() {
        // Observe profile to pre-fill the fields
        myViewModel.profile.observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.nicknameEditText.setText(profile.displayName);
                binding.bioEditText.setText(profile.bio);
                binding.emailEditText.setText(profile.email);

                Glide.with(this)
                        .load(profile.avatarUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(binding.avatarImageView);
            }
        });

        // Observe the update state
        myViewModel.profileUpdateState.observe(getViewLifecycleOwner(), state -> {
            setLoading(state == MyViewModel.UiState.LOADING);
            if (state == MyViewModel.UiState.SUCCESS) {
                Snackbar.make(binding.getRoot(), "Profile updated successfully!", Snackbar.LENGTH_SHORT).show();
                myViewModel.resetProfileUpdateState();
                // Go back to the previous screen on success
                NavHostFragment.findNavController(this).navigateUp();
            } else if (state == MyViewModel.UiState.ERROR) {
                String error = myViewModel.errorMessage.getValue();
                Snackbar.make(binding.getRoot(), error != null ? error : "An unknown error occurred.", Snackbar.LENGTH_LONG).show();
                myViewModel.resetProfileUpdateState();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.saveProgressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.saveButton.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.nicknameEditText.setEnabled(!isLoading);
        binding.bioEditText.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
