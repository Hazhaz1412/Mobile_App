package com.example.ok.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.EscrowInfo;
import com.example.ok.model.EscrowRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EscrowActivity extends AppCompatActivity {
    
    private static final String TAG = "EscrowActivity";
    
    private TextView tvPaymentId, tvEscrowStatus, tvDaysRemaining, tvHoldUntil;
    private Button btnReleasePayment, btnReportDispute, btnRefresh;
    
    private Long paymentId;
    private Long currentUserId;
    private EscrowInfo escrowInfo;
    private ApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escrow);
        
        initViews();
        getIntentData();
        setupApiService();
        setupClickListeners();
        
        // Load escrow info
        loadEscrowInfo();
    }
    
    private void initViews() {
        tvPaymentId = findViewById(R.id.tvPaymentId);
        tvEscrowStatus = findViewById(R.id.tvEscrowStatus);
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
        tvHoldUntil = findViewById(R.id.tvHoldUntil);
        
        btnReleasePayment = findViewById(R.id.btnReleasePayment);
        btnReportDispute = findViewById(R.id.btnReportDispute);
        btnRefresh = findViewById(R.id.btnRefresh);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
    
    private void getIntentData() {
        paymentId = getIntent().getLongExtra("paymentId", -1);
        if (paymentId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
    }
    
    private void setupApiService() {
        apiService = RetrofitClient.getApiService();
    }
    
    private void setupClickListeners() {
        btnReleasePayment.setOnClickListener(v -> showReleasePaymentDialog());
        btnReportDispute.setOnClickListener(v -> showReportDisputeDialog());
        btnRefresh.setOnClickListener(v -> loadEscrowInfo());
    }
    
    private void loadEscrowInfo() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải thông tin bảo vệ...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        Call<EscrowInfo> call = apiService.getEscrowInfo(paymentId);
        call.enqueue(new Callback<EscrowInfo>() {
            @Override
            public void onResponse(@NonNull Call<EscrowInfo> call, @NonNull Response<EscrowInfo> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    escrowInfo = response.body();
                    updateUI();
                } else {
                    Log.e(TAG, "Error loading escrow info: " + response.code());
                    Toast.makeText(EscrowActivity.this, "Lỗi tải thông tin: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<EscrowInfo> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to load escrow info", t);
                Toast.makeText(EscrowActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateUI() {
        if (escrowInfo == null) return;
        
        tvPaymentId.setText("Mã giao dịch: #" + escrowInfo.getPaymentId());
        
        // Status
        String statusText = getStatusText(escrowInfo.getEscrowStatus());
        tvEscrowStatus.setText("Trạng thái: " + statusText);
        
        // Days remaining
        if (escrowInfo.getDaysRemaining() > 0) {
            tvDaysRemaining.setText("Thời gian bảo vệ: " + escrowInfo.getDaysRemaining() + " ngày");
            tvDaysRemaining.setVisibility(View.VISIBLE);
        } else {
            tvDaysRemaining.setVisibility(View.GONE);
        }
        
        // Hold until date
        if (escrowInfo.getHoldUntil() != null && !escrowInfo.getHoldUntil().isEmpty()) {
            tvHoldUntil.setText("Bảo vệ đến: " + formatDate(escrowInfo.getHoldUntil()));
            tvHoldUntil.setVisibility(View.VISIBLE);
        } else {
            tvHoldUntil.setVisibility(View.GONE);
        }
        
        // Button states
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        if (escrowInfo == null) return;
        
        // Release button - chỉ hiển thị nếu user là buyer và payment đang holding
        btnReleasePayment.setVisibility(
                escrowInfo.canRelease() ? View.VISIBLE : View.GONE
        );
        
        // Report dispute button - hiển thị nếu đang holding và còn thời gian
        btnReportDispute.setVisibility(
                escrowInfo.canReportDispute() ? View.VISIBLE : View.GONE
        );
    }
    
    private String getStatusText(String status) {
        switch (status) {
            case "HOLDING": return "🔒 Đang bảo vệ";
            case "DISPUTED": return "⚠️ Có tranh chấp";
            case "RELEASED": return "✅ Đã chuyển tiền";
            case "REFUNDED": return "↩️ Đã hoàn tiền";
            default: return status;
        }
    }
    
    private String formatDate(String dateStr) {
        try {
            // Simplified date formatting - có thể cải thiện thêm
            return dateStr.substring(0, 10);
        } catch (Exception e) {
            return dateStr;
        }
    }
    
    private void showReleasePaymentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 Chấp nhận thanh toán")
                .setMessage("Bạn có chắc chắn muốn chấp nhận thanh toán này?\n\n" +
                           "✅ Tiền sẽ được chuyển cho người bán\n" +
                           "⚠️ Hành động này không thể hoàn tác")
                .setPositiveButton("✅ Chấp nhận", (dialog, which) -> releasePayment())
                .setNegativeButton("❌ Hủy", null)
                .show();
    }
    
    private void showReportDisputeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_dispute, null);
        android.widget.EditText etReason = dialogView.findViewById(R.id.etDisputeReason);
        
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Báo cáo tranh chấp")
                .setView(dialogView)
                .setPositiveButton("📝 Gửi báo cáo", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập lý do tranh chấp", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reportDispute(reason);
                })
                .setNegativeButton("❌ Hủy", null)
                .show();
    }
    
    private void releasePayment() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý chấp nhận thanh toán...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        EscrowRequest request = new EscrowRequest(currentUserId);
        
        Call<ApiResponse> call = apiService.releaseEscrow(paymentId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(EscrowActivity.this, 
                                "✅ Đã chấp nhận thanh toán! Tiền đã chuyển cho người bán.", 
                                Toast.LENGTH_LONG).show();
                        loadEscrowInfo(); // Refresh data
                    } else {
                        Toast.makeText(EscrowActivity.this, 
                                apiResponse.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EscrowActivity.this, 
                            "Lỗi chấp nhận thanh toán: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to release payment", t);
                Toast.makeText(EscrowActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void reportDispute(String reason) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi báo cáo tranh chấp...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        EscrowRequest request = new EscrowRequest(currentUserId, reason);
        
        Call<ApiResponse> call = apiService.reportDispute(paymentId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(EscrowActivity.this, 
                                "📝 Đã gửi báo cáo tranh chấp! Hệ thống sẽ xem xét trong 24-48 giờ.", 
                                Toast.LENGTH_LONG).show();
                        loadEscrowInfo(); // Refresh data
                    } else {
                        Toast.makeText(EscrowActivity.this, 
                                apiResponse.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EscrowActivity.this, 
                            "Lỗi gửi báo cáo: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to report dispute", t);
                Toast.makeText(EscrowActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
