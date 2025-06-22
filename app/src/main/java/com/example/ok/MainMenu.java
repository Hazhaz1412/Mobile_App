package com.example.ok;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatButton;
import com.example.ok.ui.*;
import com.example.ok.util.NotificationChannelManager;

public class MainMenu extends AppCompatActivity {

    private AppCompatButton btnDashboard, btnCart, btnChat, btnUser;
    private AppCompatButton currentSelectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);        // Create notification channels when MainMenu starts - critical for notifications
        NotificationChannelManager.createNotificationChannels(this);

        initViews();
        setupClickListeners();

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            // Check if opened from notification first
            if (!handleNotificationIntent()) {
                // Load default if not opened from notification
                loadFragment(new HomeFragment());
                setSelectedButton(btnDashboard);
            }
        }
    }

    private void initViews() {
        // Đảm bảo các ID đúng với layout
        btnDashboard = findViewById(R.id.btnDashboard);
        btnCart = findViewById(R.id.btnCart);
        btnChat = findViewById(R.id.btnChat);
        btnUser = findViewById(R.id.btnUser);

        // Log kiểm tra khi debug
        Log.d("MainMenu", "btnDashboard: " + (btnDashboard != null));
        Log.d("MainMenu", "btnCart: " + (btnCart != null));
        Log.d("MainMenu", "btnChat: " + (btnChat != null));
        Log.d("MainMenu", "btnUser: " + (btnUser != null));
    }

    private void setupClickListeners() {
        if (btnDashboard != null) {
            btnDashboard.setOnClickListener(v -> {
                loadFragment(new HomeFragment());
                setSelectedButton(btnDashboard);
            });
        }

        // Sửa tên button từ btnCart thành btnMyListings
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                long userId = prefs.getLong("userId", 0L);

                if (userId == 0L) {
                    Toast.makeText(this, "Bạn cần đăng nhập để xem tin đăng", Toast.LENGTH_SHORT).show();
                    // Navigate to login or show login dialog
                } else {
                    loadFragment(new MyListingsFragment());
                    setSelectedButton(btnCart);
                }
            });
        }

        // Thêm kiểm tra null cho btnChat
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                // Open chat inbox
                loadFragment(new ChatInboxFragment());
                setSelectedButton(btnChat);
            });
        }

        if (btnUser != null) {
            btnUser.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                long userId = prefs.getLong("userId", 0L);

                if (userId == 0L) {
                    Toast.makeText(this, "Bạn cần đăng nhập để xem hồ sơ", Toast.LENGTH_SHORT).show();
                    // Navigate to login
                } else {
                    UserFragment fragment = UserFragment.newInstance(userId, true);
                    loadFragment(fragment);
                    setSelectedButton(btnUser);
                }
            });
        }
    }

    private void setSelectedButton(AppCompatButton selectedButton) {
        // Reset all buttons
        resetButtonSelection();

        // Set selected button
        currentSelectedButton = selectedButton;
        selectedButton.setBackgroundResource(R.drawable.customselectedbutton);
    }

    private void resetButtonSelection() {
        btnDashboard.setBackgroundResource(R.drawable.customwhitebutton);
        btnCart.setBackgroundResource(R.drawable.customwhitebutton);
        btnChat.setBackgroundResource(R.drawable.customwhitebutton);
        btnUser.setBackgroundResource(R.drawable.customwhitebutton);
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToCreateListing() {
        replaceFragment(new CreateListingFragment());
    }

    public void navigateToTab(String tabName) {
        Fragment fragment = null;
        AppCompatButton button = null;

        switch (tabName.toLowerCase()) {
            case "home":
                fragment = new HomeFragment();
                button = btnDashboard;
                break;
            case "search":
                fragment = new SearchFragment();
                button = null; // Search không thuộc bottom menu
                break;
            case "mylistings":
                fragment = new MyListingsFragment();
                button = btnCart;
                break;
            case "user":
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                long userId = prefs.getLong("userId", 0L);
                if (userId > 0) {
                    fragment = UserFragment.newInstance(userId, true);
                    button = btnUser;
                } else {
                    Toast.makeText(this, "Bạn cần đăng nhập để xem hồ sơ", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }

        if (fragment != null) {
            loadFragment(fragment);
            if (button != null) {
                setSelectedButton(button);
            }
        }
    }    public void navigateToNotificationSettings() {
        NotificationSettingsFragment fragment = new NotificationSettingsFragment();
        loadFragment(fragment);
    }
    
    public void navigateToPaymentHistory() {
        PaymentHistoryFragment fragment = PaymentHistoryFragment.newInstance();
        loadFragment(fragment);
    }
      /**
     * Handle intent from notification taps - Enhanced for chat notifications
     */
    private boolean handleNotificationIntent() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        
        // Handle chat notification intent (new format)
        if (intent.getBooleanExtra("open_chat", false)) {
            long roomId = intent.getLongExtra("roomId", -1);
            long myId = intent.getLongExtra("myId", -1);
            long otherId = intent.getLongExtra("otherId", -1);
            String otherName = intent.getStringExtra("otherName");
            
            if (roomId != -1 && myId != -1 && otherId != -1) {
                // Open the specific chat
                ChatFragment chatFragment = ChatFragment.newInstance(roomId, myId, otherId, otherName);
                loadFragment(chatFragment);
                setSelectedButton(btnChat);
                
                Log.d("MainMenu", "✅ Opened chat from notification: roomId=" + roomId + 
                                ", other=" + otherName);
                
                // Clear the intent to prevent re-opening on orientation change
                intent.removeExtra("open_chat");
                return true;
            } else {
                Log.w("MainMenu", "❌ Invalid chat parameters in notification intent");
            }
        }
        
        // Handle legacy chat notification intent (for compatibility)
        if (intent.getBooleanExtra("openChat", false)) {
            long roomId = intent.getLongExtra("roomId", -1);
            long myId = intent.getLongExtra("myId", -1);
            long otherId = intent.getLongExtra("otherId", -1);
            String otherName = intent.getStringExtra("otherName");
            
            if (roomId != -1 && myId != -1 && otherId != -1) {
                ChatFragment chatFragment = ChatFragment.newInstance(roomId, myId, otherId, otherName);
                loadFragment(chatFragment);
                setSelectedButton(btnChat);
                
                Log.d("MainMenu", "✅ Opened chat from legacy notification: roomId=" + roomId);
                
                // Clear the intent
                intent.removeExtra("openChat");
                return true;
            }
        }
        
        // Handle other notification types (offers, listings, etc.)
        if (intent.getBooleanExtra("open_listing", false)) {
            long listingId = intent.getLongExtra("listing_id", -1);
            if (listingId != -1) {
                // Open specific listing
                ListingDetailFragment fragment = ListingDetailFragment.newInstance(listingId);
                loadFragment(fragment);
                setSelectedButton(btnDashboard);
                
                Log.d("MainMenu", "✅ Opened listing from notification: " + listingId);
                intent.removeExtra("open_listing");
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void onBackPressed() {
        // Check if there are fragments in back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }    /**
     * Navigate to other user profile from anywhere in the app
     */
    public void navigateToOtherUserProfile(long userId, String displayName) {
        try {
            OtherUserProfileFragment fragment = OtherUserProfileFragment.newInstance(userId, displayName);
            
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("OtherUserProfile")
                    .commit();
                    
            Log.d("MainMenu", "Navigated to other user profile: " + displayName + " (ID: " + userId + ")");
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to other user profile", e);
            Toast.makeText(this, "Không thể mở profile người dùng", Toast.LENGTH_SHORT).show();
        }
    }
      /**
     * Navigate to offer management for sellers
     */
    public void navigateToOfferManagement() {
        try {
            OfferManagementFragment fragment = OfferManagementFragment.newInstance();
            
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("OfferManagement")
                    .commit();
                    
            Log.d("MainMenu", "Navigated to offer management");
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to offer management", e);
            Toast.makeText(this, "Không thể mở quản lý yêu cầu giảm giá", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to my offers for buyers
     */
    public void navigateToMyOffers() {
        try {
            MyOffersFragment fragment = new MyOffersFragment();
            
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("MyOffers")
                    .commit();
                    
            Log.d("MainMenu", "Navigated to my offers");
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to my offers", e);
            Toast.makeText(this, "Không thể mở yêu cầu của tôi", Toast.LENGTH_SHORT).show();
        }
    }
}