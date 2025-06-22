package com.example.ok;

import android.app.Application;
import com.example.ok.api.RetrofitClient;

public class OkApplication extends Application {
      @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize RetrofitClient
        RetrofitClient.init(this);
        
        // Create notification channels for reliable notifications
        com.example.ok.util.NotificationChannelManager.createNotificationChannels(this);
    }
}