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
import com.example.ok.adapter.TransactionsAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionHistoryFragment extends Fragment {
    
    private static final String TAG = "TransactionHistoryFragment";
    
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TransactionsAdapter transactionsAdapter;
    private List<Transaction> transactionsList = new ArrayList<>();
    
    private ApiService apiService;
    private long userId;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_history, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initServices();
        initViews(view);
        setupRecyclerView();
        loadTransactions();
    }
    
    private void initServices() {
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();
    }
    
    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rvTransactions);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadTransactions);
    }
    
    private void setupRecyclerView() {
        transactionsAdapter = new TransactionsAdapter(requireContext(), transactionsList, 
            new TransactionsAdapter.OnTransactionActionListener() {
                @Override
                public void onCompleteTransaction(Transaction transaction) {
                    completeTransaction(transaction.getId());
                }
                
                @Override
                public void onCancelTransaction(Transaction transaction) {
                    cancelTransaction(transaction.getId());
                }
            });
        
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(transactionsAdapter);
    }
    
    private void loadTransactions() {
        swipeRefreshLayout.setRefreshing(true);
        
        Call<ApiResponse> call = apiService.getUserTransactions(userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Transaction>>(){}.getType();
                            List<Transaction> transactions = gson.fromJson(gson.toJsonTree(apiResponse.getData()), listType);
                            
                            transactionsList.clear();
                            transactionsList.addAll(transactions);
                            transactionsAdapter.notifyDataSetChanged();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing transactions data", e);
                            Toast.makeText(requireContext(), "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "No transactions found or API error: " + apiResponse.getMessage());
                        transactionsList.clear();
                        transactionsAdapter.notifyDataSetChanged();
                    }
                } else {
                    Log.e(TAG, "Error loading transactions: " + response.code());
                    Toast.makeText(requireContext(), "Lỗi tải lịch sử giao dịch", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Network error loading transactions", t);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void completeTransaction(Long transactionId) {
        Call<ApiResponse> call = apiService.completeTransaction(transactionId, userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã đánh dấu hoàn thành giao dịch", Toast.LENGTH_SHORT).show();
                        loadTransactions(); // Refresh the list
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Hoàn thành giao dịch thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Hoàn thành giao dịch thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error completing transaction", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi hoàn thành giao dịch", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void cancelTransaction(Long transactionId) {
        Call<ApiResponse> call = apiService.cancelTransaction(transactionId, userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã hủy giao dịch", Toast.LENGTH_SHORT).show();
                        loadTransactions(); // Refresh the list
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Hủy giao dịch thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Hủy giao dịch thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error cancelling transaction", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi hủy giao dịch", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
