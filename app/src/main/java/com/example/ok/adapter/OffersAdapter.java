package com.example.ok.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.Offer;
import com.example.ok.ui.OffersFragment;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OffersAdapter extends RecyclerView.Adapter<OffersAdapter.OfferViewHolder> {
    
    private Context context;
    private List<Offer> offers;
    private int offerType;
    private OnOfferActionListener listener;
    private DecimalFormat priceFormatter = new DecimalFormat("#,###");
    
    public interface OnOfferActionListener {
        void onAcceptOffer(Offer offer);
        void onRejectOffer(Offer offer);
        void onCounterOffer(Offer offer, double counterAmount, String message);
        void onWithdrawOffer(Offer offer);
    }
    
    public OffersAdapter(Context context, List<Offer> offers, int offerType, OnOfferActionListener listener) {
        this.context = context;
        this.offers = offers;
        this.offerType = offerType;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offers.get(position);
          // Basic offer info
        holder.tvListingTitle.setText(offer.getListingTitle());
        holder.tvOfferAmount.setText(priceFormatter.format(offer.getAmount()) + " VNĐ");
        holder.tvStatus.setText(getStatusText(offer.getStatus()));
        
        // Set status color
        int statusColor = getStatusColor(offer.getStatus());
        holder.tvStatus.setTextColor(context.getColor(statusColor));
        
        // User info based on offer type
        if (offerType == OffersFragment.TYPE_SENT_OFFERS) {
            // Show seller info for sent offers
            holder.tvUserName.setText("Người bán: " + offer.getSellerDisplayName());
        } else {
            // Show buyer info for received offers
            holder.tvUserName.setText("Người mua: " + offer.getBuyerDisplayName());
        }
        
        // Message
        if (offer.getMessage() != null && !offer.getMessage().trim().isEmpty()) {
            holder.tvMessage.setText(offer.getMessage());
            holder.tvMessage.setVisibility(View.VISIBLE);
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }
        
        // Date
        holder.tvDate.setText(formatDate(offer.getCreatedAt()));
        
        // Action buttons based on status and type
        setupActionButtons(holder, offer);
    }
    
    private void setupActionButtons(OfferViewHolder holder, Offer offer) {
        holder.layoutActions.removeAllViews();
        
        String status = offer.getStatus();
        
        if (offerType == OffersFragment.TYPE_RECEIVED_OFFERS && "PENDING".equals(status)) {
            // Seller can accept, reject, or counter pending offers
            addActionButton(holder, "Chấp nhận", R.color.success_color, 
                v -> listener.onAcceptOffer(offer));
                
            addActionButton(holder, "Từ chối", R.color.error_color, 
                v -> listener.onRejectOffer(offer));
                
            addActionButton(holder, "Trả giá", R.color.primary_color, 
                v -> showCounterOfferDialog(offer));
                
        } else if (offerType == OffersFragment.TYPE_SENT_OFFERS && "PENDING".equals(status)) {
            // Buyer can withdraw pending offers
            addActionButton(holder, "Rút offer", R.color.error_color, 
                v -> listener.onWithdrawOffer(offer));
        }
        
        // Hide actions layout if no buttons
        if (holder.layoutActions.getChildCount() == 0) {
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.layoutActions.setVisibility(View.VISIBLE);
        }
    }
    
    private void addActionButton(OfferViewHolder holder, String text, int colorRes, View.OnClickListener clickListener) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(context.getColor(android.R.color.white));
        button.setBackgroundTintList(context.getColorStateList(colorRes));
        button.setOnClickListener(clickListener);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);
        
        holder.layoutActions.addView(button);
    }
    
    private void showCounterOfferDialog(Offer offer) {
        // This would show a dialog for counter offer - simplified for now
        // In a real implementation, this would show a dialog similar to makeOffer
        if (listener != null) {
            // For now, just trigger with dummy values - should be replaced with proper dialog
            listener.onCounterOffer(offer, offer.getAmount().doubleValue() * 0.9, "Counter offer");
        }
    }
      private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Đang chờ";
            case "ACCEPTED": return "Đã chấp nhận";
            case "COMPLETED": return "Đã hoàn thành";
            case "REJECTED": return "Đã từ chối";
            case "WITHDRAWN": return "Đã rút";
            case "COUNTERED": return "Đã trả giá";
            default: return status;
        }
    }
    
    private int getStatusColor(String status) {
        switch (status) {
            case "PENDING": return R.color.warning_color;
            case "ACCEPTED": 
            case "COMPLETED": return R.color.success_color;
            case "REJECTED":
            case "WITHDRAWN": return R.color.error_color;
            case "COUNTERED": return R.color.info_color;
            default: return R.color.text_secondary;
        }
    }
    
    private String formatDate(String dateString) {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = apiFormat.parse(dateString);
            
            if (DateUtils.isToday(date.getTime())) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return "Hôm nay " + timeFormat.format(date);
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return dateFormat.format(date);
            }
        } catch (Exception e) {
            return dateString;
        }
    }
    
    @Override
    public int getItemCount() {
        return offers.size();
    }
      static class OfferViewHolder extends RecyclerView.ViewHolder {
        TextView tvListingTitle, tvOfferAmount, tvStatus, tvUserName, tvMessage, tvDate;
        LinearLayout layoutActions;
        
        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvOfferAmount = itemView.findViewById(R.id.tvOfferAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }
    }
}
