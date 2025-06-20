package com.example.ok.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Listing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for filtering blocked users from content
 */
public class BlockedUserFilter {
    private static final String TAG = "BlockedUserFilter";
    private static final String PREFS_NAME = "blocked_users_cache";
    private static final String KEY_BLOCKED_USERS = "blocked_user_ids";
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    private static final String KEY_CACHE_TIME = "cache_time";
    
    private static BlockedUserFilter instance;
    private Context context;
    private Set<Long> blockedUserIds = new HashSet<>();
    private boolean isLoading = false;
    private long lastCacheTime = 0;
    
    private BlockedUserFilter(Context context) {
        this.context = context.getApplicationContext();
        loadCachedBlockedUsers();
    }
    
    public static synchronized BlockedUserFilter getInstance(Context context) {
        if (instance == null) {
            instance = new BlockedUserFilter(context);
        }
        return instance;
    }
    
    /**
     * Filter listings to exclude blocked users
     */
    public List<Listing> filterListings(List<Listing> listings) {
        if (listings == null || listings.isEmpty()) {
            return listings;
        }
        
        List<Listing> filteredListings = new ArrayList<>();
        for (Listing listing : listings) {
            if (listing.getUserId() != null && 
                !blockedUserIds.contains(listing.getUserId())) {
                filteredListings.add(listing);
            }
        }
        
        Log.d(TAG, "Filtered " + (listings.size() - filteredListings.size()) + 
                " listings from blocked users. Original: " + listings.size() + 
                ", Filtered: " + filteredListings.size());
        
        return filteredListings;
    }
    
    /**
     * Check if a user is blocked
     */
    public boolean isUserBlocked(Long userId) {
        return userId != null && blockedUserIds.contains(userId);
    }
    
    /**
     * Refresh blocked users list from server
     */
    public void refreshBlockedUsers(final RefreshCallback callback) {
        if (isLoading) {
            if (callback != null) callback.onComplete(false);
            return;
        }
        
        // Check if cache is still valid
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheTime < CACHE_DURATION) {
            Log.d(TAG, "Using cached blocked users list");
            if (callback != null) callback.onComplete(true);
            return;
        }
        
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "No current user ID found");
            if (callback != null) callback.onComplete(false);
            return;
        }
        
        isLoading = true;
        Log.d(TAG, "Refreshing blocked users list from server");
        
        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.getBlockedUsers(currentUserId);
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                isLoading = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        updateBlockedUsers(apiResponse.getData());
                        if (callback != null) callback.onComplete(true);
                        return;
                    }
                }
                
                Log.w(TAG, "Failed to refresh blocked users: " + response.code());
                if (callback != null) callback.onComplete(false);
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e(TAG, "Error refreshing blocked users", t);
                if (callback != null) callback.onComplete(false);
            }
        });
    }
    
    /**
     * Add a user to blocked list (when user blocks someone)
     */
    public void addBlockedUser(Long userId) {
        if (userId != null) {
            blockedUserIds.add(userId);
            saveCachedBlockedUsers();
            Log.d(TAG, "Added user " + userId + " to blocked list");
        }
    }
    
    /**
     * Remove a user from blocked list (when user unblocks someone)
     */
    public void removeBlockedUser(Long userId) {
        if (userId != null) {
            blockedUserIds.remove(userId);
            saveCachedBlockedUsers();
            Log.d(TAG, "Removed user " + userId + " from blocked list");
        }
    }
    
    private Long getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);
        return userId == -1 ? null : userId;
    }
      private void updateBlockedUsers(Object data) {
        blockedUserIds.clear();
        
        try {
            if (data instanceof List) {
                List<?> userList = (List<?>) data;
                for (Object user : userList) {
                    Long userId = extractUserId(user);
                    if (userId != null) {
                        blockedUserIds.add(userId);
                    }
                }
            } else {
                Log.w(TAG, "Unexpected data format for blocked users: " + data.getClass().getSimpleName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing blocked users data", e);
        }
        
        lastCacheTime = System.currentTimeMillis();
        saveCachedBlockedUsers();
        Log.d(TAG, "Updated blocked users list: " + blockedUserIds.size() + " users");
    }
    
    private Long extractUserId(Object user) {
        try {
            if (user instanceof Number) {
                return ((Number) user).longValue();
            } else if (user instanceof String) {
                return Long.parseLong((String) user);
            } else if (user instanceof java.util.Map) {
                // Handle user object with properties like {id: 123, name: "..."}
                java.util.Map<?, ?> userMap = (java.util.Map<?, ?>) user;
                Object idObj = userMap.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                } else if (idObj instanceof String) {
                    return Long.parseLong((String) idObj);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract user ID from: " + user, e);
        }
        return null;
    }
    
    private void loadCachedBlockedUsers() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedIds = prefs.getString(KEY_BLOCKED_USERS, "");
        lastCacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
        
        blockedUserIds.clear();
        if (!cachedIds.isEmpty()) {
            String[] ids = cachedIds.split(",");
            for (String id : ids) {
                try {
                    blockedUserIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid cached user ID: " + id);
                }
            }
        }
        
        Log.d(TAG, "Loaded " + blockedUserIds.size() + " cached blocked users");
    }
    
    private void saveCachedBlockedUsers() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (Long userId : blockedUserIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(userId);
        }
        
        prefs.edit()
                .putString(KEY_BLOCKED_USERS, sb.toString())
                .putLong(KEY_CACHE_TIME, lastCacheTime)
                .apply();
    }
    
    public interface RefreshCallback {
        void onComplete(boolean success);
    }
}
