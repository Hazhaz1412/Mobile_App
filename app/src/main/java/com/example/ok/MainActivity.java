package com.example.ok;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

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

import com.example.ok.ui.CreateListingFragment;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

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


    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}