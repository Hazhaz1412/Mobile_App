package com.example.ok.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.model.ChatMessage;

import java.util.List;

/**
 * Dedicated notification manager for chat messages
 * Handles all chat-related notifications with improved reliability
 */
public class ChatNotificationManager {
    private static final String TAG = "ChatNotificationMgr";
    
    // Notification base ID for chat messages
    private static final int CHAT_NOTIFICATION_BASE_ID = 2000;
    
    // Preferences for notification settings
    private static final String PREFS_NAME = "ChatNotificationPrefs";
    private static final String KEY_CHAT_NOTIFICATIONS_ENABLED = "chat_notifications_enabled";
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences prefs;
    
    public ChatNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Ensure notification channels are created
        NotificationChannelManager.createNotificationChannels(this.context);
        
        Log.d(TAG, "ChatNotificationManager initialized");
    }
    
    /**
     * Show notification for new chat messages
     * This is the main method for displaying chat notifications
     */
    public void showChatNotification(long roomId, String senderName, List<ChatMessage> newMessages, 
                                   long myUserId, long otherUserId) {
        if (!isChatNotificationEnabled()) {
            Log.d(TAG, "Chat notifications are disabled by user");
            return;
        }
        
        if (!areSystemNotificationsEnabled()) {
            Log.w(TAG, "System notifications are disabled, cannot show notification");
            return;
        }
        
        if (newMessages == null || newMessages.isEmpty()) {
            Log.d(TAG, "No messages to show notification for");
            return;
        }
        
        try {            // Filter out messages from current user
            java.util.List<ChatMessage> otherUserMessages = new java.util.ArrayList<>();
            for (ChatMessage msg : newMessages) {
                if (msg.getSenderId() != myUserId) {
                    otherUserMessages.add(msg);
                }
            }
            
            if (otherUserMessages.isEmpty()) {
                Log.d(TAG, "No messages from other user to notify about");
                return;
            }
            
            ChatMessage latestMessage = otherUserMessages.get(otherUserMessages.size() - 1);
            
            String title = buildNotificationTitle(senderName, otherUserMessages.size());
            String content = buildNotificationContent(latestMessage, otherUserMessages.size());
            
            PendingIntent pendingIntent = createChatPendingIntent(roomId, myUserId, otherUserId, senderName);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup("chat_group")
                .setGroupSummary(false);
            
            // Add big text style for better readability
            if (otherUserMessages.size() == 1 && latestMessage.isText()) {
                builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(latestMessage.getContent())
                    .setBigContentTitle(title));
            }
            
            // Add actions for quick reply (if needed in future)
            // builder.addAction(createQuickReplyAction(roomId, myUserId, otherUserId));
            
            int notificationId = getNotificationId(roomId);
            notificationManager.notify(notificationId, builder.build());
            
            Log.d(TAG, "✅ Chat notification displayed successfully for room " + roomId + 
                     " with " + otherUserMessages.size() + " messages");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing chat notification", e);
        }
    }
    
    /**
     * Show notification for a single new message (used by FCM)
     */
    public void showSingleMessageNotification(long roomId, String senderName, String messageContent,
                                            long myUserId, long otherUserId) {
        if (!isChatNotificationEnabled() || !areSystemNotificationsEnabled()) {
            return;
        }
        
        try {
            String title = senderName != null ? senderName : "Tin nhắn mới";
            String content = messageContent != null && !messageContent.trim().isEmpty() 
                ? messageContent : "Bạn có tin nhắn mới";
            
            // Limit content length
            if (content.length() > 100) {
                content = content.substring(0, 97) + "...";
            }
            
            PendingIntent pendingIntent = createChatPendingIntent(roomId, myUserId, otherUserId, senderName);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));
            
            int notificationId = getNotificationId(roomId);
            notificationManager.notify(notificationId, builder.build());
            
            Log.d(TAG, "✅ Single message notification displayed for room " + roomId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing single message notification", e);
        }
    }
    
    /**
     * Clear notifications for a specific chat room
     */
    public void clearChatNotifications(long roomId) {
        try {
            int notificationId = getNotificationId(roomId);
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Cleared notifications for chat room: " + roomId);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing chat notifications", e);
        }
    }
    
    /**
     * Clear all chat notifications
     */
    public void clearAllChatNotifications() {
        try {
            // Cancel all possible chat notification IDs (assuming max 1000 rooms)
            for (int i = 0; i < 1000; i++) {
                notificationManager.cancel(CHAT_NOTIFICATION_BASE_ID + i);
            }
            Log.d(TAG, "Cleared all chat notifications");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all chat notifications", e);
        }
    }
    
    /**
     * Test notification functionality
     */
    public void showTestNotification(String testMessage) {
        try {
            Intent intent = new Intent(context, MainMenu.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle("Test Notification")
                .setContentText(testMessage != null ? testMessage : "Thông báo thử nghiệm")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);
            
            notificationManager.notify(9999, builder.build());
            Log.d(TAG, "✅ Test notification sent");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing test notification", e);
        }
    }
    
    // === Settings Methods ===
    
    public boolean isChatNotificationEnabled() {
        return prefs.getBoolean(KEY_CHAT_NOTIFICATIONS_ENABLED, true); // Default: enabled
    }
    
    public void setChatNotificationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_CHAT_NOTIFICATIONS_ENABLED, enabled).apply();
        Log.d(TAG, "Chat notifications " + (enabled ? "ENABLED" : "DISABLED"));
    }
    
    public boolean areSystemNotificationsEnabled() {
        if (notificationManager == null) {
            return false;
        }
        
        boolean globalEnabled = notificationManager.areNotificationsEnabled();
        
        // Check channel-specific settings on Android 8.0+
        boolean channelEnabled = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelEnabled = NotificationChannelManager.isChannelEnabled(context, NotificationChannelManager.CHANNEL_MESSAGES);
        }
        
        return globalEnabled && channelEnabled;
    }
    
    // === Private Helper Methods ===
    
    private String buildNotificationTitle(String senderName, int messageCount) {
        if (senderName == null || senderName.trim().isEmpty()) {
            return messageCount > 1 ? messageCount + " tin nhắn mới" : "Tin nhắn mới";
        }
        
        return messageCount > 1 ? 
            senderName + " (" + messageCount + " tin nhắn)" : 
            senderName;
    }
    
    private String buildNotificationContent(ChatMessage latestMessage, int messageCount) {
        if (messageCount > 1) {
            return messageCount + " tin nhắn mới";
        }
        
        if (latestMessage.isText()) {
            String content = latestMessage.getContent();
            return content != null && content.length() > 50 ? 
                content.substring(0, 47) + "..." : content;
        } else if (latestMessage.isImage()) {
            return "📷 Đã gửi một hình ảnh";
        } else {
            return "Tin nhắn mới";
        }
    }
    
    private PendingIntent createChatPendingIntent(long roomId, long myUserId, long otherUserId, String senderName) {
        Intent intent = new Intent(context, MainMenu.class);
        intent.putExtra("open_chat", true);
        intent.putExtra("roomId", roomId);
        intent.putExtra("myId", myUserId);
        intent.putExtra("otherId", otherUserId);
        intent.putExtra("otherName", senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        return PendingIntent.getActivity(
            context, 
            (int) roomId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private int getNotificationId(long roomId) {
        // Convert room ID to a reasonable notification ID
        return CHAT_NOTIFICATION_BASE_ID + (int) (roomId % 1000);
    }
    
    /**
     * Debug method to check notification system status
     */
    public void debugNotificationStatus() {
        Log.d(TAG, "=== CHAT NOTIFICATION DEBUG ===");
        Log.d(TAG, "System notifications enabled: " + areSystemNotificationsEnabled());
        Log.d(TAG, "Chat notifications enabled: " + isChatNotificationEnabled());
        Log.d(TAG, "NotificationManager available: " + (notificationManager != null));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            android.app.NotificationChannel channel = notificationManager.getNotificationChannel(NotificationChannelManager.CHANNEL_MESSAGES);
            if (channel != null) {
                Log.d(TAG, "Messages channel importance: " + channel.getImportance());
            } else {
                Log.e(TAG, "Messages channel not found!");
            }
        }
        
        Log.d(TAG, "=== END DEBUG ===");
    }
}
