package com.example.ok.util;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for generating and validating MoMo signatures
 */
public class MoMoSignatureUtil {
    private static final String TAG = "MoMoSignatureUtil";

    /**
     * Generate HMAC SHA256 signature for MoMo API
     *
     * @param data Raw string data to sign
     * @param key  Secret key
     * @return Hex encoded signature
     */
    public static String generateSignature(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "Error generating MoMo signature", e);
            throw new RuntimeException("Failed to generate MoMo signature", e);
        }
    }

    /**
     * Convert bytes to hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Build raw signature string from MoMo payment parameters
     *
     * @param accessKey   MoMo access key
     * @param amount      Payment amount
     * @param extraData   Extra data
     * @param ipnUrl      IPN URL
     * @param orderId     Order ID
     * @param orderInfo   Order description
     * @param partnerCode Partner code
     * @param redirectUrl Redirect URL
     * @param requestId   Request ID
     * @param requestType Request type
     * @return Raw signature string ready for HMAC
     */
    public static String buildRawSignature(
            String accessKey,
            String amount,
            String extraData,
            String ipnUrl,
            String orderId,
            String orderInfo,
            String partnerCode,
            String redirectUrl,
            String requestId,
            String requestType) {

        return "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
    }
}
