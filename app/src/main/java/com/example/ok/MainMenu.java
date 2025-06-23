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

public class MainMenu extends AppCompatActivity {    private AppCompatButton btnDashboard, btnCart, btnChat, btnUser;
    private AppCompatButton currentSelectedButton;@Override
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
    }    private void initViews() {
        btnDashboard = findViewById(R.id.btnDashboard);
        btnCart = findViewById(R.id.btnCart);
        btnChat = findViewById(R.id.btnChat);
        btnUser = findViewById(R.id.btnUser);

        // Log kiểm tra khi debug
        Log.d("MainMenu", "btnDashboard: " + (btnDashboard != null));
        Log.d("MainMenu", "btnCart: " + (btnCart != null));
        Log.d("MainMenu", "btnChat: " + (btnChat != null));
        Log.d("MainMenu", "btnUser: " + (btnUser != null));
    }private void setupClickListeners() {        // Trang chủ button
        if (btnDashboard != null) {
            btnDashboard.setOnClickListener(v -> {
                Log.d("MainMenu", getString(R.string.log_home_clicked));
                try {
                    HomeFragment homeFragment = new HomeFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
                            .commit();
                    setSelectedButton(btnDashboard);
                    Log.d("MainMenu", getString(R.string.success_home_fragment_loaded));
                } catch (Exception e) {
                    Log.e("MainMenu", getString(R.string.log_error_loading_fragment), e);
                }
            });
        } else {
            Log.e("MainMenu", "❌ btnDashboard is NULL!");
        }

        // Sửa tên button từ btnCart thành btnMyListings
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                long userId = prefs.getLong("userId", 0L);                if (userId == 0L) {
                    Toast.makeText(this, getString(R.string.error_login_required_listings), Toast.LENGTH_SHORT).show();
                    // Navigate to login or show login dialog
                } else {
                    loadFragment(new MyListingsFragment());
                    setSelectedButton(btnCart);
                }
            });
        }        // Thêm kiểm tra null cho btnChat
        if (btnChat != null) {            btnChat.setOnClickListener(v -> {
                // Open chat inbox
                loadFragment(new ChatInboxFragment());
                setSelectedButton(btnChat);
            });
        }

        if (btnUser != null) {
            btnUser.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                long userId = prefs.getLong("userId", 0L);                if (userId == 0L) {
                    Toast.makeText(this, getString(R.string.error_login_required_profile), Toast.LENGTH_SHORT).show();
                    // Navigate to login
                } else {
                    UserFragment fragment = UserFragment.newInstance(userId, true);
                    loadFragment(fragment);
                    setSelectedButton(btnUser);
                }
            });
        }
    }    private void setSelectedButton(AppCompatButton selectedButton) {
        // Reset all buttons
        resetButtonSelection();

        // Set selected button
        if (selectedButton != null) {
            currentSelectedButton = selectedButton;
            selectedButton.setBackgroundResource(R.drawable.customselectedbutton);
        }
    }    private void resetButtonSelection() {
        if (btnDashboard != null) btnDashboard.setBackgroundResource(R.drawable.customwhitebutton);
        if (btnCart != null) btnCart.setBackgroundResource(R.drawable.customwhitebutton);
        if (btnChat != null) btnChat.setBackgroundResource(R.drawable.customwhitebutton);
        if (btnUser != null) btnUser.setBackgroundResource(R.drawable.customwhitebutton);
    }public void loadFragment(Fragment fragment) {
        Log.d("MainMenu", getString(R.string.log_loading_fragment, fragment.getClass().getSimpleName()));
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss(); // Thay đổi này để tránh IllegalStateException
            Log.d("MainMenu", getString(R.string.success_fragment_loaded));
        } catch (Exception e) {
            Log.e("MainMenu", getString(R.string.log_error_loading_fragment), e);
        }
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
                    button = btnUser;                } else {
                    Toast.makeText(this, getString(R.string.error_login_required_profile), Toast.LENGTH_SHORT).show();
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
                  Log.d("MainMenu", getString(R.string.log_opened_chat_notification, roomId, otherName));
                
                // Clear the intent to prevent re-opening on orientation change
                intent.removeExtra("open_chat");
                return true;
            } else {
                Log.w("MainMenu", getString(R.string.log_invalid_chat_params));
            }
        }
        
        // Handle legacy chat notification intent (for compatibility)
        if (intent.getBooleanExtra("openChat", false)) {
            long roomId = intent.getLongExtra("roomId", -1);
            long myId = intent.getLongExtra("myId", -1);
            long otherId = intent.getLongExtra("otherId", -1);
            String otherName = intent.getStringExtra("otherName");
            
            if (roomId != -1 && myId != -1 && otherId != -1) {
                ChatFragment chatFragment = ChatFragment.newInstance(roomId, myId, otherId, otherName);                loadFragment(chatFragment);
                setSelectedButton(btnChat);
                
                Log.d("MainMenu", getString(R.string.log_opened_chat_legacy, roomId));
                
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
                ListingDetailFragment fragment = ListingDetailFragment.newInstance(listingId);                loadFragment(fragment);
                setSelectedButton(btnDashboard);
                
                Log.d("MainMenu", getString(R.string.log_opened_listing_notification, listingId));
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
            Log.d("MainMenu", getString(R.string.success_navigated_to_other_profile, displayName, userId));
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to other user profile", e);
            Toast.makeText(this, getString(R.string.error_cannot_open_user_profile), Toast.LENGTH_SHORT).show();
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
            Log.d("MainMenu", getString(R.string.success_navigated_to_offer_management));
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to offer management", e);
            Toast.makeText(this, getString(R.string.error_cannot_open_offer_management), Toast.LENGTH_SHORT).show();
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
            Log.d("MainMenu", getString(R.string.success_navigated_to_my_offers));
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to my offers", e);
            Toast.makeText(this, getString(R.string.error_cannot_open_my_offers), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to favorites from user profile
     */
    public void navigateToFavorites() {
        try {
            FavoritesFragment fragment = new FavoritesFragment();
            
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack("Favorites")
                    .commit();                    
            Log.d("MainMenu", getString(R.string.success_navigated_to_favorites));
        } catch (Exception e) {
            Log.e("MainMenu", "Error navigating to favorites", e);
            Toast.makeText(this, getString(R.string.error_cannot_open_favorites), Toast.LENGTH_SHORT).show();
        }
    }
}