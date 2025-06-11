package com.example.ok;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.PasswordUpdateRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPassword extends AppCompatActivity {

    private static final String TAG = "ResetPassword";
    private EditText etToken;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etToken = findViewById(R.id.etToken);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Initialize RetrofitClient before using API services
        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();

        Uri data = getIntent().getData();
        if (data != null) {
            String token = data.getQueryParameter("token");
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "Token from deep link: " + token.substring(0, 5) + "...");
                etToken.setText(token);
            }
        }

        // Check for token from intent extra
        String token = getIntent().getStringExtra("token");
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "Token from intent: " + token.substring(0, 5) + "...");
            etToken.setText(token);
        }

        // Set click listeners
        btnResetPassword.setOnClickListener(v -> resetPassword());

        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> {
                Intent intent = new Intent(ResetPassword.this, Login.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void resetPassword() {
        String token = etToken.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validate inputs
        if (token.isEmpty()) {
            etToken.setError("Token không được để trống");
            etToken.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("Mật khẩu không được để trống");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đặt lại mật khẩu...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Disable button during API call
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Đang xử lý...");

        // Log token info for debugging
        Log.d(TAG, "Resetting password with token length: " + token.length());

        // Create request
        PasswordUpdateRequest request = new PasswordUpdateRequest(token, newPassword);

        // Make API call
        Call<ApiResponse> call = apiService.resetPassword(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                progressDialog.dismiss();


                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Đặt lại mật khẩu");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "Password reset successful");


                        new AlertDialog.Builder(ResetPassword.this)
                                .setTitle("Thành công")
                                .setMessage("Mật khẩu đã được đặt lại thành công!")
                                .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                                    Intent intent = new Intent(ResetPassword.this, Login.class);
                                    intent.putExtra("password_reset_success", true);
                                    startActivity(intent);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        Log.e(TAG, "API returned error: " + apiResponse.getMessage());
                        Toast.makeText(ResetPassword.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        // Parse error response
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";

                        Log.e(TAG, "HTTP Error: " + response.code() + " - " + errorBody);

                        // Try to parse as ApiResponse
                        try {
                            Gson gson = new Gson();
                            ApiResponse errorResponse = gson.fromJson(errorBody, ApiResponse.class);

                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                Toast.makeText(ResetPassword.this,
                                        errorResponse.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ResetPassword.this,
                                        "Lỗi: " + response.code(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ResetPassword.this,
                                    "Lỗi: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                        Toast.makeText(ResetPassword.this,
                                "Lỗi xử lý phản hồi: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Hide loading dialog
                progressDialog.dismiss();

                // Re-enable button
                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("Đặt lại mật khẩu");

                // Log detailed error
                Log.e(TAG, "Network error", t);

                // Show user-friendly error message based on exception type
                String errorMessage;
                if (t instanceof UnknownHostException) {
                    errorMessage = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
                } else if (t instanceof SocketTimeoutException) {
                    errorMessage = "Kết nối đến máy chủ quá thời gian. Vui lòng thử lại sau.";
                } else if (t instanceof IOException) {
                    errorMessage = "Lỗi kết nối: " + t.getMessage();
                } else {
                    errorMessage = "Lỗi không xác định: " + t.getMessage();
                }

                Toast.makeText(ResetPassword.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}