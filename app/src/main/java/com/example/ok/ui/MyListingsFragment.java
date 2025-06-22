package com.example.ok.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ListingAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.Listing;
import com.example.ok.model.PagedApiResponse;
import com.example.ok.ui.CreateListingFragment;
import com.example.ok.ui.ListingDetailFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyListingsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabCreate;
    private ListingAdapter adapter;
    private ApiService apiService;

    private Button btnAll, btnAvailable, btnSold, btnPaused, btnExpired;
    private TextView tvTotalListings, tvTotalViews, tvSoldCount;
    private LinearLayout layoutEmpty;
    private Button btnCreateFirst;
    private ImageButton btnFilter, btnSort;
    private String currentFilter = "ALL";
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_listings, container, false);        initViews(view);
        setupRecyclerView();
        setupClickListeners();

        // Initialize RetrofitClient before using API services
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();
        loadMyListings();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        fabCreate = view.findViewById(R.id.fab_create);

        // Filter buttons
        btnAll = view.findViewById(R.id.btn_all);
        btnAvailable = view.findViewById(R.id.btn_available);
        btnSold = view.findViewById(R.id.btn_sold);
        btnPaused = view.findViewById(R.id.btn_paused);
        btnExpired = view.findViewById(R.id.btn_expired);

        // Stats
        tvTotalListings = view.findViewById(R.id.tv_total_listings);
        tvTotalViews = view.findViewById(R.id.tv_total_views);
        tvSoldCount = view.findViewById(R.id.tv_sold_count);

        // Empty state
        layoutEmpty = view.findViewById(R.id.layout_empty);
        btnCreateFirst = view.findViewById(R.id.btn_create_first);

        // Other buttons
        btnFilter = view.findViewById(R.id.btn_filter);
        btnSort = view.findViewById(R.id.btn_sort);
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);        adapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {            @Override
            public void onListingClick(Listing listing) {
                // Navigate to listing detail
                if (getActivity() instanceof MainMenu) {
                    ListingDetailFragment fragment = ListingDetailFragment.newInstance(listing.getId());
                    ((MainMenu) getActivity()).replaceFragment(fragment);
                }
            }

            @Override
            public void onListingLongClick(Listing listing) {
                showListingOptions(listing);
            }
        });
    }    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadMyListings);

        fabCreate.setOnClickListener(v -> {
            if (getActivity() instanceof MainMenu) {
                ((MainMenu) getActivity()).navigateToCreateListing();
            }
        });
        
        // 🔥 THÊM: Filter button listeners
        btnAll.setOnClickListener(v -> filterListings("ALL"));
        btnAvailable.setOnClickListener(v -> filterListings("AVAILABLE"));
        btnSold.setOnClickListener(v -> filterListings("SOLD"));
        btnPaused.setOnClickListener(v -> filterListings("PAUSED"));
        btnExpired.setOnClickListener(v -> filterListings("EXPIRED"));
        
        // Empty state button
        if (btnCreateFirst != null) {
            btnCreateFirst.setOnClickListener(v -> {
                if (getActivity() instanceof MainMenu) {
                    ((MainMenu) getActivity()).navigateToCreateListing();
                }
            });
        }
    }

    private void loadMyListings() {
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE); // Đã đúng, giữ nguyên
        long userId = prefs.getLong("userId", -1);

        if (userId == -1) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }        Call<PagedApiResponse<Listing>> call = apiService.getUserListings(userId, currentFilter, 0, 100);
        call.enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<Listing>> call,
                                   Response<PagedApiResponse<Listing>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<Listing> pagedResponse = response.body();
                    if (pagedResponse.isSuccess()) {
                        List<Listing> listings = pagedResponse.getData() != null ? pagedResponse.getData() : new ArrayList<>();
                        adapter.setListings(listings);
                        updateStats(listings);
                    } else {
                        Toast.makeText(getContext(), pagedResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        updateStats(new ArrayList<>());
                    }
                } else {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show();
                    updateStats(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<PagedApiResponse<Listing>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                updateStats(new ArrayList<>());
            }
        });
    }    private void editListing(Listing listing) {
        // Navigate to CreateListingFragment in edit mode
        if (getActivity() instanceof MainMenu) {
            CreateListingFragment editFragment = CreateListingFragment.newInstanceForEdit(listing);
            ((MainMenu) getActivity()).replaceFragment(editFragment);
        }
    }

    private void showListingOptions(Listing listing) {
        String[] options = {"Xem chi tiết", "Chỉnh sửa", "Tạm dừng", "Xóa"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(listing.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {                        case 0: // Xem chi tiết
                            if (getActivity() instanceof MainMenu) {
                                ListingDetailFragment fragment = ListingDetailFragment.newInstance(listing.getId());
                                ((MainMenu) getActivity()).replaceFragment(fragment);
                            }
                            break;
                        case 1: // Chỉnh sửa
                            editListing(listing);
                            break;
                        case 2: // Tạm dừng
                            pauseListing(listing);
                            break;
                        case 3: // Xóa
                            deleteListing(listing);
                            break;
                    }
                })
                .show();
    }
    
    private void pauseListing(Listing listing) {
        Toast.makeText(getContext(), "Tạm dừng: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Implement pause listing API call
    }
    
    private void deleteListing(Listing listing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xóa tin đăng")
                .setMessage("Bạn có chắc chắn muốn xóa tin đăng \"" + listing.getTitle() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Toast.makeText(getContext(), "Đã xóa: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
                    // TODO: Implement delete listing API call
                    loadMyListings(); // Refresh list
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void filterListings(String status) {
        currentFilter = status;
        updateFilterButtons(status);
        loadMyListings();
    }
    
    private void updateFilterButtons(String selectedStatus) {
        // Reset all buttons
        btnAll.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnAll.setTextColor(getResources().getColor(R.color.text_secondary));
        
        btnAvailable.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnAvailable.setTextColor(getResources().getColor(R.color.text_secondary));
        
        btnSold.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnSold.setTextColor(getResources().getColor(R.color.text_secondary));
        
        btnPaused.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnPaused.setTextColor(getResources().getColor(R.color.text_secondary));
        
        btnExpired.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnExpired.setTextColor(getResources().getColor(R.color.text_secondary));
        
        // Set selected button
        Button selectedButton = null;
        switch (selectedStatus) {
            case "ALL":
                selectedButton = btnAll;
                break;
            case "AVAILABLE":
                selectedButton = btnAvailable;
                break;
            case "SOLD":
                selectedButton = btnSold;
                break;
            case "PAUSED":
                selectedButton = btnPaused;
                break;
            case "EXPIRED":
                selectedButton = btnExpired;
                break;
        }
        
        if (selectedButton != null) {
            selectedButton.setBackgroundResource(R.drawable.bg_filter_selected);
            selectedButton.setTextColor(getResources().getColor(R.color.white));
        }
    }
    
    private void updateStats(List<Listing> listings) {
        if (listings == null) {
            tvTotalListings.setText("0");
            tvTotalViews.setText("0");
            tvSoldCount.setText("0");
            return;
        }
        
        int totalListings = listings.size();
        int totalViews = 0;
        int soldCount = 0;
        
        for (Listing listing : listings) {
            if (listing.getViewCount() != null) {
                totalViews += listing.getViewCount();
            }
            if ("SOLD".equals(listing.getStatus())) {
                soldCount++;
            }
        }
        
        tvTotalListings.setText(String.valueOf(totalListings));
        tvTotalViews.setText(String.valueOf(totalViews));
        tvSoldCount.setText(String.valueOf(soldCount));
        
        // Show/hide empty state
        if (totalListings == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}