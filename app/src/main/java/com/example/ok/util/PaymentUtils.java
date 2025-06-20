package com.example.ok.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.ok.api.ApiService;
import com.example.ok.model.Payment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Utility class for payment-related operations
 */
public class PaymentUtils {
    private static final String TAG = "PaymentUtils";

    /**
     * Interface to handle pending payment check results
     */    public interface PendingPaymentCallback {
        void onNoPendingPayment();
        void onPendingPaymentFound(Payment payment);
        void onError(String message);
    }

    /**
     * Check if there's a pending payment for a listing
     *
     * @param context     Android context
     * @param apiService  API service instance
     * @param listingId   ID of the listing to check
     * @param callback    Callback to handle results
     */
    public static void checkPendingPayment(Context context, ApiService apiService, Long listingId, PendingPaymentCallback callback) {
        // Get user ID from preferences
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
        
        if (userId <= 0) {
            callback.onError("Không tìm thấy thông tin người dùng");
            return;
        }        // Call API to check for pending payments
        Call<Payment> call = apiService.getPendingPaymentForListing(userId, listingId);
        call.enqueue(new Callback<Payment>() {
            @Override
            public void onResponse(@NonNull Call<Payment> call, @NonNull Response<Payment> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                    // Found existing pending payment
                    Payment existingPayment = response.body();
                    Log.d(TAG, "Found pending payment: " + existingPayment.getId() + 
                          " with status: " + existingPayment.getStatus());
                    
                    callback.onPendingPaymentFound(existingPayment);
                } else {
                    // No pending payment
                    callback.onNoPendingPayment();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Payment> call, @NonNull Throwable t) {
                Log.e(TAG, "Error checking pending payment", t);
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    /**
     * Show dialog for existing pending payment
     * 
     * @param context   Android context
     * @param payment   The pending payment
     * @param onContinue Callback when user chooses to continue with existing payment
     * @param onCancel   Callback when user chooses to cancel existing payment
     */    public static void showExistingPaymentDialog(
            Context context, 
            Payment payment, 
            Runnable onContinue,
            Runnable onCancel) {
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Giao dịch đang chờ xử lý");
        
        String message = String.format(
            "Đã tồn tại giao dịch cho sản phẩm này:\n\n" +
            "• Mã giao dịch: %s\n" +
            "• Số tiền: %s VNĐ\n" +
            "• Phương thức: %s\n" +
            "• Trạng thái: %s\n\n" +
            "Bạn muốn làm gì với giao dịch này?",
            payment.getTransactionId(),
            payment.getAmount(),
            getPaymentMethodText(payment.getPaymentMethodType()),
            getStatusText(payment.getStatus())
        );
        
        builder.setMessage(message);
        
        // Continue with existing payment
        builder.setPositiveButton("Tiếp tục thanh toán", (dialog, which) -> {
            if (onContinue != null) {
                onContinue.run();
            }
        });
        
        // Cancel existing payment
        builder.setNegativeButton("Hủy và tạo mới", (dialog, which) -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });
        
        // Just close
        builder.setNeutralButton("Đóng", null);
        
        builder.show();
    }
    
    private static String getStatusText(String status) {
        if (status == null) return "Unknown";
        
        switch (status) {
            case "PENDING": return "Chờ thanh toán";
            case "PROCESSING": return "Đang xử lý";
            case "COMPLETED": return "Hoàn thành";
            case "FAILED": return "Thất bại";
            case "CANCELLED": return "Đã hủy";
            case "EXPIRED": return "Hết hạn";
            case "REFUNDED": return "Hoàn tiền";
            default: return status;
        }
    }
    
    private static String getPaymentMethodText(String method) {
        if (method == null) return "Unknown";
        
        switch (method) {
            case "MOMO": return "Ví MoMo";
            case "VISA": return "Thẻ Visa";
            case "MASTERCARD": return "Thẻ Mastercard";
            case "CASH": return "Tiền mặt (COD)";
            case "STRIPE": return "Stripe";
            default: return method;
        }
    }
}
