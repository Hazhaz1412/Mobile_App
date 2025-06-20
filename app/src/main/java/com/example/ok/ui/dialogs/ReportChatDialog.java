package com.example.ok.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.ReportReason;
import com.example.ok.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dialog for reporting chat rooms/conversations
 */
public class ReportChatDialog {
    
    private Context context;
    private Long chatRoomId;
    private String chatPartnerName;
    private Long reporterId;
    private ApiService apiService;
    private OnReportSubmittedListener listener;
    
    public interface OnReportSubmittedListener {
        void onReportSubmitted(boolean success, String message);
    }
    
    public ReportChatDialog(Context context, Long chatRoomId, String chatPartnerName, Long reporterId) {
        this.context = context;
        this.chatRoomId = chatRoomId;
        this.chatPartnerName = chatPartnerName;
        this.reporterId = reporterId;
        this.apiService = RetrofitClient.getApiService();
    }
    
    public void setOnReportSubmittedListener(OnReportSubmittedListener listener) {
        this.listener = listener;
    }
    
    public void show() {
        // Create custom dialog layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_chat, null);
        
        RadioGroup reasonGroup = dialogView.findViewById(R.id.rg_report_reasons);
        EditText etDescription = dialogView.findViewById(R.id.et_report_description);
        
        // Add report reasons as radio buttons
        ReportReason[] reasons = ReportReason.getChatReportReasons();
        for (ReportReason reason : reasons) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(reason.getDisplayName());
            radioButton.setTag(reason);
            reasonGroup.addView(radioButton);
        }
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Báo cáo cuộc trò chuyện")
                .setMessage("Người dùng: " + (chatPartnerName != null ? chatPartnerName : "Không xác định"))
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", null) // Will be overridden
                .setNegativeButton("Hủy", null)
                .create();
        
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Get selected reason
                int selectedId = reasonGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(context, "Vui lòng chọn lý do báo cáo", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                RadioButton selectedRadio = dialogView.findViewById(selectedId);
                ReportReason selectedReason = (ReportReason) selectedRadio.getTag();
                String description = etDescription.getText().toString().trim();
                
                // Validate description for "Other" reason
                if (selectedReason == ReportReason.OTHER && description.isEmpty()) {
                    Toast.makeText(context, "Vui lòng mô tả chi tiết lý do báo cáo", Toast.LENGTH_SHORT).show();
                    etDescription.requestFocus();
                    return;
                }
                
                // Submit report
                submitReport(selectedReason.getDisplayName(), description);
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    private void submitReport(String reason, String description) {
        Call<ApiResponse> call = apiService.reportChatRoom(chatRoomId, reporterId, reason, description);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(context, "Đã gửi báo cáo thành công", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onReportSubmitted(true, "Báo cáo đã được gửi");
                        }
                    } else {
                        String errorMsg = apiResponse.getMessage() != null ? 
                            apiResponse.getMessage() : "Không thể gửi báo cáo";
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onReportSubmitted(false, errorMsg);
                        }
                    }
                } else {
                    String errorMsg = "Lỗi kết nối: " + response.code();
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onReportSubmitted(false, errorMsg);
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onReportSubmitted(false, errorMsg);
                }
            }
        });
    }
}
