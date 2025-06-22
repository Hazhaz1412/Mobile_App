package com.example.ok.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.ok.MainActivity;
import com.example.ok.R;
import com.example.ok.util.NotificationChannelManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    
    // Notification IDs
    private static final int NOTIFICATION_ID_MESSAGES = 1001;
    private static final int NOTIFICATION_ID_OFFERS = 1002;
    private static final int NOTIFICATION_ID_LISTINGS = 1003;
    private static final int NOTIFICATION_ID_PROMOTIONS = 1004;
    private static final int NOTIFICATION_ID_GENERAL = 1005;

    @Override
    public void onCreate() {
        super.onCreate();
        // Ensure notification channels are created
        NotificationChannelManager.createNotificationChannels(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage);
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();
        
        // Send token to server
        sendTokenToServer(token);
    }    private void handleDataMessage(java.util.Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        
        if (type == null || title == null || body == null) {
            return;
        }

        switch (type) {
            case "NEW_MESSAGE":
                showMessageNotification(title, body, data);
                break;
            case "PRICE_OFFER":
                showOfferNotification(title, body, data);
                break;
            case "LISTING_UPDATE":
                showListingNotification(title, body, data);
                break;
            case "PROMOTION":
                showPromotionNotification(title, body, data);
                break;
            default:
                showGeneralNotification(title, body);
                break;
        }
    }

    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        showGeneralNotification(title, body);
    }    private void showMessageNotification(String title, String body, java.util.Map<String, String> data) {
        try {
            // Use ChatNotificationManager for better reliability
            com.example.ok.util.ChatNotificationManager chatNotificationManager = 
                new com.example.ok.util.ChatNotificationManager(this);
            
            // Extract data from Firebase message
            String senderIdStr = data.get("senderId");
            String senderName = data.get("senderName");
            String roomIdStr = data.get("roomId");
            
            // Get current user ID
            SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            long myUserId = prefs.getLong("userId", -1);
            
            if (senderIdStr != null && roomIdStr != null && myUserId != -1) {
                try {
                    long senderId = Long.parseLong(senderIdStr);
                    long roomId = Long.parseLong(roomIdStr);
                    
                    // Don't show notification for own messages
                    if (senderId != myUserId) {
                        chatNotificationManager.showSingleMessageNotification(
                            roomId, 
                            senderName != null ? senderName : title, 
                            body, 
                            myUserId, 
                            senderId
                        );
                        
                        Log.d(TAG, "✅ FCM chat notification processed via ChatNotificationManager");
                        return;
                    } else {
                        Log.d(TAG, "Skipping notification for own message");
                        return;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing sender/room IDs from FCM data", e);
                }
            }
            
            // Fallback to old method if data parsing fails
            Log.w(TAG, "FCM data incomplete, falling back to basic notification");
            showBasicMessageNotification(title, body, data);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced FCM message notification", e);
            // Fallback to old method
            showBasicMessageNotification(title, body, data);
        }
    }
    
    private void showBasicMessageNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add chat-specific data
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        if (senderId != null && senderName != null) {
            intent.putExtra("open_chat", true);
            intent.putExtra("other_user_id", Long.parseLong(senderId));
            intent.putExtra("other_user_name", senderName);
        }PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_MESSAGES, notificationBuilder.build());
    }

    private void showOfferNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        String listingId = data.get("listingId");
        if (listingId != null) {
            intent.putExtra("open_listing", true);
            intent.putExtra("listing_id", Long.parseLong(listingId));
        }        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_OFFERS)
                .setSmallIcon(R.drawable.cart) // Using existing cart icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_OFFERS, notificationBuilder.build());
    }

    private void showListingNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_LISTINGS)
                .setSmallIcon(R.drawable.add) // Using existing add icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_LISTINGS, notificationBuilder.build());
    }

    private void showPromotionNotification(String title, String body, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_PROMOTIONS)
                .setSmallIcon(R.drawable.star) // Using existing star icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_PROMOTIONS, notificationBuilder.build());
    }

    private void showGeneralNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.notification) // Using existing notification icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_GENERAL, notificationBuilder.build());
    }    private void sendTokenToServer(String token) {
        Log.d(TAG, "Sending FCM token to server: " + token);
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
        
        if (userId != -1) {
            // Initialize API service if needed
            com.example.ok.api.RetrofitClient.init(this);
            com.example.ok.api.ApiService apiService = com.example.ok.api.RetrofitClient.getApiService();
            
            // Send token to server
            retrofit2.Call<com.example.ok.model.ApiResponse> call = apiService.updateFcmToken(userId, token);
            call.enqueue(new retrofit2.Callback<com.example.ok.model.ApiResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.ok.model.ApiResponse> call, 
                                     retrofit2.Response<com.example.ok.model.ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d(TAG, "FCM token sent to server successfully");
                    } else {
                        Log.e(TAG, "Failed to send FCM token to server: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(retrofit2.Call<com.example.ok.model.ApiResponse> call, Throwable t) {
                    Log.e(TAG, "Error sending FCM token to server", t);
                }
            });
        } else {
            Log.w(TAG, "User not logged in, cannot send FCM token to server");
        }
    }
}
