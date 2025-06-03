package com.example.ok;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.ok.ui.HomeFragment; // Đảm bảo import đúng package
import com.example.ok.ui.UserFragment; // Đảm bảo import đúng package

public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnUser = findViewById(R.id.btnUser);
        Button btnDashboard = findViewById(R.id.btnDashboard);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        btnDashboard.setOnClickListener(v -> loadFragment(new HomeFragment()));
        btnUser.setOnClickListener(v -> {
            // Lấy userId từ UserPrefs - PHẢI ĐÚNG TÊN SHARED PREFS
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            long userId = prefs.getLong("userId", 0L);

            if (userId == 0L) {
                Toast.makeText(this, "Bạn cần đăng nhập để xem hồ sơ", Toast.LENGTH_SHORT).show();
                // Có thể chuyển qua màn hình đăng nhập tại đây
                // Intent loginIntent = new Intent(this, Login.class);
                // startActivity(loginIntent);
            } else {
                // Tạo fragment với userId
                UserFragment fragment = UserFragment.newInstance(userId, true);
                loadFragment(fragment);
            }
        });

    }
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}