package com.example.ok.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ok.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment<BannerItem> extends Fragment {

    private ViewPager2 bannerViewPager;
    private androidx.recyclerview.widget.RecyclerView categoriesRecyclerView;
    private androidx.recyclerview.widget.RecyclerView featuredItemsRecyclerView;
    private androidx.recyclerview.widget.RecyclerView recentItemsRecyclerView;
    private FloatingActionButton fabAddItem;

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
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        featuredItemsRecyclerView = view.findViewById(R.id.featuredItemsRecyclerView);
        recentItemsRecyclerView = view.findViewById(R.id.recentItemsRecyclerView);
        fabAddItem = view.findViewById(R.id.fabAddItem);

        // Search bar click listener
        view.findViewById(R.id.searchBar).setOnClickListener(v -> {
            // Chuyển đến màn hình tìm kiếm
            Toast.makeText(requireContext(), "Tìm kiếm", Toast.LENGTH_SHORT).show();
        });

        // FAB click listener
        fabAddItem.setOnClickListener(v -> {
            // Mở màn hình đăng tin mới
            Toast.makeText(requireContext(), "Thêm tin đăng mới", Toast.LENGTH_SHORT).show();
        });

        // Set up see all buttons
        setupSeeAllButtons(view);
    }

    private void setupSeeAllButtons(View view) {
        view.findViewById(R.id.tvSeeAllFeatured).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Xem tất cả sản phẩm nổi bật", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.tvSeeAllRecent).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Xem tất cả sản phẩm gần đây", Toast.LENGTH_SHORT).show();
        });
    }


}