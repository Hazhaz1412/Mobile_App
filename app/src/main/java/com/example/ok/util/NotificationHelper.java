package com.example.ok.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String PREFS_NAME = "NotificationPrefs";
    
    // Notification types
    public static final String NOTIF_MESSAGES = "messages";
    public static final String NOTIF_OFFERS = "offers";
    public static final String NOTIF_LISTINGS = "listings";
    public static final String NOTIF_PROMOTIONS = "promotions";

    private Context context;
    private SharedPreferences prefs;

    public NotificationHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Initialize FCM and get token
    public void initializeNotifications() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token: " + token);

                    // Save token to preferences
                    saveTokenToPreferences(token);
                    
                    // Send token to server
                    sendTokenToServer(token);
                });

        // Subscribe to default topics
        subscribeToTopics();
    }

    // Subscribe to FCM topics
    private void subscribeToTopics() {
        FirebaseMessaging messaging = FirebaseMessaging.getInstance();
        
        if (isNotificationEnabled(NOTIF_MESSAGES)) {
            messaging.subscribeToTopic("messages");
        }
        
        if (isNotificationEnabled(NOTIF_OFFERS)) {
            messaging.subscribeToTopic("offers");
        }
        
        if (isNotificationEnabled(NOTIF_LISTINGS)) {
            messaging.subscribeToTopic("listings");
        }
        
        if (isNotificationEnabled(NOTIF_PROMOTIONS)) {
            messaging.subscribeToTopic("promotions");
        }
    }

    // Check if notification type is enabled
    public boolean isNotificationEnabled(String type) {
        return prefs.getBoolean(type, true); // Default to enabled
    }

    // Enable/disable notification type
    public void setNotificationEnabled(String type, boolean enabled) {
        prefs.edit().putBoolean(type, enabled).apply();
        
        FirebaseMessaging messaging = FirebaseMessaging.getInstance();
        if (enabled) {
            messaging.subscribeToTopic(type);
        } else {
            messaging.unsubscribeFromTopic(type);
        }
    }

    // Check if notifications are enabled globally
    public boolean areNotificationsEnabled() {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager.areNotificationsEnabled();
    }

    // Save FCM token to preferences
    private void saveTokenToPreferences(String token) {
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userPrefs.edit().putString("fcm_token", token).apply();
    }

    // Send FCM token to server
    private void sendTokenToServer(String token) {
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Long userId = userPrefs.getLong("userId", -1);
        
        if (userId == -1) {
            Log.d(TAG, "User not logged in, token will be sent after login");
            return;
        }

        RetrofitClient.init(context);
        ApiService apiService = RetrofitClient.getApiService();
        
        // TODO: Add this method to ApiService
        // Call<ApiResponse> call = apiService.updateFcmToken(userId, token);
        // call.enqueue(new Callback<ApiResponse>() {
        //     @Override
        //     public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
        //         if (response.isSuccessful()) {
        //             Log.d(TAG, "FCM token sent to server successfully");
        //         } else {
        //             Log.e(TAG, "Failed to send FCM token to server");
        //         }
        //     }
        //
        //     @Override
        //     public void onFailure(Call<ApiResponse> call, Throwable t) {
        //         Log.e(TAG, "Error sending FCM token to server", t);
        //     }
        // });
        
        Log.d(TAG, "TODO: Implement updateFcmToken API endpoint");
    }

    // Get notification settings summary
    public String getNotificationSettingsSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (!areNotificationsEnabled()) {
            summary.append("Thông báo đã bị tắt trong cài đặt hệ thống");
            return summary.toString();
        }
        
        int enabledCount = 0;
        if (isNotificationEnabled(NOTIF_MESSAGES)) enabledCount++;
        if (isNotificationEnabled(NOTIF_OFFERS)) enabledCount++;
        if (isNotificationEnabled(NOTIF_LISTINGS)) enabledCount++;
        if (isNotificationEnabled(NOTIF_PROMOTIONS)) enabledCount++;
        
        summary.append(enabledCount).append("/4 loại thông báo đã bật");
        return summary.toString();
    }

    // Reset all notification settings to default
    public void resetToDefaults() {
        prefs.edit().clear().apply();
        subscribeToTopics();
    }
}
