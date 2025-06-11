// ApiService.java
package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.CreateListingRequest;
import com.example.ok.model.Listing;
import com.example.ok.model.PagedApiResponse;
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
import retrofit2.http.Query;

import com.example.ok.model.UpdateListingRequest;
import com.example.ok.model.UserProfileRequest;
import com.example.ok.model.UserProfileResponse;

import java.util.List;
public interface ApiService {
    @POST("api/auth/register")
    Call<ApiResponse> register(@Body RegisterRequest request);
    @POST("api/auth/logout")
    Call<ApiResponse> logout();

    // In ApiService.java interface
    @GET("api/users/{userId}/profile")
    Call<UserProfileResponse> getUserProfile(@Path("userId") Long userId);

    @Multipart
    @POST("api/users/{userId}/profile-image")
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



    // Listing endpoints
    @POST("api/listings")
    Call<ApiResponse> createListing(
            @Query("userId") Long userId,
            @Body CreateListingRequest request
    );

    @Multipart
    @POST("api/listings/{listingId}/images")
    Call<ApiResponse> uploadImages(
            @Path("listingId") Long listingId,
            @Query("userId") Long userId,
            @Part List<MultipartBody.Part> images
    );

    @PUT("api/listings/{listingId}")
    Call<ApiResponse> updateListing(
            @Path("listingId") Long listingId,
            @Query("userId") Long userId,
            @Body UpdateListingRequest request
    );

    @DELETE("api/listings/{listingId}")
    Call<ApiResponse> deleteListing(
            @Path("listingId") Long listingId,
            @Query("userId") Long userId
    );

    @GET("api/listings/user/{userId}")
    Call<PagedApiResponse<Listing>> getUserListings(
            @Path("userId") Long userId,
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/listings")
    Call<PagedApiResponse<Listing>> getAvailableListings(
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/listings/category/{categoryId}")
    Call<PagedApiResponse<Listing>> getListingsByCategory(
            @Path("categoryId") Long categoryId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/listings/search")
    Call<PagedApiResponse<Listing>> searchListings(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/listings/{listingId}")
    Call<ApiResponse> getListingDetail(
            @Path("listingId") Long listingId
    );

    @POST("api/listings/{listingId}/interact")
    Call<ApiResponse> incrementInteraction(
            @Path("listingId") Long listingId
    );    @GET("api/listings/categories")
    Call<ApiResponse> getAllCategories();

    @GET("api/listings/conditions")
    Call<ApiResponse> getAllConditions();
    
    // User blocking and reporting
    @POST("api/users/{userId}/block/{targetUserId}")
    Call<ApiResponse> blockUser(@Path("userId") Long userId, @Path("targetUserId") Long targetUserId);
    
    @POST("api/users/{userId}/unblock/{targetUserId}")
    Call<ApiResponse> unblockUser(@Path("userId") Long userId, @Path("targetUserId") Long targetUserId);
    
    @POST("api/users/{userId}/report/{targetUserId}")
    Call<ApiResponse> reportUser(@Path("userId") Long userId, @Path("targetUserId") Long targetUserId, @Query("reason") String reason);
      @GET("api/users/{userId}/blocked")
    Call<ApiResponse> getBlockedUsers(@Path("userId") Long userId);
    
    // FCM Token management
    @POST("api/users/{userId}/fcm-token")
    Call<ApiResponse> updateFcmToken(@Path("userId") Long userId, @Query("token") String fcmToken);
    
    @DELETE("api/users/{userId}/fcm-token")
    Call<ApiResponse> removeFcmToken(@Path("userId") Long userId);
}