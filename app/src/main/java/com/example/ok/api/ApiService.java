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
import com.example.ok.model.CancelPaymentRequest;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.example.ok.model.UpdateListingRequest;
import com.example.ok.model.UserProfileRequest;
import com.example.ok.model.UserProfileResponse;
import com.example.ok.model.Report;
import com.example.ok.model.BlockUserRequest;
import com.example.ok.model.CreateOfferRequest;
import com.example.ok.model.RespondToOfferRequest;
import com.example.ok.model.Offer;
import com.example.ok.model.OfferResponse;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.model.Transaction;
import com.example.ok.model.WithdrawOfferRequest;

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
    );    @GET("api/listings/{listingId}")
    Call<ApiResponse> getListingDetail(
            @Path("listingId") Long listingId
    );
    
    @GET("api/listings/{listingId}")
    Call<Listing> getListingById(
            @Path("listingId") Long listingId
    );

    @POST("api/listings/{listingId}/interact")
    Call<ApiResponse> incrementInteraction(
            @Path("listingId") Long listingId
    );    @GET("api/listings/categories")
    Call<ApiResponse> getAllCategories();

    @GET("api/listings/conditions")
    Call<ApiResponse> getAllConditions();      // User blocking and reporting  
    @POST("api/users/block")
    Call<ApiResponse> blockUser(@Query("userId") Long userId, @Query("targetUserId") Long targetUserId, @Body BlockUserRequest request);
      @POST("api/users/unblock")
    Call<ApiResponse> unblockUser(@Query("userId") Long userId, @Query("targetUserId") Long targetUserId);
    
    @GET("api/users/block-status")
    Call<ApiResponse> checkBlockStatus(@Query("userId") Long userId, @Query("targetUserId") Long targetUserId);
      // Alternative blocking endpoints (in case backend uses different format)
    @POST("api/users/{targetUserId}/block")
    Call<ApiResponse> blockUserPath(@Path("targetUserId") Long targetUserId, @Body BlockUserRequest request);
    
    @POST("api/users/{userId}/unblock/{targetUserId}")
    Call<ApiResponse> unblockUserPath(@Path("userId") Long userId, @Path("targetUserId") Long targetUserId);
    
    // Correct unblock endpoint matching backend (DELETE method)
    @DELETE("api/users/{targetUserId}/block")
    Call<ApiResponse> unblockUser(@Path("targetUserId") Long targetUserId);
      @POST("api/users/{userId}/report/{targetUserId}")
    Call<ApiResponse> reportUser(@Path("userId") Long userId, @Path("targetUserId") Long targetUserId, @Query("reason") String reason);
    
    @GET("api/users/{userId}/blocked")
    Call<ApiResponse> getBlockedUsers(@Path("userId") Long userId);
    
    // === ENHANCED REPORTING ENDPOINTS ===
    
    /**
     * Report a listing for policy violations
     */
    @POST("api/listings/{listingId}/report")
    Call<ApiResponse> reportListing(
        @Path("listingId") Long listingId,
        @Query("reporterId") Long reporterId,
        @Query("reason") String reason,
        @Query("description") String description
    );
    
    /**
     * Report a chat for inappropriate content
     */
    @POST("api/chat/rooms/{chatRoomId}/report")
    Call<ApiResponse> reportChatRoom(
        @Path("chatRoomId") Long chatRoomId,
        @Query("reporterId") Long reporterId,
        @Query("reason") String reason,
        @Query("description") String description
    );
    
    /**
     * Get reports submitted by a user
     */
    @GET("api/reports/user/{userId}/submitted")
    Call<ApiResponse> getMyReports(@Path("userId") Long userId);
    
    /**
     * Enhanced user reporting with description
     */
    @POST("api/users/{userId}/report/{targetUserId}/detailed")
    Call<ApiResponse> reportUserDetailed(
        @Path("userId") Long userId,
        @Path("targetUserId") Long targetUserId,
        @Query("reason") String reason,
        @Query("description") String description
    );
    
    // FCM Token management
    @POST("api/users/{userId}/fcm-token")
    Call<ApiResponse> updateFcmToken(@Path("userId") Long userId, @Query("token") String fcmToken);
    
    @DELETE("api/users/{userId}/fcm-token")
    Call<ApiResponse> removeFcmToken(@Path("userId") Long userId);
      // ========== PAYMENT SYSTEM ENDPOINTS ==========
    
    // Payment processing endpoints
    @POST("api/v1/payments")
    Call<ApiResponse> createPayment(@Body com.example.ok.model.PaymentRequest request);
      @POST("api/v1/payments")
    Call<com.example.ok.model.MoMoPaymentResponse> processMoMoPayment(@Body com.example.ok.model.PaymentRequest request);
    
    @POST("api/v1/payments")
    Call<ApiResponse> processCardPayment(@Body com.example.ok.model.PaymentRequest request);
    
    @GET("api/v1/payments/{paymentId}/status")
    Call<ApiResponse> getPaymentStatus(@Path("paymentId") Long paymentId);
    
    @POST("api/v1/payments/{paymentId}/confirm")
    Call<ApiResponse> confirmPayment(@Path("paymentId") Long paymentId);
      @POST("api/v1/payments/{paymentId}/cancel")
    Call<java.util.Map<String, Object>> cancelPayment(@Path("paymentId") Long paymentId, @Query("userId") Long userId);
    
    // Manual update payment status for testing
    @PUT("api/v1/payments/{paymentId}/status")
    Call<Object> updatePaymentStatus(@Path("paymentId") Long paymentId, @Query("status") String status, @Query("transactionId") String transactionId);

    // Get payment history
    @GET("api/v1/payments/user/{userId}")
    Call<ApiResponse> getUserPayments(@Path("userId") Long userId);
    
    @GET("api/v1/payments/user/{userId}/history")
    Call<List<com.example.ok.model.Payment>> getPaymentHistory(
        @Path("userId") Long userId,
        @Query("page") int page,
        @Query("size") int size
    );
    
    // Check pending payment for a specific listing
    @GET("api/v1/payments/user/{userId}/listing/{listingId}/pending")
    Call<com.example.ok.model.Payment> getPendingPaymentForListing(
        @Path("userId") Long userId,
        @Path("listingId") Long listingId
    );
    
    // Payment methods management
    @GET("api/v1/payment-methods/user/{userId}")
    Call<List<com.example.ok.model.PaymentMethod>> getUserPaymentMethods(@Path("userId") Long userId);
    
    @POST("api/v1/payment-methods/user/{userId}")
    Call<ApiResponse> addPaymentMethod(
        @Path("userId") Long userId,
        @Body com.example.ok.model.PaymentMethod paymentMethod
    );
    
    @PUT("api/v1/payment-methods/{methodId}")
    Call<ApiResponse> updatePaymentMethod(
        @Path("methodId") Long methodId,
        @Body com.example.ok.model.PaymentMethod paymentMethod
    );
    
    @DELETE("api/v1/payment-methods/{methodId}")
    Call<ApiResponse> deletePaymentMethod(@Path("methodId") Long methodId);
    
    @POST("api/v1/payment-methods/{methodId}/set-default")
    Call<ApiResponse> setDefaultPaymentMethod(@Path("methodId") Long methodId);
    
    // Escrow system endpoints
    @POST("api/v1/payments/{paymentId}/escrow/hold")
    Call<ApiResponse> holdEscrowPayment(@Path("paymentId") Long paymentId);
    
    @POST("api/v1/payments/{paymentId}/escrow/release")
    Call<ApiResponse> releaseEscrowPayment(@Path("paymentId") Long paymentId);
      @POST("api/v1/payments/{paymentId}/escrow/refund")
    Call<ApiResponse> refundEscrowPayment(@Path("paymentId") Long paymentId);    // ========== STRIPE PAYMENT ENDPOINTS ==========
    
    @POST("api/v1/payments/stripe/create")
    Call<Map<String, Object>> createStripePayment(@Body com.example.ok.model.PaymentRequest request);
    
    @GET("api/v1/payments/success/stripe")
    Call<Map<String, Object>> handleStripeSuccess(
        @Query("session_id") String sessionId,
        @Query("payment_id") Long paymentId
    );
    
    @GET("api/v1/payments/cancel/stripe")
    Call<Map<String, Object>> handleStripeCancel(@Query("payment_id") Long paymentId);
    
    // ========== VISA PAYMENT ENDPOINTS ==========
    
    @POST("api/v1/payments/visa/create")
    Call<Map<String, Object>> createVisaPayment(@Body com.example.ok.model.PaymentRequest request);
    
    @POST("api/v1/payments/callback/visa")
    Call<Map<String, Object>> handleVisaCallback(@Body Map<String, Object> callbackData);
    
    @GET("api/v1/payments/success/visa")
    Call<Map<String, Object>> handleVisaSuccess(
        @Query("transaction_id") String transactionId,
        @Query("payment_id") Long paymentId
    );
    
    @GET("api/v1/payments/cancel/visa")
    Call<Map<String, Object>> handleVisaCancel(@Query("payment_id") Long paymentId);
    
    // ========== ESCROW ENDPOINTS ==========
    
    @POST("api/v1/escrow/{paymentId}/release")
    Call<ApiResponse> releaseEscrow(@Path("paymentId") Long paymentId, @Body com.example.ok.model.EscrowRequest request);
    
    @POST("api/v1/escrow/{paymentId}/dispute")
    Call<ApiResponse> reportDispute(@Path("paymentId") Long paymentId, @Body com.example.ok.model.EscrowRequest request);
    
    @GET("api/v1/escrow/{paymentId}/info")
    Call<com.example.ok.model.EscrowInfo> getEscrowInfo(@Path("paymentId") Long paymentId);

    // MoMo mock update payment status (new endpoint)
    @POST("momo/mock-update")
    Call<ApiResponse> updateMoMoPaymentStatus(
            @Query("orderId") String orderId,
            @Query("status") String status);

    // ========== OFFER ENDPOINTS ==========
    
    // Create an offer on a listing
    @POST("api/offers")
    Call<ApiResponse> createOffer(
            @Query("buyerId") Long buyerId,
            @Body CreateOfferRequest request
    );
    
    // Respond to an offer (accept/reject/counter)
    @POST("api/offers/{offerId}/respond")
    Call<ApiResponse> respondToOffer(
            @Path("offerId") Long offerId,
            @Query("sellerId") Long sellerId,
            @Body RespondToOfferRequest request
    );
    
    // Withdraw an offer
    @DELETE("api/offers/{offerId}")
    Call<ApiResponse> withdrawOffer(
            @Path("offerId") Long offerId,
            @Query("buyerId") Long buyerId
    );
    
    // Get offers for a specific listing
    @GET("api/offers/listing/{listingId}")
    Call<ApiResponse> getOffersForListing(
            @Path("listingId") Long listingId,
            @Query("sellerId") Long sellerId
    );
      // Get offers made by a user (as buyer)
    @GET("api/offers/buyer/{buyerId}")
    Call<PagedApiResponse<OfferResponse>> getOffersByBuyer(@Path("buyerId") Long buyerId);    // Get offers received by a user (as seller)
    @GET("api/offers/seller/{sellerId}")
    Call<PagedApiResponse<OfferResponse>> getOffersBySeller(@Path("sellerId") Long sellerId);

    // Check offer status and purchasability
    @GET("api/offers/{offerId}/status")
    Call<ApiResponse> checkOfferStatus(@Path("offerId") Long offerId);
    
    // Get offer details with updated status
    @GET("api/offers/{offerId}/details")
    Call<ApiResponse> getOfferDetails(@Path("offerId") Long offerId);
    
    // Check if listing is available for purchase
    @GET("api/offers/listing/{listingId}/available")
    Call<ApiResponse> checkListingAvailability(@Path("listingId") Long listingId);
    
    // ========== TRANSACTION ENDPOINTS ==========
    
    // Create a direct purchase transaction
    @POST("api/transactions/purchase")
    Call<ApiResponse> createDirectPurchase(
            @Query("buyerId") Long buyerId,
            @Query("listingId") Long listingId
    );
    
    // Create transaction from accepted offer
    @POST("api/transactions/from-offer/{offerId}")
    Call<ApiResponse> createTransactionFromOffer(
            @Path("offerId") Long offerId,
            @Query("buyerId") Long buyerId
    );
    
    // Mark transaction as completed (item sold)
    @PUT("api/transactions/{transactionId}/complete")
    Call<ApiResponse> completeTransaction(
            @Path("transactionId") Long transactionId,
            @Query("sellerId") Long sellerId
    );
    
    // Cancel a transaction
    @PUT("api/transactions/{transactionId}/cancel")
    Call<ApiResponse> cancelTransaction(
            @Path("transactionId") Long transactionId,
            @Query("userId") Long userId
    );
      // Get transaction history for a user
    @GET("api/transactions/user/{userId}")
    Call<ApiResponse> getUserTransactions(@Path("userId") Long userId);
    
    // Get transaction by offer ID
    @GET("api/transactions/offer/{offerId}")
    Call<ApiResponse> getTransactionByOfferId(@Path("offerId") Long offerId);
    
    // Get transaction statistics for a user
    @GET("api/transactions/user/{userId}/stats")
    Call<ApiResponse> getUserTransactionStats(@Path("userId") Long userId);
    
    // Find transaction by listing and buyer
    @GET("api/transactions/find-by-listing")
    Call<ApiResponse> findTransactionByListing(@Query("listingId") Long listingId, @Query("buyerId") Long buyerId);    // Rating endpoints
    @POST("api/ratings")
    Call<ApiResponse> createRating(@Header("User-ID") Long userId, @Body com.example.ok.model.CreateRatingRequest request);
    
    @GET("api/ratings/user/{userId}/received")
    Call<ApiResponse> getUserReceivedRatings(@Path("userId") Long userId, @Query("page") int page, @Query("size") int size);
    
    @GET("api/ratings/user/{userId}/stats")  
    Call<ApiResponse> getUserRatingStats(@Path("userId") Long userId);
    
    @GET("api/ratings/can-rate/{transactionId}")
    Call<ApiResponse> canUserRateTransaction(@Path("transactionId") Long transactionId, @Query("userId") Long userId);
    
    @GET("api/transactions/completed/for-rating")
    Call<ApiResponse> getCompletedTransactionsForRating(@Query("userId") Long userId, @Query("page") int page, @Query("size") int size);
}