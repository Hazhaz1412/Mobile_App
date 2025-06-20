package com.example.ok.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "user_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        
        // Migrate data from old SharedPreferences if needed
        migrateFromOldPreferences(context);
    }
    
    private void migrateFromOldPreferences(Context context) {
        // Check if we need to migrate from old UserPrefs
        if (getUserId() == 0) {
            SharedPreferences oldPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            long oldUserId = oldPrefs.getLong("userId", 0);
            boolean oldIsLoggedIn = oldPrefs.getBoolean("isLoggedIn", false);
            
            if (oldUserId != 0 && oldIsLoggedIn) {
                Log.d("SessionManager", "Migrating user data from old preferences");
                Log.d("SessionManager", "Old userId: " + oldUserId + ", isLoggedIn: " + oldIsLoggedIn);
                
                // Migrate the data
                saveUserId(oldUserId);
                editor.putBoolean(KEY_IS_LOGGED_IN, oldIsLoggedIn);
                editor.apply();
                
                Log.d("SessionManager", "Migration complete - New userId: " + getUserId());
            }
        }
    }

    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public void saveUserId(long userId) {
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, 0);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}