package com.example.ok.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.ChatRoom;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatInboxAdapter extends RecyclerView.Adapter<ChatInboxAdapter.ChatRoomViewHolder> {

    private Context context;
    private List<ChatRoom> chatRooms;
    private long currentUserId;
    private OnChatRoomClickListener listener;
    private OnUserProfileClickListener userProfileListener;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private Calendar calendar;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }
    
    public interface OnUserProfileClickListener {
        void onUserProfileClick(Long userId, String userName);
    }

    public ChatInboxAdapter(Context context, List<ChatRoom> chatRooms, long currentUserId, OnChatRoomClickListener listener) {
        this.context = context;
        this.chatRooms = chatRooms;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.calendar = Calendar.getInstance();
    }
    
    public void setOnUserProfileClickListener(OnUserProfileClickListener listener) {
        this.userProfileListener = listener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);

        // Get the other user's info
        String otherUserName = chatRoom.getOtherUserName(currentUserId);
        String otherUserAvatar = chatRoom.getOtherUserAvatar(currentUserId);
        Long otherUserId = chatRoom.getOtherUserId(currentUserId);

        // Set user name
        holder.tvUserName.setText(otherUserName);

        // Set user avatar
        if (otherUserAvatar != null && !otherUserAvatar.isEmpty()) {
            Glide.with(context)
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Set up click listeners for user profile viewing
        View.OnClickListener userProfileClickListener = v -> {
            if (userProfileListener != null && otherUserId != null) {
                userProfileListener.onUserProfileClick(otherUserId, otherUserName);
            }
        };
        
        // Add click listeners to avatar and username
        holder.ivAvatar.setOnClickListener(userProfileClickListener);
        holder.tvUserName.setOnClickListener(userProfileClickListener);

        // Set last message
        String lastMessageContent = chatRoom.getLastMessageContent();
        int unreadCount = chatRoom.getUnreadCount();
        
        if (lastMessageContent != null && !lastMessageContent.isEmpty()) {
            holder.tvLastMessage.setText(lastMessageContent);
        } else if (unreadCount > 0) {
            // If no last message content but there are unread messages, show unread count
            holder.tvLastMessage.setText(context.getString(R.string.unread_messages, unreadCount));
        } else {
            holder.tvLastMessage.setText(context.getString(R.string.no_messages_yet));
        }
        
        // Set time
        if (chatRoom.getLastMessageTime() != null && !chatRoom.getLastMessageTime().isEmpty()) {
            holder.tvTime.setText(formatMessageTime(chatRoom.getLastMessageTime()));
        } else {
            holder.tvTime.setText("");
        }

        // Set unread count
        if (unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(Math.min(unreadCount, 99)));
            
            // Make last message bold if unread
            holder.tvLastMessage.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
            holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.textSecondary));
            holder.tvLastMessage.setTypeface(holder.tvLastMessage.getTypeface(), android.graphics.Typeface.NORMAL);
        }

        // Set listing info
        if (chatRoom.getListingId() != null && chatRoom.getListingId() > 0 &&
                chatRoom.getListingTitle() != null && !chatRoom.getListingTitle().isEmpty()) {
            holder.layoutListing.setVisibility(View.VISIBLE);
            
            // Set listing title
            holder.tvListingTitle.setText(chatRoom.getListingTitle());
            
            // Set listing price
            if (chatRoom.getListingPrice() != null) {
                holder.tvListingPrice.setText(formatPrice(chatRoom.getListingPrice()));
            } else {
                holder.tvListingPrice.setText("");
            }
            
            // Set listing image
            if (chatRoom.getListingImageUrl() != null && !chatRoom.getListingImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(chatRoom.getListingImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(holder.ivListingImage);
            } else {
                holder.ivListingImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            holder.layoutListing.setVisibility(View.GONE);
        }

        // Set click listener for the entire item (excluding user profile clicks)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatRoomClick(chatRoom);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatRooms != null ? chatRooms.size() : 0;
    }    private String formatMessageTime(String datetimeStr) {
        try {
            // Parse ISO datetime string (2025-06-10T12:56:44.272811)
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            Date messageDate = isoFormat.parse(datetimeStr);
            
            if (messageDate == null) {
                return "";
            }
            
            long timestamp = messageDate.getTime();
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < DateUtils.DAY_IN_MILLIS) {
                // Today: show time
                return timeFormat.format(messageDate);
            } else if (diff < 2 * DateUtils.DAY_IN_MILLIS) {
                // Yesterday
                return "Hôm qua";
            } else if (diff < 7 * DateUtils.DAY_IN_MILLIS) {
                // This week: show day of week
                calendar.setTime(messageDate);
                String[] weekDays = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"};
                return weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
            } else {
                // Older: show date
                return dateFormat.format(messageDate);
            }
        } catch (Exception e) {
            // If parsing fails, try a simpler format or return empty
            try {
                // Try without microseconds
                SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date messageDate = simpleFormat.parse(datetimeStr);
                return timeFormat.format(messageDate);
            } catch (Exception e2) {
                return "";
            }
        }
    }
    
    private String formatPrice(Long price) {
        if (price == null) return "";
        
        // Format with thousand separators
        java.text.NumberFormat numberFormat = java.text.NumberFormat.getInstance(Locale.getDefault());
        return numberFormat.format(price) + " đ";
    }

    static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnreadCount;
        LinearLayout layoutListing;
        ImageView ivListingImage;
        TextView tvListingTitle;
        TextView tvListingPrice;

        ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            layoutListing = itemView.findViewById(R.id.layoutListing);
            ivListingImage = itemView.findViewById(R.id.ivListingImage);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvListingPrice = itemView.findViewById(R.id.tvListingPrice);
        }
    }
}
