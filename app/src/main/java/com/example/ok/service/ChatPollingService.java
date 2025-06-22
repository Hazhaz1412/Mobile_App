package com.example.ok.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ok.api.ChatApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ChatMessage;
import com.example.ok.util.ChatNotificationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Background service để polling tin nhắn chat độc lập với UI lifecycle
 * Giúp user nhận notification ngay cả khi không mở ChatFragment
 */
public class ChatPollingService extends Service {
    private static final String TAG = "ChatPollingService";
    
    // Polling intervals
    private static final int POLLING_INTERVAL_BACKGROUND = 5000; // 5 seconds for background
    private static final int POLLING_INTERVAL_FOREGROUND = 2000; // 2 seconds when in foreground
    
    // Service state
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private boolean isServiceRunning = false;
    
    // Chat and notification management
    private ChatApiService chatApiService;
    private ChatNotificationManager notificationManager;
    private long myUserId = -1;
    
    // Track active chat rooms and their latest message IDs
    private final Map<Long, Long> activeRooms = new ConcurrentHashMap<>();
    private final Map<Long, String> roomToUserName = new ConcurrentHashMap<>();
    private final Map<Long, Long> roomToOtherId = new ConcurrentHashMap<>();
    
    // Track which rooms are currently visible (to avoid spam notifications)
    private long currentVisibleRoomId = -1;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🔥 ChatPollingService created");
        
        // Initialize handler
        pollingHandler = new Handler(Looper.getMainLooper());
        
        // Initialize API service
        RetrofitClient.init(this);
        chatApiService = RetrofitClient.getChatApiService();
        
        // Initialize notification manager
        notificationManager = new ChatNotificationManager(this);
        
        // Get user ID from preferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        myUserId = prefs.getLong("userId", -1);
        
        Log.d(TAG, "✅ Service initialized with userId: " + myUserId);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🚀 Service start command received");
        
