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
import com.example.ok.adapter.OffersAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.model.OfferResponse;
import com.example.ok.model.Offer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OffersFragment extends Fragment {
    
    private static final String TAG = "OffersFragment";
    private static final String ARG_OFFER_TYPE = "offer_type";
    
    public static final int TYPE_SENT_OFFERS = 1; // Offers made by user (as buyer)
    public static final int TYPE_RECEIVED_OFFERS = 2; // Offers received by user (as seller)
    
    private RecyclerView rvOffers;
    private SwipeRefreshLayout swipeRefreshLayout;
    private OffersAdapter offersAdapter;
    private List<Offer> offersList = new ArrayList<>();
    
    private ApiService apiService;
    private int offerType;
    private long userId;
    
    public static OffersFragment newInstance(int offerType) {
        OffersFragment fragment = new OffersFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_OFFER_TYPE, offerType);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            offerType = getArguments().getInt(ARG_OFFER_TYPE, TYPE_SENT_OFFERS);
        }
        
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offers, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initServices();
        initViews(view);
        setupRecyclerView();
        loadOffers();
    }
    
    private void initServices() {
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();
    }
    
    private void initViews(View view) {
        rvOffers = view.findViewById(R.id.rvOffers);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadOffers);
    }
    
    private void setupRecyclerView() {
        offersAdapter = new OffersAdapter(requireContext(), offersList, offerType, 
            new OffersAdapter.OnOfferActionListener() {
                @Override
                public void onAcceptOffer(Offer offer) {
                    respondToOffer(offer.getId(), "ACCEPT", null, null);
                }
                
                @Override
                public void onRejectOffer(Offer offer) {
                    respondToOffer(offer.getId(), "REJECT", null, null);
                }
                
                @Override
                public void onCounterOffer(Offer offer, double counterAmount, String message) {
                    respondToOffer(offer.getId(), "COUNTER", counterAmount, message);
                }
                
                @Override
                public void onWithdrawOffer(Offer offer) {
                    withdrawOffer(offer.getId());
                }
            });
        
        rvOffers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOffers.setAdapter(offersAdapter);
    }
      private void loadOffers() {
        swipeRefreshLayout.setRefreshing(true);
        
        if (offerType == TYPE_SENT_OFFERS) {
            Call<PagedApiResponse<OfferResponse>> call = apiService.getOffersByBuyer(userId);
            call.enqueue(getOfferResponseCallback());
        } else {
            Call<PagedApiResponse<OfferResponse>> call = apiService.getOffersBySeller(userId);
            call.enqueue(getOfferResponseCallback());
        }
    }
    
    private Callback<PagedApiResponse<OfferResponse>> getOfferResponseCallback() {
        return new Callback<PagedApiResponse<OfferResponse>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<OfferResponse>> call, Response<PagedApiResponse<OfferResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<OfferResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        try {
                            offersList.clear();
                            for (OfferResponse offerResponse : apiResponse.getData()) {
                                offersList.add(offerResponse.toOffer());
                            }
                            offersAdapter.notifyDataSetChanged();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing offers data", e);
                            Toast.makeText(requireContext(), "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "No offers found or API error: " + apiResponse.getMessage());
                        offersList.clear();
                        offersAdapter.notifyDataSetChanged();
                    }
                } else {
                    Log.e(TAG, "Error loading offers: " + response.code());
                    Toast.makeText(requireContext(), "Lỗi tải danh sách offers", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<PagedApiResponse<OfferResponse>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Network error loading offers", t);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        };
    }
    
    private void respondToOffer(Long offerId, String action, Double counterAmount, String message) {
        com.example.ok.model.RespondToOfferRequest request = new com.example.ok.model.RespondToOfferRequest();
        request.setAction(action);
        if (counterAmount != null) {
            request.setCounterAmount(new java.math.BigDecimal(counterAmount));
        }
        request.setMessage(message);
        
        Call<ApiResponse> call = apiService.respondToOffer(offerId, userId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã phản hồi offer thành công", Toast.LENGTH_SHORT).show();
                        loadOffers(); // Refresh the list
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Phản hồi offer thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Phản hồi offer thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error responding to offer", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi phản hồi offer", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void withdrawOffer(Long offerId) {
        Call<ApiResponse> call = apiService.withdrawOffer(offerId, userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã rút offer thành công", Toast.LENGTH_SHORT).show();
                        loadOffers(); // Refresh the list
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Rút offer thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Rút offer thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error withdrawing offer", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi rút offer", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
