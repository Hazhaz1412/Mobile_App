// Login.java
package com.example.ok;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.example.ok.model.GoogleAuthRequest;
import com.example.ok.model.LoginRequest;
import com.google.gson.Gson;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;


import org.w3c.dom.Text;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ApiService apiService;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnLogin = findViewById(R.id.login);
        tvRegister = findViewById(R.id.register);
        Button authGG = findViewById(R.id.button2);
        authGG = findViewById(R.id.button2);

        TextView fgpass = findViewById(R.id.fgpassjump);
        fgpass.setOnClickListener(v -> {
            Log.d("Login", "Forgot password clicked");
            Intent intent = new Intent(Login.this, ForgotPass.class);
            startActivity(intent);
        });


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("275122292032-7ujrq4ubkofbhv1iuvm1jda3kvimh53s.apps.googleusercontent.com")
                .requestEmail()
                .build();

// Build a GoogleSignInClient with the options
        googleSignInClient = GoogleSignIn.getClient(this, gso);

// Set up the button click listener
        authGG.setOnClickListener(v -> signInWithGoogle());

        apiService = RetrofitClient.getApiService();

        if (getIntent().hasExtra("registration_success")) {
            Toast.makeText(this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản.",
                    Toast.LENGTH_LONG).show();
        }

        btnLogin.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, register.class);
            startActivity(intent);
        });
    }

    private void authenticateWithBackend(String idToken, String email, String displayName) {

        GoogleAuthRequest request = new GoogleAuthRequest(idToken, email, displayName);

        Call<ApiResponse> call = apiService.googleAuth(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Handle successful login
                        Object dataObj = apiResponse.getData();
                        saveUserSession(dataObj);

                        // Navigate to main screen
                        Toast.makeText(Login.this, "Google login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Login.this, MainMenu.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(Login.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("GOOGLE_AUTH", "Error: " + errorBody);

                        Gson gson = new Gson();
                        ApiResponse errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                        Toast.makeText(Login.this, errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(Login.this, "Error: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("GOOGLE_AUTH", "Network error", t);
                Toast.makeText(Login.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            findViewById(R.id.button2).setEnabled(true);

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                // Get ID token
                String idToken = account.getIdToken();

                // Send token to your backend
                authenticateWithBackend(idToken, account.getEmail(), account.getDisplayName());
            } catch (ApiException e) {
                // Google Sign In failed
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void signInWithGoogle() {
        // Show loading state
        findViewById(R.id.button2).setEnabled(false);

        // Start Google Sign-In flow
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validar campos
        if (email.isEmpty()) {
            etEmail.setError("Email không được để trống");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Mật khẩu không được để trống");
            return;
        }

        // Desabilitar botão durante login
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // Criar requisição
        LoginRequest request = new LoginRequest(email, password);

        // Fazer chamada API
        Call<ApiResponse> call = apiService.login(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Convert Object to Long
                        Object dataObj = apiResponse.getData();
                        Long userId = null;

                        if (dataObj instanceof Integer) {
                            userId = ((Integer) dataObj).longValue();
                        } else if (dataObj instanceof Long) {
                            userId = (Long) dataObj;
                        } else if (dataObj instanceof Double) {
                            userId = ((Double) dataObj).longValue();
                        } else if (dataObj instanceof String) {
                            try {
                                userId = Long.parseLong((String) dataObj);
                            } catch (NumberFormatException e) {
                                Log.e("LOGIN", "Could not parse user ID", e);
                            }
                        }

                        if (userId != null) {
                            saveUserSession(userId);

                            Intent intent = new Intent(Login.this, MainMenu.class);
                            startActivity(intent);
                            // Rest of your code...
                        } else {
                            Log.e("LOGIN", "Invalid user ID data type: " + dataObj.getClass().getName());
                        }
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("LOGIN_ERROR", errorBody);

                        // Tentar analisar o erro como ApiResponse
                        Gson gson = new Gson();
                        ApiResponse errorResponse = gson.fromJson(errorBody, ApiResponse.class);

                        Toast.makeText(Login.this,
                                errorResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(Login.this,
                                "Lỗi đăng nhập: " + response.code(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                Log.e("LOGIN_ERROR", "Network error", t);
                Toast.makeText(Login.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserSession(Object userIdObj) {
        long userId;

        if (userIdObj instanceof Integer) {
            userId = ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            userId = (Long) userIdObj;
        } else if (userIdObj instanceof Double) {
            userId = ((Double) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            try {
                userId = Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                Log.e("LOGIN", "Invalid user ID", e);
                return;
            }
        } else {
            Log.e("LOGIN", "Invalid user ID type", new IllegalArgumentException());
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("userId", userId);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }
}