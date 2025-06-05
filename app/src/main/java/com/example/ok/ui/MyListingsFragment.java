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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        View view = inflater.inflate(R.layout.fragment_my_listings, container, false);

        initViews(view);
        setupRecyclerView();
        setupClickListeners();

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
        recyclerView.setAdapter(adapter);

        adapter.setOnListingClickListener(new ListingAdapter.OnListingClickListener() {
            @Override
            public void onListingClick(Listing listing) {
                editListing(listing);
            }

            @Override
            public void onListingLongClick(Listing listing) {
                showListingOptions(listing);
            }
        });
    }

    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadMyListings);

        fabCreate.setOnClickListener(v -> {
            if (getActivity() instanceof MainMenu) {
                ((MainMenu) getActivity()).navigateToCreateListing();
            }
        });
    }

    private void loadMyListings() {
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE); // Đã đúng, giữ nguyên
        long userId = prefs.getLong("userId", -1);

        if (userId == -1) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<PagedApiResponse<Listing>> call = apiService.getUserListings(userId, "ALL", 0, 20);
        call.enqueue(new Callback<PagedApiResponse<Listing>>() {
            @Override
            public void onResponse(Call<PagedApiResponse<Listing>> call,
                                   Response<PagedApiResponse<Listing>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    PagedApiResponse<Listing> pagedResponse = response.body();
                    if (pagedResponse.isSuccess()) {
                        adapter.setListings(pagedResponse.getData());
                    } else {
                        Toast.makeText(getContext(), pagedResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<PagedApiResponse<Listing>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editListing(Listing listing) {
        Toast.makeText(getContext(), "Chỉnh sửa: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void showListingOptions(Listing listing) {
        Toast.makeText(getContext(), "Tùy chọn cho: " + listing.getTitle(), Toast.LENGTH_SHORT).show();
    }
}