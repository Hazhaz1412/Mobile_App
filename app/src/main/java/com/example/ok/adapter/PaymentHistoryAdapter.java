package com.example.ok.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.model.Payment;
import com.example.ok.model.PaymentMethod;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {
      private List<Payment> payments;
    private OnPaymentClickListener listener;
    private DecimalFormat priceFormat = new DecimalFormat("#,###");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());    public interface OnPaymentClickListener {
        void onPaymentClicked(Payment payment);
        void onCancelPayment(Payment payment);
        void onRateUser(Payment payment);
    }
    
    public PaymentHistoryAdapter(List<Payment> payments, OnPaymentClickListener listener) {
        this.payments = payments;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Payment payment = payments.get(position);
        holder.bind(payment);
    }
    
    @Override
    public int getItemCount() {
        return payments.size();
    }
      class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPaymentIcon;
        private TextView tvListingTitle;
        private TextView tvAmount;
        private TextView tvStatus;
        private TextView tvDate;        private TextView tvTransactionId;
        private TextView tvSellerName;
        private Button btnCancelPayment;
        private Button btnRateUser;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPaymentIcon = itemView.findViewById(R.id.ivPaymentIcon);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            btnCancelPayment = itemView.findViewById(R.id.btnCancelPayment);
            btnRateUser = itemView.findViewById(R.id.btnRateUser);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPaymentClicked(payments.get(position));
                }
            });            btnCancelPayment.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCancelPayment(payments.get(position));
                }
            });
            
            btnRateUser.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRateUser(payments.get(position));
                }
            });
        }
        
        public void bind(Payment payment) {
            // Set listing title
            tvListingTitle.setText(payment.getListingTitle() != null ? 
                    payment.getListingTitle() : "Sản phẩm");
              // Set amount - format without comma for cleaner look like in the image
            String amountText = String.format("%.0f VND", payment.getAmount());
            tvAmount.setText(amountText);
            
            // Set status with color
            String statusText = getStatusText(payment.getStatus());
            tvStatus.setText(statusText);
            tvStatus.setTextColor(getStatusColor(payment.getStatus()));
            
            // Set date
            if (payment.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(payment.getCreatedAt()));
            }
            
            // Set transaction ID
            tvTransactionId.setText("ID: " + (payment.getTransactionId() != null ? 
                    payment.getTransactionId() : "N/A"));
            
            // Set seller name
            tvSellerName.setText("Người bán: " + (payment.getSellerName() != null ? 
                    payment.getSellerName() : "N/A"));
              // Set payment method icon
            setPaymentMethodIcon(payment.getPaymentMethodType());
              // Show/hide buttons based on payment status
            if (canCancelPayment(payment)) {
                btnCancelPayment.setVisibility(View.VISIBLE);
            } else {
                btnCancelPayment.setVisibility(View.GONE);
            }
            
            if (canRateUser(payment)) {
                btnRateUser.setVisibility(View.VISIBLE);
            } else {
                btnRateUser.setVisibility(View.GONE);
            }
        }
        
        private void setPaymentMethodIcon(String paymentMethodType) {
            switch (paymentMethodType) {
                case PaymentMethod.MOMO:
                    ivPaymentIcon.setImageResource(R.drawable.ic_momo);
                    break;
                case PaymentMethod.VISA:
                    ivPaymentIcon.setImageResource(R.drawable.ic_visa);
                    break;
                case PaymentMethod.MASTERCARD:
                    ivPaymentIcon.setImageResource(R.drawable.ic_mastercard);
                    break;
                case PaymentMethod.CASH:
                    ivPaymentIcon.setImageResource(R.drawable.ic_cash);
                    break;
                default:
                    ivPaymentIcon.setImageResource(R.drawable.ic_payment);
                    break;
            }
        }
        
        private String getStatusText(String status) {
            switch (status) {
                case Payment.STATUS_COMPLETED:
                    return "Hoàn thành";
                case Payment.STATUS_PROCESSING:
                    return "Đang xử lý";
                case Payment.STATUS_PENDING:
                    return "Chờ xử lý";
                case Payment.STATUS_FAILED:
                    return "Thất bại";
                case Payment.STATUS_CANCELLED:
                    return "Đã hủy";
                case Payment.STATUS_REFUNDED:
                    return "Đã hoàn tiền";
                default:
                    return status;
            }
        }
        
        private int getStatusColor(String status) {
            switch (status) {
                case Payment.STATUS_COMPLETED:
                    return Color.parseColor("#4CAF50"); // Green
                case Payment.STATUS_PROCESSING:
                    return Color.parseColor("#FF9800"); // Orange
                case Payment.STATUS_PENDING:
                    return Color.parseColor("#2196F3"); // Blue                case Payment.STATUS_FAILED:
                case Payment.STATUS_CANCELLED:
                    return Color.parseColor("#F44336"); // Red
                case Payment.STATUS_REFUNDED:
                    return Color.parseColor("#9C27B0"); // Purple
                default:
                    return Color.parseColor("#757575"); // Gray
            }
        }
    }
      private boolean canCancelPayment(Payment payment) {
        String status = payment.getStatus();
        return Payment.STATUS_PENDING.equals(status) || Payment.STATUS_PROCESSING.equals(status);
    }
    
    private boolean canRateUser(Payment payment) {
        String status = payment.getStatus();
        return Payment.STATUS_COMPLETED.equals(status);
    }
}
