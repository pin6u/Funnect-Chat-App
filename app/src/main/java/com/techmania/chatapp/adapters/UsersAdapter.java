package com.techmania.chatapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.databinding.UsersItemLayoutBinding;
import com.techmania.chatapp.models.User;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> {

    private ArrayList<User> usersList;
    private OnUserClickListener onUserClickListener;

    public UsersAdapter(ArrayList<User> usersList, OnUserClickListener onUserClickListener) {
        this.usersList = usersList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        UsersItemLayoutBinding binding = UsersItemLayoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new UsersViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = usersList.get(position);

        holder.binding.textViewItem.setText(user.getUserName());

        // ✅ Load profile image
        if (user.getImageUrl() != null && !user.getImageUrl().equals("null")) {
            Picasso.get().load(user.getImageUrl()).into(holder.binding.imageViewUserItem);
        } else {
            holder.binding.imageViewUserItem.setImageResource(R.drawable.default_profile_photo);
        }

        // ✅ Show online dot if user is online
        if (user.getStatus() != null && user.getStatus().equals("online")) {
            holder.binding.onlineDot.setVisibility(View.VISIBLE);
        } else {
            holder.binding.onlineDot.setVisibility(View.GONE);
        }

        // ✅ Set user status text (online or last seen)
        if (user.getStatus() != null) {
            holder.binding.textViewStatus.setText(user.getStatus());
        } else {
            holder.binding.textViewStatus.setText("offline");
        }

        // ✅ Handle user click
        holder.binding.linearLayoutUserItem.setOnClickListener(v -> {
            onUserClickListener.OnUserClicked(user);
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder {
        UsersItemLayoutBinding binding;

        public UsersViewHolder(@NonNull UsersItemLayoutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnUserClickListener {
        void OnUserClicked(User user);
    }
}
