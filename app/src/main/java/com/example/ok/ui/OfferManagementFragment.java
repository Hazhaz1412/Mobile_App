package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.ok.adapter.OfferAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.model.OfferResponse;
import com.example.ok.model.Offer;
import com.example.ok.model.RespondToOfferRequest;
import com.example.ok.util.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferManagementFragment extends Fragment {
    private static final String TAG = "OfferManagementFragment";
    
    private RecyclerView recyclerView;
    private OfferAdapter offerAdapter;
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
    
    private int currentTab = 0; // 0: All, 1: Pending, 2: Accepted, 3: Rejected    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(requireContext());
        currentUserId = sessionManager.getUserId();
        
        Log.d(TAG, "OfferManagement - Current User ID: " + currentUserId);
        Log.d(TAG, "OfferManagement - Is Logged In: " + sessionManager.isLoggedIn());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offer_management, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupSwipeRefresh();
          if (currentUserId != 0) {
            loadOffers();
        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem offers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewOffers);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayoutOffers);
        emptyView = view.findViewById(R.id.emptyView);
        loadingView = view.findViewById(R.id.loadingView);
    }
    
    private void setupRecyclerView() {
        offerAdapter = new OfferAdapter(requireContext(), new ArrayList<>(), this::onOfferClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(offerAdapter);
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
        swipeRefreshLayout.setOnRefreshListener(this::loadOffers);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent
        );
    }
      private void loadOffers() {
        showLoading(true);
        
        Call<PagedApiResponse<OfferResponse>> call = apiService.getOffersBySeller(currentUserId);
        call.enqueue(new Callback<PagedApiResponse<OfferResponse>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<OfferResponse>> call, Response<PagedApiResponse<OfferResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<OfferResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        parseOffers(apiResponse);
                        filterOffers();
                    } else {
                        showError(apiResponse.getMessage());
                    }
                } else {
                    showError("Không thể tải danh sách offers");
                    Log.e(TAG, "Error loading offers: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<PagedApiResponse<OfferResponse>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showLoading(false);
                showError("Lỗi kết nối: " + t.getMessage());
                Log.e(TAG, "Network error loading offers", t);
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
                            pendingOffers.add(offer);
                            break;
                        case "ACCEPTED":
                            acceptedOffers.add(offer);
                            break;
                        case "REJECTED":
                            rejectedOffers.add(offer);
                            break;
                        // Note: COUNTERED offers are still considered pending for UI purposes
                        case "COUNTERED":
                            pendingOffers.add(offer);
                            break;
                    }
                }
            }
        }
        
        Log.d(TAG, "Parsed offers - Total: " + allOffers.size() + 
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
        
        offerAdapter.updateOffers(filteredOffers);
        
        // Show/hide empty view
        if (filteredOffers.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
      private void onOfferClick(Offer offer) {
        // Show offer details dialog
        OfferDetailDialog dialog = OfferDetailDialog.newInstance(offer);
        dialog.setOnOfferResponseListener(new OfferDetailDialog.OnOfferResponseListener() {
            @Override
            public void onOfferResponse(Long offerId, String action, String message) {
                OfferManagementFragment.this.onOfferResponse(offerId, action, message);
            }
            
            @Override
            public void onOfferResponse(Long offerId, String action, String message, BigDecimal counterAmount) {
                OfferManagementFragment.this.onOfferResponse(offerId, action, message, counterAmount);
            }
        });
        dialog.show(getParentFragmentManager(), "OfferDetailDialog");
    }
      private void onOfferResponse(Long offerId, String action, String message) {
        // Handle offer response (accept, reject, counter)
        RespondToOfferRequest request = new RespondToOfferRequest(action, message);
        
        Call<ApiResponse> call = apiService.respondToOffer(offerId, currentUserId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), 
                                      "Phản hồi offer thành công!", 
                                      Toast.LENGTH_SHORT).show();
                        loadOffers(); // Refresh the list
                    } else {
                        showError(apiResponse.getMessage());
                    }
                } else {
                    showError("Không thể phản hồi offer");
                    Log.e(TAG, "Error responding to offer: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showError("Lỗi kết nối: " + t.getMessage());
                Log.e(TAG, "Network error responding to offer", t);
            }
        });
    }
    
    private void onOfferResponse(Long offerId, String action, String message, BigDecimal counterAmount) {
        // Handle counter offer
        RespondToOfferRequest request = new RespondToOfferRequest(action, counterAmount, message);
        
        Call<ApiResponse> call = apiService.respondToOffer(offerId, currentUserId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), 
                                      "Counter offer gửi thành công!", 
                                      Toast.LENGTH_SHORT).show();
                        loadOffers(); // Refresh the list
                    } else {
                        showError(apiResponse.getMessage());
                    }
                } else {
                    showError("Không thể gửi counter offer");
                    Log.e(TAG, "Error sending counter offer: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                showError("Lỗi kết nối: " + t.getMessage());
                Log.e(TAG, "Network error sending counter offer", t);
            }
        });
    }
    
    private void showLoading(boolean show) {
        if (show) {
            loadingView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            loadingView.setVisibility(View.GONE);
        }
    }
    
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    public static OfferManagementFragment newInstance() {
        return new OfferManagementFragment();
    }
}
