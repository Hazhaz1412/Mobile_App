package com.example.ok.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.Offer;

import java.text.NumberFormat;
import java.util.Locale;

public class MyOfferDetailDialog extends Dialog {
    
    private Offer offer;
    private OnOfferActionListener listener;
      public interface OnOfferActionListener {
        void onWithdrawOffer(Offer offer);
        void onViewListing(Offer offer);
        void onBuyNow(Offer offer);
    }
    
    public MyOfferDetailDialog(@NonNull Context context, Offer offer, OnOfferActionListener listener) {
        super(context);
        this.offer = offer;
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_my_offer_detail);
        
        setupViews();
        populateData();
    }
      private void setupViews() {
        findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        
        Button btnWithdraw = findViewById(R.id.btnWithdraw);
        Button btnViewListing = findViewById(R.id.btnViewListing);
        Button btnBuyNow = findViewById(R.id.btnBuyNow);
          // Show/hide buttons based on offer status
        String status = offer.getStatus();
        
        // Buy Now button - check completion status first
        if ("COMPLETED".equalsIgnoreCase(status) || offer.isHasPaidTransaction()) {
            // Already completed/paid - show disabled button with different text
            btnBuyNow.setText("ĐÃ HOÀN THÀNH");
            btnBuyNow.setEnabled(false);
            btnBuyNow.setVisibility(View.VISIBLE);
            btnBuyNow.setBackgroundColor(getContext().getColor(R.color.text_secondary));
        } else if ("ACCEPTED".equalsIgnoreCase(status)) {
            // Accepted but not paid yet - show buy button
            btnBuyNow.setText("MUA NGAY");
            btnBuyNow.setEnabled(true);
            btnBuyNow.setVisibility(View.VISIBLE);
            btnBuyNow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBuyNow(offer);
                }
                dismiss();
            });
        } else {
            btnBuyNow.setVisibility(View.GONE);
        }
        
        // Withdraw button - only show for pending or countered offers
        if ("PENDING".equalsIgnoreCase(status) || "COUNTERED".equalsIgnoreCase(status)) {
            btnWithdraw.setVisibility(View.VISIBLE);
            btnWithdraw.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWithdrawOffer(offer);
                }
                dismiss();
            });
        } else {
            btnWithdraw.setVisibility(View.GONE);
        }
        
        btnViewListing.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewListing(offer);
            }
            dismiss();
        });
    }
    
    private void populateData() {
        // Product image
        ImageView ivProduct = findViewById(R.id.ivProduct);
        if (offer.getListing() != null && offer.getListing().getImageUrl() != null) {
            Glide.with(getContext())
                .load(offer.getListing().getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(ivProduct);
        }
        
        // Product title
        TextView tvProductTitle = findViewById(R.id.tvProductTitle);
        if (offer.getListing() != null) {
            tvProductTitle.setText(offer.getListing().getTitle());
        }
        
        // Seller info
        TextView tvSellerName = findViewById(R.id.tvSellerName);
        if (offer.getSellerDisplayName() != null) {
            tvSellerName.setText("Người bán: " + offer.getSellerDisplayName());
        }
        
        // Price info
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        TextView tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        if (offer.getListing() != null) {
            tvOriginalPrice.setText("Giá gốc: " + numberFormat.format(offer.getListing().getPrice()));
        }
        
        TextView tvOfferAmount = findViewById(R.id.tvOfferAmount);
        tvOfferAmount.setText("Giá đề xuất: " + numberFormat.format(offer.getOfferAmount()));
        
        // Discount calculation
        TextView tvDiscount = findViewById(R.id.tvDiscount);
        if (offer.getListing() != null) {
            double originalPrice = offer.getListing().getPrice().doubleValue();
            double offerAmount = offer.getOfferAmount().doubleValue();
            double discountPercent = ((originalPrice - offerAmount) / originalPrice) * 100;
            tvDiscount.setText(String.format("Giảm %.0f%%", discountPercent));
        }
        
        // Message
        TextView tvMessage = findViewById(R.id.tvMessage);
        if (offer.getMessage() != null && !offer.getMessage().isEmpty()) {
            tvMessage.setText("Tin nhắn: " + offer.getMessage());
            tvMessage.setVisibility(View.VISIBLE);
        } else {
            tvMessage.setVisibility(View.GONE);
        }
        
        // Status
        TextView tvStatus = findViewById(R.id.tvStatus);
        View statusIndicator = findViewById(R.id.statusIndicator);
        
        String statusText;
        int statusColor;
        
        String status = offer.getStatus();
        if (status == null) status = "PENDING";
          switch (status.toUpperCase()) {
            case "PENDING":
                statusText = "⏳ Chờ phản hồi từ người bán";
                statusColor = R.color.warning_color;
                break;
            case "ACCEPTED":
                statusText = "✅ Đã được chấp nhận";
                statusColor = R.color.success_color;
                break;
            case "COMPLETED":
                statusText = "🎉 Đã hoàn thành thanh toán";
                statusColor = R.color.success_color;
                break;
            case "REJECTED":
                statusText = "❌ Bị từ chối";
                statusColor = R.color.error_color;
                break;
            case "WITHDRAWN":
                statusText = "🔙 Đã rút lại";
                statusColor = R.color.text_secondary;
                break;
            case "COUNTERED":
                statusText = "↩️ Người bán đã trả giá";
                statusColor = R.color.info_color;
                break;
            default:
                statusText = "❓ Không xác định";
                statusColor = R.color.text_secondary;
                break;
        }
        
        tvStatus.setText(statusText);
        tvStatus.setTextColor(getContext().getColor(statusColor));
        statusIndicator.setBackgroundColor(getContext().getColor(statusColor));
        
        // Created date
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
        if (offer.getCreatedAt() != null) {
            tvCreatedAt.setText("Ngày tạo: " + offer.getCreatedAt());
        }
        
        // Update date
        TextView tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        if (offer.getUpdatedAt() != null && !offer.getUpdatedAt().equals(offer.getCreatedAt())) {
            tvUpdatedAt.setText("Cập nhật: " + offer.getUpdatedAt());
            tvUpdatedAt.setVisibility(View.VISIBLE);
        } else {
            tvUpdatedAt.setVisibility(View.GONE);
        }
    }
}
