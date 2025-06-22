package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ListingAdapter;
import com.example.ok.adapter.HomeRecommendationsAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // UI Components từ layout
    private ImageView ivUserAvatar;
    private MaterialCardView searchBar;
    private RecyclerView featuredItemsRecyclerView;
    private RecyclerView recommendationsRecyclerView;
    private FloatingActionButton fabAddItem;

    // Category containers
    private LinearLayout categoriesGrid;
    private List<Category> categories = new ArrayList<>();

    // API Services
    private ApiService apiService;
    private ListingApiService listingApiService;

    // Adapters
    private ListingAdapter featuredAdapter;
    private HomeRecommendationsAdapter recommendationsAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Initialize API services
        initApiServices();

        // Setup UI
        setupUI();

        // Setup adapters
        setupAdapters();

        // Setup click listeners
        setupClickListeners();

        // Load data
        loadData();
    }

    private void initViews(View view) {
        // User avatar
        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);

        // Search bar
        searchBar = view.findViewById(R.id.searchBar);

        // RecyclerView
        featuredItemsRecyclerView = view.findViewById(R.id.featuredItemsRecyclerView);
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView);

        // FAB
        fabAddItem = view.findViewById(R.id.fabAddItem);

        // Categories grid - tìm LinearLayout chứa categories
        categoriesGrid = findCategoriesGrid(view);
    }    private LinearLayout findCategoriesGrid(View view) {
        // Tìm LinearLayout chứa 7 categories trong 2 hàng
        // Categories grid là LinearLayout có orientation="vertical" chứa 2 hàng
        return findLinearLayoutWithCategories(view);
    }

    private LinearLayout findLinearLayoutWithCategories(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            // Check if this is the categories LinearLayout container (vertical)
            if (view instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) view;
                if (linearLayout.getOrientation() == LinearLayout.VERTICAL &&
                        linearLayout.getChildCount() == 2) {
                    // Check if both children are horizontal LinearLayouts with categories
                    View firstRow = linearLayout.getChildAt(0);
                    View secondRow = linearLayout.getChildAt(1);
                    
                    if (firstRow instanceof LinearLayout && secondRow instanceof LinearLayout) {
                        LinearLayout firstRowLayout = (LinearLayout) firstRow;
                        LinearLayout secondRowLayout = (LinearLayout) secondRow;
                        
                        // First row should have 4 categories, second row should have 3
                        if (firstRowLayout.getOrientation() == LinearLayout.HORIZONTAL &&
                            firstRowLayout.getChildCount() == 4 &&
                            secondRowLayout.getOrientation() == LinearLayout.HORIZONTAL &&
                            secondRowLayout.getChildCount() == 3) {
                            
                            // Verify first child of first row has category structure
                            View firstCategory = firstRowLayout.getChildAt(0);
                            if (firstCategory instanceof LinearLayout) {
                                LinearLayout categoryLayout = (LinearLayout) firstCategory;
                                if (categoryLayout.getChildCount() >= 2) {
                                    View cardView = categoryLayout.getChildAt(0);
                                    if (cardView instanceof MaterialCardView) {
                                        return linearLayout; // Found the categories container!
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recursively search in children
            for (int i = 0; i < group.getChildCount(); i++) {
                LinearLayout result = findLinearLayoutWithCategories(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void initApiServices() {
        try {
            // Initialize RetrofitClient before using any API services
            RetrofitClient.init(requireContext());
            apiService = RetrofitClient.getApiService();
            listingApiService = RetrofitClient.getListingApiService();        } catch (Exception e) {
            safeShowToast("Lỗi khởi tạo dịch vụ");
        }
    }

    private void setupUI() {
        // Load user avatar
        loadUserAvatar();
    }

    private void loadUserAvatar() {
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String avatarUrl = prefs.getString("avatarUrl", null);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(ivUserAvatar);
        } else {
            // Set default avatar
            ivUserAvatar.setImageResource(R.drawable.user);
        }
    }

    private void setupAdapters() {
        // Featured items adapter với grid layout
        featuredAdapter = new ListingAdapter(getContext(), true); // true = grid layout

        // Sử dụng GridLayoutManager cho featured items với 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        featuredItemsRecyclerView.setLayoutManager(gridLayoutManager);
        featuredItemsRecyclerView.setAdapter(featuredAdapter);
        featuredItemsRecyclerView.setNestedScrollingEnabled(false);

        // Recommendations adapter
        recommendationsAdapter = new HomeRecommendationsAdapter(getContext());
        recommendationsRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);
        recommendationsRecyclerView.setNestedScrollingEnabled(false);

        // Set click listener for featured items
        featuredAdapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                navigateToListingDetail(listing);
            }

            @Override
            public void onListingLongClick(Listing listing) {
                showListingOptions(listing);
            }
        });

        // Set click listener for recommendations
        recommendationsAdapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                navigateToListingDetail(listing);
            }

            @Override
            public void onListingLongClick(Listing listing) {
                showListingOptions(listing);
            }
        });
    }

    private void setupClickListeners() {
        // Search bar click listener
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> navigateToSearch());
        }

        // User avatar click listener - messenger functionality
        if (ivUserAvatar != null) {
            ivUserAvatar.setOnClickListener(v -> navigateToMessages());
        }

        // FAB click listener
        if (fabAddItem != null) {
            fabAddItem.setOnClickListener(v -> navigateToCreateListing());
        }

        // Notification button
        View btnNotification = getView().findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> navigateToNotifications());
        }

        // See all featured items
        View tvSeeAllFeatured = getView().findViewById(R.id.tvSeeAllFeatured);
        if (tvSeeAllFeatured != null) {
            tvSeeAllFeatured.setOnClickListener(v -> navigateToAllListings("featured"));
        }

        // Setup category click listeners
        setupCategoryClickListeners();
    }    private void setupCategoryClickListeners() {
        // Categories will be loaded from API and setup in updateCategoriesUI()
        // This method is kept for backward compatibility
    }private void loadData() {
        loadCategories();
        loadHomeRecommendations();
        loadFeaturedListings();
    }    private void loadHomeRecommendations() {
        if (listingApiService == null) {
            safeShowToast("Dịch vụ chưa sẵn sàng");
            return;
        }

        // Lấy userId, lat, lng từ SharedPreferences hoặc null nếu chưa có
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        Long userId = prefs.contains("userId") ? prefs.getLong("userId", 0) : null;
        Double lat = prefs.contains("lat") ? Double.longBitsToDouble(prefs.getLong("lat", 0)) : null;
        Double lng = prefs.contains("lng") ? Double.longBitsToDouble(prefs.getLong("lng", 0)) : null;
        Call<PagedApiResponse<CategoryWithListingsResponse>> call = listingApiService.getHomeRecommendations(userId, lat, lng, 0, 10);
        call.enqueue(new Callback<PagedApiResponse<CategoryWithListingsResponse>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<CategoryWithListingsResponse>> call, Response<PagedApiResponse<CategoryWithListingsResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<CategoryWithListingsResponse> pagedResponse = response.body();
                    if (pagedResponse.isSuccess()) {
                        List<CategoryWithListingsResponse> categories = pagedResponse.getData();
                        if (categories != null && !categories.isEmpty()) {
                            recommendationsAdapter.setCategories(categories);
                        } else {
                            // Show empty state for recommendations
                        }                    } else {
                        safeShowToast("Không thể tải gợi ý: " + pagedResponse.getMessage());
                    }
                } else {
                    safeShowToast("Lỗi tải gợi ý: " + response.code());
                }
            }            @Override
            public void onFailure(Call<PagedApiResponse<CategoryWithListingsResponse>> call, Throwable t) {
                safeShowToast("Lỗi kết nối gợi ý: " + t.getMessage());
            }
        });
    }    private void loadFeaturedListings() {
        if (listingApiService == null) {
            safeShowToast("Dịch vụ chưa sẵn sàng");
            return;
        }

        // Load featured/recent listings
        Call<PagedApiResponse<Listing>> call = listingApiService.getAvailableListings(0, 10);
        call.enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<Listing>> call,
                                   Response<PagedApiResponse<Listing>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<Listing> pagedResponse = response.body();
                    if (pagedResponse.isSuccess()) {
                        List<Listing> listings = pagedResponse.getData();
                        if (listings != null && !listings.isEmpty()) {
                            featuredAdapter.setListings(listings);
                        } else {
                            // Show empty state
                            showEmptyState();
                        }                    } else {
                        safeShowToast("Không thể tải sản phẩm: " + pagedResponse.getMessage());
                    }
                } else {
                    safeShowToast("Lỗi tải dữ liệu: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PagedApiResponse<Listing>> call, Throwable t) {
                // Check if fragment is still attached before showing toast
                safeShowToast("Lỗi kết nối: " + t.getMessage());
            }
        });
    }    private void showEmptyState() {
        // TODO: Show empty state view
        safeShowToast("Chưa có sản phẩm nào");
    }// Navigation methods
    private void navigateToCategoryListings(String categoryName, Long categoryId) {
        Log.d("HomeFragment", "Navigating to category: " + categoryName + " (ID: " + categoryId + ")");
        
        // Navigate to SearchFragment with category filter
        if (getActivity() instanceof MainMenu) {
            SearchFragment searchFragment = SearchFragment.newInstance(categoryId, categoryName);
            ((MainMenu) getActivity()).replaceFragment(searchFragment);        } else {
            safeShowToast("Xem danh mục: " + categoryName);
        }
    }

    private void navigateToListingDetail(Listing listing) {
        if (getActivity() instanceof MainMenu) {
            ListingDetailFragment detailFragment = ListingDetailFragment.newInstance(listing.getId());
            ((MainMenu) getActivity()).replaceFragment(detailFragment);
        }
    }

    private void navigateToCreateListing() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToCreateListing();
        }
    }

    private void navigateToSearch() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToTab("search");
        }
    }

    private void navigateToMessages() {        // Avatar click should go to messages/chat
        safeShowToast("Mở tin nhắn");
        // TODO: Navigate to messages
        // if (getActivity() instanceof MainMenu) {
        //     ((MainMenu) getActivity()).navigateToTab("messages");
        // }
    }

    private void navigateToNotifications() {
        safeShowToast("Thông báo");
        // TODO: Implement navigation to notifications
        // Navigation.findNavController(getView()).navigate(R.id.action_home_to_notifications);
    }

    private void navigateToAllListings(String type) {
        safeShowToast("Xem tất cả " + type);
        // TODO: Implement navigation to all listings
        // Bundle args = new Bundle();
        // args.putString("listingType", type);
        // Navigation.findNavController(getView()).navigate(R.id.action_home_to_all_listings, args);
    }    private void showListingOptions(Listing listing) {
        // TODO: Show bottom sheet or popup menu with options
        safeShowToast("Tùy chọn cho: " + listing.getTitle());
    }

    private void loadCategories() {        if (apiService == null) {
            safeShowToast("Dịch vụ chưa sẵn sàng");
            return;
        }

        Call<ApiResponse> call = apiService.getAllCategories();
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        try {
                            // Parse categories từ JSON
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            Type listType = new TypeToken<List<Category>>(){}.getType();
                            categories = gson.fromJson(json, listType);
                              Log.d("HomeFragment", "Loaded " + categories.size() + " categories");
                            
                            // Update UI với categories mới - check if fragment is still attached
                            if (isAdded() && getContext() != null) {
                                updateCategoriesUI();
                            } else {
                                Log.w("HomeFragment", "Fragment not attached, skipping UI update");
                            }
                              } catch (Exception e) {
                            Log.e("HomeFragment", "Error parsing categories", e);
                            // Check if fragment is still attached before calling setupDefaultCategories
                            if (isAdded() && getContext() != null) {
                                setupDefaultCategories();
                            }
                        }
                    } else {
                        setupDefaultCategories();
                    }
                } else {
                    setupDefaultCategories();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("HomeFragment", "Failed to load categories", t);
                setupDefaultCategories();
            }
        });    }    private void setupDefaultCategories() {
        // Check if fragment is still attached to avoid crashes
        if (!isAdded() || getContext() == null) {
            Log.w("HomeFragment", "Fragment not attached, cannot setup default categories");
            return;
        }
        
        categories.clear();
        // Sử dụng thứ tự giống như API response
        categories.add(new Category(1L, "Điện tử", null, null, true));
        categories.add(new Category(2L, "Thời trang", null, null, true));
        categories.add(new Category(3L, "Gia dụng", null, null, true));
        categories.add(new Category(4L, "Xe cộ", null, null, true));
        categories.add(new Category(5L, "Sách & Văn phòng phẩm", null, null, true));
        categories.add(new Category(6L, "Thể thao & Giải trí", null, null, true));
        categories.add(new Category(7L, "Khác", null, null, true));
        
        updateCategoriesUI();
    }private void updateCategoriesUI() {
        // Check if fragment is still attached to avoid crashes
        if (!isAdded() || getContext() == null) {
            Log.w("HomeFragment", "Fragment not attached, cannot update categories UI");
            return;
        }
        
        if (categoriesGrid == null || categories.isEmpty()) {
            return;
        }

        // Categories grid có cấu trúc: LinearLayout vertical chứa 2 hàng
        // Hàng 1: 4 categories (index 0-3)
        // Hàng 2: 3 categories (index 4-6)
        
        if (categoriesGrid.getChildCount() >= 2) {
            // Hàng đầu tiên (4 categories)
            View firstRowView = categoriesGrid.getChildAt(0);
            if (firstRowView instanceof LinearLayout) {
                LinearLayout firstRow = (LinearLayout) firstRowView;
                updateCategoryRow(firstRow, 0, Math.min(4, categories.size()));
            }
            
            // Hàng thứ hai (3 categories)
            View secondRowView = categoriesGrid.getChildAt(1);
            if (secondRowView instanceof LinearLayout) {
                LinearLayout secondRow = (LinearLayout) secondRowView;
                updateCategoryRow(secondRow, 4, Math.min(categories.size(), 7));
            }
        }
    }    private void updateCategoryRow(LinearLayout row, int startIndex, int endIndex) {
        // Check if fragment is still attached to avoid crashes
        if (!isAdded() || getContext() == null) {
            Log.w("HomeFragment", "Fragment not attached, cannot update category row");
            return;
        }
        
        for (int i = 0; i < row.getChildCount() && (startIndex + i) < endIndex; i++) {
            int categoryIndex = startIndex + i;
            View categoryView = row.getChildAt(i);
            
            if (categoryView instanceof LinearLayout && categoryIndex < categories.size()) {
                LinearLayout categoryLayout = (LinearLayout) categoryView;
                
                // Tìm TextView trong category layout
                TextView categoryText = findCategoryTextView(categoryLayout);
                if (categoryText != null) {
                    categoryText.setText(categories.get(categoryIndex).getName());
                }
                
                // Set click listener cho category
                final int finalCategoryIndex = categoryIndex;
                categoryView.setClickable(true);
                categoryView.setFocusable(true);
                
                try {
                    categoryView.setBackground(getResources().getDrawable(android.R.drawable.menuitem_background, null));
                } catch (Exception e) {
                    Log.w("HomeFragment", "Could not set background", e);
                }
                
                categoryView.setOnClickListener(v -> {
                    Category category = categories.get(finalCategoryIndex);
                    navigateToCategoryListings(category.getName(), category.getId());
                });
            }
        }
    }

    private TextView findCategoryTextView(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            } else if (child instanceof ViewGroup) {
                TextView result = findCategoryTextView((ViewGroup) child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user avatar in case it was updated
        loadUserAvatar();

        // Optionally refresh listings
        loadFeaturedListings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up to prevent memory leaks
        if (featuredAdapter != null) {
            featuredAdapter.setOnListingClickListener(null);
        }
        if (recommendationsAdapter != null) {
            recommendationsAdapter.setOnListingClickListener(null);
        }
    }
    
    // Utility method to safely show toast
    private void safeShowToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}