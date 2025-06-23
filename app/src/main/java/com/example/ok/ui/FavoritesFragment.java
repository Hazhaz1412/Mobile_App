package com.example.ok.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.R;
import com.example.ok.adapter.ListingAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.Listing;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.ui.HomeFragment;
import com.example.ok.ui.ListingDetailFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

/**
 * Fragment hiển thị danh sách sản phẩm yêu thích của user
 */
public class FavoritesFragment extends Fragment {
    private static final String TAG = "FavoritesFragment";    // UI Components
    private RecyclerView recyclerFavorites;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private View layoutEmptyState;
    private Button btnRetryFavorites;
    private Button fabAddListing;
    
    // Data
    private List<Listing> favoritesList = new ArrayList<>();
    private ListingAdapter favoritesAdapter;
    private long userId = -1;
    
    // API
    private ApiService apiService;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
        
        // Initialize API service
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();
        
        Log.d(TAG, "FavoritesFragment created for userId: " + userId);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
          initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();
        setupFAB();
        setupRetryButton();
        
        // Load favorites
        if (userId != -1) {
            // First test other APIs to confirm authentication works
            testAuthentication();
            loadFavorites();
        } else {
            showNotLoggedInState();
        }
    }
    
    /**
     * Test authentication with other APIs to confirm auth works
     */
    private void testAuthentication() {
        Log.d(TAG, "🧪 Testing authentication with other APIs first...");        // Test with getUserListings API which should work
        apiService.getUserListings(userId, "ACTIVE", 0, 5).enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Response<PagedApiResponse<Listing>> response) {
                Log.d(TAG, "🧪 Auth test with getUserListings: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Authentication works with other APIs");
                } else {
                    Log.e(TAG, "❌ Authentication issue with all APIs: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Throwable t) {
                Log.e(TAG, "🧪 Auth test network error", t);
            }
        });
    }      private void initViews(View view) {
        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnRetryFavorites = view.findViewById(R.id.btnRetryFavorites);
        fabAddListing = view.findViewById(R.id.fabAddListing);
    }
      private void setupRecyclerView() {
        // Setup GridLayoutManager với 2 cột
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        recyclerFavorites.setLayoutManager(layoutManager);
        
        // Setup adapter với grid layout
        favoritesAdapter = new ListingAdapter(requireContext(), true);
        recyclerFavorites.setAdapter(favoritesAdapter);
        
        // Set click listener
        favoritesAdapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                openListingDetail(listing);
            }
            
            @Override
            public void onListingLongClick(Listing listing) {
                // Handle long click if needed
            }
        });
        
        Log.d(TAG, "RecyclerView setup completed");
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            Log.d(TAG, "Pull to refresh triggered");
            loadFavorites();
        });        // Set refresh colors
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_red_light,
            android.R.color.holo_green_light,
            android.R.color.holo_blue_light
        );
    }
    
    private void setupFAB() {
        if (fabAddListing != null) {
            fabAddListing.setOnClickListener(v -> {
                // Navigate to CreateListingActivity or AddListingFragment
                try {
                    // Navigate to Home tab and trigger add listing
                    requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                    
                    // Show message to user
                    Toast.makeText(requireContext(), "Chuyển đến trang chủ để đăng sản phẩm", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to home", e);
                    Toast.makeText(requireContext(), "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void setupRetryButton() {
        if (btnRetryFavorites != null) {
            btnRetryFavorites.setOnClickListener(v -> {
                Log.d(TAG, "User clicked retry button");
                loadFavorites();
            });
        }
    }

    /**
     * Load user's favorite listings from API
     */
    private void loadFavorites() {
        if (userId == -1) {
            showNotLoggedInState();
            return;
        }
        
        Log.d(TAG, "🔄 Loading favorites for userId: " + userId);
        
        // Debug authentication
        SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String authToken = authPrefs.getString("auth_token", "");
        Log.d(TAG, "🔑 Auth token available: " + (!authToken.isEmpty()));
        Log.d(TAG, "🔑 Auth token length: " + authToken.length());
        if (!authToken.isEmpty()) {
            Log.d(TAG, "🔑 Auth token first 20 chars: " + authToken.substring(0, Math.min(20, authToken.length())) + "...");
        }
        
        showLoading(true);
        
        apiService.getFavoriteListings(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                
                Log.d(TAG, "🔍 Favorites API Response Code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    
                    if (apiResponse.isSuccess()) {
                        processFavoritesResponse(apiResponse);
                    } else {
                        String errorMsg = apiResponse.getMessage();
                        Log.e(TAG, "API returned error: " + errorMsg);
                        showError("Lỗi: " + errorMsg);
                    }                } else if (response.code() == 403) {
                    // Handle 403 Forbidden specifically 
                    Log.e(TAG, "🚫 403 Forbidden - Favorites API not available");
                    Log.e(TAG, "This likely means the favorites feature is not implemented on the backend yet");
                    
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "403 Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading 403 error body", e);
                    }
                    
                    // Show development message immediately instead of trying alternatives
                    showApiNotAvailableState();                } else {
                    Log.e(TAG, "Response unsuccessful: " + response.code());
                    
                    // Try alternative API for other error codes (not 403)
                    if (response.code() != 403) {
                        Log.d(TAG, "🔄 Trying fallback method for error code: " + response.code());
                        tryAlternativeFavoritesAPI();
                    } else {
                        showError("Không thể tải danh sách yêu thích: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);                Log.e(TAG, "Network error loading favorites", t);
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
    
    /**
     * Try alternative API for favorites (fallback)
     */
    private void tryAlternativeFavoritesAPI() {
        Log.d(TAG, "🔄 Trying alternative favorites API with pagination...");
        
        // Try with getUserFavorites which uses PagedApiResponse
        apiService.getUserFavorites(userId, 0, 50).enqueue(new Callback<PagedApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<PagedApiResponse> call, @NonNull Response<PagedApiResponse> response) {
                Log.d(TAG, "🔍 Alternative Favorites API Response Code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse pagedResponse = response.body();
                    
                    if (pagedResponse.isSuccess()) {
                        Log.d(TAG, "✅ Alternative favorites API works!");
                        processPagedFavoritesResponse(pagedResponse);
                    } else {
                        String errorMsg = pagedResponse.getMessage();
                        Log.e(TAG, "Alternative API returned error: " + errorMsg);
                        showFallbackEmptyState();
                    }
                } else {
                    Log.e(TAG, "Alternative API also failed: " + response.code());
                    showFallbackEmptyState();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<PagedApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Alternative API network error", t);
                showFallbackEmptyState();
            }
        });
    }
    
    /**
     * Process paged favorites response from alternative API
     */
    private void processPagedFavoritesResponse(PagedApiResponse pagedResponse) {
        try {
            Object data = pagedResponse.getData();
            
            if (data != null) {
                // Convert data to List<Listing>
                Gson gson = new Gson();
                String jsonString = gson.toJson(data);
                Type listType = new TypeToken<List<Listing>>(){}.getType();
                List<Listing> newFavorites = gson.fromJson(jsonString, listType);
                
                if (newFavorites != null) {
                    favoritesList.clear();
                    favoritesList.addAll(newFavorites);
                    favoritesAdapter.setListings(favoritesList);
                    
                    Log.d(TAG, "✅ Loaded " + newFavorites.size() + " favorite listings via alternative API");
                    
                    // Show/hide empty state
                    if (favoritesList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    Log.w(TAG, "Alternative favorites list is null after parsing");
                    showEmptyState();
                }
            } else {
                Log.w(TAG, "Alternative API response data is null");
                showEmptyState();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing alternative favorites response", e);
            showFallbackEmptyState();
        }
    }
      /**
     * Show fallback empty state when API not available
     */
    private void showFallbackEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setText("💝 Tính năng yêu thích đang được cập nhật\n\nHệ thống đang phát triển tính năng này. Vui lòng quay lại sau!");
        }
        
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        
        if (btnRetryFavorites != null) {
            btnRetryFavorites.setVisibility(View.VISIBLE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.GONE);
        }
        
        // Show a toast for better UX
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), "Tính năng yêu thích đang được phát triển", Toast.LENGTH_LONG).show();
        }
    }
      /**
     * Show API not available state with user-friendly message
     */
    private void showApiNotAvailableState() {
        if (tvEmptyState != null) {
            tvEmptyState.setText("💝 Tính năng Yêu thích đang được phát triển\n\n" +
                    "Chúng tôi đang hoàn thiện tính năng này để mang đến trải nghiệm tốt nhất.\n\n" +
                    "Bạn có thể tạm thời lưu lại ID của sản phẩm yêu thích hoặc quay lại sau!\n\n" +
                    "Cảm ơn bạn đã kiên nhẫn! 🙏");
        }
        
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        
        if (btnRetryFavorites != null) {
            btnRetryFavorites.setVisibility(View.VISIBLE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.GONE);
        }
        
        // Show a informative toast
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), 
                "Tính năng yêu thích sẽ sớm được cập nhật! 🚀", 
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Process favorites response from API
     */
    private void processFavoritesResponse(ApiResponse apiResponse) {
        try {
            Object data = apiResponse.getData();
            
            if (data != null) {
                // Convert data to List<Listing>
                Gson gson = new Gson();
                String jsonString = gson.toJson(data);
                Type listType = new TypeToken<List<Listing>>(){}.getType();
                List<Listing> newFavorites = gson.fromJson(jsonString, listType);
                  if (newFavorites != null) {
                    favoritesList.clear();
                    favoritesList.addAll(newFavorites);
                    favoritesAdapter.setListings(favoritesList);
                    
                    Log.d(TAG, "✅ Loaded " + newFavorites.size() + " favorite listings");
                    
                    // Show/hide empty state
                    if (favoritesList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    Log.w(TAG, "Favorites list is null after parsing");
                    showEmptyState();
                }
            } else {
                Log.w(TAG, "API response data is null");
                showEmptyState();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing favorites response", e);
            showError("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }
    
    /**
     * Open listing detail when item clicked
     */
    private void openListingDetail(Listing listing) {
        if (listing == null) {
            Log.w(TAG, "Cannot open detail - listing is null");
            return;
        }
        
        Log.d(TAG, "Opening detail for listing: " + listing.getTitle());
        
        // Navigate to ListingDetailFragment
        Bundle args = new Bundle();
        args.putLong("listingId", listing.getId());
        args.putString("fromFragment", "FavoritesFragment");
        
        ListingDetailFragment detailFragment = new ListingDetailFragment();
        detailFragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit();
    }
      /**
     * Show loading state
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }    /**
     * Show empty state when no favorites
     */
    private void showEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setText("💝 Chưa có sản phẩm yêu thích nào\n\n" +
                    "🔍 Khám phá và tìm những sản phẩm yêu thích\n" +
                    "❤️ Nhấn icon trái tim để lưu vào danh sách\n" +
                    "📱 Quay lại đây để xem các sản phẩm đã lưu!");
        }
        
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        
        if (btnRetryFavorites != null) {
            btnRetryFavorites.setVisibility(View.GONE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.GONE);
        }
    }
      /**
     * Hide empty state
     */
    private void hideEmptyState() {
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.GONE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.VISIBLE);
        }
    }
      /**
     * Show not logged in state
     */    private void showNotLoggedInState() {
        if (tvEmptyState != null) {
            tvEmptyState.setText(getString(R.string.error_login_required_favorites));
        }
        
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        
        if (btnRetryFavorites != null) {
            btnRetryFavorites.setVisibility(View.GONE);
        }
        
        if (recyclerFavorites != null) {
            recyclerFavorites.setVisibility(View.GONE);
        }
    }
    
    /**
     * Show error message
     */
    private void showError(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        
        if (favoritesList.isEmpty()) {
            showEmptyState();
        }
    }
    
    /**
     * Refresh favorites when fragment becomes visible
     */
    @Override
    public void onResume() {
        super.onResume();
        
        // Refresh favorites khi user quay lại (có thể đã favorite/unfavorite ở màn hình khác)
        if (userId != -1 && !favoritesList.isEmpty()) {
            Log.d(TAG, "Fragment resumed - refreshing favorites");
            loadFavorites();
        }
    }
}
