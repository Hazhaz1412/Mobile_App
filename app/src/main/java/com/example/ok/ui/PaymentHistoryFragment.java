package com.example.ok.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.R;
import com.example.ok.adapter.PaymentHistoryAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Payment;
import com.example.ok.model.Transaction;
import com.example.ok.model.CancelPaymentRequest;
import com.example.ok.model.Listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentHistoryFragment extends Fragment {
    private static final String TAG = "PaymentHistoryFragment";
    
    // UI Components
    private RecyclerView rvPaymentHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    
    // Data
    private List<Payment> payments = new ArrayList<>();
    private PaymentHistoryAdapter adapter;
    private ApiService apiService;
    private Long currentUserId;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    public static PaymentHistoryFragment newInstance() {
        return new PaymentHistoryFragment();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payment_history, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initData();
        setupRecyclerView();
        loadPaymentHistory(false);
    }
    
    private void initViews(View view) {
        rvPaymentHistory = view.findViewById(R.id.rvPaymentHistory);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        swipeRefreshLayout.setOnRefreshListener(() -> refreshPaymentHistory());
    }
    
    private void initData() {
        apiService = RetrofitClient.getApiService();
        
        // Get current user ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", 
                android.content.Context.MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
    }
      private void setupRecyclerView() {        adapter = new PaymentHistoryAdapter(payments, new PaymentHistoryAdapter.OnPaymentClickListener() {
            @Override
            public void onPaymentClicked(Payment payment) {
                onPaymentItemClicked(payment);
            }
            
            @Override
            public void onCancelPayment(Payment payment) {
                onCancelPaymentClicked(payment);
            }
            
            @Override
            public void onRateUser(Payment payment) {
                onRateUserClicked(payment);
            }
        });
        rvPaymentHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPaymentHistory.setAdapter(adapter);
        
        // Add scroll listener for pagination
        rvPaymentHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadPaymentHistory(true);
                    }
                }
            }
        });
    }
    
    private void refreshPaymentHistory() {
        currentPage = 0;
        hasMoreData = true;
        payments.clear();
        adapter.notifyDataSetChanged();
        loadPaymentHistory(false);
    }
      private void loadPaymentHistory(boolean isLoadMore) {
        if (isLoading) return;
        
        isLoading = true;
        
        if (!isLoadMore) {
            progressBar.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        
        Call<List<Payment>> call = apiService.getPaymentHistory(currentUserId, currentPage, 20);
        call.enqueue(new Callback<List<Payment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Payment>> call, @NonNull Response<List<Payment>> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Payment> newPayments = response.body();
                    handlePaymentHistoryResponse(newPayments, isLoadMore);
                    Log.d(TAG, "Loaded " + newPayments.size() + " payment records from API");
                } else {
                    Log.w(TAG, "Failed to load payment history: " + response.code());
                    showEmptyState();
                    Toast.makeText(requireContext(), 
                            "Lỗi tải lịch sử thanh toán: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<Payment>> call, @NonNull Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                
                Log.e(TAG, "Error loading payment history", t);
                showEmptyState();
                Toast.makeText(requireContext(), 
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }      private void handlePaymentHistoryResponse(List<Payment> newPayments, boolean isLoadMore) {
        try {
            if (newPayments == null) {
                newPayments = new ArrayList<>();
            }
            
            if (!isLoadMore) {
                payments.clear();
            }
            
            if (newPayments.isEmpty()) {
                hasMoreData = false;
                if (payments.isEmpty()) {
                    showEmptyState();
                }
            } else {
                payments.addAll(newPayments);
                currentPage++;
                adapter.notifyDataSetChanged();
                
                if (newPayments.size() < 20) {
                    hasMoreData = false;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling payment history response", e);
            // Fallback to sample data for demo
            if (payments.isEmpty()) {
                List<Payment> samplePayments = createSamplePayments();
                payments.addAll(samplePayments);
                adapter.notifyDataSetChanged();
            } else {
                showEmptyState();
            }
        }
    }
    
    private List<Payment> createSamplePayments() {
        // This is sample data - replace with actual API response parsing
        List<Payment> samplePayments = new ArrayList<>();
        
        if (currentPage == 0) {
            Payment payment1 = new Payment(1L, 101L, currentUserId, 201L, 
                    "MOMO", 150000.0, "Thanh toán iPhone 12");
            payment1.setId(1L);
            payment1.setStatus(Payment.STATUS_COMPLETED);
            payment1.setListingTitle("iPhone 12 Pro Max");
            payment1.setSellerName("Nguyễn Văn A");
            payment1.setTransactionId("TXN001");
            
            Payment payment2 = new Payment(2L, 102L, currentUserId, 202L, 
                    "VISA", 2500000.0, "Thanh toán Laptop Gaming");
            payment2.setId(2L);
            payment2.setStatus(Payment.STATUS_PROCESSING);
            payment2.setListingTitle("Laptop Gaming Asus ROG");
            payment2.setSellerName("Trần Thị B");
            payment2.setTransactionId("TXN002");
            
            Payment payment3 = new Payment(3L, 103L, currentUserId, 203L, 
                    "CASH", 500000.0, "Thanh toán Áo khoác");
            payment3.setId(3L);
            payment3.setStatus(Payment.STATUS_COMPLETED);
            payment3.setListingTitle("Áo khoác Nike");
            payment3.setSellerName("Lê Văn C");
            payment3.setTransactionId("TXN003");
            
            samplePayments.add(payment1);
            samplePayments.add(payment2);
            samplePayments.add(payment3);
        }
        
        return samplePayments;
    }
    
    private void showEmptyState() {
        if (payments.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Chưa có giao dịch nào");
        }
    }
    
    private void onPaymentItemClicked(Payment payment) {
        // Show payment details dialog or navigate to detail screen
        showPaymentDetailsDialog(payment);
    }
    
    private void showPaymentDetailsDialog(Payment payment) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_details, null);
        
        // Populate dialog with payment details
        TextView tvTitle = dialogView.findViewById(R.id.tvPaymentTitle);
        TextView tvAmount = dialogView.findViewById(R.id.tvPaymentAmount);
        TextView tvStatus = dialogView.findViewById(R.id.tvPaymentStatus);
        TextView tvMethod = dialogView.findViewById(R.id.tvPaymentMethod);
        TextView tvTransactionId = dialogView.findViewById(R.id.tvTransactionId);
        TextView tvDate = dialogView.findViewById(R.id.tvPaymentDate);
        
        tvTitle.setText(payment.getListingTitle());
        tvAmount.setText(String.format("%,.0f VNĐ", payment.getAmount()));
        tvStatus.setText(getStatusText(payment.getStatus()));
        tvMethod.setText(getPaymentMethodText(payment.getPaymentMethodType()));
        tvTransactionId.setText("ID: " + payment.getTransactionId());
        
        if (payment.getCreatedAt() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", 
                    java.util.Locale.getDefault());
            tvDate.setText(sdf.format(payment.getCreatedAt()));
        }
        
        builder.setView(dialogView)
                .setTitle("Chi tiết giao dịch")
                .setPositiveButton("Đóng", null);
        
        builder.create().show();
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
      private String getPaymentMethodText(String method) {
        switch (method) {
            case "MOMO":
                return "Ví MoMo";
            case "VISA":
                return "Thẻ Visa";
            case "MASTERCARD":
                return "Thẻ Mastercard";
            case "CASH":
                return "Tiền mặt (COD)";
            default:
                return method;
        }
    }
    
    private void onCancelPaymentClicked(Payment payment) {
        // Kiểm tra xem có thể hủy đơn hàng không
        if (!canCancelPayment(payment)) {
            Toast.makeText(requireContext(), "Không thể hủy đơn hàng ở trạng thái hiện tại", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Hiển thị dialog xác nhận hủy
        showCancelPaymentDialog(payment);
    }
    
    private boolean canCancelPayment(Payment payment) {
        String status = payment.getStatus();
        return Payment.STATUS_PENDING.equals(status) || Payment.STATUS_PROCESSING.equals(status);
    }
    
    private void showCancelPaymentDialog(Payment payment) {
        androidx.appcompat.app.AlertDialog.Builder builder = 
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        
        // Tạo custom view cho dialog
        View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        
        // Tạo EditText để nhập lý do hủy
        android.widget.EditText etReason = new android.widget.EditText(requireContext());
        etReason.setHint("Nhập lý do hủy đơn hàng (không bắt buộc)");
        etReason.setMinLines(2);
        etReason.setMaxLines(4);
        
        builder.setTitle("Xác nhận hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #" + payment.getTransactionId() + "?")
                .setView(etReason)
                .setPositiveButton("Hủy đơn hàng", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "Không có lý do";
                    }
                    cancelPayment(payment, reason);
                })
                .setNegativeButton("Không", null);
        
        builder.create().show();
    }
    
    private void cancelPayment(Payment payment, String reason) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Tạo request với userId từ SharedPreferences
        CancelPaymentRequest request = new CancelPaymentRequest(currentUserId, reason, "BUYER");
        
        Call<java.util.Map<String, Object>> call = apiService.cancelPayment(payment.getId(), currentUserId);
        call.enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String, Object>> call, 
                    Response<java.util.Map<String, Object>> response) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");
                    String message = (String) result.get("message");
                    
                    if (Boolean.TRUE.equals(success)) {
                        Toast.makeText(requireContext(), "Hủy đơn hàng thành công", 
                                Toast.LENGTH_SHORT).show();
                        
                        // Cập nhật trạng thái payment trong danh sách
                        payment.setStatus(Payment.STATUS_CANCELLED);
                        adapter.notifyDataSetChanged();
                        
                        // Hoặc refresh lại toàn bộ danh sách
                        refreshPaymentHistory();
                    } else {
                        Toast.makeText(requireContext(), 
                                message != null ? message : "Không thể hủy đơn hàng", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Cancel payment failed: " + response.code());
                    Toast.makeText(requireContext(), "Lỗi khi hủy đơn hàng", 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<java.util.Map<String, Object>> call, Throwable t) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                Log.e(TAG, "Cancel payment error", t);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }    private void onRateUserClicked(Payment payment) {
        // Check if we have seller ID from payment
        if (payment.getSellerId() != null && payment.getSellerId() != -1) {
            // Navigate directly to RatingActivity
            navigateToRatingActivity(payment, payment.getSellerId(), payment.getSellerName());
        } else if (payment.getListingId() != null) {
            // Fetch listing details to get seller information
            fetchListingForRating(payment);
        } else {
            Toast.makeText(requireContext(), "Không thể tải thông tin người bán", Toast.LENGTH_SHORT).show();
        }
    }
      private void fetchListingForRating(Payment payment) {
        // Show loading
        // You might want to add a progress indicator here
        
        apiService.getListingDetail(payment.getListingId()).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Parse the listing data from ApiResponse
                        try {
                            // The data is a Map containing listing information
                            Map<String, Object> listingData = (Map<String, Object>) apiResponse.getData();
                            Long sellerId = null;
                            String sellerName = null;
                            
                            if (listingData.get("userId") instanceof Number) {
                                sellerId = ((Number) listingData.get("userId")).longValue();
                            }
                            if (listingData.get("userDisplayName") instanceof String) {
                                sellerName = (String) listingData.get("userDisplayName");
                            }
                            
                            if (sellerId != null && sellerId != -1) {
                                navigateToRatingActivity(payment, sellerId, sellerName);
                            } else {
                                Toast.makeText(requireContext(), "Không tìm thấy thông tin người bán", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("PaymentHistory", "Error parsing listing data: " + e.getMessage());
                            Toast.makeText(requireContext(), "Lỗi xử lý thông tin người bán", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Không thể tải thông tin người bán", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải thông tin người bán", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("PaymentHistory", "Failed to fetch listing: " + t.getMessage());
                Toast.makeText(requireContext(), "Lỗi kết nối khi tải thông tin người bán", Toast.LENGTH_SHORT).show();
            }
        });
    }    private void navigateToRatingActivity(Payment payment, Long sellerId, String sellerName) {
        // Add debug logging
        Log.d("PaymentHistory", "NavigateToRating - PaymentId: " + payment.getId() + 
              ", SellerId: " + sellerId + ", SellerName: " + sellerName);
              
        // Navigate to RatingActivity
        Intent intent = new Intent(requireContext(), RatingActivity.class);
        intent.putExtra("listingId", payment.getListingId()); // Pass listing ID instead of transaction ID
        intent.putExtra("ratedUserId", sellerId != null ? sellerId : -1L);
        intent.putExtra("ratedUserName", sellerName != null ? sellerName : "Người bán");
        intent.putExtra("listingTitle", payment.getListingTitle());
        intent.putExtra("isRatingBuyer", false); // Rating the seller
        
        startActivity(intent);
    }
}
