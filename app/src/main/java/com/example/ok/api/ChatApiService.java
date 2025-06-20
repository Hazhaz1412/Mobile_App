package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.ChatMessage;
import com.example.ok.model.ChatRoom;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatApiService {    /**
     * Create a chat room between two users
     */
    @POST("api/chat/rooms")
    Call<ApiResponse> createChatRoom(@Body Map<String, Long> request);
    
    /**
     * Create a chat room between two users - direct ChatRoom response
     */
    @POST("api/chat/rooms")
    Call<ChatRoom> createChatRoomDirect(@Body Map<String, Long> request);
      /**
     * Get all chat rooms for a user
     */
    @GET("api/chat/rooms/user/{userId}")
    Call<ApiResponse> getUserChatRooms(@Path("userId") Long userId);
    
    /**
     * Get all chat rooms for a user - direct List<ChatRoom> response
     */
    @GET("api/chat/rooms/user/{userId}")
    Call<List<ChatRoom>> getUserChatRoomsDirect(@Path("userId") Long userId);
    
    /**
     * Get a specific chat room
     */
    @GET("api/chat/rooms/{roomId}/user/{userId}")
    Call<ApiResponse> getChatRoomById(@Path("roomId") Long roomId, @Path("userId") Long userId);
      /**
     * Get all messages in a chat room
     */
    @GET("api/chat/messages/{chatRoomId}/user/{userId}")
    Call<ApiResponse> getChatMessages(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId);
            
    /**
     * Get all messages in a chat room - direct List<ChatMessage> response
     */
    @GET("api/chat/messages/{chatRoomId}/user/{userId}")
    Call<List<ChatMessage>> getChatMessagesDirect(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId);
      /**
     * Send a text message
     */
    @POST("api/chat/messages")
    Call<ApiResponse> sendTextMessage(@Body Map<String, Object> request);
    
    /**
     * Send a text message - direct ChatMessage response
     */
    @POST("api/chat/messages")
    Call<ChatMessage> sendTextMessageDirect(@Body Map<String, Object> request);
      /**
     * Send an image message
     */
    @Multipart
    @POST("api/chat/messages/image")
    Call<ApiResponse> sendImageMessage(
            @Query("chatRoomId") Long chatRoomId,
            @Query("senderId") Long senderId,
            @Part MultipartBody.Part imageFile);
            
    /**
     * Mark messages as read
     */
    @PUT("api/chat/messages/read/{chatRoomId}/user/{userId}")
    Call<ApiResponse> markMessagesAsRead(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId);
    
    /**
     * Block a chat room
     */
    @PUT("api/chat/rooms/{chatRoomId}/block/{userId}")
    Call<ApiResponse> blockChatRoom(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId);
      /**
     * Report a chat room
     */
    @PUT("api/chat/rooms/{chatRoomId}/report/{userId}")
    Call<ApiResponse> reportChatRoom(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId,
            @Query("reason") String reason,
            @Query("description") String description);
    
    /**
     * Unblock a chat room
     */
    @PUT("api/chat/rooms/{chatRoomId}/unblock/{userId}")
    Call<ApiResponse> unblockChatRoom(
            @Path("chatRoomId") Long chatRoomId,
            @Path("userId") Long userId);
}
