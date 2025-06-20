package com.example.ok.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.R;
import com.example.ok.adapter.ReportAdapter;
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
 * Fragment for admin to manage reports and moderation
 */
public class AdminModerationFragment extends Fragment {
    
    private static final String TAG = "AdminModeration";
    
    private RecyclerView recyclerView;
    private ReportAdapter reportAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner spinnerStatus, spinnerType;
    private Button btnRefresh;
    
    private ApiService apiService;
    private List<Report> reportList = new ArrayList<>();
    
    private String selectedStatus = "PENDING";
    private String selectedType = "ALL";
    private int currentPage = 0;
    private boolean isLoading = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_moderation, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupSpinners();
        setupSwipeRefresh();
        
        apiService = RetrofitClient.getApiService();
        
        loadReports();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_reports);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        spinnerStatus = view.findViewById(R.id.spinner_status);
        spinnerType = view.findViewById(R.id.spinner_type);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        
        btnRefresh.setOnClickListener(v -> {
            currentPage = 0;
            loadReports();
        });
    }
    
    private void setupRecyclerView() {
        reportAdapter = new ReportAdapter(reportList, new ReportAdapter.OnReportActionListener() {
            @Override
            public void onReviewReport(Report report) {
                showReviewDialog(report);
            }
            
            @Override
            public void onResolveReport(Report report) {
                showResolveDialog(report);
            }
            
            @Override
            public void onDismissReport(Report report) {
                showDismissDialog(report);
            }
            
            @Override
            public void onViewDetails(Report report) {
                showReportDetails(report);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(reportAdapter);
        
        // Add pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreReports();
                    }
                }
            }
        });
    }
    
    private void setupSpinners() {
        // Status spinner
        String[] statusItems = {"Tất cả", "Chờ xử lý", "Đã xem xét", "Đã giải quyết", "Đã bỏ qua"};
        String[] statusValues = {"ALL", "PENDING", "REVIEWED", "RESOLVED", "DISMISSED"};
        
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, statusItems);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusValues[position];
                currentPage = 0;
                loadReports();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Type spinner
        String[] typeItems = {"Tất cả", "Người dùng", "Tin đăng", "Trò chuyện"};
        String[] typeValues = {"ALL", "USER", "LISTING", "CHAT"};
        
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, typeItems);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = typeValues[position];
                currentPage = 0;
                loadReports();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 0;
            loadReports();
        });
    }
    
    private void loadReports() {
        if (isLoading) return;
        
        isLoading = true;
        swipeRefreshLayout.setRefreshing(true);
        
        String status = selectedStatus.equals("ALL") ? null : selectedStatus;
        String type = selectedType.equals("ALL") ? null : selectedType;
        
        // Note: This would need to be implemented in the backend
        // For now, we'll show a placeholder
        Toast.makeText(getContext(), "Đang tải báo cáo...", Toast.LENGTH_SHORT).show();
        
        // Simulate API call
        new android.os.Handler().postDelayed(() -> {
            if (currentPage == 0) {
                reportList.clear();
            }
            
            // Add some dummy data for demonstration
            // In real implementation, this would come from the API
            
            reportAdapter.notifyDataSetChanged();
            isLoading = false;
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }
    
    private void loadMoreReports() {
        currentPage++;
        loadReports();
    }
    
    private void showReviewDialog(Report report) {
        // Implementation for reviewing a report
        Toast.makeText(getContext(), "Đánh dấu đã xem xét báo cáo", Toast.LENGTH_SHORT).show();
    }
    
    private void showResolveDialog(Report report) {
        // Implementation for resolving a report
        Toast.makeText(getContext(), "Giải quyết báo cáo", Toast.LENGTH_SHORT).show();
    }
    
    private void showDismissDialog(Report report) {
        // Implementation for dismissing a report
        Toast.makeText(getContext(), "Bỏ qua báo cáo", Toast.LENGTH_SHORT).show();
    }
    
    private void showReportDetails(Report report) {
        // Implementation for showing report details
        Toast.makeText(getContext(), "Chi tiết báo cáo", Toast.LENGTH_SHORT).show();
    }
}
