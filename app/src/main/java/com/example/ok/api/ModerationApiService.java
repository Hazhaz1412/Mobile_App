package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.Report;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API service interface for moderation and reporting functionality
 */
public interface ModerationApiService {
    
    // === REPORTING ENDPOINTS ===
    
    /**
     * Report a user for inappropriate behavior
     */
    @POST("api/moderation/reports/user")
    Call<ApiResponse> reportUser(@Body Map<String, Object> reportData);
    
    /**
     * Report a listing for policy violations
     */
    @POST("api/moderation/reports/listing")
    Call<ApiResponse> reportListing(@Body Map<String, Object> reportData);
    
    /**
     * Report a chat room for inappropriate content
     */
    @POST("api/moderation/reports/chat")
    Call<ApiResponse> reportChat(@Body Map<String, Object> reportData);
    
    /**
     * Get all reports submitted by a user
     */
    @GET("api/moderation/reports/user/{userId}/submitted")
    Call<ApiResponse> getReportsSubmittedByUser(@Path("userId") Long userId);
    
    /**
     * Get all reports against a user
     */
    @GET("api/moderation/reports/user/{userId}/received")
    Call<ApiResponse> getReportsAgainstUser(@Path("userId") Long userId);
    
    // === ADMIN MODERATION ENDPOINTS ===
    
    /**
     * Get all pending reports for admin review
     */
    @GET("api/moderation/reports/pending")
    Call<ApiResponse> getPendingReports(
        @Query("page") int page,
        @Query("size") int size,
        @Query("type") String type // USER, LISTING, CHAT, or null for all
    );
    
    /**
     * Get all reports (for admin dashboard)
     */
    @GET("api/moderation/reports")
    Call<ApiResponse> getAllReports(
        @Query("page") int page,
        @Query("size") int size,
        @Query("status") String status, // PENDING, REVIEWED, RESOLVED, DISMISSED
        @Query("type") String type
    );
    
    /**
     * Review a report (mark as reviewed)
     */
    @PUT("api/moderation/reports/{reportId}/review")
    Call<ApiResponse> reviewReport(
        @Path("reportId") Long reportId,
        @Body Map<String, Object> reviewData
    );
    
    /**
     * Resolve a report with action taken
     */
    @PUT("api/moderation/reports/{reportId}/resolve")
    Call<ApiResponse> resolveReport(
        @Path("reportId") Long reportId,
        @Body Map<String, Object> resolutionData
    );
    
    /**
     * Dismiss a report (no action needed)
     */
    @PUT("api/moderation/reports/{reportId}/dismiss")
    Call<ApiResponse> dismissReport(
        @Path("reportId") Long reportId,
        @Body Map<String, Object> dismissalData
    );
    
    // === BLOCKING ENDPOINTS (Enhanced) ===
    
    /**
     * Block a user
     */
    @POST("api/moderation/block/user")
    Call<ApiResponse> blockUser(@Body Map<String, Object> blockData);
    
    /**
     * Unblock a user
     */
    @POST("api/moderation/unblock/user")
    Call<ApiResponse> unblockUser(@Body Map<String, Object> unblockData);
    
    /**
     * Get list of blocked users
     */
    @GET("api/moderation/blocked-users/{userId}")
    Call<ApiResponse> getBlockedUsers(@Path("userId") Long userId);
    
    /**
     * Check if user is blocked
     */
    @GET("api/moderation/block-status/{userId}/target/{targetUserId}")
    Call<ApiResponse> getBlockStatus(
        @Path("userId") Long userId, 
        @Path("targetUserId") Long targetUserId
    );
    
    // === ADMIN ACTIONS ===
    
    /**
     * Temporarily ban a user
     */
    @POST("api/moderation/actions/temporary-ban")
    Call<ApiResponse> temporaryBanUser(@Body Map<String, Object> banData);
    
    /**
     * Permanently ban a user
     */
    @POST("api/moderation/actions/permanent-ban")
    Call<ApiResponse> permanentBanUser(@Body Map<String, Object> banData);
    
    /**
     * Issue a warning to a user
     */
    @POST("api/moderation/actions/warning")
    Call<ApiResponse> issueWarning(@Body Map<String, Object> warningData);
    
    /**
     * Remove content (listing, chat message, etc.)
     */
    @POST("api/moderation/actions/remove-content")
    Call<ApiResponse> removeContent(@Body Map<String, Object> contentData);
    
    /**
     * Get moderation statistics
     */
    @GET("api/moderation/stats")
    Call<ApiResponse> getModerationStats();
    
    /**
     * Get user moderation history
     */
    @GET("api/moderation/history/user/{userId}")
    Call<ApiResponse> getUserModerationHistory(@Path("userId") Long userId);
}
