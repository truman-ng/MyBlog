package com.segi.student.blog.ui.my;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.segi.student.blog.AuthActivity;
import com.segi.student.blog.R;
import com.segi.student.blog.databinding.FragmentMyBinding;
import com.google.android.material.snackbar.Snackbar;

public class MyFragment extends Fragment {

    private FragmentMyBinding binding;
    private MyViewModel myViewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImageFromGallery();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.my_page_permission_denied, Snackbar.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    myViewModel.updateAvatar(imageUri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMyBinding.inflate(inflater, container, false);
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);

        // *** Call the new method to set up UI content ***
        setupListItems();

        setupClickListeners();
        observeViewModel();

        return binding.getRoot();
    }

    // *** NEW METHOD to set text and icons for each item ***
    private void setupListItems() {
        setupListItem(binding.itemEditProfile.getRoot(), R.drawable.ic_edit, R.string.my_page_edit_profile);
        setupListItem(binding.itemAccountSecurity.getRoot(), R.drawable.ic_security, R.string.my_page_account_security);
        setupListItem(binding.itemDrafts.getRoot(), R.drawable.ic_drafts, R.string.my_page_drafts);
        setupListItem(binding.itemReadingHistory.getRoot(), R.drawable.ic_history, R.string.my_page_reading_history);
        setupListItem(binding.itemLikes.getRoot(), R.drawable.ic_thumb_up, R.string.my_page_likes);
        setupListItem(binding.itemFollowing.getRoot(), R.drawable.ic_people, R.string.my_page_following);
        setupListItem(binding.itemSignOut.getRoot(), R.drawable.ic_logout, R.string.my_page_sign_out);
    }

    private void setupListItem(View itemView, int iconRes, int titleRes) {
        ImageView icon = itemView.findViewById(R.id.item_icon);
        TextView title = itemView.findViewById(R.id.item_title);
        icon.setImageResource(iconRes);
        title.setText(titleRes);
    }

    private void setupClickListeners() {
        binding.avatarImageView.setOnClickListener(v -> checkPermissionAndPickImage());

        // Sign Out
        binding.itemSignOut.getRoot().setOnClickListener(v -> {
            myViewModel.signOut();
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Add other click listeners here
        binding.itemEditProfile.getRoot().setOnClickListener(v ->
                NavHostFragment.findNavController(MyFragment.this)
                        .navigate(R.id.action_myFragment_to_editProfileFragment)
        );
        binding.itemAccountSecurity.getRoot().setOnClickListener(v ->
                NavHostFragment.findNavController(MyFragment.this)
                        .navigate(R.id.action_myFragment_to_accountSecurityFragment)
        );
    }

    private void observeViewModel() {
        myViewModel.profile.observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) return;

            // Set default display name if it's empty
            binding.displayNameTextView.setText(profile.displayName != null && !profile.displayName.isEmpty() ? profile.displayName : getString(R.string.my_page_display_name_placeholder));
            binding.emailTextView.setText(profile.email);

            Glide.with(this)
                    .load(profile.avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.avatarImageView);
        });

        myViewModel.avatarUploadState.observe(getViewLifecycleOwner(), state -> {
            binding.avatarProgressIndicator.setVisibility(state == MyViewModel.UiState.LOADING ? View.VISIBLE : View.GONE);
            if (state == MyViewModel.UiState.SUCCESS) {
                Snackbar.make(binding.getRoot(), R.string.my_page_upload_success, Snackbar.LENGTH_SHORT).show();
                myViewModel.resetAvatarUploadState();
            } else if (state == MyViewModel.UiState.ERROR) {
                String error = myViewModel.errorMessage.getValue();
                Snackbar.make(binding.getRoot(), error != null ? error : getString(R.string.my_page_upload_failure), Snackbar.LENGTH_LONG).show();
                myViewModel.resetAvatarUploadState();
            }
        });

        // (The profileLoadState observer remains the same)
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
