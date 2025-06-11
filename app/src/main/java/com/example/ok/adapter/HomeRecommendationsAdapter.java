package com.example.ok.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ok.R;
import com.example.ok.model.CategoryWithListingsResponse;
import com.example.ok.model.Listing;
import java.util.ArrayList;
import java.util.List;

public class HomeRecommendationsAdapter extends RecyclerView.Adapter<HomeRecommendationsAdapter.CategoryViewHolder> {
    private final Context context;
    private List<CategoryWithListingsResponse> categories = new ArrayList<>();
    private ListingAdapter.OnListingClickListener listingClickListener;

    public HomeRecommendationsAdapter(Context context) {
        this.context = context;
    }

    public void setCategories(List<CategoryWithListingsResponse> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnListingClickListener(ListingAdapter.OnListingClickListener listener) {
        this.listingClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_category_with_listings, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryWithListingsResponse category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryName;
        private final RecyclerView rvListings;
        private ListingAdapter listingAdapter;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            rvListings = itemView.findViewById(R.id.rvListings);
        }

        public void bind(CategoryWithListingsResponse category) {
            tvCategoryName.setText(category.getCategoryName());
            listingAdapter = new ListingAdapter(context, true);
            listingAdapter.setListings(category.getListings());
            listingAdapter.setOnListingClickListener(listingClickListener);
            rvListings.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            rvListings.setAdapter(listingAdapter);
        }
    }
}
