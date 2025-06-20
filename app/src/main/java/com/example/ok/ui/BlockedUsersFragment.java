package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.R;
import com.example.ok.adapter.BlockedUsersAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.BlockedUser;
import com.example.ok.utils.BlockedUserFilter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment hiển thị danh sách người dùng đã chặn và cho phép bỏ chặn
 */
public class BlockedUsersFragment extends Fragment {
    private static final String TAG = "BlockedUsersFragment";
      // UI Components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    
    // Adapter
    private BlockedUsersAdapter adapter;
    
    // Data
    private List<BlockedUser> blockedUsers = new ArrayList<>();
    private boolean isLoading = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocked_users, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        
        loadBlockedUsers();
        
        return view;
    }
      private void initViews(View view) {
        // Back button
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
          recyclerView = view.findViewById(R.id.recyclerBlockedUsers);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Layout states
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);
        Button btnRetry = view.findViewById(R.id.btnRetry);
        
        btnRetry.setOnClickListener(v -> loadBlockedUsers());
    }
      private void setupRecyclerView() {
        adapter = new BlockedUsersAdapter(getContext(), blockedUsers);
        adapter.setOnUnblockClickListener(this::showUnblockConfirmDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadBlockedUsers);
    }
    
    private void loadBlockedUsers() {
        if (isLoading) return;
        
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Không thể xác định người dùng hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isLoading = true;
        showLoading();
        
        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.getBlockedUsers(currentUserId);
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded() || getContext() == null) return;
                
                isLoading = false;
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        parseBlockedUsers(apiResponse.getData());
                    } else {
                        showEmptyState();
                    }
                } else {
                    Log.e(TAG, "Failed to load blocked users: " + response.code());
                    Toast.makeText(getContext(), "Không thể tải danh sách người dùng đã chặn", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                
                isLoading = false;
                hideLoading();
                
                Log.e(TAG, "Error loading blocked users", t);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private void parseBlockedUsers(Object data) {
        blockedUsers.clear();
        
        try {
            if (data instanceof List) {
                List<?> userList = (List<?>) data;
                for (Object userObj : userList) {
                    BlockedUser blockedUser = parseBlockedUser(userObj);
                    if (blockedUser != null) {
                        blockedUsers.add(blockedUser);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing blocked users", e);
        }
        
        adapter.updateList(blockedUsers);
        
        if (blockedUsers.isEmpty()) {
            showEmptyState();
        } else {
            showContent();
        }
        
        Log.d(TAG, "Loaded " + blockedUsers.size() + " blocked users");
    }
    
    private BlockedUser parseBlockedUser(Object userObj) {
        try {
            if (userObj instanceof java.util.Map) {
                java.util.Map<?, ?> userMap = (java.util.Map<?, ?>) userObj;
                
                Long userId = extractLong(userMap.get("id"));
                String displayName = extractString(userMap.get("displayName"));
                String profilePicture = extractString(userMap.get("profilePicture"));
                String blockedAt = extractString(userMap.get("blockedAt"));
                  if (userId != null && displayName != null) {
                    // Parse date if available
                    Date parsedDate = null;
                    if (blockedAt != null) {
                        try {
                            // Try to parse ISO date format
                            parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault()).parse(blockedAt);
                        } catch (Exception e) {
                            try {
                                // Try alternative format
                                parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(blockedAt);
                            } catch (Exception e2) {
                                // Use current date as fallback
                                parsedDate = new Date();
                            }
                        }
                    }
                    return new BlockedUser(userId, displayName, profilePicture, parsedDate);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing blocked user: " + userObj, e);
        }
        return null;
    }
    
    private Long extractLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractString(Object obj) {
        return obj != null ? obj.toString() : null;
    }
      private void showUnblockConfirmDialog(BlockedUser user, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bỏ chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn bỏ chặn " + user.getDisplayName() + "?")
                .setPositiveButton("Bỏ chặn", (dialog, which) -> unblockUser(user, position))
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void unblockUser(BlockedUser user, int position) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Không thể xác định người dùng hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }
          ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.unblockUser(user.getUserId());
        
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded() || getContext() == null) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // Remove from adapter
                        adapter.removeUser(position);
                        
                        // Update BlockedUserFilter
                        BlockedUserFilter.getInstance(getContext()).removeBlockedUser(user.getUserId());
                        
                        Toast.makeText(getContext(), "Đã bỏ chặn " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                        
                        if (blockedUsers.isEmpty()) {
                            showEmptyState();
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể bỏ chặn: " + apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Không thể bỏ chặn người dùng", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                
                Log.e(TAG, "Error unblocking user", t);
                Toast.makeText(getContext(), "Lỗi bỏ chặn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private Long getCurrentUserId() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);
        return userId == -1 ? null : userId;
    }
      private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }
    
    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }
    
    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
    }
    
    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }
    
    private void showErrorState() {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
    }
}
