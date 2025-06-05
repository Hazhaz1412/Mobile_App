package com.example.ok.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.ok.MainMenu; // Thêm import này
import com.example.ok.R;
import com.example.ok.adapter.DetailImageAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class ListingDetailFragment extends Fragment {

    private static final String TAG = "ListingDetailFragment";
    private static final String ARG_LISTING_ID = "listingId";

    // Views
    private ViewPager2 vpImages;
    private TextView tvImageCounter;
    private TextView tvTitle, tvPrice, tvDescription, tvLocation, tvViews, tvCreatedDate;
    private TextView tvCategory, tvCondition, tvStatus;
    private FlexboxLayout flexboxTags;
    private CircleImageView ivSellerAvatar;
    private TextView tvSellerName, tvSellerJoinDate;
    private RatingBar rbSellerRating;
    private Button btnContact, btnFavorite, btnShare, btnReport;
    private FloatingActionButton fabEdit;
    private LinearLayout layoutSellerInfo, layoutActions, layoutLoading;
    private ScrollView scrollViewContent;

    // Data
    private Listing listing;
    private User seller;
    private List<String> imageUrls = new ArrayList<>();
    private boolean isCurrentUserListing = false;
    private boolean isFavorited = false;
    private long listingId;

    // Services
    private ListingApiService listingApiService;
    private ApiService apiService;

    public static ListingDetailFragment newInstance(long listingId) {
        ListingDetailFragment fragment = new ListingDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listingId = getArguments().getLong(ARG_LISTING_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initServices();
        initViews(view);
        setupClickListeners();

        if (listingId != -1) {
            loadListingDetail(listingId);
        } else {
            showError("Không tìm thấy sản phẩm");
            navigateBack();
        }
    }

    private void initServices() {
        try {
            listingApiService = RetrofitClient.getListingApiService();
            apiService = RetrofitClient.getApiService();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing services", e);
            showError("Lỗi khởi tạo dịch vụ");
        }
    }

    private void initViews(View view) {
        // Loading and content views
        layoutLoading = view.findViewById(R.id.layoutLoading);
        scrollViewContent = view.findViewById(R.id.scrollViewContent);

        // Image gallery
        vpImages = view.findViewById(R.id.vpImages);
        tvImageCounter = view.findViewById(R.id.tvImageCounter);

        // Product info
        tvTitle = view.findViewById(R.id.tvTitle);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvViews = view.findViewById(R.id.tvViews);
        tvCreatedDate = view.findViewById(R.id.tvCreatedDate);

        // Categories and tags
        tvCategory = view.findViewById(R.id.tvCategory);
        tvCondition = view.findViewById(R.id.tvCondition);
        tvStatus = view.findViewById(R.id.tvStatus);
        flexboxTags = view.findViewById(R.id.flexboxTags);

        // Seller info
        layoutSellerInfo = view.findViewById(R.id.layoutSellerInfo);
        ivSellerAvatar = view.findViewById(R.id.ivSellerAvatar);
        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvSellerJoinDate = view.findViewById(R.id.tvSellerJoinDate);
        rbSellerRating = view.findViewById(R.id.rbSellerRating);

        // Action buttons
        layoutActions = view.findViewById(R.id.layoutActions);
        btnContact = view.findViewById(R.id.btnContact);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnShare = view.findViewById(R.id.btnShare);
        btnReport = view.findViewById(R.id.btnReport);
        fabEdit = view.findViewById(R.id.fabEdit);

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> navigateBack());
    }

    private void setupClickListeners() {
        // Contact seller
        btnContact.setOnClickListener(v -> contactSeller());

        // Add to favorites
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // Share listing
        btnShare.setOnClickListener(v -> shareListing());

        // Report listing
        btnReport.setOnClickListener(v -> reportListing());

        // Edit listing (if owner)
        fabEdit.setOnClickListener(v -> editListing());

        // View seller profile
        layoutSellerInfo.setOnClickListener(v -> viewSellerProfile());
    }

    private void loadListingDetail(long listingId) {
        Log.d(TAG, "Loading listing detail for ID: " + listingId);

        showLoadingState();

        Call<ApiResponse> call = listingApiService.getListingById(listingId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                hideLoadingState();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            parseListing(apiResponse.getData());
                            displayListingInfo();
                            loadSellerInfo();
                            checkOwnership();
                            setupImageGallery();

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing listing data", e);
                            showError("Lỗi xử lý dữ liệu sản phẩm");
                        }
                    } else {
                        showError(apiResponse.getMessage());
                    }
                } else {
                    showError("Không thể tải thông tin sản phẩm");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                hideLoadingState();
                Log.e(TAG, "Network error loading listing", t);
                showError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void parseListing(Object data) {
        if (data instanceof Listing) {
            listing = (Listing) data;
        } else {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(data);
            listing = gson.fromJson(json, Listing.class);
        }
    }

    private void displayListingInfo() {
        if (listing == null) return;

        // Basic info
        tvTitle.setText(listing.getTitle());

        // Format price
        DecimalFormat formatter = new DecimalFormat("#,###");
        tvPrice.setText(formatter.format(listing.getPrice()) + " VNĐ");

        tvDescription.setText(listing.getDescription());
        tvLocation.setText(listing.getLocationText() != null ? listing.getLocationText() : "Không xác định");

        // Views and date
        tvViews.setText(listing.getViews() + " lượt xem");

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (listing.getCreatedAt() != null) {
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date createdDate = apiFormat.parse(listing.getCreatedAt());
                tvCreatedDate.setText("Đăng ngày " + dateFormat.format(createdDate));
            }
        } catch (Exception e) {
            // Fallback nếu không parse được date
            tvCreatedDate.setText("Đăng gần đây");
            Log.d(TAG, "Could not parse date: " + listing.getCreatedAt());
        }

        // Category and condition
        if (listing.getCategory() != null) {
            tvCategory.setText(listing.getCategory().getName());
        }

        if (listing.getCondition() != null) {
            tvCondition.setText(listing.getCondition().getName());
        }

        // Status
        tvStatus.setText(listing.getStatus());
        updateStatusColor();

        // Tags
        displayTags();

        // Images
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            imageUrls.clear();
            for (ListingImage image : listing.getImages()) {
                imageUrls.add(image.getImageUrl());
            }
        }
    }

    private void displayTags() {
        flexboxTags.removeAllViews();

        if (listing.getTags() != null && !listing.getTags().isEmpty()) {
            for (String tag : listing.getTags()) {
                Chip chip = new Chip(requireContext());
                chip.setText("#" + tag);
                chip.setChipBackgroundColorResource(R.color.primary_color);
                chip.setTextColor(getResources().getColor(R.color.white));
                chip.setTextSize(12);

                FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 4, 8, 4);
                chip.setLayoutParams(params);

                flexboxTags.addView(chip);
            }
            flexboxTags.setVisibility(View.VISIBLE);
        } else {
            flexboxTags.setVisibility(View.GONE);
        }
    }

    private void setupImageGallery() {
        if (imageUrls.isEmpty()) {
            imageUrls.add(""); // Empty URL for placeholder
        }

        DetailImageAdapter imageAdapter = new DetailImageAdapter(requireContext(), imageUrls);
        vpImages.setAdapter(imageAdapter);

        updateImageCounter();

        vpImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageCounter();
            }
        });
    }

    private void updateImageCounter() {
        int currentPosition = vpImages.getCurrentItem() + 1;
        int totalImages = imageUrls.size();

        if (totalImages > 1) {
            tvImageCounter.setText(currentPosition + "/" + totalImages);
            tvImageCounter.setVisibility(View.VISIBLE);
        } else {
            tvImageCounter.setVisibility(View.GONE);
        }
    }

    private void loadSellerInfo() {
        if (listing == null || listing.getUserId() == null) return;

        Call<UserProfileResponse> call = apiService.getUserProfile(listing.getUserId());
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse userResponse = response.body();
                    if (userResponse.isSuccess()) {
                        seller = userResponse.getData();
                        displaySellerInfo();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading seller info", t);
            }
        });
    }

    private void displaySellerInfo() {
        if (seller == null) return;

        tvSellerName.setText(seller.getDisplayName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        if (seller.getCreatedAt() != null) {
            tvSellerJoinDate.setText("Thành viên từ " + dateFormat.format(seller.getCreatedAt()));
        } else {
            tvSellerJoinDate.setText("Thành viên từ 2024");
        }

        // Avatar
        if (seller.getAvatarUrl() != null && !seller.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(seller.getAvatarUrl())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(ivSellerAvatar);
        } else {
            ivSellerAvatar.setImageResource(R.drawable.user);
        }

        rbSellerRating.setRating(4.5f);
    }

    private void checkOwnership() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long currentUserId = prefs.getLong("userId", -1);

        if (listing != null && listing.getUserId() != null) {
            isCurrentUserListing = (currentUserId == listing.getUserId());

            if (isCurrentUserListing) {
                btnContact.setVisibility(View.GONE);
                fabEdit.setVisibility(View.VISIBLE);
                layoutSellerInfo.setVisibility(View.GONE);
            } else {
                btnContact.setVisibility(View.VISIBLE);
                fabEdit.setVisibility(View.GONE);
                layoutSellerInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateStatusColor() {
        if (listing == null) return;

        String status = listing.getStatus();
        int colorRes;

        switch (status) {
            case "AVAILABLE":
                colorRes = R.color.status_available;
                break;
            case "SOLD":
                colorRes = R.color.status_sold;
                break;
            case "PAUSED":
                colorRes = R.color.status_paused;
                break;
            default:
                colorRes = R.color.text_secondary;
                break;
        }

        tvStatus.setTextColor(getResources().getColor(colorRes));
    }

    private void contactSeller() {
        if (seller == null) {
            Toast.makeText(requireContext(), "Không thể liên hệ người bán", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Liên hệ người bán")
                .setMessage("Bạn muốn liên hệ với " + seller.getDisplayName() + " qua:")
                .setPositiveButton("Tin nhắn", (dialog, which) -> {
                    // TODO: Navigate to chat
                    Toast.makeText(requireContext(), "Tính năng chat đang phát triển", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Gọi điện", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Tính năng gọi điện đang phát triển", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleFavorite() {
        isFavorited = !isFavorited;
        updateFavoriteButton();

        String message = isFavorited ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateFavoriteButton() {
        if (isFavorited) {
            btnFavorite.setText("❤️");
            btnFavorite.setBackgroundTintList(requireContext().getColorStateList(R.color.status_sold));
        } else {
            btnFavorite.setText("🤍");
            btnFavorite.setBackgroundTintList(requireContext().getColorStateList(R.color.text_secondary));
        }
    }

    private void shareListing() {
        if (listing == null) return;

        String shareText = listing.getTitle() + "\n" +
                "Giá: " + new DecimalFormat("#,###").format(listing.getPrice()) + " VNĐ\n" +
                "Mô tả: " + listing.getDescription() + "\n\n" +
                "Xem chi tiết tại ứng dụng OK";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sản phẩm: " + listing.getTitle());

        startActivity(Intent.createChooser(shareIntent, "Chia sẻ sản phẩm"));
    }

    private void reportListing() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo tin đăng")
                .setMessage("Bạn có chắc muốn báo cáo tin đăng này?")
                .setPositiveButton("Báo cáo", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Đã gửi báo cáo", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void editListing() {
        if (listing == null) return;

        // TODO: Navigate to edit listing fragment with data
        Toast.makeText(requireContext(), "Tính năng chỉnh sửa đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void viewSellerProfile() {
        if (seller == null) return;

        // TODO: Navigate to seller profile fragment
        Toast.makeText(requireContext(), "Xem profile của " + seller.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    private void showLoadingState() {
        layoutLoading.setVisibility(View.VISIBLE);
        scrollViewContent.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        layoutLoading.setVisibility(View.GONE);
        scrollViewContent.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            // Navigate to home if no back stack
            if (getActivity() instanceof MainMenu) {
                ((MainMenu) getActivity()).navigateToTab("home");
            }
        }
    }
}