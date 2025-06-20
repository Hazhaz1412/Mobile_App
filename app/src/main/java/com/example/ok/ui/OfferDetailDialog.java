package com.example.ok.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Offer;
import com.example.ok.model.RespondToOfferRequest;
import com.example.ok.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferDetailDialog extends DialogFragment {
    private static final String TAG = "OfferDetailDialog";
    private static final String ARG_OFFER = "offer";
    
    private Offer offer;
    private OnOfferResponseListener listener;
    
    private ImageView ivListing;
    private TextView tvListingTitle;
    private TextView tvBuyerName;
    private TextView tvOriginalPrice;
    private TextView tvOfferAmount;
    private TextView tvDiscount;
    private TextView tvMessage;
    private TextView tvStatus;
    private TextView tvCreatedAt;
    private Button btnAccept;
    private Button btnReject;
    private Button btnCounter;
    private EditText etCounterAmount;
    private EditText etResponseMessage;
      private ApiService apiService;
    private SessionManager sessionManager;
    private long currentUserId;
      public interface OnOfferResponseListener {
        void onOfferResponse(Long offerId, String action, String message);
        default void onOfferResponse(Long offerId, String action, String message, java.math.BigDecimal counterAmount) {
            // Default implementation for backward compatibility
            onOfferResponse(offerId, action, message);
        }
    }
    
    public static OfferDetailDialog newInstance(Offer offer) {
        OfferDetailDialog dialog = new OfferDetailDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OFFER, offer);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            offer = (Offer) getArguments().getSerializable(ARG_OFFER);
        }
          apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(requireContext());
        currentUserId = sessionManager.getUserId();
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_offer_detail, null);
        
        initViews(view);
        setupViews();
        setupButtons();
        
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }
    
    private void initViews(View view) {
        ivListing = view.findViewById(R.id.ivListing);
        tvListingTitle = view.findViewById(R.id.tvListingTitle);
        tvBuyerName = view.findViewById(R.id.tvBuyerName);
        tvOriginalPrice = view.findViewById(R.id.tvOriginalPrice);
        tvOfferAmount = view.findViewById(R.id.tvOfferAmount);
        tvDiscount = view.findViewById(R.id.tvDiscount);
        tvMessage = view.findViewById(R.id.tvMessage);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        btnCounter = view.findViewById(R.id.btnCounter);
        etCounterAmount = view.findViewById(R.id.etCounterAmount);
        etResponseMessage = view.findViewById(R.id.etResponseMessage);
    }
    
    private void setupViews() {
        if (offer == null) return;
        
        // Set listing info
        if (offer.getListing() != null) {
            tvListingTitle.setText(offer.getListing().getTitle());
            
            // Load listing image
            if (offer.getListing().getImageUrl() != null && !offer.getListing().getImageUrl().isEmpty()) {
                Glide.with(requireContext())
                    .load(offer.getListing().getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivListing);
            } else {
                ivListing.setImageResource(R.drawable.placeholder_image);
            }
            
            // Set original price
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String originalPriceStr = numberFormat.format(offer.getListing().getPrice());
            tvOriginalPrice.setText("Giá gốc: " + originalPriceStr);
        }
        
        // Set buyer name
        if (offer.getBuyer() != null) {
            tvBuyerName.setText("👤 " + offer.getBuyer().getUsername());
        }
        
        // Set offer amount
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String offerAmountStr = numberFormat.format(offer.getOfferAmount());
        tvOfferAmount.setText(offerAmountStr);
          // Calculate and show discount percentage
        if (offer.getListing() != null) {
            double originalPrice = offer.getListing().getPrice().doubleValue();
            double offerAmount = offer.getOfferAmount().doubleValue();
            double discountPercent = ((originalPrice - offerAmount) / originalPrice) * 100;
            
            tvDiscount.setText(String.format("-%.0f%%", discountPercent));
        }
        
        // Set message
        if (offer.getMessage() != null && !offer.getMessage().isEmpty()) {
            tvMessage.setText(offer.getMessage());
        } else {
            tvMessage.setText("Không có tin nhắn");
        }
        
        // Set status and enable/disable buttons
        setupStatusAndButtons();
        
        // Set created date
        if (offer.getCreatedAt() != null) {
            tvCreatedAt.setText("Đã gửi: " + offer.getCreatedAt());
        }
    }
      private void setupStatusAndButtons() {
        if ("PENDING".equalsIgnoreCase(offer.getStatus())) {
            // Show action buttons for pending offers
            btnAccept.setVisibility(View.VISIBLE);
            btnReject.setVisibility(View.VISIBLE);
            btnCounter.setVisibility(View.VISIBLE);
            etCounterAmount.setVisibility(View.VISIBLE);
            etResponseMessage.setVisibility(View.VISIBLE);
            
            tvStatus.setText("⏳ Chờ phản hồi của bạn");
            tvStatus.setTextColor(requireContext().getColor(R.color.warning_color));
        } else {
            // Hide action buttons for processed offers
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnCounter.setVisibility(View.GONE);
            etCounterAmount.setVisibility(View.GONE);
            etResponseMessage.setVisibility(View.GONE);
              String statusText;
            int statusColor;
            String status = offer.getStatus();
            if (status == null) status = "PENDING";
            
            switch (status.toUpperCase()) {
                case "ACCEPTED":
                    statusText = "✅ Đã chấp nhận";
                    statusColor = R.color.success_color;
                    break;
                case "REJECTED":
                    statusText = "❌ Đã từ chối";
                    statusColor = R.color.error_color;
                    break;
                case "WITHDRAWN":
                    statusText = "🔙 Người mua đã rút lại";
                    statusColor = R.color.text_secondary;
                    break;
                case "COUNTERED":
                    statusText = "↩️ Đã trả giá";
                    statusColor = R.color.info_color;
                    break;
                default:
                    statusText = "❓ Không xác định";
                    statusColor = R.color.text_secondary;
                    break;
            }
            
            tvStatus.setText(statusText);
            tvStatus.setTextColor(requireContext().getColor(statusColor));
        }
    }
    
    private void setupButtons() {
        btnAccept.setOnClickListener(v -> respondToOffer("ACCEPT", null));
        btnReject.setOnClickListener(v -> respondToOffer("REJECT", etResponseMessage.getText().toString()));
        
        btnCounter.setOnClickListener(v -> {
            String counterAmountStr = etCounterAmount.getText().toString().trim();
            if (counterAmountStr.isEmpty()) {
                etCounterAmount.setError("Vui lòng nhập giá trả lại");
                return;
            }
            
            try {
                double counterAmount = Double.parseDouble(counterAmountStr);
                if (counterAmount <= 0) {
                    etCounterAmount.setError("Giá phải lớn hơn 0");
                    return;
                }
                
                respondToOffer("COUNTER", etResponseMessage.getText().toString());
            } catch (NumberFormatException e) {
                etCounterAmount.setError("Giá không hợp lệ");
            }
        });
    }
      private void respondToOffer(String action, String message) {
        if (currentUserId == 0) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để phản hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable buttons to prevent multiple clicks
        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);
        btnCounter.setEnabled(false);
        
        RespondToOfferRequest request = new RespondToOfferRequest();
        request.setAction(action);
        request.setMessage(message);
        
        if ("COUNTER".equals(action)) {
            String counterAmountStr = etCounterAmount.getText().toString().trim();
            try {
                double counterAmount = Double.parseDouble(counterAmountStr);
                request.setCounterAmount(new java.math.BigDecimal(counterAmount));
            } catch (NumberFormatException e) {
                enableButtons();
                etCounterAmount.setError("Giá không hợp lệ");
                return;
            }
        }
        
        Call<ApiResponse> call = apiService.respondToOffer(offer.getId(), currentUserId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                enableButtons();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "✅ Phản hồi thành công!", Toast.LENGTH_SHORT).show();
                          if (listener != null) {
                            if ("COUNTER".equals(action) && request.getCounterAmount() != null) {
                                listener.onOfferResponse(offer.getId(), action, message, request.getCounterAmount());
                            } else {
                                listener.onOfferResponse(offer.getId(), action, message);
                            }
                        }
                        
                        dismiss();
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Phản hồi thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Phản hồi thất bại", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error responding to offer: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                enableButtons();
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error responding to offer", t);
            }
        });
    }
    
    private void enableButtons() {
        btnAccept.setEnabled(true);
        btnReject.setEnabled(true);
        btnCounter.setEnabled(true);
    }
    
    public void setOnOfferResponseListener(OnOfferResponseListener listener) {
        this.listener = listener;
    }
}
