package com.example.ok;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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
import com.example.ok.model.RegisterRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class register extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etDisplayName;
    private Button btnRegister, ggregis;
    private ApiService apiService;
    private ProgressDialog progressDialog;

    // Google Sign-In
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient googleSignInClient;

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
        ggregis = findViewById(R.id.ggregis);
        TextView login = findViewById(R.id.loginline);
        TextView firstLine = findViewById(R.id.optionLogin3);
        TextView secondLine = findViewById(R.id.optionLogin4);

        // Initialize API service
        apiService = RetrofitClient.getApiService();

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng ký...");
        progressDialog.setCancelable(false);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("275122292032-7ujrq4ubkofbhv1iuvm1jda3kvimh53s.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        firstLine.setTextSize(16);
        secondLine.setTextSize(20);

        // Set up listeners
        login.setOnClickListener(v -> {
            Intent intent = new Intent(register.this, Login.class);
            startActivity(intent);
            finish();
        });

        btnRegister.setOnClickListener(v -> registerUser());

        // Google Sign-In button listener
        ggregis.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        // Show loading state
        ggregis.setEnabled(false);
        ggregis.setText("Đang xử lý...");

        // Start Google Sign-In flow
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            ggregis.setEnabled(true);
            ggregis.setText("Đăng ký với Google");

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                // Get ID token
                String idToken = account.getIdToken();

                // Send token to your backend for registration
                authenticateWithBackend(idToken, account.getEmail(), account.getDisplayName());
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Google sign in failed", e);
                Toast.makeText(this, "Google đăng ký thất bại: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void authenticateWithBackend(String idToken, String email, String displayName) {
        progressDialog.setMessage("Đang đăng ký với Google...");
        progressDialog.show();

        GoogleAuthRequest request = new GoogleAuthRequest(idToken, email, displayName);

        Call<ApiResponse> call = apiService.googleAuth(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Handle successful registration/login
                        Object dataObj = apiResponse.getData();
                        saveUserSession(dataObj);

                        // Navigate to main screen
                        Toast.makeText(register.this, "Đăng ký Google thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(register.this, MainMenu.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(register.this, apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e("GOOGLE_AUTH", "Error: " + errorBody);

                        Gson gson = new Gson();
                        ApiResponse errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                        Toast.makeText(register.this, errorResponse.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(register.this, "Lỗi: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("GOOGLE_AUTH", "Network error", t);
                Toast.makeText(register.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                Log.e("REGISTER", "Invalid user ID", e);
                return;
            }
        } else {
            Log.e("REGISTER", "Invalid user ID type", new IllegalArgumentException());
            return;
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("userId", userId);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }

    // ...existing code...
    private void registerUser() {
        // Get field values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();

        // Validate fields
        if (!validateInput(email, password, displayName)) {
            return;
        }

        // Show progress dialog
        progressDialog.show();

        // Disable button during registration
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        // Create request
        RegisterRequest request = new RegisterRequest(email, password, displayName);

        // Make API call
        Call<ApiResponse> call = apiService.register(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // Registration successful
                        Toast.makeText(register.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();

                        // Navigate to login screen with success flag
                        Intent intent = new Intent(register.this, Login.class);
                        intent.putExtra("registration_success", true);
                        intent.putExtra("registered_email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        // Registration failed - show server message
                        Toast.makeText(register.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Server error
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressDialog.dismiss();
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng ký");

                Log.e("REGISTER_ERROR", "Network error", t);
                Toast.makeText(register.this,
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ...existing code...
    private boolean validateInput(String email, String password, String displayName) {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);
        etDisplayName.setError(null);

        boolean isValid = true;

        // Validate email
        if (email.isEmpty()) {
            etEmail.setError("Email không được để trống");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        // Validate password
        if (password.isEmpty()) {
            etPassword.setError("Mật khẩu không được để trống");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        } else if (!password.matches(".*[A-Z].*")) {
            etPassword.setError("Mật khẩu phải có ít nhất 1 chữ hoa");
            isValid = false;
        } else if (!password.matches(".*[a-z].*")) {
            etPassword.setError("Mật khẩu phải có ít nhất 1 chữ thường");
            isValid = false;
        } else if (!password.matches(".*\\d.*")) {
            etPassword.setError("Mật khẩu phải có ít nhất 1 chữ số");
            isValid = false;
        }

        // Validate display name
        if (displayName.isEmpty()) {
            etDisplayName.setError("Tên hiển thị không được để trống");
            isValid = false;
        } else if (displayName.length() < 2) {
            etDisplayName.setError("Tên hiển thị phải có ít nhất 2 ký tự");
            isValid = false;
        } else if (displayName.length() > 50) {
            etDisplayName.setError("Tên hiển thị không được quá 50 ký tự");
            isValid = false;
        }

        return isValid;
    }

    private void handleErrorResponse(Response<ApiResponse> response) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                Log.e("REGISTER_ERROR", "Error response: " + errorBody);

                // Try to parse error as ApiResponse
                Gson gson = new Gson();
                ApiResponse errorResponse = gson.fromJson(errorBody, ApiResponse.class);

                if (errorResponse != null && errorResponse.getMessage() != null) {
                    Toast.makeText(register.this,
                            errorResponse.getMessage(),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(register.this,
                            "Lỗi server: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(register.this,
                        "Lỗi server: " + response.code(),
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("REGISTER_ERROR", "Error parsing error response", e);
            Toast.makeText(register.this,
                    "Lỗi server: " + response.code(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}