// ApiService.java
package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.RegisterRequest;
import com.example.ok.model.LoginRequest;
import com.example.ok.model.GoogleAuthRequest;
import com.example.ok.model.PasswordResetRequest;
import com.example.ok.model.PasswordUpdateRequest;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import com.example.ok.model.UserProfileRequest;
import com.example.ok.model.UserProfileResponse;

public interface ApiService {
    @POST("api/auth/register")
    Call<ApiResponse> register(@Body RegisterRequest request);
    @POST("api/auth/logout")
    Call<ApiResponse> logout();

    // In ApiService.java interface
    @GET("api/users/{userId}/profile")
    Call<UserProfileResponse> getUserProfile(@Path("userId") Long userId);

    @Multipart
    @POST("api/profiles/{userId}/image")
    Call<ApiResponse> uploadProfileImage(@Path("userId") Long userId, @Part MultipartBody.Part image);

    @PUT("api/users/{userId}/profile")
    Call<ApiResponse> updateUserProfile(@Path("userId") Long userId, @Body UserProfileRequest request);

    @POST("api/users/{userId}/deactivate")
    Call<ApiResponse> deactivateAccount(@Path("userId") Long userId);

    @DELETE("api/users/{userId}")
    Call<ApiResponse> deleteAccount(@Path("userId") Long userId);




    @POST("api/auth/reset-password")
    Call<ApiResponse> resetPassword(@Body PasswordUpdateRequest request);
    @POST("api/auth/forgot-password")
    Call<ApiResponse> requestPasswordReset(@Body PasswordResetRequest request);

    @POST("api/auth/google")
    Call<ApiResponse> googleAuth(@Body GoogleAuthRequest request);
    @POST("api/auth/login")
    Call<ApiResponse> login(@Body LoginRequest request);
    @POST("api/auth/activate/{userId}")
    Call<ApiResponse> activateUser(@Path("userId") Long userId);
}