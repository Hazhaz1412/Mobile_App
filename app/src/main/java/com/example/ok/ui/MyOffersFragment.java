package com.example.ok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.ok.adapter.MyOffersAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.model.OfferResponse;
import com.example.ok.model.Offer;
import com.example.ok.model.WithdrawOfferRequest;
import com.example.ok.util.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyOffersFragment extends Fragment {
    private static final String TAG = "MyOffersFragment";
    
    private RecyclerView recyclerView;
    private MyOffersAdapter offersAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;
    private View emptyView;
    private View loadingView;
    
    private ApiService apiService;
    private SessionManager sessionManager;
    private long currentUserId;
    
    private List<Offer> allOffers = new ArrayList<>();
    private List<Offer> pendingOffers = new ArrayList<>();
    private List<Offer> acceptedOffers = new ArrayList<>();
    private List<Offer> rejectedOffers = new ArrayList<>();
    
    private int currentTab = 0; // 0: All, 1: Pending, 2: Accepted, 3: Rejected

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(requireContext());
        currentUserId = sessionManager.getUserId();
        
        Log.d(TAG, "MyOffers - Current User ID: " + currentUserId);
        Log.d(TAG, "MyOffers - Is Logged In: " + sessionManager.isLoggedIn());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_offers, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupSwipeRefresh();
        
        if (currentUserId != 0) {
            loadMyOffers();
        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem offers của bạn", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMyOffers);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayoutMyOffers);
        emptyView = view.findViewById(R.id.emptyView);
        loadingView = view.findViewById(R.id.loadingView);
    }
    
    private void setupRecyclerView() {
        offersAdapter = new MyOffersAdapter(requireContext(), new ArrayList<>(), this::onOfferClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(offersAdapter);
    }
    
    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Chờ duyệt"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã chấp nhận"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã từ chối"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterOffers();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadMyOffers);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent
        );
    }
    
    private void loadMyOffers() {
        showLoading(true);
        swipeRefreshLayout.setRefreshing(true);
        
        Log.d(TAG, "Loading offers for buyer ID: " + currentUserId);
        
        Call<PagedApiResponse<OfferResponse>> call = apiService.getOffersByBuyer(currentUserId);
        call.enqueue(new Callback<PagedApiResponse<OfferResponse>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<OfferResponse>> call, Response<PagedApiResponse<OfferResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<OfferResponse> apiResponse = response.body();                    if (apiResponse.isSuccess()) {
                        parseOffers(apiResponse);
                        // Check payment status for accepted offers
                        checkPaymentStatusForAcceptedOffers();
                        filterOffers();
                        Log.d(TAG, "Successfully loaded " + allOffers.size() + " offers for buyer");
                    } else {
                        showError(apiResponse.getMessage());
                        Log.e(TAG, "API error: " + apiResponse.getMessage());
                    }
                } else {
                    showError("Không thể tải danh sách offers của bạn");
                    Log.e(TAG, "Error loading buyer offers: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<PagedApiResponse<OfferResponse>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                showError("Lỗi kết nối: " + t.getMessage());
                Log.e(TAG, "Network error loading buyer offers", t);
            }
        });
    }
    
    private void parseOffers(PagedApiResponse<OfferResponse> response) {
        allOffers.clear();
        pendingOffers.clear();
        acceptedOffers.clear();
        rejectedOffers.clear();
        
        List<OfferResponse> offerResponses = response.getData();
        if (offerResponses != null) {
            for (OfferResponse offerResponse : offerResponses) {
                Offer offer = offerResponse.toOffer();
                allOffers.add(offer);
                
                // Filter by status
                String status = offer.getStatus();
                if (status != null) {
                    switch (status.toUpperCase()) {
                        case "PENDING":
                        case "COUNTERED":
                            pendingOffers.add(offer);
                            break;
                        case "ACCEPTED":
                            acceptedOffers.add(offer);
                            break;
                        case "REJECTED":
                        case "WITHDRAWN":
                            rejectedOffers.add(offer);
                            break;
                    }
                }
            }
        }
        
        Log.d(TAG, "Parsed buyer offers - Total: " + allOffers.size() + 
              ", Pending: " + pendingOffers.size() + 
              ", Accepted: " + acceptedOffers.size() + 
              ", Rejected: " + rejectedOffers.size());
    }
    
    private void filterOffers() {
        List<Offer> filteredOffers;
        switch (currentTab) {
            case 1: // Pending
                filteredOffers = pendingOffers;
                break;
            case 2: // Accepted
                filteredOffers = acceptedOffers;
                break;
            case 3: // Rejected
                filteredOffers = rejectedOffers;
                break;
            default: // All
                filteredOffers = allOffers;
                break;
        }
        
        offersAdapter.updateOffers(filteredOffers);
        
        // Show/hide empty view
        if (filteredOffers.isEmpty()) {
            showEmptyView();
        } else {
            hideEmptyView();
        }
    }
      private void onOfferClick(Offer offer) {
        // Show offer detail dialog for buyer view
        MyOfferDetailDialog dialog = new MyOfferDetailDialog(requireContext(), offer, new MyOfferDetailDialog.OnOfferActionListener() {
            @Override
            public void onWithdrawOffer(Offer offer) {
                withdrawOffer(offer);
            }
            
            @Override
            public void onViewListing(Offer offer) {
                // Navigate to listing detail
                // TODO: Implement navigation to listing detail
                Toast.makeText(requireContext(), "Xem chi tiết sản phẩm: " + offer.getListingTitle(), Toast.LENGTH_SHORT).show();            }
            
            @Override
            public void onBuyNow(Offer offer) {
                // Navigate to payment with accepted offer price
                navigateToPayment(offer);
            }
        });
        dialog.show();
    }
    
    private void navigateToPayment(Offer offer) {
        // Check if offer is accepted
        if (!"ACCEPTED".equalsIgnoreCase(offer.getStatus())) {
            Toast.makeText(requireContext(), "Chỉ có thể thanh toán khi offer đã được chấp nhận", Toast.LENGTH_SHORT).show();
            return;
        }
          Log.d(TAG, "Navigating to payment for offer: " + offer.getId() + 
                   ", price: " + offer.getAmount() + 
                   ", listing: " + offer.getListingTitle());
          // Start PaymentActivity with offer details
        Intent intent = new Intent(requireContext(), PaymentActivity.class);
        intent.putExtra("LISTING_ID", offer.getListingId());
        intent.putExtra("LISTING_TITLE", offer.getListingTitle());
        intent.putExtra("OFFER_ID", offer.getId());
        intent.putExtra("listingPrice", offer.getAmount().doubleValue()); // Fix: Use correct key
        intent.putExtra("IS_OFFER_PAYMENT", true); // Flag to indicate this is from accepted offer
        
        startActivity(intent);
          Toast.makeText(requireContext(), "Chuyển sang trang thanh toán với giá " + 
                       String.format("%.0f", offer.getAmount().doubleValue()) + " VND", Toast.LENGTH_SHORT).show();
    }
    
    private void checkPaymentStatusForAcceptedOffers() {
        for (Offer offer : acceptedOffers) {
            checkOfferPaymentStatus(offer);
        }
    }
    
    private void checkOfferPaymentStatus(Offer offer) {
        Call<ApiResponse> call = apiService.getTransactionByOfferId(offer.getId());
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Offer has a transaction - check if it's completed
                        try {
                            Map<String, Object> transactionData = (Map<String, Object>) apiResponse.getData();
                            String status = (String) transactionData.get("status");
                            if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                                offer.setHasPaidTransaction(true);
                                Log.d(TAG, "Offer " + offer.getId() + " has been paid");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing transaction data for offer " + offer.getId(), e);
                        }
                    }
                }
                // Update UI after checking payment status
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        offersAdapter.notifyDataSetChanged();
                    });
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error checking payment status for offer " + offer.getId(), t);
            }
        });
    }
    
    private void withdrawOffer(Offer offer) {
        if (!"PENDING".equalsIgnoreCase(offer.getStatus()) && 
            !"COUNTERED".equalsIgnoreCase(offer.getStatus())) {
            Toast.makeText(requireContext(), "Không thể rút lại offer này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        WithdrawOfferRequest request = new WithdrawOfferRequest(offer.getId());
        
        Call<ApiResponse> call = apiService.withdrawOffer(request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã rút lại offer thành công", Toast.LENGTH_SHORT).show();
                        loadMyOffers(); // Reload data
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể rút lại offer", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error withdrawing offer", t);
            }
        });
    }
    
    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
    }
    
    private void hideEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }
        recyclerView.setVisibility(View.VISIBLE);
    }
    
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
