package com.example.ok.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Utility class to manage notification channels creation
 */
public class NotificationChannelManager {
    private static final String TAG = "NotificationChannels";
    
    // Notification channel IDs - must match with Firebase service
    public static final String CHANNEL_MESSAGES = "messages";
    public static final String CHANNEL_OFFERS = "offers";
    public static final String CHANNEL_LISTINGS = "listings";
    public static final String CHANNEL_PROMOTIONS = "promotions";
    
    /**
     * Create all notification channels
     * This should be called when the app starts
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels...");
            
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null, cannot create channels");
                return;
            }

            // Messages channel - HIGH priority for chat messages
            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Tin nhắn",
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Thông báo tin nhắn mới");
            messagesChannel.enableVibration(true);
            messagesChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            messagesChannel.setShowBadge(true);

            // Offers channel - HIGH priority for business offers
            NotificationChannel offersChannel = new NotificationChannel(
                    CHANNEL_OFFERS,
                    "Báo giá",
                    NotificationManager.IMPORTANCE_HIGH
            );
            offersChannel.setDescription("Thông báo báo giá mới");
            offersChannel.enableVibration(true);
            offersChannel.setShowBadge(true);

            // Listings channel - DEFAULT priority for general updates
            NotificationChannel listingsChannel = new NotificationChannel(
                    CHANNEL_LISTINGS,
                    "Tin đăng",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            listingsChannel.setDescription("Cập nhật tin đăng");
            listingsChannel.setShowBadge(false);

            // Promotions channel - LOW priority for marketing
            NotificationChannel promotionsChannel = new NotificationChannel(
                    CHANNEL_PROMOTIONS,
                    "Khuyến mãi",
                    NotificationManager.IMPORTANCE_LOW
            );
            promotionsChannel.setDescription("Thông báo khuyến mãi và quảng cáo");
            promotionsChannel.setShowBadge(false);

            // Create all channels
            notificationManager.createNotificationChannel(messagesChannel);
            notificationManager.createNotificationChannel(offersChannel);
            notificationManager.createNotificationChannel(listingsChannel);
            notificationManager.createNotificationChannel(promotionsChannel);
            
            Log.d(TAG, "✅ All notification channels created successfully");
            
            // Log channel info for debugging
            logChannelInfo(notificationManager);
            
        } else {
            Log.d(TAG, "Android version < O, notification channels not needed");
        }
    }
    
    /**
     * Log information about existing channels for debugging
     */
    private static void logChannelInfo(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "=== NOTIFICATION CHANNELS INFO ===");
            for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
                Log.d(TAG, String.format("Channel: %s | Name: %s | Importance: %d", 
                    channel.getId(), channel.getName(), channel.getImportance()));
            }
            
            // Check if notifications are enabled globally
            boolean areEnabled = notificationManager.areNotificationsEnabled();
            Log.d(TAG, "Global notifications enabled: " + areEnabled);
            Log.d(TAG, "=== END CHANNELS INFO ===");
        }
    }
    
    /**
     * Check if a specific channel exists and is enabled
     */
    public static boolean isChannelEnabled(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
                return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
        }
        return true; // For older Android versions, assume enabled
    }
}
