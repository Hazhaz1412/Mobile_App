package com.example.ok.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.CreateRatingRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RatingActivity extends AppCompatActivity {
    
    private static final String TAG = "RatingActivity";
      private TextView tvRatingTitle;
    private TextView tvRatingSubtitle;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmitRating;
    private Button btnSkipRating;
    private ApiService apiService;
    
    private Long listingId;
    private Long ratedUserId;
    private Long currentUserId;
    private String ratedUserName;
    private String listingTitle;
    private boolean isRatingBuyer; // true if rating buyer, false if rating seller
    
    private Long transactionId; // Will be fetched from API
      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);        // Initialize API service
        apiService = RetrofitClient.getApiService();
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
        
        // Initialize views first
        initializeViews();
        
        // Setup UI
        setupUI();
        
        // Setup listeners
        setupListeners();
        
        // Get data from intent and fetch transaction ID (must be after views are initialized)
        getIntentData();
    }    private void getIntentData() {
        Intent intent = getIntent();
        listingId = intent.getLongExtra("listingId", -1);
        ratedUserId = intent.getLongExtra("ratedUserId", -1);
        ratedUserName = intent.getStringExtra("ratedUserName");
        listingTitle = intent.getStringExtra("listingTitle");
        isRatingBuyer = intent.getBooleanExtra("isRatingBuyer", false);
        
        // Add debug logging
        Log.d(TAG, "Intent data - ListingId: " + listingId + 
              ", RatedUserId: " + ratedUserId + ", RatedUserName: " + ratedUserName);
        
        if (listingId == -1 || ratedUserId == -1) {
            Log.e(TAG, "Invalid listing or user ID");
            Toast.makeText(this, "Lỗi: Không thể tải thông tin đánh giá", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Update UI with intent data
        updateUIWithIntentData();
        
        // Fetch the correct transaction ID
        fetchTransactionId();
    }
    
    private void initializeViews() {
        tvRatingTitle = findViewById(R.id.tvRatingTitle);
        tvRatingSubtitle = findViewById(R.id.tvRatingSubtitle);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);
        btnSubmitRating = findViewById(R.id.btnSubmitRating);
        btnSkipRating = findViewById(R.id.btnSkipRating);
    }
      private void setupUI() {
        // Set default rating
        ratingBar.setRating(5.0f);
        
        // Set initial loading state
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Đang tải...");
    }
    
    private void updateUIWithIntentData() {
        // Set title based on who we're rating
        String roleText = isRatingBuyer ? "người mua" : "người bán";
        tvRatingTitle.setText("Đánh giá " + roleText);
        
        // Set subtitle with user name and listing
        String subtitle = String.format("Đánh giá %s %s\ncho giao dịch: %s", 
            roleText, 
            ratedUserName != null ? ratedUserName : "người dùng", 
            listingTitle != null ? listingTitle : "Giao dịch");
        tvRatingSubtitle.setText(subtitle);
    }
    
    private void setupListeners() {
        btnSubmitRating.setOnClickListener(v -> submitRating());
        btnSkipRating.setOnClickListener(v -> skipRating());
        
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
    
    private void fetchTransactionId() {
        Log.d(TAG, "Fetching transaction ID for listingId: " + listingId + ", buyerId: " + currentUserId);
        
        // Disable submit button while fetching
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Đang tải...");
        
        Call<ApiResponse> call = apiService.findTransactionByListing(listingId, currentUserId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {                        try {
                            // Parse transaction data
                            Map<String, Object> transactionData = (Map<String, Object>) apiResponse.getData();
                            if (transactionData.get("transactionId") instanceof Number) {
                                transactionId = ((Number) transactionData.get("transactionId")).longValue();
                                Log.d(TAG, "Transaction ID found: " + transactionId);
                                
                                // Enable submit button
                                btnSubmitRating.setEnabled(true);
                                btnSubmitRating.setText("Gửi đánh giá");
                            } else {
                                Log.e(TAG, "TransactionId field not found or not a number in response");
                                handleTransactionNotFound();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing transaction data", e);
                            handleTransactionNotFound();
                        }
                    } else {
                        Log.e(TAG, "API response not successful: " + apiResponse.getMessage());
                        handleTransactionNotFound();
                    }
                } else {
                    Log.e(TAG, "HTTP response not successful: " + response.code());
                    handleTransactionNotFound();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch transaction ID", t);
                handleTransactionNotFound();
            }
        });
    }
    
    private void handleTransactionNotFound() {
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Không tìm thấy giao dịch");
        Toast.makeText(this, "Không tìm thấy giao dịch hoàn thành cho sản phẩm này", Toast.LENGTH_LONG).show();
    }
      private void submitRating() {
        if (transactionId == null || transactionId == -1) {
            Toast.makeText(this, "Không thể gửi đánh giá. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();
        
        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create rating request
        CreateRatingRequest request = new CreateRatingRequest();
        request.setTransactionId(transactionId);
        request.setRatedUserId(ratedUserId);
        request.setRating((int) rating);
        if (!comment.isEmpty()) {
            request.setComment(comment);
        }
        
        // Show loading
        btnSubmitRating.setEnabled(false);
        btnSubmitRating.setText("Đang gửi...");
        
        // Call API
        Call<ApiResponse> call = apiService.createRating(currentUserId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnSubmitRating.setEnabled(true);
                btnSubmitRating.setText("Gửi đánh giá");
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(RatingActivity.this, 
                            "Đánh giá đã được gửi thành công!", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RatingActivity.this, 
                            "Lỗi: " + apiResponse.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(RatingActivity.this, 
                        "Không thể gửi đánh giá. Vui lòng thử lại.", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnSubmitRating.setEnabled(true);
                btnSubmitRating.setText("Gửi đánh giá");
                
                Log.e(TAG, "Error submitting rating", t);
                Toast.makeText(RatingActivity.this, 
                    "Lỗi kết nối. Vui lòng thử lại.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void skipRating() {
        Toast.makeText(this, "Bạn có thể đánh giá sau trong lịch sử giao dịch", Toast.LENGTH_SHORT).show();
        finish();
    }
}
