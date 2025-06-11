package com.example.ok;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;
import android.widget.ImageView;

import com.example.ok.test.NotificationTester;
import com.example.ok.ui.CreateListingFragment;
import com.example.ok.util.NotificationChannelManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Create notification channels FIRST - this is critical for notifications to work
        NotificationChannelManager.createNotificationChannels(this);
        
        // Get FCM token for notifications
        initializeFCM();

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // User is already logged in, go directly to MainMenu
            Intent intent = new Intent(MainActivity.this, MainMenu.class);
            startActivity(intent);
            finish(); // This prevents going back to MainActivity when pressing back
            return;
        }

        // Continue with normal MainActivity setup for non-logged in users
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView introduce = findViewById(R.id.introduce);
        introduce.setText(R.string.introduce);
        introduce.setTextSize(20);

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setText(R.string.next);

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });
    }

    private void initializeFCM() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d("FCM", "FCM Registration Token: " + token);
                    
                    // Save token to SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit().putString("fcm_token", token).apply();
                    
                    // Send token to server if user is logged in
                    sendTokenToServerIfLoggedIn(token);
                });
    }
    
    private void sendTokenToServerIfLoggedIn(String token) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
        
        if (userId != -1) {
            // User is logged in, send token to server
            // This will be handled by the FirebaseMessagingService's sendTokenToServer method
            Log.d("FCM", "User logged in, token will be sent to server via FCM service");
        }
        
        // Test notification system
        testNotificationSystem();
    }
    
    /**
     * Test the notification system to verify it's working correctly
     */
    private void testNotificationSystem() {
        Log.d("MainActivity", "=== TESTING NOTIFICATION SYSTEM ===");
        
        // Run notification tests
        NotificationTester.testNotificationSystem(this);
        
        // Test local notification after a small delay
        new Handler(getMainLooper()).postDelayed(() -> {
            Log.d("MainActivity", "Testing local notification...");
            NotificationTester.testLocalNotification(this);
        }, 2000); // 2 second delay
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}