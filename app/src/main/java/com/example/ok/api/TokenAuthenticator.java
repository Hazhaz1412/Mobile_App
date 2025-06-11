package com.example.ok.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.example.ok.Login;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.JwtAuthResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class TokenAuthenticator implements Authenticator {
    private static final String TAG = "TokenAuthenticator";
    private Context context;    public TokenAuthenticator(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null in TokenAuthenticator");
        }
        this.context = context.getApplicationContext();
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // Nếu token đã hết hạn (401), thử refresh token
        if (response.code() == 401) {
            Log.d(TAG, "Token expired, trying to refresh");
            
            SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String refreshToken = prefs.getString("refresh_token", "");
            
            if (TextUtils.isEmpty(refreshToken)) {
                Log.d(TAG, "No refresh token available");
                navigateToLogin();
                return null; // Không có refresh token, cần đăng nhập lại
            }
              // Gọi API refresh token
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("refreshToken", refreshToken);
            
            // Ensure RetrofitClient is initialized
            if (RetrofitClient.getAuthApiService() == null) {
                Log.e(TAG, "AuthApiService is null, cannot refresh token");
                navigateToLogin();
                return null;
            }
            
            Call<ApiResponse> call = RetrofitClient.getAuthApiService().refreshToken(requestBody);
            
            try {
                retrofit2.Response<ApiResponse> refreshResponse = call.execute();
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().isSuccess()) {
                    JwtAuthResponse authResponse = refreshResponse.body().getDataAs(JwtAuthResponse.class);
                    if (authResponse != null) {
                        String newToken = authResponse.getToken();
                        String newRefreshToken = authResponse.getRefreshToken();
                        
                        // Lưu token mới
                        prefs.edit()
                            .putString("auth_token", newToken)
                            .putString("refresh_token", newRefreshToken)
                            .apply();
                        
                        Log.d(TAG, "Token refreshed successfully");
                        
                        // Thêm token mới vào request và thử lại
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + newToken)
                                .build();
                    }
                } else {
                    Log.d(TAG, "Failed to refresh token: " + 
                        (refreshResponse.body() != null ? refreshResponse.body().getMessage() : "No response body"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error refreshing token", e);
            }
            
            // Nếu refresh token thất bại, cần đăng nhập lại
            navigateToLogin();
        }
        
        return null;
    }
    
    private void navigateToLogin() {
        Log.d(TAG, "Navigating to login screen");
        Intent intent = new Intent(context, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
