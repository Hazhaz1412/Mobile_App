package com.example.ok.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.ok.util.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Test class to verify push notification implementation
 */
public class NotificationTester {
    private static final String TAG = "NotificationTester";
    
    public static void testNotificationSystem(Context context) {
        Log.d(TAG, "=== TESTING NOTIFICATION SYSTEM ===");
        
        // Test 1: Check Firebase Messaging availability
        try {
            FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "✅ Firebase FCM Token retrieved successfully");
                        Log.d(TAG, "Token length: " + (token != null ? token.length() : 0));
                        
                        // Save token for testing
                        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("fcm_token", token).apply();
                        
                    } else {
                        Log.e(TAG, "❌ Failed to get FCM token", task.getException());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase Messaging not available", e);
        }
        
        // Test 2: Check NotificationHelper
        try {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            
            // Test notification channels
            boolean messagesEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_MESSAGES);
            boolean offersEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_OFFERS);
            boolean listingsEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_LISTINGS);
            boolean promotionsEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_PROMOTIONS);
            
            Log.d(TAG, "✅ NotificationHelper initialized successfully");
            Log.d(TAG, "Messages notifications: " + (messagesEnabled ? "ENABLED" : "DISABLED"));
            Log.d(TAG, "Offers notifications: " + (offersEnabled ? "ENABLED" : "DISABLED"));
            Log.d(TAG, "Listings notifications: " + (listingsEnabled ? "ENABLED" : "DISABLED"));
            Log.d(TAG, "Promotions notifications: " + (promotionsEnabled ? "ENABLED" : "DISABLED"));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ NotificationHelper error", e);
        }
        
        // Test 3: Check notification permissions
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            boolean notificationsEnabled = notificationManager.areNotificationsEnabled();
            Log.d(TAG, "System notifications: " + (notificationsEnabled ? "ENABLED" : "DISABLED"));
            
            if (!notificationsEnabled) {
                Log.w(TAG, "⚠️ System notifications are disabled - user needs to enable in settings");
            }
        }
        
        Log.d(TAG, "=== NOTIFICATION TEST COMPLETE ===");
    }
    
    /**
     * Test local notification creation
     */
    public static void testLocalNotification(Context context) {
        Log.d(TAG, "=== TESTING LOCAL NOTIFICATION ===");
        
        try {
            NotificationHelper helper = new NotificationHelper(context);
            
            // Create a test notification
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            androidx.core.app.NotificationCompat.Builder builder = 
                new androidx.core.app.NotificationCompat.Builder(context, "messages")
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("Test Notification")
                    .setContentText("Push notification system is working!")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            
            notificationManager.notify(999, builder.build());
            Log.d(TAG, "✅ Test notification sent successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send test notification", e);
        }
        
        Log.d(TAG, "=== LOCAL NOTIFICATION TEST COMPLETE ===");
    }
}
