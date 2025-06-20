package com.example.ok.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.BlockUserRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper class for debugging API endpoints and connectivity
 */
public class ApiDebugHelper {
    private static final String TAG = "ApiDebugHelper";
    
    /**
     * Test block user endpoint with detailed logging
     */
    public static void testBlockUserEndpoint(Context context, Long currentUserId, Long targetUserId) {
        if (context == null || currentUserId == null || targetUserId == null) {
            Log.e(TAG, "Invalid parameters for testing block user endpoint");
            return;
        }
        
        ApiService apiService = RetrofitClient.getApiService();
        
        Log.i(TAG, "=== TESTING BLOCK USER ENDPOINTS ===");
        Log.i(TAG, "Current User ID: " + currentUserId);
        Log.i(TAG, "Target User ID: " + targetUserId);
        
        // Test Path-based endpoint first
        testPathBasedBlock(context, apiService, currentUserId, targetUserId);
    }
    
    private static void testPathBasedBlock(Context context, ApiService apiService, Long currentUserId, Long targetUserId) {
        Log.i(TAG, "Testing Path-based endpoint: POST /api/users/{userId}/block/{targetUserId}");
        
        Call<ApiResponse> call = apiService.blockUserPath(targetUserId, new BlockUserRequest("Debug test block"));
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                String url = call.request().url().toString();
                String method = call.request().method();
                
                Log.i(TAG, "Path-based request: " + method + " " + url);
                Log.i(TAG, "Response code: " + response.code());
                Log.i(TAG, "Response message: " + response.message());
                
                if (response.body() != null) {
                    Log.i(TAG, "Response body success: " + response.body().isSuccess());
                    Log.i(TAG, "Response body message: " + response.body().getMessage());
                }
                
                if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.w(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
                
                // Test Query-based endpoint as fallback
                testQueryBasedBlock(context, apiService, currentUserId, targetUserId);
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                String url = call.request().url().toString();
                String method = call.request().method();
                
                Log.e(TAG, "Path-based request failed: " + method + " " + url, t);
                
                // Test Query-based endpoint as fallback
                testQueryBasedBlock(context, apiService, currentUserId, targetUserId);
            }
        });
    }
    
    private static void testQueryBasedBlock(Context context, ApiService apiService, Long currentUserId, Long targetUserId) {
        Log.i(TAG, "Testing Query-based endpoint: POST /api/users/block?userId=&targetUserId=");
        
        Call<ApiResponse> call = apiService.blockUser(currentUserId, targetUserId, new BlockUserRequest("Debug test block"));
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                String url = call.request().url().toString();
                String method = call.request().method();
                
                Log.i(TAG, "Query-based request: " + method + " " + url);
                Log.i(TAG, "Response code: " + response.code());
                Log.i(TAG, "Response message: " + response.message());
                
                if (response.body() != null) {
                    Log.i(TAG, "Response body success: " + response.body().isSuccess());
                    Log.i(TAG, "Response body message: " + response.body().getMessage());
                }
                
                if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.w(TAG, "Error body: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
                
                Log.i(TAG, "=== BLOCK USER ENDPOINT TESTING COMPLETE ===");
                
                // Show summary to user
                showTestResult(context, response.code());
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                String url = call.request().url().toString();
                String method = call.request().method();
                
                Log.e(TAG, "Query-based request failed: " + method + " " + url, t);
                Log.i(TAG, "=== BLOCK USER ENDPOINT TESTING COMPLETE ===");
                
                // Show summary to user
                showTestResult(context, -1);
            }
        });
    }
    
    private static void showTestResult(Context context, int responseCode) {
        String message;
        switch (responseCode) {
            case 200:
            case 201:
                message = "✅ Endpoint hoạt động bình thường";
                break;
            case 403:
                message = "⚠️ Lỗi 403: Backend chưa hỗ trợ tính năng chặn người dùng hoặc thiếu quyền";
                break;
            case 404:
                message = "❌ Lỗi 404: Endpoint không tồn tại";
                break;
            case 500:
                message = "⚠️ Lỗi 500: Lỗi máy chủ";
                break;
            case -1:
                message = "❌ Lỗi kết nối mạng";
                break;
            default:
                message = "⚠️ Lỗi không xác định (Code: " + responseCode + ")";
        }
        
        Toast.makeText(context, "Test API: " + message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Get user-friendly error message for HTTP response codes
     */
    public static String getErrorMessage(int responseCode, String feature) {
        switch (responseCode) {
            case 403:
                return "❌ Không có quyền thực hiện " + feature + ".\n\n" +
                       "Có thể do:\n" +
                       "• Backend chưa implement tính năng này\n" +
                       "• Tài khoản thiếu quyền hạn\n" +
                       "• API endpoint cần cập nhật\n\n" +
                       "Vui lòng liên hệ support để được hỗ trợ.";
            case 404:
                return "❌ " + feature + " không khả dụng.\n\nEndpoint không tồn tại trên server.";
            case 400:
                return "⚠️ Yêu cầu " + feature + " không hợp lệ.\n\nVui lòng thử lại.";
            case 409:
                return "⚠️ " + feature + " đã được thực hiện trước đó.";
            case 500:
                return "⚠️ Lỗi máy chủ khi thực hiện " + feature + ".\n\nVui lòng thử lại sau.";
            default:
                return "❌ Không thể thực hiện " + feature + ".\n\nMã lỗi: " + responseCode;
        }
    }
}
