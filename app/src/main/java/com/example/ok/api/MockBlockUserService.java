package com.example.ok.api;

import android.util.Log;

import com.example.ok.model.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Mock Block User Service để test khi backend chưa sẵn sàng
 * Sử dụng tạm thời cho demo purposes
 */
public class MockBlockUserService {
    private static final String TAG = "MockBlockUserService";
    
    /**
     * Mock block user success response
     */
    public static void mockBlockUserSuccess(Long currentUserId, Long targetUserId, 
                                          Callback<ApiResponse> callback) {
        Log.i(TAG, "🎭 MOCK: Blocking user " + targetUserId + " by user " + currentUserId);
        
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            ApiResponse mockResponse = new ApiResponse(true, "Đã chặn người dùng thành công (MOCK)");
            Response<ApiResponse> response = Response.success(mockResponse);
            callback.onResponse(null, response);
        }, 1000); // 1 second delay to simulate network
    }
    
    /**
     * Mock block user already blocked response
     */
    public static void mockBlockUserAlreadyBlocked(Long currentUserId, Long targetUserId, 
                                                  Callback<ApiResponse> callback) {
        Log.i(TAG, "🎭 MOCK: User " + targetUserId + " already blocked by user " + currentUserId);
        
        new android.os.Handler().postDelayed(() -> {
            ApiResponse mockResponse = new ApiResponse(false, "Bạn đã chặn người dùng này trước đó (MOCK)");
            Response<ApiResponse> response = Response.success(mockResponse);
            callback.onResponse(null, response);
        }, 800);
    }
    
    /**
     * Mock network error
     */
    public static void mockNetworkError(Callback<ApiResponse> callback) {
        Log.i(TAG, "🎭 MOCK: Network error simulation");
        
        new android.os.Handler().postDelayed(() -> {
            callback.onFailure(null, new Exception("Mock network error"));
        }, 500);
    }
    
    /**
     * Mock 403 forbidden (current backend state)
     */
    public static void mockForbiddenError(Callback<ApiResponse> callback) {
        Log.i(TAG, "🎭 MOCK: 403 Forbidden simulation");
        
        new android.os.Handler().postDelayed(() -> {
            Response<ApiResponse> response = Response.error(403, 
                okhttp3.ResponseBody.create(null, ""));
            callback.onResponse(null, response);
        }, 600);
    }
}
