package com.example.ok.api;

import com.example.ok.model.ApiResponse;
import com.example.ok.model.Payment;
import com.example.ok.model.PaymentMethod;
import com.example.ok.model.PaymentRequest;
import com.example.ok.model.PaymentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface PaymentApiService {
    
    // Payment processing endpoints
    @POST("api/payments/create")
    Call<PaymentResponse> createPayment(@Body PaymentRequest request);
    
    @POST("api/payments/momo")
    Call<PaymentResponse> processMoMoPayment(@Body PaymentRequest request);
    
    @POST("api/payments/card")
    Call<PaymentResponse> processCardPayment(@Body PaymentRequest request);
      @GET("api/payments/{paymentId}/status")
    Call<ApiResponse> getPaymentStatus(@Path("paymentId") Long paymentId);
    
    @POST("api/payments/{paymentId}/confirm")
    Call<ApiResponse> confirmPayment(@Path("paymentId") Long paymentId);
    
    @POST("api/payments/{paymentId}/cancel")
    Call<ApiResponse> cancelPayment(@Path("paymentId") Long paymentId);
    
    // Payment history
    @GET("api/payments/user/{userId}")
    Call<ApiResponse> getUserPayments(@Path("userId") Long userId);
    
    @GET("api/payments/user/{userId}/history")
    Call<ApiResponse> getPaymentHistory(
        @Path("userId") Long userId,
        @Query("page") int page,
        @Query("size") int size
    );
    
    // Payment methods management
    @GET("api/payment-methods/user/{userId}")
    Call<ApiResponse> getUserPaymentMethods(@Path("userId") Long userId);
    
    @POST("api/payment-methods/user/{userId}")
    Call<ApiResponse> addPaymentMethod(
        @Path("userId") Long userId,
        @Body PaymentMethod paymentMethod
    );
    
    @PUT("api/payment-methods/{methodId}")
    Call<ApiResponse> updatePaymentMethod(
        @Path("methodId") Long methodId,
        @Body PaymentMethod paymentMethod
    );
    
    @DELETE("api/payment-methods/{methodId}")
    Call<ApiResponse> deletePaymentMethod(@Path("methodId") Long methodId);
    
    @POST("api/payment-methods/{methodId}/set-default")
    Call<ApiResponse> setDefaultPaymentMethod(@Path("methodId") Long methodId);
    
    // Escrow system endpoints
    @POST("api/payments/{paymentId}/escrow/hold")
    Call<ApiResponse> holdEscrowPayment(@Path("paymentId") Long paymentId);
    
    @POST("api/payments/{paymentId}/escrow/release")
    Call<ApiResponse> releaseEscrowPayment(@Path("paymentId") Long paymentId);
    
    @POST("api/payments/{paymentId}/escrow/refund")
    Call<ApiResponse> refundEscrowPayment(@Path("paymentId") Long paymentId);
}
