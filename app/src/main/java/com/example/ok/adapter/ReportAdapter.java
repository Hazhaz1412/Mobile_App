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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying reports in admin moderation interface
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    
    private List<Report> reportList;
    private OnReportActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    
    public interface OnReportActionListener {
        void onReviewReport(Report report);
        void onResolveReport(Report report);
        void onDismissReport(Report report);
        void onViewDetails(Report report);
    }
    
    public ReportAdapter(List<Report> reportList, OnReportActionListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.bind(report);
    }
    
    @Override
    public int getItemCount() {
        return reportList.size();
    }
    
    class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportType, tvReportReason, tvReporterName, tvReportedItem, 
                 tvStatus, tvCreatedAt, tvDescription;
        Button btnReview, btnResolve, btnDismiss, btnDetails;
        View statusIndicator;
        
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvReportType = itemView.findViewById(R.id.tv_report_type);
            tvReportReason = itemView.findViewById(R.id.tv_report_reason);
            tvReporterName = itemView.findViewById(R.id.tv_reporter_name);
            tvReportedItem = itemView.findViewById(R.id.tv_reported_item);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
            tvDescription = itemView.findViewById(R.id.tv_description);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            
            btnReview = itemView.findViewById(R.id.btn_review);
            btnResolve = itemView.findViewById(R.id.btn_resolve);
            btnDismiss = itemView.findViewById(R.id.btn_dismiss);
            btnDetails = itemView.findViewById(R.id.btn_details);
        }
        
        public void bind(Report report) {
            // Set report type with icon
            String typeDisplay = getReportTypeDisplay(report.getReportType());
            tvReportType.setText(typeDisplay);
            
            // Set reason
            tvReportReason.setText(report.getReason());
            
            // Set reporter name
            tvReporterName.setText("Báo cáo bởi: " + 
                (report.getReporterName() != null ? report.getReporterName() : "Ẩn danh"));
            
            // Set reported item
            String reportedItem = getReportedItemDisplay(report);
            tvReportedItem.setText(reportedItem);
            
            // Set status
            String statusDisplay = getStatusDisplay(report.getStatus());
            tvStatus.setText(statusDisplay);
            setStatusIndicator(report.getStatus());
            
            // Set created date
            tvCreatedAt.setText(formatDate(report.getCreatedAt()));
            
            // Set description
            if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                tvDescription.setText(report.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }
            
            // Setup buttons based on status
            setupButtons(report);
        }
        
        private String getReportTypeDisplay(String type) {
            switch (type) {
                case "USER": return "👤 Người dùng";
                case "LISTING": return "📝 Tin đăng";
                case "CHAT": return "💬 Trò chuyện";
                default: return type;
            }
        }
        
        private String getReportedItemDisplay(Report report) {
            switch (report.getReportType()) {
                case "USER":
                    return "Người dùng: " + 
                        (report.getReportedUserName() != null ? report.getReportedUserName() : "ID: " + report.getReportedUserId());
                case "LISTING":
                    return "Tin đăng: " + 
                        (report.getReportedListingTitle() != null ? report.getReportedListingTitle() : "ID: " + report.getReportedListingId());
                case "CHAT":
                    return "Trò chuyện: ID " + report.getReportedChatRoomId();
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
            // Reset visibility
            btnReview.setVisibility(View.GONE);
            btnResolve.setVisibility(View.GONE);
            btnDismiss.setVisibility(View.GONE);
            btnDetails.setVisibility(View.VISIBLE);
            
            // Show buttons based on status
            switch (report.getStatus()) {
                case "PENDING":
                    btnReview.setVisibility(View.VISIBLE);
                    btnResolve.setVisibility(View.VISIBLE);
                    btnDismiss.setVisibility(View.VISIBLE);
                    break;
                case "REVIEWED":
                    btnResolve.setVisibility(View.VISIBLE);
                    btnDismiss.setVisibility(View.VISIBLE);
                    break;
                // RESOLVED and DISMISSED reports only show details button
            }
            
            // Set click listeners
            btnReview.setOnClickListener(v -> {
                if (listener != null) listener.onReviewReport(report);
            });
            
            btnResolve.setOnClickListener(v -> {
                if (listener != null) listener.onResolveReport(report);
            });
            
            btnDismiss.setOnClickListener(v -> {
                if (listener != null) listener.onDismissReport(report);
            });
            
            btnDetails.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(report);
            });
        }
        
        private String formatDate(String dateString) {
            try {
                // Assuming the date comes in ISO format from backend
                // You might need to adjust this based on your actual date format
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return dateFormat.format(date);
            } catch (Exception e) {
                return dateString; // Return as-is if parsing fails
            }
        }
    }
}
