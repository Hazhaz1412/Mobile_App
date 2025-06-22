package com.example.ok;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ok.api.RetrofitClient;
import com.example.ok.service.ChatPollingService;

public class OkApplication extends Application {
    private static final String TAG = "OkApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "🚀 Application starting...");
        
        // Initialize RetrofitClient
        RetrofitClient.init(this);
        
        // Create notification channels for reliable notifications
        com.example.ok.util.NotificationChannelManager.createNotificationChannels(this);
        
        // Initialize NotificationHelper for FCM token management
        com.example.ok.util.NotificationHelper notificationHelper = new com.example.ok.util.NotificationHelper(this);
        notificationHelper.initializeNotifications();
        
        // 🔥 NEW: Start ChatPollingService if user is logged in
        startChatPollingServiceIfLoggedIn();
        
        Log.d(TAG, "✅ Application initialization complete");
    }
    
    /**
     * Start background chat polling service if user is logged in
     */
    private void startChatPollingServiceIfLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);
        
        if (userId != -1) {
            Log.d(TAG, "🔄 User logged in (ID: " + userId + ") - starting background chat polling service");
            ChatPollingService.startPollingService(this);
        } else {
            Log.d(TAG, "👤 User not logged in - skipping chat polling service");
        }
    }
}