package com.example.ok.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.Listing;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private Context context;
    private List<Listing> listings;
    private OnListingClickListener listener;
    private boolean isGridLayout = false; // Flag để xác định layout type

    public interface OnListingClickListener {
        void onListingClick(Listing listing);
        void onListingLongClick(Listing listing);
    }

    public ListingAdapter(Context context) {
        this.context = context;
        this.listings = new ArrayList<>();
    }

    public ListingAdapter(Context context, boolean isGridLayout) {
        this.context = context;
        this.listings = new ArrayList<>();
        this.isGridLayout = isGridLayout;
    }

    public void setOnListingClickListener(OnListingClickListener listener) {
        this.listener = listener;
    }

    public void setListings(List<Listing> listings) {
        this.listings.clear();
        if (listings != null) {
            this.listings.addAll(listings);
        }
        notifyDataSetChanged();

        Log.d("ListingAdapter", "Set listings count: " + this.listings.size());
        for (int i = 0; i < Math.min(3, this.listings.size()); i++) {
            Listing listing = this.listings.get(i);
            Log.d("ListingAdapter", "Listing " + i + ": " + listing.getTitle());
        }
    }

    public void addListings(List<Listing> newListings) {
        int startPosition = this.listings.size();
        this.listings.addAll(newListings);
        notifyItemRangeInserted(startPosition, newListings.size());
    }

    public void clear() {
        this.listings.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout khác nhau tùy vào grid hay list
        int layoutId = isGridLayout ? R.layout.item_listing_grid : R.layout.item_listing;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        Log.d("ListingAdapter", "Binding position " + position + ": " + listing.getTitle());
        holder.bind(listing);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public class ListingViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPrimary;
        private TextView tvTitle;
        private TextView tvPrice;
        private TextView tvLocation;
        private TextView tvCategory;
        private TextView tvCondition;
        private TextView tvViews;
        private TextView tvStatus;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);

            ivPrimary = itemView.findViewById(R.id.iv_primary);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvCondition = itemView.findViewById(R.id.tv_condition);
            tvViews = itemView.findViewById(R.id.tv_views);
            tvStatus = itemView.findViewById(R.id.tv_status);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onListingClick(listings.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onListingLongClick(listings.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Listing listing) {
            try {
                // Set title
                if (tvTitle != null) {
                    tvTitle.setText(listing.getTitle() != null ? listing.getTitle() : "Không có tiêu đề");
                }

                // Format and set price
                if (tvPrice != null && listing.getPrice() != null) {
                    DecimalFormat formatter = new DecimalFormat("#,###");
                    tvPrice.setText(formatter.format(listing.getPrice()) + " VNĐ");
                } else if (tvPrice != null) {
                    tvPrice.setText("Liên hệ");
                }

                // Set location
                if (tvLocation != null) {
                    tvLocation.setText(listing.getLocationText() != null ?
                            listing.getLocationText() : "Không xác định");
                }

                // Set category
                if (tvCategory != null) {
                    tvCategory.setText(listing.getCategoryName() != null ?
                            listing.getCategoryName() : "Khác");
                }

                // Set condition
                if (tvCondition != null) {
                    tvCondition.setText(listing.getConditionName() != null ?
                            listing.getConditionName() : "Không rõ");
                }

                // Set views
                if (tvViews != null) {
                    int viewCount = listing.getViewCount() != null ? listing.getViewCount() : 0;
                    tvViews.setText(viewCount + " lượt xem");
                }

                // Set status
                if (tvStatus != null) {
                    String status = listing.getStatus() != null ? listing.getStatus() : "AVAILABLE";
                    tvStatus.setText(getStatusText(status));
                    tvStatus.setTextColor(getStatusColor(status));
                }

                // Load primary image - SỬA CHÍNH CHỖ NÀY
                if (ivPrimary != null) {
                    String imageUrl = null;

                    // Try to get image from different sources
                    if (listing.getPrimaryImageUrl() != null && !listing.getPrimaryImageUrl().isEmpty()) {
                        imageUrl = listing.getPrimaryImageUrl();
                    } else if (listing.getImageUrls() != null && !listing.getImageUrls().isEmpty()) {
                        // SỬA: Sử dụng getImageUrls() thay vì getImages()
                        imageUrl = listing.getImageUrls().get(0);
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Log.d("ListingAdapter", "Loading image: " + imageUrl);

                        Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.image)
                                .error(R.drawable.image)
                                .centerCrop()
                                .into(ivPrimary);
                    } else {
                        Log.d("ListingAdapter", "No image available for: " + listing.getTitle());
                        ivPrimary.setImageResource(R.drawable.image);
                    }
                }

            } catch (Exception e) {
                Log.e("ListingAdapter", "Error binding listing: " + listing.getTitle(), e);
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "AVAILABLE": return "Có sẵn";
                case "SOLD": return "Đã bán";
                case "PAUSED": return "Tạm dừng";
                default: return status;
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "AVAILABLE": return context.getResources().getColor(R.color.status_available, null);
                case "SOLD": return context.getResources().getColor(R.color.status_sold, null);
                case "PAUSED": return context.getResources().getColor(R.color.status_paused, null);
                default: return context.getResources().getColor(R.color.black, null);
            }
        }
    }
}