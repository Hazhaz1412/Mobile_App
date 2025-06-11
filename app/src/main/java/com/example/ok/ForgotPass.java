package com.example.ok;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.ok.model.PasswordResetRequest;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.app.ProgressDialog;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
public class ForgotPass extends AppCompatActivity {

    private EditText etEmail;
    private Button btnForgotPass;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_pass);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etEmail = findViewById(R.id.txtEmailForgot1);
        btnForgotPass = findViewById(R.id.btnForgotPass);

        // Initialize RetrofitClient before using API services
        RetrofitClient.init(this);
        apiService = RetrofitClient.getApiService();

        // Set click listener
        btnForgotPass.setOnClickListener(v -> {
            requestPasswordReset();
        });
    }

    private void requestPasswordReset() {
        String email = etEmail.getText().toString().trim();

        // Validate email with regex pattern
        if (email.isEmpty()) {
            etEmail.setError("Email không được để trống");
            return;
        }

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (!email.matches(emailPattern)) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi yêu cầu...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Disable button during API call
        btnForgotPass.setEnabled(false);
        btnForgotPass.setText("Đang xử lý...");

        // Create request
        PasswordResetRequest request = new PasswordResetRequest(email);

        // Make API call
        Call<ApiResponse> call = apiService.requestPasswordReset(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                // Hide loading dialog
                progressDialog.dismiss();

                // Re-enable button
                btnForgotPass.setEnabled(true);
                btnForgotPass.setText("Gửi yêu cầu");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Log.d("PASSWORD_RESET", "Reset request successful: " + apiResponse.getMessage());

                        // Show success dialog with options
                        new AlertDialog.Builder(ForgotPass.this)
                                .setTitle("Email đã được gửi")
                                .setMessage("Hướng dẫn đặt lại mật khẩu đã được gửi đến email " + email + ". Bạn có muốn tiếp tục đến màn hình nhập mã token?")
                                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                                    Intent intent = new Intent(ForgotPass.this, ResetPassword.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Để sau", (dialog, which) -> {
                                    finish(); // Return to login screen
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        // Show error message from API
                        Log.e("PASSWORD_RESET", "API error: " + apiResponse.getMessage());
                        Toast.makeText(ForgotPass.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        // Parse error response
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";

                        Log.e("PASSWORD_RESET", "HTTP Error: " + response.code() + " - " + errorBody);

                        // Try to parse the error as JSON
                        try {
                            JSONObject jsonError = new JSONObject(errorBody);
                            String message = jsonError.optString("message", "Lỗi không xác định");
                            Toast.makeText(ForgotPass.this, message, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            // If not valid JSON, show raw error
                            Toast.makeText(ForgotPass.this,
                                    "Lỗi: " + response.code() + " - " + errorBody,
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("PASSWORD_RESET", "Error processing response", e);
                        Toast.makeText(ForgotPass.this,
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
                btnForgotPass.setEnabled(true);
                btnForgotPass.setText("Gửi yêu cầu");

                // Log detailed error
                Log.e("PASSWORD_RESET", "Network error", t);

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

                Toast.makeText(ForgotPass.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}