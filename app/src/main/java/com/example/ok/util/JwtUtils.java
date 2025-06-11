package com.example.ok.util;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class JwtUtils {
    private static final String TAG = "JwtUtils";

    /**
     * Giải mã JWT token để lấy userId
     * @param token JWT token
     * @return userId hoặc null nếu có lỗi
     */
    public static Long getUserIdFromToken(String token) {
        try {
            // JWT có cấu trúc: header.payload.signature
            String[] parts = token.split("\\.");
            
            // Chỉ cần phần payload (phần thứ 2)
            if (parts.length < 2) {
                Log.e(TAG, "Invalid token format");
                return null;
            }
            
            // Giải mã phần payload từ base64
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE), "UTF-8");
            
            // Parse JSON
            JSONObject jsonPayload = new JSONObject(payload);
            
            // Lấy userId từ trường "sub" hoặc "userId" tùy theo cách backend mã hóa
            if (jsonPayload.has("sub")) {
                return Long.parseLong(jsonPayload.getString("sub"));
            } else if (jsonPayload.has("userId")) {
                return jsonPayload.getLong("userId");
            } else {
                Log.e(TAG, "Token does not contain user ID");
                return null;
            }
        } catch (UnsupportedEncodingException | JSONException | NumberFormatException e) {
            Log.e(TAG, "Error parsing JWT token", e);
            return null;
        }
    }
}
