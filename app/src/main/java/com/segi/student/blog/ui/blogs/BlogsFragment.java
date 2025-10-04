package com.segi.student.blog.ui.blogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.segi.student.blog.databinding.FragmentBlogsBinding;

public class BlogsFragment extends Fragment {

    private FragmentBlogsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        BlogsViewModel blogsViewModel =
                new ViewModelProvider(this).get(BlogsViewModel.class);

        binding = FragmentBlogsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textBlogs;
        blogsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
