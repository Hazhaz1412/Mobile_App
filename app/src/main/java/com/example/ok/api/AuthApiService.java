package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.LoginRequest;
import com.example.ok.model.RegisterRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    
    /**
     * Đăng nhập
     */
    @POST("api/auth/login")
    Call<ApiResponse> login(@Body LoginRequest loginRequest);
    
    /**
     * Đăng ký
     */
    @POST("api/auth/register")
    Call<ApiResponse> register(@Body RegisterRequest registerRequest);
    
    /**
     * Refresh token
     */
    @POST("api/auth/refresh")
    Call<ApiResponse> refreshToken(@Body Map<String, String> refreshToken);
    
    /**
     * Đăng xuất
     */
    @POST("api/auth/logout")
    Call<ApiResponse> logout(@Body Map<String, String> refreshToken);
}
