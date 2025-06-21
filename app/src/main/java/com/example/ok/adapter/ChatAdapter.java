package com.example.ok.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    // 🔥 Interface cho edit/delete callbacks
    public interface OnMessageActionListener {
        void onEditMessage(ChatMessage message, int position);
        void onDeleteMessage(ChatMessage message, int position);
        void onCopyMessage(ChatMessage message);
    }

    private List<ChatMessage> messages;
    private Context context;
    private Long currentUserId;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private Calendar calendar;
    private OnMessageActionListener actionListener;    public ChatAdapter(Context context, List<ChatMessage> messages, Long currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.calendar = Calendar.getInstance();
    }
    
    // 🔥 Setter cho action listener
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        // Set message content
        holder.tvContent.setText(message.getContent());
        
        // Set message time
        Date messageDate = message.getDateFromTimestamp();
        holder.tvTime.setText(timeFormat.format(messageDate));
        
        // Handle message alignment and background
        boolean isMyMessage = message.getSenderId().equals(currentUserId);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.layoutMessage.getLayoutParams();
        
        if (isMyMessage) {
            params.gravity = Gravity.END;
            params.setMarginStart(64);
            params.setMarginEnd(0);
            holder.layoutMessage.setBackgroundResource(R.drawable.bg_message_sent);
        } else {
            params.gravity = Gravity.START;
            params.setMarginStart(0);
            params.setMarginEnd(64);
            holder.layoutMessage.setBackgroundResource(R.drawable.bg_message_received);
        }
        holder.layoutMessage.setLayoutParams(params);
        
        // 🔥 Thêm long click listener cho tin nhắn của mình
        if (isMyMessage && actionListener != null) {
            holder.layoutMessage.setOnLongClickListener(v -> {
                showMessageActionMenu(v, message, position);
                return true;
            });
        } else {
            // Clear listener cho tin nhắn của người khác
            holder.layoutMessage.setOnLongClickListener(null);
        }
        
        // Handle image messages
        if (message.isImage()) {
            // For image messages, content contains the image URL
            String imageUrl = message.getContent();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                holder.tvContent.setVisibility(View.GONE); // Hide text content for images
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .centerCrop()
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setVisibility(View.GONE);
                holder.tvContent.setVisibility(View.VISIBLE);
            }
        } else {
            // For text messages, show content and hide image
            holder.ivImage.setVisibility(View.GONE);
            holder.tvContent.setVisibility(View.VISIBLE);
        }
        
        // Show date divider if needed
        if (shouldShowDateDivider(position)) {
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.tvDate.setText(dateFormat.format(messageDate));
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }
    }

    private boolean shouldShowDateDivider(int position) {
        if (position == 0) {
            return true;
        }
        
        // Get current and previous message dates
        ChatMessage current = messages.get(position);
        ChatMessage previous = messages.get(position - 1);
        
        calendar.setTime(current.getDateFromTimestamp());
        int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
        int currentYear = calendar.get(Calendar.YEAR);
        
        calendar.setTime(previous.getDateFromTimestamp());
        int previousDay = calendar.get(Calendar.DAY_OF_YEAR);
        int previousYear = calendar.get(Calendar.YEAR);
        
        // Show divider if the day changed
        return currentYear != previousYear || currentDay != previousDay;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    // 🔥 Methods để update/remove message
    public void updateMessage(int position, ChatMessage updatedMessage) {
        if (position >= 0 && position < messages.size()) {
            messages.set(position, updatedMessage);
            notifyItemChanged(position);
        }
    }
    
    public void removeMessage(int position) {
        if (position >= 0 && position < messages.size()) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
    }

    // 🔥 Method để hiện popup menu cho tin nhắn
    private void showMessageActionMenu(View view, ChatMessage message, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.message_action_menu, popup.getMenu());
        
        // Ẩn option "Sửa" nếu là tin nhắn hình ảnh
        if (message.isImage()) {
            popup.getMenu().findItem(R.id.action_edit).setVisible(false);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                if (actionListener != null) {
                    actionListener.onEditMessage(message, position);
                }
                return true;
            } else if (itemId == R.id.action_delete) {
                if (actionListener != null) {
                    actionListener.onDeleteMessage(message, position);
                }
                return true;
            } else if (itemId == R.id.action_copy) {
                if (actionListener != null) {
                    actionListener.onCopyMessage(message);
                }
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime, tvDate;
        ImageView ivImage;
        LinearLayout layoutMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivImage = itemView.findViewById(R.id.ivImage);
            layoutMessage = itemView.findViewById(R.id.layoutMessage);
        }
    }
}
