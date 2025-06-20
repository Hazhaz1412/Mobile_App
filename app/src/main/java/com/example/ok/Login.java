// Login.java
package com.example.ok;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
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
import com.example.ok.api.AuthApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.GoogleAuthRequest;
import com.example.ok.model.JwtAuthResponse;
import com.example.ok.model.LoginRequest;
import com.example.ok.util.JwtUtils;
import com.example.ok.util.SessionManager;
import com.google.gson.Gson;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Text;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Login extends AppCompatActivity {    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ApiService apiService;
    private AuthApiService authApiService;
    private SessionManager sessionManager;

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login2);

        // Initialize RetrofitClient with context
        RetrofitClient.init(this);

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
        authGG.setOnClickListener(v -> signInWithGoogle());        // Initialize RetrofitClient before using any API services
        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();
        authApiService = RetrofitClient.getAuthApiService();
        sessionManager = new SessionManager(this);

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
                        // Handle successful login - check if it's JWT response format
                        try {
                            JwtAuthResponse authResponse = apiResponse.getDataAs(JwtAuthResponse.class);
                            if (authResponse != null && authResponse.getToken() != null) {
                                // Save JWT tokens to auth_prefs for API authentication
                                SharedPreferences authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                                SharedPreferences.Editor authEditor = authPrefs.edit();
                                authEditor.putString("auth_token", authResponse.getToken());
                                authEditor.putString("refresh_token", authResponse.getRefreshToken());
                                authEditor.commit(); // Use commit() instead of apply() for immediate write
                                
                                Log.d("GOOGLE_AUTH", "=== JWT TOKEN SAVED (Google Auth) ===");
                                Log.d("GOOGLE_AUTH", "Token length: " + authResponse.getToken().length());
                                Log.d("GOOGLE_AUTH", "Token saved to auth_prefs: auth_token");
                                Log.d("GOOGLE_AUTH", "Verification - token retrieved: " + authPrefs.getString("auth_token", "").length());
                                Log.d("GOOGLE_AUTH", "=== END TOKEN SAVE ===");
                                
                                // Get userId from token and save user session
                                Long userId = getUserIdFromToken(authResponse.getToken());
                                if (userId != null) {
                                    saveUserSession(userId);
                                } else {
                                    Log.e("GOOGLE_AUTH", "Could not extract userId from token");
                                }
                            } else {
                                // Fallback to old format - save user ID directly
                                Object dataObj = apiResponse.getData();
                                saveUserSession(dataObj);
                            }
                        } catch (Exception e) {
                            Log.e("GOOGLE_AUTH", "Error parsing auth response", e);
                            // Fallback to old format
                            Object dataObj = apiResponse.getData();
                            saveUserSession(dataObj);
                        }

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
    }    private void performLogin() {
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
        Call<ApiResponse> call = authApiService.login(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // Lấy JWT response
                        JwtAuthResponse authResponse = apiResponse.getDataAs(JwtAuthResponse.class);
                        if (authResponse != null) {
                            // Lưu token vào SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("auth_token", authResponse.getToken());
                            editor.putString("refresh_token", authResponse.getRefreshToken());
                            editor.commit(); // Use commit() instead of apply() for immediate write
                            
                            Log.d("LOGIN", "=== JWT TOKEN SAVED (Regular Login) ===");
                            Log.d("LOGIN", "Token length: " + authResponse.getToken().length());
                            Log.d("LOGIN", "Token saved to auth_prefs: auth_token");
                            Log.d("LOGIN", "Verification - token retrieved: " + prefs.getString("auth_token", "").length());
                            Log.d("LOGIN", "=== END TOKEN SAVE ===");
                            
                            // Thêm hàm lấy userId từ token (nếu cần)
                            Long userId = getUserIdFromToken(authResponse.getToken());
                            if (userId != null) {
                                saveUserSession(userId);
                            }

                            Intent intent = new Intent(Login.this, MainMenu.class);
                            startActivity(intent);
                            finish(); // Đóng màn hình đăng nhập
                        } else {
                            Toast.makeText(Login.this, 
                                "Lỗi xử lý dữ liệu đăng nhập", 
                                Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(Login.this, 
                            apiResponse.getMessage(), 
                            Toast.LENGTH_LONG).show();
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
    }    // Phương thức giải mã token JWT để lấy userId
    private Long getUserIdFromToken(String token) {
        return JwtUtils.getUserIdFromToken(token);
    }

    private void saveUserSession(Long userId) {
        if (userId != null) {
            saveUserSession((Object) userId);
        } else {
            Log.e("LOGIN", "UserId is null, cannot save session");
        }
    }private void saveUserSession(Object userIdObj) {
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

        // Use SessionManager to save user session
        sessionManager.saveUserId(userId);
        
        Log.d("LOGIN", "=== USER SESSION SAVED ===");
        Log.d("LOGIN", "UserId saved: " + userId);
        Log.d("LOGIN", "SessionManager getUserId(): " + sessionManager.getUserId());
        Log.d("LOGIN", "=== END SESSION SAVE ===");
        
        // Keep the old SharedPreferences for backward compatibility (if needed elsewhere)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("userId", userId);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
        
        // Send FCM token to server after successful login
        sendFcmTokenToServer(userId);
    }
    
    private void sendFcmTokenToServer(long userId) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String fcmToken = prefs.getString("fcm_token", null);
        
        if (fcmToken != null) {
            ApiService apiService = RetrofitClient.getApiService();
            Call<ApiResponse> call = apiService.updateFcmToken(userId, fcmToken);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d("LOGIN", "FCM token sent to server successfully");
                    } else {
                        Log.e("LOGIN", "Failed to send FCM token to server: " + response.code());
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("LOGIN", "Error sending FCM token to server", t);
                }
            });
        }
    }
    
    /**
     * Static method to handle logout from anywhere in the app
     */
    public static void logout(Context context, boolean redirectToLogin) {
        // Get the refresh token
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String refreshToken = prefs.getString("refresh_token", "");
        
        if (!refreshToken.isEmpty()) {
            // Initialize RetrofitClient if needed
            RetrofitClient.init(context);
            AuthApiService authApiService = RetrofitClient.getAuthApiService();
            
            // Send logout request to server
            Map<String, String> request = new HashMap<>();
            request.put("refreshToken", refreshToken);
            
            authApiService.logout(request).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    Log.d("LOGOUT", "Logout successful on server");
                    // Continue with local logout regardless of server response
                    completeLogout(context, redirectToLogin);
                }
                
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Log.e("LOGOUT", "Failed to logout on server", t);
                    // Continue with local logout
                    completeLogout(context, redirectToLogin);
                }
            });
        } else {
            // No refresh token, just do local logout
            completeLogout(context, redirectToLogin);
        }
    }
      private static void completeLogout(Context context, boolean redirectToLogin) {
        // Clear authentication tokens
        SharedPreferences authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        authPrefs.edit()
            .remove("auth_token")
            .remove("refresh_token")
            .apply();
        
        // Clear user session using SessionManager
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.clearSession();
        
        // Clear legacy user session for backward compatibility
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userPrefs.edit()
            .remove("userId")
            .putBoolean("isLoggedIn", false)
            .apply();
        
        if (redirectToLogin) {
            // Redirect to login screen
            Intent intent = new Intent(context, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
    }
}