        if (intent != null) {
            String action = intent.getStringExtra("action");
            
            if ("START_POLLING".equals(action)) {
                startPolling();
            } else if ("STOP_POLLING".equals(action)) {
                stopPolling();
            } else if ("ADD_ROOM".equals(action)) {
                long roomId = intent.getLongExtra("roomId", -1);
                long otherId = intent.getLongExtra("otherId", -1);
                String otherName = intent.getStringExtra("otherName");
                long latestMessageId = intent.getLongExtra("latestMessageId", 0);
                addRoom(roomId, otherId, otherName, latestMessageId);
            } else if ("REMOVE_ROOM".equals(action)) {
                long roomId = intent.getLongExtra("roomId", -1);
                removeRoom(roomId);
            } else if ("SET_VISIBLE_ROOM".equals(action)) {
                long roomId = intent.getLongExtra("roomId", -1);
                setVisibleRoom(roomId);
            }
        }
        
        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Unbound service
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔥 ChatPollingService destroyed");
        stopPolling();
    }
    
    /**
     * Start background polling for all active rooms
     */
    private void startPolling() {
        if (isServiceRunning) {
            Log.d(TAG, "⚠️ Polling already running");
            return;
        }
        
        if (myUserId == -1) {
            Log.e(TAG, "❌ Cannot start polling - invalid user ID");
            return;
        }
        
        Log.d(TAG, "🔄 Starting background polling for chat messages");
        isServiceRunning = true;
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isServiceRunning) {
                    Log.d(TAG, "❌ Polling stopped, not scheduling next run");
                    return;
                }
                
                Log.d(TAG, "📡 Background polling for " + activeRooms.size() + " active rooms");
                
                // Poll each active room
                for (Map.Entry<Long, Long> entry : activeRooms.entrySet()) {
                    long roomId = entry.getKey();
                    long latestMessageId = entry.getValue();
                    pollRoomForNewMessages(roomId, latestMessageId);
                }
                
                // Schedule next polling
                if (isServiceRunning) {
                    // Use shorter interval if any room is visible (user actively chatting)
                    int interval = (currentVisibleRoomId != -1) ? 
                        POLLING_INTERVAL_FOREGROUND : POLLING_INTERVAL_BACKGROUND;
                    
                    pollingHandler.postDelayed(this, interval);
                    Log.d(TAG, "⏰ Next polling in " + interval + "ms");
                }
            }
        };
        
        // Start immediately
        pollingHandler.post(pollingRunnable);
    }
    
    /**
     * Stop background polling
     */
    private void stopPolling() {
        Log.d(TAG, "🛑 Stopping background polling");
        isServiceRunning = false;
        
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }
    
    /**
     * Add a chat room to be monitored by background polling
     */
    private void addRoom(long roomId, long otherId, String otherName, long latestMessageId) {
        Log.d(TAG, "➕ Adding room " + roomId + " for background polling");
        Log.d(TAG, "   Other user: " + otherName + " (ID: " + otherId + ")");
        Log.d(TAG, "   Latest message ID: " + latestMessageId);
        
        activeRooms.put(roomId, latestMessageId);
        roomToUserName.put(roomId, otherName);
        roomToOtherId.put(roomId, otherId);
        
        // Start polling if not already running
        if (!isServiceRunning) {
            startPolling();
        }
    }
    
    /**
     * Remove a chat room from background polling
     */
    private void removeRoom(long roomId) {
        Log.d(TAG, "➖ Removing room " + roomId + " from background polling");
        
        activeRooms.remove(roomId);
        roomToUserName.remove(roomId);
        roomToOtherId.remove(roomId);
        
        if (currentVisibleRoomId == roomId) {
            currentVisibleRoomId = -1;
        }
        
        // Stop polling if no rooms left
        if (activeRooms.isEmpty()) {
            stopPolling();
        }
    }
    
    /**
     * Set which room is currently visible (to avoid spam notifications)
     */
    private void setVisibleRoom(long roomId) {
        currentVisibleRoomId = roomId;
        Log.d(TAG, "👁️ Set visible room: " + roomId);
        
        // Clear notifications for this room since user is viewing it
        if (notificationManager != null) {
            notificationManager.clearChatNotifications(roomId);
        }
    }
    
    /**
     * Poll a specific room for new messages
     */
    private void pollRoomForNewMessages(long roomId, long latestMessageId) {
        Log.d(TAG, "📡 Polling room " + roomId + " for messages after ID " + latestMessageId);
        
        chatApiService.getChatMessagesDirect(roomId, myUserId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessage> allMessages = response.body();
                    List<ChatMessage> newMessages = new ArrayList<>();
                    
                    // Filter for new messages
                    long newestMessageId = latestMessageId;
                    for (ChatMessage message : allMessages) {
                        if (message.getIdSafely() > latestMessageId) {
                            // Set imageUrl for IMAGE messages
                            if ("IMAGE".equals(message.getType()) && message.getContent() != null) {
                                message.setImageUrl(message.getContent());
                            }
                            newMessages.add(message);
                            newestMessageId = Math.max(newestMessageId, message.getIdSafely());
                        }
                    }
                    
                    // Update latest message ID
                    if (newestMessageId > latestMessageId) {
                        activeRooms.put(roomId, newestMessageId);
                        Log.d(TAG, "📊 Updated room " + roomId + " latest message ID: " + newestMessageId);
                    }
                    
                    // Show notifications for new messages if room is not visible
                    if (!newMessages.isEmpty() && roomId != currentVisibleRoomId) {
                        String otherName = roomToUserName.get(roomId);
                        Long otherId = roomToOtherId.get(roomId);
                        
                        if (otherName != null && otherId != null && notificationManager != null) {
                            Log.d(TAG, "🔔 Showing notification for " + newMessages.size() + 
                                      " new messages in room " + roomId + " (not visible)");
                            notificationManager.showChatNotification(roomId, otherName, newMessages, myUserId, otherId);
                        }
                    } else if (!newMessages.isEmpty()) {
                        Log.d(TAG, "📱 " + newMessages.size() + " new messages in visible room " + roomId + " - no notification");
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Error polling room " + roomId + ": " + t.getMessage());
            }
        });
    }
    
    /**
     * Helper methods for external components to interact with service
     */
    public static void startPollingService(android.content.Context context) {
        Intent intent = new Intent(context, ChatPollingService.class);
        intent.putExtra("action", "START_POLLING");
        context.startService(intent);
    }
    
    public static void stopPollingService(android.content.Context context) {
        Intent intent = new Intent(context, ChatPollingService.class);
        intent.putExtra("action", "STOP_POLLING");
        context.startService(intent);
        context.stopService(intent);
    }
    
    public static void addRoomToPolling(android.content.Context context, long roomId, long otherId, String otherName, long latestMessageId) {
        Intent intent = new Intent(context, ChatPollingService.class);
        intent.putExtra("action", "ADD_ROOM");
        intent.putExtra("roomId", roomId);
        intent.putExtra("otherId", otherId);
        intent.putExtra("otherName", otherName);
        intent.putExtra("latestMessageId", latestMessageId);
        context.startService(intent);
    }
    
    public static void removeRoomFromPolling(android.content.Context context, long roomId) {
        Intent intent = new Intent(context, ChatPollingService.class);
        intent.putExtra("action", "REMOVE_ROOM");
        intent.putExtra("roomId", roomId);
        context.startService(intent);
    }
    
    public static void setVisibleRoom(android.content.Context context, long roomId) {
        Intent intent = new Intent(context, ChatPollingService.class);
        intent.putExtra("action", "SET_VISIBLE_ROOM");
        intent.putExtra("roomId", roomId);
        context.startService(intent);
    }
}
