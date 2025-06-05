package com.example.ok.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ListingAdapter;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.Listing;
import com.example.ok.model.PagedApiResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    
    private static final String TAG = "SearchFragment";
    
    // UI Components
    private EditText etSearch;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    
    // Filter buttons
    private Button btnFilterAll;
    private Button btnFilterPrice;
    private Button btnFilterCategory;
    private Button btnFilterLocation;
    
    // Filter values
    private Long selectedCategoryId = null;
    private Double minPrice = null;
    private Double maxPrice = null;
    private String selectedLocation = null;
    private String currentQuery = "";
    
    // Adapter
    private ListingAdapter adapter;
    
    // Service
    private ListingApiService listingApiService;
    
    // Pagination
    private int currentPage = 0;
    private final int PAGE_SIZE = 20;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        initApiService();
        initViews(view);
        setupRecyclerView();
        setupListeners();
        
        // Load initial data
        performSearch();
        
        return view;
    }
    
    private void initApiService() {
        try {
            listingApiService = RetrofitClient.getListingApiService();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing API service", e);
            Toast.makeText(getContext(), "Lỗi khởi tạo dịch vụ", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyView = view.findViewById(R.id.layoutEmpty);
        
        // Filter buttons
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterPrice = view.findViewById(R.id.btnFilterPrice);
        btnFilterCategory = view.findViewById(R.id.btnFilterCategory);
        btnFilterLocation = view.findViewById(R.id.btnFilterLocation);
        
        // Set up filter button colors
        updateFilterButtonAppearance();
    }
    
    private void setupRecyclerView() {
        adapter = new ListingAdapter(getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        
        // Set up click listener
        adapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                navigateToListingDetail(listing);
            }
            
            @Override
            public void onListingLongClick(Listing listing) {
                // Optional: Implement long click action
            }
        });
        
        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_SIZE) {
                            loadMoreItems();
                        }
                    }
                }
            }
        });
    }
    
    private void setupListeners() {
        // Search input
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
            }
        });
        
        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::refreshSearch);
        
        // Filter buttons
        btnFilterAll.setOnClickListener(v -> resetFilters());
        btnFilterPrice.setOnClickListener(v -> showPriceFilterDialog());
        btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());
        btnFilterLocation.setOnClickListener(v -> showLocationFilterDialog());
    }
    
    private void performSearch() {
        isLoading = true;
        currentPage = 0;
        isLastPage = false;
        
        if (listingApiService == null) {
            Toast.makeText(getContext(), "Dịch vụ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            isLoading = false;
            swipeRefresh.setRefreshing(false);
            return;
        }
        
        Call<PagedApiResponse<Listing>> call = listingApiService.searchListings(
                currentQuery,
                selectedCategoryId,
                null, // conditionId - Có thể thêm sau
                minPrice,
                maxPrice,
                selectedLocation,
                currentPage,
                PAGE_SIZE
        );
        
        // Log request parameters
        Log.d(TAG, "Request params - categoryId: " + selectedCategoryId + 
                     ", minPrice: " + minPrice + 
                     ", maxPrice: " + maxPrice + 
                     ", location: " + selectedLocation);
        
        call.enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Response<PagedApiResponse<Listing>> response) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<Listing> pagedResponse = response.body();
                    
                    if (pagedResponse.isSuccess()) {
                        List<Listing> listings = pagedResponse.getData();
                        
                        if (listings != null && !listings.isEmpty()) {
                            // Thêm lọc client-side
                            if (selectedCategoryId != null || minPrice != null || maxPrice != null || selectedLocation != null) {
                                List<Listing> filteredListings = new ArrayList<>();
                                
                                for (Listing listing : listings) {
                                    boolean match = true;
                                    
                                    // Lọc theo category
                                    if (selectedCategoryId != null && !selectedCategoryId.equals(listing.getCategoryId())) {
                                        match = false;
                                    }
                                    
                                    // Lọc theo giá
                                    if (match && minPrice != null && listing.getPrice().doubleValue() < minPrice) {
                                        match = false;
                                    }
                                    
                                    if (match && maxPrice != null && listing.getPrice().doubleValue() > maxPrice) {
                                        match = false;
                                    }
                                    
                                    // Lọc theo địa điểm
                                    if (match && selectedLocation != null && !listing.getLocationText().contains(selectedLocation)) {
                                        match = false;
                                    }
                                    
                                    if (match) {
                                        filteredListings.add(listing);
                                    }
                                }
                                
                                // Hiển thị danh sách đã lọc
                                adapter.setListings(filteredListings);
                                Toast.makeText(getContext(), "Tìm thấy " + filteredListings.size() + " kết quả", Toast.LENGTH_SHORT).show();
                                
                                if (filteredListings.isEmpty()) {
                                    showEmptyView();
                                } else {
                                    showContentView();
                                }
                            } else {
                                // Không có bộ lọc, hiển thị tất cả kết quả
                                adapter.setListings(listings);
                                Toast.makeText(getContext(), "Tìm thấy " + listings.size() + " kết quả", Toast.LENGTH_SHORT).show();
                                showContentView();
                            }
                            
                            // Check if this is the last page
                            if (listings.size() < PAGE_SIZE) {
                                isLastPage = true;
                            }
                        } else {
                            adapter.clear();
                            showEmptyView();
                        }
                    } else {
                        adapter.clear();
                        showEmptyView();
                        Toast.makeText(getContext(), pagedResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    adapter.clear();
                    showEmptyView();
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Throwable t) {
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                adapter.clear();
                showEmptyView();
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Search error", t);
            }
        });
    }
    
    private void loadMoreItems() {
        isLoading = true;
        currentPage++;
        
        Call<PagedApiResponse<Listing>> call = listingApiService.searchListings(
                currentQuery,
                selectedCategoryId,
                null, // conditionId
                minPrice,
                maxPrice,
                selectedLocation,
                currentPage,
                PAGE_SIZE
        );
        
        call.enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Response<PagedApiResponse<Listing>> response) {
                isLoading = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<Listing> pagedResponse = response.body();
                    
                    if (pagedResponse.isSuccess()) {
                        List<Listing> newListings = pagedResponse.getData();
                        
                        if (newListings != null && !newListings.isEmpty()) {
                            adapter.addListings(newListings);
                            
                            // Check if this is the last page
                            if (newListings.size() < PAGE_SIZE) {
                                isLastPage = true;
                            }
                        } else {
                            isLastPage = true;
                        }
                    } else {
                        Toast.makeText(getContext(), pagedResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<PagedApiResponse<Listing>> call, @NonNull Throwable t) {
                isLoading = false;
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Load more error", t);
            }
        });
    }
    
    private void refreshSearch() {
        performSearch();
    }
    
    private void showPriceFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_price_filter, null);
        dialog.setContentView(dialogView);
        
        RangeSlider priceSlider = dialogView.findViewById(R.id.priceRangeSlider);
        TextView tvMinPrice = dialogView.findViewById(R.id.tvMinPrice);
        TextView tvMaxPrice = dialogView.findViewById(R.id.tvMaxPrice);
        Button btnApply = dialogView.findViewById(R.id.btnApplyPriceFilter);
        Button btnReset = dialogView.findViewById(R.id.btnResetPriceFilter);
        
        // Set initial values
        priceSlider.setValueFrom(0f);
        priceSlider.setValueTo(10000000f); // 10 million VND
        
        if (minPrice != null && maxPrice != null) {
            priceSlider.setValues(minPrice.floatValue(), maxPrice.floatValue());
            tvMinPrice.setText(formatPrice(minPrice.floatValue()));
            tvMaxPrice.setText(formatPrice(maxPrice.floatValue()));
        } else {
            priceSlider.setValues(0f, 10000000f);
            tvMinPrice.setText(formatPrice(0f));
            tvMaxPrice.setText(formatPrice(10000000f));
        }
        
        // Update text when slider changes
        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tvMinPrice.setText(formatPrice(values.get(0)));
            tvMaxPrice.setText(formatPrice(values.get(1)));
        });
        
        // Apply button
        btnApply.setOnClickListener(v -> {
            List<Float> values = priceSlider.getValues();
            minPrice = (double) values.get(0).floatValue();
            maxPrice = (double) values.get(1).floatValue();
            
            // If max is the default maximum and min is 0, consider it as no filter
            if (minPrice == 0 && maxPrice == 10000000) {
                minPrice = null;
                maxPrice = null;
            }
            
            updateFilterButtonAppearance();
            performSearch(); // Gọi search ngay khi áp dụng filter
            dialog.dismiss();
            
            // Thêm dòng này để hiển thị thông báo
            if (minPrice != null || maxPrice != null) {
                Toast.makeText(getContext(), "Đã lọc theo giá", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Reset button
        btnReset.setOnClickListener(v -> {
            minPrice = null;
            maxPrice = null;
            priceSlider.setValues(0f, 10000000f);
            tvMinPrice.setText(formatPrice(0f));
            tvMaxPrice.setText(formatPrice(10000000f));
        });
        
        dialog.show();
    }
    
    private String formatPrice(float price) {
        if (price >= 1000000) {
            return String.format("%.1f triệu", price / 1000000);
        } else if (price >= 1000) {
            return String.format("%.1f nghìn", price / 1000);
        } else {
            return String.format("%.0f đ", price);
        }
    }
    
    private void showCategoryFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_filter, null);
        dialog.setContentView(dialogView);
        
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvCategories);
        Button btnResetCategory = dialogView.findViewById(R.id.btnResetCategory);
        
        // Tạo adapter cho dialog
        SearchCategoryAdapter categoryAdapter = new SearchCategoryAdapter(getCategories());
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(categoryAdapter);
        
        // Set selected category
        if (selectedCategoryId != null) {
            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
        }
        
        // Category selection listener
        categoryAdapter.setOnCategoryClickListener(category -> {
            selectedCategoryId = category.getId();
            updateFilterButtonAppearance();
            performSearch();
            dialog.dismiss();
        });
        
        // Reset button
        btnResetCategory.setOnClickListener(v -> {
            selectedCategoryId = null;
            updateFilterButtonAppearance();
            performSearch();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showLocationFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_location_filter, null);
        dialog.setContentView(dialogView);
        
        EditText etLocationSearch = dialogView.findViewById(R.id.etLocationSearch);
        Button btnApplyLocation = dialogView.findViewById(R.id.btnApplyLocation);
        Button btnResetLocation = dialogView.findViewById(R.id.btnResetLocation);
        RecyclerView rvPopularLocations = dialogView.findViewById(R.id.rvPopularLocations);
        
        // Set current location if exists
        if (selectedLocation != null) {
            etLocationSearch.setText(selectedLocation);
        }
        
        // Set up popular locations
        LocationAdapter locationAdapter = new LocationAdapter(getPopularLocations());
        rvPopularLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPopularLocations.setAdapter(locationAdapter);
        
        // Popular location click
        locationAdapter.setOnLocationClickListener(location -> {
            etLocationSearch.setText(location);
        });
        
        // Apply button
        btnApplyLocation.setOnClickListener(v -> {
            String location = etLocationSearch.getText().toString().trim();
            if (!location.isEmpty()) {
                selectedLocation = location;
            } else {
                selectedLocation = null;
            }
            updateFilterButtonAppearance();
            performSearch();
            dialog.dismiss();
        });
        
        // Reset button
        btnResetLocation.setOnClickListener(v -> {
            selectedLocation = null;
            etLocationSearch.setText("");
            updateFilterButtonAppearance();
            performSearch();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void resetFilters() {
        currentQuery = etSearch.getText().toString().trim();
        selectedCategoryId = null;
        minPrice = null;
        maxPrice = null;
        selectedLocation = null;
        
        updateFilterButtonAppearance();
        performSearch();
    }
    
    private void updateFilterButtonAppearance() {
        // Reset all buttons to default appearance
        btnFilterAll.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnFilterPrice.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnFilterCategory.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnFilterLocation.setBackgroundResource(R.drawable.bg_filter_unselected);
        
        btnFilterAll.setTextColor(getResources().getColor(R.color.text_secondary));
        btnFilterPrice.setTextColor(getResources().getColor(R.color.text_secondary));
        btnFilterCategory.setTextColor(getResources().getColor(R.color.text_secondary));
        btnFilterLocation.setTextColor(getResources().getColor(R.color.text_secondary));
        
        // Highlight active filters
        if (minPrice != null || maxPrice != null) {
            btnFilterPrice.setBackgroundResource(R.drawable.bg_filter_selected);
            btnFilterPrice.setTextColor(getResources().getColor(R.color.primary_color));
        }
        
        if (selectedCategoryId != null) {
            btnFilterCategory.setBackgroundResource(R.drawable.bg_filter_selected);
            btnFilterCategory.setTextColor(getResources().getColor(R.color.primary_color));
        }
        
        if (selectedLocation != null) {
            btnFilterLocation.setBackgroundResource(R.drawable.bg_filter_selected);
            btnFilterLocation.setTextColor(getResources().getColor(R.color.primary_color));
        }
        
        // If no filters are applied, highlight "All" button
        if (minPrice == null && maxPrice == null && selectedCategoryId == null && selectedLocation == null) {
            btnFilterAll.setBackgroundResource(R.drawable.bg_filter_selected);
            btnFilterAll.setTextColor(getResources().getColor(R.color.primary_color));
        }
        
        // Thêm cập nhật text để hiển thị giá trị lọc
        if (minPrice != null || maxPrice != null) {
            String priceText = "Giá: ";
            if (minPrice != null) {
                priceText += formatPrice(minPrice.floatValue());
            } else {
                priceText += "0đ";
            }
            
            priceText += " - ";
            
            if (maxPrice != null) {
                priceText += formatPrice(maxPrice.floatValue());
            } else {
                priceText += "∞";
            }
            
            btnFilterPrice.setText(priceText);
        } else {
            btnFilterPrice.setText("Giá");
        }
        
        if (selectedCategoryId != null) {
            // Tìm tên category từ ID
            for (Category category : getCategories()) {
                if (category.getId().equals(selectedCategoryId)) {
                    btnFilterCategory.setText(category.getName());
                    break;
                }
            }
        } else {
            btnFilterCategory.setText("Danh mục");
        }
        
        if (selectedLocation != null) {
            btnFilterLocation.setText(selectedLocation);
        } else {
            btnFilterLocation.setText("Vị trí");
        }
    }
    
    private void showContentView() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }
    
    private void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }
    
    private void navigateToListingDetail(Listing listing) {
        if (getActivity() instanceof MainMenu) {
            ListingDetailFragment detailFragment = ListingDetailFragment.newInstance(listing.getId());
            ((MainMenu) getActivity()).replaceFragment(detailFragment);
        }
    }
    
    // Helper classes for dialogs
    
    // Category model and adapter
    private static class Category {
        private final Long id;
        private final String name;
        
        public Category(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
    }
    
    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        
        private final List<Category> categories;
        private Long selectedCategoryId = null;
        private OnCategoryClickListener listener;
        
        public interface OnCategoryClickListener {
            void onCategoryClick(Category category);
        }
        
        public CategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }
        
        public void setSelectedCategoryId(Long id) {
            this.selectedCategoryId = id;
            notifyDataSetChanged();
        }
        
        public void setOnCategoryClickListener(OnCategoryClickListener listener) {
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.bind(category, selectedCategoryId != null && selectedCategoryId.equals(category.getId()));
        }
        
        @Override
        public int getItemCount() {
            return categories.size();
        }
        
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvCategory;
            
            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
            }
            
            public void bind(Category category, boolean isSelected) {
                tvCategory.setText(category.getName());
                
                if (isSelected) {
                    tvCategory.setBackgroundResource(R.drawable.check);
                    tvCategory.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_color));
                } else {
                    tvCategory.setBackgroundResource(R.drawable.unselect);
                    tvCategory.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
                }
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCategoryClick(category);
                    }
                });
            }
        }
    }
    
    // Location adapter
    private static class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {
        
        private final List<String> locations;
        private OnLocationClickListener listener;
        
        public interface OnLocationClickListener {
            void onLocationClick(String location);
        }
        
        public LocationAdapter(List<String> locations) {
            this.locations = locations;
        }
        
        public void setOnLocationClickListener(OnLocationClickListener listener) {
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
            return new LocationViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
            holder.bind(locations.get(position));
        }
        
        @Override
        public int getItemCount() {
            return locations.size();
        }
        
        class LocationViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvLocation;
            
            public LocationViewHolder(@NonNull View itemView) {
                super(itemView);
                tvLocation = itemView.findViewById(R.id.tvLocation);
            }
            
            public void bind(String location) {
                tvLocation.setText(location);
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onLocationClick(location);
                    }
                });
            }
        }
    }
    
    // Data providers
    private List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category(1L, "Điện tử"));
        categories.add(new Category(2L, "Thời trang"));
        categories.add(new Category(3L, "Đồ gia dụng"));
        categories.add(new Category(4L, "Thể thao & Giải trí"));
        categories.add(new Category(5L, "Đồ trẻ em"));
        categories.add(new Category(6L, "Sách & Văn phòng phẩm"));
        categories.add(new Category(7L, "Khác"));
        return categories;
    }
    
    private List<String> getPopularLocations() {
        List<String> locations = new ArrayList<>();
        locations.add("Hà Nội");
        locations.add("TP. Hồ Chí Minh");
        locations.add("Đà Nẵng");
        locations.add("Hải Phòng");
        locations.add("Cần Thơ");
        locations.add("Huế");
        locations.add("Nha Trang");
        locations.add("Vũng Tàu");
        return locations;
    }
    
    // SearchCategoryAdapter class riêng cho màn hình tìm kiếm
    private static class SearchCategoryAdapter extends RecyclerView.Adapter<SearchCategoryAdapter.CategoryViewHolder> {
        
        private final List<Category> categories;
        private Long selectedCategoryId = null;
        private OnCategoryClickListener listener;
        
        public interface OnCategoryClickListener {
            void onCategoryClick(Category category);
        }
        
        public SearchCategoryAdapter(List<Category> categories) {
            this.categories = categories;
        }
        
        public void setSelectedCategoryId(Long id) {
            this.selectedCategoryId = id;
            notifyDataSetChanged();
        }
        
        public void setOnCategoryClickListener(OnCategoryClickListener listener) {
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.bind(category, selectedCategoryId != null && selectedCategoryId.equals(category.getId()));
        }
        
        @Override
        public int getItemCount() {
            return categories.size();
        }
        
        class CategoryViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvCategory;
            
            public CategoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
            }
            
            public void bind(Category category, boolean isSelected) {
                tvCategory.setText(category.getName());
                
                if (isSelected) {
                    tvCategory.setBackgroundResource(R.drawable.bg_category_selected);
                    tvCategory.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_color));
                } else {
                    tvCategory.setBackgroundResource(R.drawable.bg_category_unselected);
                    tvCategory.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary));
                }
                
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCategoryClick(category);
                    }
                });
            }
        }
    }
}