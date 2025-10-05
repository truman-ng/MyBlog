package com.segi.student.blog.ui.my;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.segi.student.blog.databinding.ItemUserBinding;

public class UserListAdapter extends ListAdapter<UserItem, UserListAdapter.UserViewHolder> {

    public UserListAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(UserItem user) {
            binding.userDisplayName.setText(user.getDisplayName());
            binding.userBio.setText(user.getBio());
            Glide.with(itemView.getContext())
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .into(binding.userAvatar);
        }
    }

    private static final DiffUtil.ItemCallback<UserItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserItem oldItem, @NonNull UserItem newItem) {
            return oldItem.getUid().equals(newItem.getUid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserItem oldItem, @NonNull UserItem newItem) {
            return oldItem.getDisplayName().equals(newItem.getDisplayName())
                    && oldItem.getAvatarUrl().equals(newItem.getAvatarUrl());
        }
    };
}
