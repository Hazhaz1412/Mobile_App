package com.example.ok.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.model.Report;

import java.util.List;

/**
 * Adapter for displaying user's submitted reports
 */
public class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.MyReportViewHolder> {
    
    private List<Report> reportList;
    private OnReportActionListener listener;
    
    public interface OnReportActionListener {
        void onViewDetails(Report report);
        void onCancelReport(Report report);
    }
    
    public MyReportAdapter(List<Report> reportList, OnReportActionListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public MyReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_report, parent, false);
        return new MyReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MyReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.bind(report);
    }
    
    @Override
    public int getItemCount() {
        return reportList.size();
    }
    
    class MyReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportType, tvReportReason, tvReportedItem, tvStatus, tvCreatedAt;
        Button btnDetails, btnCancel;
        View statusIndicator;
        
        public MyReportViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvReportType = itemView.findViewById(R.id.tv_report_type);
            tvReportReason = itemView.findViewById(R.id.tv_report_reason);
            tvReportedItem = itemView.findViewById(R.id.tv_reported_item);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            
            btnDetails = itemView.findViewById(R.id.btn_details);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }
        
        public void bind(Report report) {
            // Set report type with icon
            tvReportType.setText(getReportTypeDisplay(report.getReportType()));
            
            // Set reason
            tvReportReason.setText(report.getReason());
            
            // Set reported item
            String reportedItem = getReportedItemDisplay(report);
            tvReportedItem.setText(reportedItem);
            
            // Set status
            String statusDisplay = getStatusDisplay(report.getStatus());
            tvStatus.setText(statusDisplay);
            setStatusIndicator(report.getStatus());
            
            // Set created date
            tvCreatedAt.setText(formatDate(report.getCreatedAt()));
            
            // Setup buttons
            setupButtons(report);
        }
        
        private String getReportTypeDisplay(String type) {
            switch (type) {
                case "USER": return "👤 Báo cáo người dùng";
                case "LISTING": return "📝 Báo cáo tin đăng";
                case "CHAT": return "💬 Báo cáo trò chuyện";
                default: return type;
            }
        }
        
        private String getReportedItemDisplay(Report report) {
            switch (report.getReportType()) {
                case "USER":
                    return report.getReportedUserName() != null ? 
                        report.getReportedUserName() : "ID: " + report.getReportedUserId();
                case "LISTING":
                    return report.getReportedListingTitle() != null ? 
                        report.getReportedListingTitle() : "ID: " + report.getReportedListingId();
                case "CHAT":
                    return "Trò chuyện ID: " + report.getReportedChatRoomId();
                default:
                    return "Không xác định";
            }
        }
        
        private String getStatusDisplay(String status) {
            switch (status) {
                case "PENDING": return "⏳ Chờ xử lý";
                case "REVIEWED": return "👀 Đã xem xét";
                case "RESOLVED": return "✅ Đã giải quyết";
                case "DISMISSED": return "❌ Đã bỏ qua";
                default: return status;
            }
        }
        
        private void setStatusIndicator(String status) {
            Context context = itemView.getContext();
            int color;
            switch (status) {
                case "PENDING":
                    color = context.getColor(R.color.colorWarning);
                    break;
                case "REVIEWED":
                    color = context.getColor(R.color.colorInfo);
                    break;
                case "RESOLVED":
                    color = context.getColor(R.color.colorSuccess);
                    break;
                case "DISMISSED":
                    color = context.getColor(R.color.colorError);
                    break;
                default:
                    color = context.getColor(R.color.colorPrimary);
                    break;
            }
            statusIndicator.setBackgroundColor(color);
        }
        
        private void setupButtons(Report report) {
            // Always show details button
            btnDetails.setVisibility(View.VISIBLE);
            
            // Only show cancel button for pending reports
            if (report.isPending()) {
                btnCancel.setVisibility(View.VISIBLE);
            } else {
                btnCancel.setVisibility(View.GONE);
            }
            
            // Set click listeners
            btnDetails.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(report);
            });
            
            btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelReport(report);
            });
        }
        
        private String formatDate(String dateString) {
            try {
                // Simple date formatting - you can improve this
                if (dateString != null && dateString.length() >= 10) {
                    return dateString.substring(0, 10);
                }
                return dateString;
            } catch (Exception e) {
                return dateString != null ? dateString : "";
            }
        }
    }
}
