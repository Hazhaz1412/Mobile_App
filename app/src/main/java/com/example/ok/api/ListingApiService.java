package com.example.ok.api;

import com.example.ok.model.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ListingApiService {
    
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

    // Test endpoint - GIỮ NGUYÊN
    @Multipart
    @POST("api/listings/test-upload")
    Call<ApiResponse> testUploadImage(
            @Part MultipartBody.Part image  // KHÔNG có tên parameter
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

    @GET("api/listings/{listingId}")
    Call<ApiResponse> getListingDetail(
            @Path("listingId") Long listingId
    );

    @GET("api/categories")
    Call<ApiResponse> getAllCategories();

    @GET("api/conditions")
    Call<ApiResponse> getAllConditions();

    @GET("api/listings/{id}")
    Call<ApiResponse> getListingById(@Path("id") Long id);

    @GET("api/listings/search")
    Call<PagedApiResponse<Listing>> searchListings(
            @Query("keyword") String keyword,
            @Query("categoryId") Long categoryId,
            @Query("conditionId") Long conditionId,
            @Query("minPrice") Double minPrice,
            @Query("maxPrice") Double maxPrice,
            @Query("location") String location,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/listings/category/{categoryId}")
    Call<PagedApiResponse<Listing>> getListingsByCategory(
            @Path("categoryId") Long categoryId,
            @Query("page") int page,
            @Query("size") int size
    );
}