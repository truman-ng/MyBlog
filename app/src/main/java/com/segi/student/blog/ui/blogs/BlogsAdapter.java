package com.segi.student.blog.ui.blogs;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.segi.student.blog.R;
import com.segi.student.blog.databinding.ItemBlogPostBinding;

public class BlogsAdapter extends ListAdapter<BlogsViewModel.Post, BlogsAdapter.BlogViewHolder> {

    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    public interface OnItemClickListener {
        void onItemClick(BlogsViewModel.Post post);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    // --- 2. NEW: Interface for long clicks ---
    public interface OnItemLongClickListener {
        void onItemLongClick(BlogsViewModel.Post post);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public BlogsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<BlogsViewModel.Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<BlogsViewModel.Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull BlogsViewModel.Post oldItem, @NonNull BlogsViewModel.Post newItem) {
            return oldItem.getPostId().equals(newItem.getPostId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BlogsViewModel.Post oldItem, @NonNull BlogsViewModel.Post newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlogPostBinding binding = ItemBlogPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BlogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {
        BlogsViewModel.Post currentPost = getItem(position);
        holder.bind(currentPost, listener, longClickListener);
    }

    static class BlogViewHolder extends RecyclerView.ViewHolder {
        private final ItemBlogPostBinding binding;

        public BlogViewHolder(ItemBlogPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final BlogsViewModel.Post post, final OnItemClickListener listener,
                         final OnItemLongClickListener longClickListener) {
            binding.titleText.setText(post.getTitle());

            // Cover Image
            if (post.getCoverUrl() != null && !post.getCoverUrl().isEmpty()) {
                binding.coverImage.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(post.getCoverUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.coverImage);
            } else {
                binding.coverImage.setVisibility(View.GONE);
            }

            // --- START: 修改作者信息处理 ---
            // 因为没有作者信息，我们直接隐藏作者相关的UI
            binding.authorAvatar.setVisibility(View.GONE);
            binding.authorName.setVisibility(View.GONE);
            binding.metaSeparator.setVisibility(View.GONE);
            binding.postTime.setVisibility(View.GONE);
            // --- END: 修改作者信息处理 ---

            // --- Handle short click ---
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(post);
                }
            });

            // --- 5. NEW: Handle long click ---
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(post);
                    return true; // Consume the long-click event
                }
                return false;
            });
            // Relative Time (如果需要，可以取消注释并调整布局)
            /*
            String relativeTime = DateUtils.getRelativeTimeSpanString(
                    post.getUpdatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
            binding.postTime.setText(relativeTime);
            */
        }
    }
}
