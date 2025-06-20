package com.example.ok.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_AUTH_TOKEN = "authToken";
    
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    
    public SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    // User ID
    public void setUserId(long userId) {
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }
    
    public long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }
    
    // Email
    public void setEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
    
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }
    
    // Login status
    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    // Auth token
    public void setAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }
    
    public String getAuthToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, "");
    }
    
    // Clear all data (for logout)
    public void clearAll() {
        editor.clear();
        editor.apply();
    }
    
    // Check if user data exists
    public boolean hasUserData() {
        return getUserId() != -1 && !getEmail().isEmpty();
    }
}
