package com.example.ok;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class register extends AppCompatActivity {

    private EditText etEmail, etPassword, etDisplayName;
    private Button btnRegister;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        etEmail = findViewById(R.id.editTextText4);
        etPassword = findViewById(R.id.editTextTextPassword2);
        etDisplayName = findViewById(R.id.editTextText3);
        btnRegister = findViewById(R.id.login2);
        TextView login = findViewById(R.id.loginline);
        TextView firstLine = findViewById(R.id.optionLogin3);
        TextView secondLine = findViewById(R.id.optionLogin4);

        // Initialize API service
        apiService = RetrofitClient.getApiService();

        firstLine.setTextSize(16);
        secondLine.setTextSize(20);

        // Set up listeners
        login.setOnClickListener(v -> {
            Intent intent = new Intent(register.this, Login.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Get field values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();

        // Validate fields
        if (email.isEmpty()) {
            etEmail.setError("Email cannot be empty");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password cannot be empty");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (displayName.isEmpty()) {
            etDisplayName.setError("Display name cannot be empty");
            return;
        }

        // Disable button during registration
        btnRegister.setEnabled(false);

        // Create request
        RegisterRequest request = new RegisterRequest(email, password, displayName);

        // Make API call
        Call<ApiResponse> call = apiService.register(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(register.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();

                        // Navigate to login screen
                        Intent intent = new Intent(register.this, Login.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(register.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(register.this,
                            "Server error: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                Toast.makeText(register.this,
                        "Connection error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}