package com.example.ok.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "https://zn8vnhrf-8080.asse.devtunnels.ms/";
    private static Context mContext;
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize with null context");
            return;
        }
        mContext = context.getApplicationContext();
        isInitialized = true;
        Log.d(TAG, "RetrofitClient initialized with context");
    }

    private static void checkInitialized() {
        if (!isInitialized || mContext == null) {
            Log.e(TAG, "RetrofitClient not initialized properly");
            throw new IllegalStateException("RetrofitClient not initialized. Call RetrofitClient.init(context) first.");
        }
    }    public static Retrofit getClient() {
        checkInitialized();
        
        if (retrofit == null) {
            // Thêm logging để debug
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new AuthInterceptor())
                    .authenticator(new TokenAuthenticator(mContext))
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;    }    public static ApiService getApiService() {
        checkInitialized();
        return getClient().create(ApiService.class);
    }
    
    // THÊM METHOD NÀY NẾU CHƯA CÓ
    public static ListingApiService getListingApiService() {
        checkInitialized();
        return getClient().create(ListingApiService.class);
    }
    
    // Method for getting ChatApiService
    public static ChatApiService getChatApiService() {
        checkInitialized();
        return getClient().create(ChatApiService.class);
    }
      // Method for getting AuthApiService
    public static AuthApiService getAuthApiService() {
        checkInitialized();
        return getClient().create(AuthApiService.class);
    }    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            if (mContext == null) {
                throw new IllegalStateException("RetrofitClient not initialized. Call RetrofitClient.init(context) first.");
            }
            
            SharedPreferences prefs = mContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", "");
            
            Request original = chain.request();
            
            // Enhanced debug logging for message requests
            boolean isMessageRequest = original.url().toString().contains("/chat/messages/");
            if (isMessageRequest) {
                Log.d(TAG, "=== AuthInterceptor MESSAGE REQUEST ===");
                Log.d(TAG, "Request URL: " + original.url());
                Log.d(TAG, "Request method: " + original.method());
                Log.d(TAG, "Token available: " + (!token.isEmpty()));
                Log.d(TAG, "Token length: " + token.length());
                if (!token.isEmpty()) {
                    Log.d(TAG, "Token first 20 chars: " + token.substring(0, Math.min(20, token.length())) + "...");
                }
                Log.d(TAG, "=== END AuthInterceptor DEBUG ===");
            } else {
                Log.d(TAG, "AuthInterceptor - Request URL: " + original.url());
                Log.d(TAG, "AuthInterceptor - Token available: " + (!token.isEmpty()));
            }
            
            // Thêm token JWT vào header của mỗi request
            Request.Builder requestBuilder = original.newBuilder()
                    .header("Authorization", "Bearer " + token);
            
            Request request = requestBuilder.build();
            Response response = chain.proceed(request);
            
            // Log response for message requests
            if (isMessageRequest) {
                Log.d(TAG, "=== AuthInterceptor RESPONSE ===");
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());
                Log.d(TAG, "=== END RESPONSE DEBUG ===");
            }
            
            return response;
        }
    }
}