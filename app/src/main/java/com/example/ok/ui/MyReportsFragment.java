package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.R;
import com.example.ok.adapter.MyReportAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Report;
import com.example.ok.api.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment to display user's submitted reports
 */
public class MyReportsFragment extends Fragment {
    
    private static final String TAG = "MyReports";
    
    private RecyclerView recyclerView;
    private MyReportAdapter reportAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    
    private ApiService apiService;
    private List<Report> reportList = new ArrayList<>();
    private long currentUserId;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_reports, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        
        // Get current user ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getLong("user_id", -1);
        
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return view;
        }
        
        apiService = RetrofitClient.getApiService();
        
        loadMyReports();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_my_reports);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);
    }
    
    private void setupRecyclerView() {
        reportAdapter = new MyReportAdapter(reportList, new MyReportAdapter.OnReportActionListener() {
            @Override
            public void onViewDetails(Report report) {
                showReportDetails(report);
            }
            
            @Override
            public void onCancelReport(Report report) {
                // Only allow canceling pending reports
                if (report.isPending()) {
                    showCancelReportDialog(report);
                }
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(reportAdapter);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadMyReports);
    }
    
    private void loadMyReports() {
        swipeRefreshLayout.setRefreshing(true);
        
        Call<ApiResponse> call = apiService.getMyReports(currentUserId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded() || getContext() == null) return;
                
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // Parse reports from response
                        // This would need to be implemented based on your API response format
                        updateReportsList(new ArrayList<>()); // Placeholder
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Không thể tải báo cáo", 
                            Toast.LENGTH_SHORT).show();
                        showEmptyView();
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi kết nối: " + response.code(), Toast.LENGTH_SHORT).show();
                    showEmptyView();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyView();
            }
        });
    }
    
    private void updateReportsList(List<Report> reports) {
        reportList.clear();
        reportList.addAll(reports);
        reportAdapter.notifyDataSetChanged();
        
        if (reports.isEmpty()) {
            showEmptyView();
        } else {
            hideEmptyView();
        }
    }
    
    private void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
    
    private void hideEmptyView() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }
    
    private void showReportDetails(Report report) {
        // Create a detailed view dialog
        StringBuilder details = new StringBuilder();
        details.append("Loại báo cáo: ").append(getReportTypeDisplay(report.getReportType())).append("\n\n");
        details.append("Lý do: ").append(report.getReason()).append("\n\n");
        
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            details.append("Mô tả: ").append(report.getDescription()).append("\n\n");
        }
        
        details.append("Trạng thái: ").append(getStatusDisplay(report.getStatus())).append("\n\n");
        details.append("Ngày gửi: ").append(formatDate(report.getCreatedAt()));
        
        if (report.getReviewedAt() != null) {
            details.append("\nNgày xem xét: ").append(formatDate(report.getReviewedAt()));
        }
        
        if (report.getReviewNotes() != null && !report.getReviewNotes().isEmpty()) {
            details.append("\nGhi chú của admin: ").append(report.getReviewNotes());
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Chi tiết báo cáo")
                .setMessage(details.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }
    
    private void showCancelReportDialog(Report report) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hủy báo cáo")
                .setMessage("Bạn có chắc chắn muốn hủy báo cáo này?")
                .setPositiveButton("Hủy báo cáo", (dialog, which) -> {
                    // Implementation would depend on your API
                    Toast.makeText(requireContext(), "Tính năng hủy báo cáo sẽ được cập nhật", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Không", null)
                .show();
    }
    
    private String getReportTypeDisplay(String type) {
        switch (type) {
            case "USER": return "Người dùng";
            case "LISTING": return "Tin đăng";
            case "CHAT": return "Trò chuyện";
            default: return type;
        }
    }
    
    private String getStatusDisplay(String status) {
        switch (status) {
            case "PENDING": return "Chờ xử lý";
            case "REVIEWED": return "Đã xem xét";
            case "RESOLVED": return "Đã giải quyết";
            case "DISMISSED": return "Đã bỏ qua";
            default: return status;
        }
    }
    
    private String formatDate(String dateString) {
        // Simple date formatting - implement proper formatting as needed
        if (dateString != null) {
            try {
                return dateString.substring(0, 10); // Extract date part
            } catch (Exception e) {
                return dateString;
            }
        }
        return "";
    }
}
