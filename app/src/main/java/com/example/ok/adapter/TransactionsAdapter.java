package com.example.ok.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.model.Transaction;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {
    
    private Context context;
    private List<Transaction> transactions;
    private OnTransactionActionListener listener;
    private DecimalFormat priceFormatter = new DecimalFormat("#,###");
    private long currentUserId;
    
    public interface OnTransactionActionListener {
        void onCompleteTransaction(Transaction transaction);
        void onCancelTransaction(Transaction transaction);
    }
    
    public TransactionsAdapter(Context context, List<Transaction> transactions, OnTransactionActionListener listener) {
        this.context = context;
        this.transactions = transactions;
        this.listener = listener;
        
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
    }
    
    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        // Basic transaction info
        holder.tvListingTitle.setText(transaction.getListingTitle());
        holder.tvTransactionAmount.setText(priceFormatter.format(transaction.getAmount()) + " VNĐ");
        holder.tvTransactionStatus.setText(getStatusText(transaction.getStatus()));
        
        // Set status color
        int statusColor = getStatusColor(transaction.getStatus());
        holder.tvTransactionStatus.setTextColor(context.getColor(statusColor));
        
        // User info based on current user role
        boolean isSeller = currentUserId == transaction.getSellerId();
        if (isSeller) {
            holder.tvUserRole.setText("Bạn là người bán");
            holder.tvOtherUser.setText("Người mua: " + transaction.getBuyerDisplayName());
        } else {
            holder.tvUserRole.setText("Bạn là người mua");
            holder.tvOtherUser.setText("Người bán: " + transaction.getSellerDisplayName());
        }
        
        // Dates
        holder.tvCreatedDate.setText("Tạo: " + formatDate(transaction.getCreatedAt()));
        if (transaction.getCompletedAt() != null) {
            holder.tvCompletedDate.setText("Hoàn thành: " + formatDate(transaction.getCompletedAt()));
            holder.tvCompletedDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvCompletedDate.setVisibility(View.GONE);
        }
        
        // Action buttons based on status and user role
        setupActionButtons(holder, transaction, isSeller);
    }
    
    private void setupActionButtons(TransactionViewHolder holder, Transaction transaction, boolean isSeller) {
        holder.layoutActions.removeAllViews();
        
        String status = transaction.getStatus();
        
        if ("PENDING".equals(status)) {
            if (isSeller) {
                // Seller can mark as completed
                addActionButton(holder, "Đánh dấu đã bán", R.color.success_color, 
                    v -> listener.onCompleteTransaction(transaction));
            }
            
            // Both can cancel
            addActionButton(holder, "Hủy giao dịch", R.color.error_color, 
                v -> listener.onCancelTransaction(transaction));
        }
        
        // Hide actions layout if no buttons
        if (holder.layoutActions.getChildCount() == 0) {
            holder.layoutActions.setVisibility(View.GONE);
        } else {
            holder.layoutActions.setVisibility(View.VISIBLE);
        }
    }
    
    private void addActionButton(TransactionViewHolder holder, String text, int colorRes, View.OnClickListener clickListener) {
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
    
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING": return "Đang chờ";
            case "COMPLETED": return "Đã hoàn thành";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }
    
    private int getStatusColor(String status) {
        switch (status) {
            case "PENDING": return R.color.warning_color;
            case "COMPLETED": return R.color.success_color;
            case "CANCELLED": return R.color.error_color;
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
        return transactions.size();
    }
    
    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvListingTitle, tvTransactionAmount, tvTransactionStatus, tvUserRole, tvOtherUser;
        TextView tvCreatedDate, tvCompletedDate;
        LinearLayout layoutActions;
        
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvTransactionStatus = itemView.findViewById(R.id.tvTransactionStatus);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvOtherUser = itemView.findViewById(R.id.tvOtherUser);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvCompletedDate = itemView.findViewById(R.id.tvCompletedDate);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }
    }
}
