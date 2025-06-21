package com.example.ok.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.Offer;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyOffersAdapter extends RecyclerView.Adapter<MyOffersAdapter.MyOfferViewHolder> {
    
    private Context context;
    private List<Offer> offers;
    private OnOfferClickListener listener;
    
    public interface OnOfferClickListener {
        void onOfferClick(Offer offer);
    }
    
    public MyOffersAdapter(Context context, List<Offer> offers, OnOfferClickListener listener) {
        this.context = context;
        this.offers = offers;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public MyOfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_offer, parent, false);
        return new MyOfferViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MyOfferViewHolder holder, int position) {
        Offer offer = offers.get(position);
        holder.bind(offer);
    }
    
    @Override
    public int getItemCount() {
        return offers.size();
    }
    
    public void updateOffers(List<Offer> newOffers) {
        this.offers = newOffers;
        notifyDataSetChanged();
    }
    
    class MyOfferViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView ivListing;
        private TextView tvListingTitle;
        private TextView tvSellerName;
        private TextView tvOriginalPrice;
        private TextView tvMyOfferAmount;
        private TextView tvDiscount;
        private TextView tvMessage;
        private TextView tvStatus;
        private TextView tvCreatedAt;
        private Button btnViewDetails;
        private View statusIndicator;
        
        public MyOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivListing = itemView.findViewById(R.id.ivListing);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvMyOfferAmount = itemView.findViewById(R.id.tvMyOfferAmount);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            
            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOfferClick(offers.get(getAdapterPosition()));
                }
            });
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOfferClick(offers.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(Offer offer) {
            // Set listing info
            if (offer.getListing() != null) {
                tvListingTitle.setText(offer.getListing().getTitle());
                
                // Load listing image
                if (offer.getListing().getImageUrl() != null && !offer.getListing().getImageUrl().isEmpty()) {
                    Glide.with(context)
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
            
            // Set seller name (for buyer's view, show who they're making offer to)
            if (offer.getSellerDisplayName() != null) {
                tvSellerName.setText("👤 " + offer.getSellerDisplayName());
            }
            
            // Set my offer amount
            NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String offerAmountStr = numberFormat.format(offer.getOfferAmount());
            tvMyOfferAmount.setText("Giá đề xuất: " + offerAmountStr);
            
            // Calculate and show discount percentage
            if (offer.getListing() != null) {
                double originalPrice = offer.getListing().getPrice().doubleValue();
                double offerAmount = offer.getOfferAmount().doubleValue();
                double discountPercent = ((originalPrice - offerAmount) / originalPrice) * 100;
                
                tvDiscount.setText(String.format("-%.0f%%", discountPercent));
                tvDiscount.setTextColor(context.getColor(
                    discountPercent > 0 ? R.color.success_color : R.color.text_secondary
                ));
            }
            
            // Set message
            if (offer.getMessage() != null && !offer.getMessage().isEmpty()) {
                tvMessage.setText("💬 " + offer.getMessage());
                tvMessage.setVisibility(View.VISIBLE);
            } else {
                tvMessage.setVisibility(View.GONE);
            }
            
            // Set status
            String statusText;
            int statusColor;
            int indicatorColor;
            
            String status = offer.getStatus();
            if (status == null) status = "PENDING";
              switch (status.toUpperCase()) {
                case "PENDING":
                    statusText = "⏳ Chờ phản hồi";
                    statusColor = R.color.warning_color;
                    indicatorColor = R.color.warning_color;
                    break;
                case "ACCEPTED":
                    statusText = "✅ Đã chấp nhận";
                    statusColor = R.color.success_color;
                    indicatorColor = R.color.success_color;
                    break;
                case "COMPLETED":
                    statusText = "🎉 Đã hoàn thành";
                    statusColor = R.color.success_color;
                    indicatorColor = R.color.success_color;
                    break;
                case "REJECTED":
                    statusText = "❌ Bị từ chối";
                    statusColor = R.color.error_color;
                    indicatorColor = R.color.error_color;
                    break;
                case "WITHDRAWN":
                    statusText = "🔙 Đã rút lại";
                    statusColor = R.color.text_secondary;
                    indicatorColor = R.color.text_secondary;
                    break;
                case "COUNTERED":
                    statusText = "↩️ Được trả giá";
                    statusColor = R.color.info_color;
                    indicatorColor = R.color.info_color;
                    break;
                default:
                    statusText = "❓ Không xác định";
                    statusColor = R.color.text_secondary;
                    indicatorColor = R.color.text_secondary;
                    break;
            }
            
            tvStatus.setText(statusText);
            tvStatus.setTextColor(context.getColor(statusColor));
            statusIndicator.setBackgroundColor(context.getColor(indicatorColor));
            
            // Set created date
            if (offer.getCreatedAt() != null) {
                tvCreatedAt.setText(formatRelativeTime(offer.getCreatedAt()));
            }
              // Update button text based on status
            String offerStatus = offer.getStatus();
            if ("PENDING".equalsIgnoreCase(offerStatus) || "COUNTERED".equalsIgnoreCase(offerStatus)) {
                btnViewDetails.setText("Xem chi tiết");
                btnViewDetails.setBackgroundTintList(context.getColorStateList(R.color.colorPrimary));
            } else if ("ACCEPTED".equalsIgnoreCase(offerStatus)) {
                btnViewDetails.setText("Đã chấp nhận");
                btnViewDetails.setBackgroundTintList(context.getColorStateList(R.color.success_color));
            } else if ("COMPLETED".equalsIgnoreCase(offerStatus)) {
                btnViewDetails.setText("Đã hoàn thành");
                btnViewDetails.setBackgroundTintList(context.getColorStateList(R.color.success_color));
            } else if ("REJECTED".equalsIgnoreCase(offerStatus)) {
                btnViewDetails.setText("Bị từ chối");
                btnViewDetails.setBackgroundTintList(context.getColorStateList(R.color.error_color));
            } else {
                btnViewDetails.setText("Xem chi tiết");
                btnViewDetails.setBackgroundTintList(context.getColorStateList(R.color.text_secondary));
            }
        }
        
        private String formatRelativeTime(String createdAt) {
            // TODO: Implement relative time formatting
            // For now, just return the raw timestamp
            return createdAt;
        }
    }
}
