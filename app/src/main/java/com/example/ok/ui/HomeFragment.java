package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.ok.api.ApiService;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // UI Components từ layout
    private ImageView ivUserAvatar;
    private MaterialCardView searchBar;
    private RecyclerView featuredItemsRecyclerView;
    private FloatingActionButton fabAddItem;

    // Category containers
    private LinearLayout categoriesGrid;

    // API Services
    private ApiService apiService;
    private ListingApiService listingApiService;

    // Adapters
    private ListingAdapter featuredAdapter;

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

        // FAB
        fabAddItem = view.findViewById(R.id.fabAddItem);

        // Categories grid - tìm LinearLayout chứa categories
        categoriesGrid = findCategoriesGrid(view);
    }

    private LinearLayout findCategoriesGrid(View view) {
        // Tìm LinearLayout chứa 5 categories bằng cách traverse
        // Categories grid là LinearLayout có weightSum="5" và orientation="horizontal"
        return findLinearLayoutWithCategories(view);
    }

    private LinearLayout findLinearLayoutWithCategories(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            // Check if this is the categories LinearLayout
            if (view instanceof LinearLayout) {
                LinearLayout linearLayout = (LinearLayout) view;
                if (linearLayout.getOrientation() == LinearLayout.HORIZONTAL &&
                        linearLayout.getChildCount() == 5) {
                    // Verify it contains category items by checking first child
                    View firstChild = linearLayout.getChildAt(0);
                    if (firstChild instanceof LinearLayout) {
                        LinearLayout firstCategory = (LinearLayout) firstChild;
                        // Check if it has MaterialCardView and TextView (category structure)
                        if (firstCategory.getChildCount() >= 2) {
                            View cardView = firstCategory.getChildAt(0);
                            if (cardView instanceof MaterialCardView) {
                                return linearLayout; // Found it!
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
            apiService = RetrofitClient.getApiService();
            listingApiService = RetrofitClient.getListingApiService();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khởi tạo dịch vụ", Toast.LENGTH_SHORT).show();
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
    }

    private void setupCategoryClickListeners() {
        if (categoriesGrid == null) {
            return;
        }

        // Array of category info: {name, categoryId}
        String[][] categoryInfo = {
                {"Điện tử", "1"},
                {"Gia dụng", "3"},
                {"Khác", null},
                {"Sách & Văn phòng phẩm", "5"},
                {"Thể thao & Giải trí", "6"}
        };

        // Set click listeners for each category
        for (int i = 0; i < categoriesGrid.getChildCount() && i < categoryInfo.length; i++) {
            View categoryView = categoriesGrid.getChildAt(i);
            final int categoryIndex = i;

            if (categoryView instanceof LinearLayout) {
                // Make the entire category LinearLayout clickable
                categoryView.setClickable(true);
                categoryView.setFocusable(true);
                categoryView.setBackground(getResources().getDrawable(android.R.drawable.menuitem_background, null));

                categoryView.setOnClickListener(v -> {
                    String categoryName = categoryInfo[categoryIndex][0];
                    String categoryIdStr = categoryInfo[categoryIndex][1];
                    Long categoryId = categoryIdStr != null ? Long.parseLong(categoryIdStr) : null;

                    navigateToCategoryListings(categoryName, categoryId);
                });

                // Add ripple effect
                categoryView.setOnTouchListener((v, event) -> {
                    v.performClick();
                    return false;
                });
            }
        }
    }

    private void loadData() {
        loadFeaturedListings();
    }

    private void loadFeaturedListings() {
        if (listingApiService == null) {
            Toast.makeText(getContext(), "Dịch vụ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
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
                        }
                    } else {
                        Toast.makeText(getContext(), "Không thể tải sản phẩm: " + pagedResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PagedApiResponse<Listing>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        // TODO: Show empty state view
        Toast.makeText(getContext(), "Chưa có sản phẩm nào", Toast.LENGTH_SHORT).show();
    }

    // Navigation methods
    private void navigateToCategoryListings(String categoryName, Long categoryId) {
        Toast.makeText(getContext(), "Xem danh mục: " + categoryName, Toast.LENGTH_SHORT).show();
        // TODO: Implement navigation to category listings
        // Bundle args = new Bundle();
        // args.putString("categoryName", categoryName);
        // if (categoryId != null) args.putLong("categoryId", categoryId);
        // Navigation.findNavController(getView()).navigate(R.id.action_home_to_category, args);
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

    private void navigateToMessages() {
        // Avatar click should go to messages/chat
        Toast.makeText(getContext(), "Mở tin nhắn", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to messages
        // if (getActivity() instanceof MainMenu) {
        //     ((MainMenu) getActivity()).navigateToTab("messages");
        // }
    }

    private void navigateToNotifications() {
        Toast.makeText(getContext(), "Thông báo", Toast.LENGTH_SHORT).show();
        // TODO: Implement navigation to notifications
        // Navigation.findNavController(getView()).navigate(R.id.action_home_to_notifications);
    }

    private void navigateToAllListings(String type) {
        Toast.makeText(getContext(), "Xem tất cả " + type, Toast.LENGTH_SHORT).show();
        // TODO: Implement navigation to all listings
        // Bundle args = new Bundle();
        // args.putString("listingType", type);
        // Navigation.findNavController(getView()).navigate(R.id.action_home_to_all_listings, args);
    }

    private void showListingOptions(Listing listing) {
        // TODO: Show bottom sheet or popup menu with options
        Toast.makeText(getContext(), "Tùy chọn cho: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
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
    }
}