package com.example.ok;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatButton;
import com.example.ok.ui.*;

public class MainMenu extends AppCompatActivity {

    private AppCompatButton btnDashboard, btnCart, btnChat, btnUser;
    private AppCompatButton currentSelectedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        initViews();
        setupClickListeners();

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            setSelectedButton(btnDashboard);
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
                Toast.makeText(this, "Tính năng chat đang phát triển", Toast.LENGTH_SHORT).show();
                // Có thể bổ sung code điều hướng đến màn hình chat sau này
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
        switch (tabName.toLowerCase()) {
            case "home":
            case "dashboard":
                btnDashboard.performClick();
                break;
            case "cart":
                btnCart.performClick();
                break;
            case "chat":
            case "messages":
                btnChat.performClick();
                break;

            case "user":
            case "profile":
                btnUser.performClick();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // Check if there are fragments in back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}