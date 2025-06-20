package com.example.ok.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.BlockedUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlockedUsersAdapter extends RecyclerView.Adapter<BlockedUsersAdapter.ViewHolder> {
    
    private Context context;
    private List<BlockedUser> blockedUsers;
    private OnUnblockClickListener onUnblockClickListener;
    
    public interface OnUnblockClickListener {
        void onUnblockClick(BlockedUser user, int position);
    }
    
    public BlockedUsersAdapter(Context context, List<BlockedUser> blockedUsers) {
        this.context = context;
        this.blockedUsers = blockedUsers;
    }
    
    public void setOnUnblockClickListener(OnUnblockClickListener listener) {
        this.onUnblockClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedUser user = blockedUsers.get(position);
        
        // Set user name
        holder.tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");
        
        // Set blocked date
        if (user.getBlockedAt() != null) {
            String formattedDate = formatBlockedDate(user.getBlockedAt());
            holder.tvBlockedDate.setText("Đã chặn lúc " + formattedDate);
        } else {
            holder.tvBlockedDate.setText("Đã chặn");
        }
        
        // Load user avatar
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfilePicture())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
        
        // Set unblock button click listener
        holder.btnUnblock.setOnClickListener(v -> {
            if (onUnblockClickListener != null) {
                onUnblockClickListener.onUnblockClick(user, position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return blockedUsers.size();
    }
    
    private String formatBlockedDate(Date date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            return dateFormat.format(date);
        } catch (Exception e) {
            return "Không xác định";
        }
    }
    
    // Method to remove user from list after unblocking
    public void removeUser(int position) {
        if (position >= 0 && position < blockedUsers.size()) {
            blockedUsers.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, blockedUsers.size());
        }
    }
    
    // Method to update the entire list
    public void updateList(List<BlockedUser> newList) {
        this.blockedUsers.clear();
        this.blockedUsers.addAll(newList);
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvBlockedDate;
        Button btnUnblock;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvBlockedDate = itemView.findViewById(R.id.tvBlockedDate);
            btnUnblock = itemView.findViewById(R.id.btnUnblock);
        }
    }
}
