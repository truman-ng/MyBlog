package com.segi.student.blog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.segi.student.blog.databinding.ActivityWriteBlogBinding;
import io.noties.markwon.Markwon;

public class WriteBlogActivity extends AppCompatActivity {

    private ActivityWriteBlogBinding binding;
    private WriteBlogViewModel viewModel;
    private Markwon markwon;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) pickImageFromGallery();
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    viewModel.setCoverLocalUri(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteBlogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(WriteBlogViewModel.class);
        markwon = Markwon.create(this);

        setupToolbar();
        setupTabs();
        setupInputListeners();
        setupClickListeners();
        observeViewModel();
        setupOnBackPressed();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_edit).setIcon(R.drawable.ic_edit_tab));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_preview).setIcon(R.drawable.ic_preview_tab));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean isPreview = tab.getPosition() == 1;
                binding.contentEditText.setVisibility(isPreview ? View.GONE : View.VISIBLE);
                binding.contentPreviewTextView.setVisibility(isPreview ? View.VISIBLE : View.GONE);
                if (isPreview) {
                    markwon.setMarkdown(binding.contentPreviewTextView, binding.contentEditText.getText().toString());
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupInputListeners() {
        // Listener for the Title field
        binding.titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setTitle(s.toString());
            }
        });

        // Listener for the Content field
        binding.contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setContent(s.toString());
            }
        });
    }

    private void setupClickListeners() {
        binding.coverCard.setOnClickListener(v -> checkPermissionAndPickImage());
        binding.changeCoverButton.setOnClickListener(v -> checkPermissionAndPickImage());
        binding.removeCoverButton.setOnClickListener(v -> viewModel.removeCover());
        binding.saveDraftButton.setOnClickListener(v -> viewModel.saveDraft());
        binding.publishButton.setOnClickListener(v -> viewModel.publish());
    }

    private void observeViewModel() {
        viewModel.formState.observe(this, state -> {
            // Update cover image view
            Uri localUri = state.localCoverUri;
            String remoteUrl = state.remoteCoverUrl;
            if (localUri != null) {
                Glide.with(this).load(localUri).into(binding.coverImageView);
                showCoverImageUI(true);
            } else if (remoteUrl != null) {
                Glide.with(this).load(remoteUrl).into(binding.coverImageView);
                showCoverImageUI(true);
            } else {
                binding.coverImageView.setImageDrawable(null);
                showCoverImageUI(false);
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            binding.progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setInputsEnabled(!isLoading);
        });

        viewModel.error.observe(this, error -> {
            if (error != null) {
                Snackbar.make(binding.rootLayout, error, Snackbar.LENGTH_LONG).show();
                viewModel.clearEventMessages();
            }
        });

        viewModel.successMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearEventMessages();
                if (message.contains("published")) finish();
            }
        });

        viewModel.backEvent.observe(this, event -> {
            if (event != null) {
                if (event == WriteBlogViewModel.BackEvent.PROMPT_USER) {
                    showUnsavedChangesDialog();
                } else {
                    finish();
                }
                viewModel.clearEventMessages();
            }
        });
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                viewModel.onBackAttempt();
            }
        });
    }

    private void showCoverImageUI(boolean hasImage) {
        binding.addCoverButton.setVisibility(hasImage ? View.GONE : View.VISIBLE);
        binding.changeCoverButton.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        binding.removeCoverButton.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        binding.coverCard.setClickable(!hasImage);
    }

    private void setInputsEnabled(boolean enabled) {
        binding.titleEditText.setEnabled(enabled);
        binding.contentEditText.setEnabled(enabled);
        binding.saveDraftButton.setEnabled(enabled);
        binding.publishButton.setEnabled(enabled);
        binding.coverCard.setEnabled(enabled);
        binding.changeCoverButton.setEnabled(enabled);
        binding.removeCoverButton.setEnabled(enabled);
    }

    private void checkPermissionAndPickImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void showUnsavedChangesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_back_title)
                .setMessage(R.string.dialog_back_message)
                // Use the standard Android string for "Cancel"
                .setNeutralButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setNegativeButton(R.string.dialog_discard_and_exit, (dialog, which) -> viewModel.discardAndExit())
                .setPositiveButton(R.string.dialog_save_draft_and_exit, (dialog, which) -> {
                    viewModel.saveDraft();
                    // The success observer will handle finishing the activity.
                })
                .show();
    }
}
