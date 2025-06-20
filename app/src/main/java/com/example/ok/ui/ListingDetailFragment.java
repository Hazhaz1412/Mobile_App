package com.example.ok.ui;

import android.content.Context;
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
    private Button btnContact, btnFavorite, btnShare, btnReport, btnBuy;
    private Button btnMakeOffer; // Add offer button
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
            // Initialize RetrofitClient before using any API services
            RetrofitClient.init(requireContext());
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

        layoutActions = view.findViewById(R.id.layoutActions);
        btnContact = view.findViewById(R.id.btnContact);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnShare = view.findViewById(R.id.btnShare);
        btnReport = view.findViewById(R.id.btnReport);
        btnBuy = view.findViewById(R.id.btnBuy);
        btnMakeOffer = view.findViewById(R.id.btnMakeOffer);
        fabEdit = view.findViewById(R.id.fabEdit);

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
        
        // Also make seller name and avatar clickable individually
        tvSellerName.setOnClickListener(v -> viewSellerProfile());
        ivSellerAvatar.setOnClickListener(v -> viewSellerProfile());

        // Buy now
        btnBuy.setOnClickListener(v -> buyListing());
        
        // Make offer
        btnMakeOffer.setOnClickListener(v -> showMakeOfferDialog());
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
                    Log.d(TAG, "API Response: success=" + apiResponse.isSuccess() + ", message=" + apiResponse.getMessage());
                    
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
                    Log.e(TAG, "API call failed with code: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Could not read error body", e);
                        }
                    }
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
            Log.d(TAG, "Listing JSON data: " + json);
            listing = gson.fromJson(json, Listing.class);
        }
        
        // Check if userDisplayName is received correctly
        if (listing != null) {
            Log.d(TAG, "Parsed listing - userDisplayName: " + listing.getUserDisplayName() +
                    ", userId: " + listing.getUserId());
        }
    }

    private void displayListingInfo() {
        if (listing == null) return;

        tvTitle.setText(listing.getTitle());

        DecimalFormat formatter = new DecimalFormat("#,###");
        tvPrice.setText(formatter.format(listing.getPrice()) + " VNĐ");

        tvDescription.setText(listing.getDescription());
        tvLocation.setText(listing.getLocationText() != null ? listing.getLocationText() : "Không xác định");

        tvViews.setText(listing.getViews() + " lượt xem");

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            if (listing.getCreatedAt() != null) {
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date createdDate = apiFormat.parse(listing.getCreatedAt());
                tvCreatedDate.setText("Đăng ngày " + dateFormat.format(createdDate));
            }
        } catch (Exception e) {
            tvCreatedDate.setText("Đăng gần đây");
            Log.d(TAG, "Could not parse date: " + listing.getCreatedAt());
        }

        if (listing.getCategory() != null) {
            tvCategory.setText(listing.getCategory().getName());
        }

        if (listing.getCondition() != null) {
            tvCondition.setText(listing.getCondition().getName());
        }

        tvStatus.setText(listing.getStatus());
        updateStatusColor();

        displayTags();

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
            imageUrls.add("");
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
        if (listing == null || listing.getUserId() == null) {
            Log.d(TAG, "Cannot load seller info: listing is null or userId is null");
            return;
        }

        Log.d(TAG, "Loading seller info for userId: " + listing.getUserId());
        Call<UserProfileResponse> call = apiService.getUserProfile(listing.getUserId());
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse userResponse = response.body();
                    
                    // Log raw response data for debugging
                    Log.d(TAG, "Seller API raw data: userId=" + userResponse.getUserId() + 
                          ", displayName=" + userResponse.getDisplayName());
                    
                    // The success check is now improved in UserProfileResponse class
                    Log.d(TAG, "Seller API response: success=" + userResponse.isSuccess());
                    
                    if (userResponse.isSuccess()) {
                        seller = userResponse.getData();
                        if (seller != null) {
                            Log.d(TAG, "Loaded seller info: displayName=" + seller.getDisplayName() + 
                                    ", avatarUrl=" + seller.getAvatarUrl());
                        } else {
                            Log.d(TAG, "Seller data is null even though response was successful");
                        }
                        displaySellerInfo();
                    } else {
                        Log.d(TAG, "Seller API response was not successful");
                        displaySellerInfo();
                    }
                } else {
                    Log.e(TAG, "Failed to load seller info, HTTP status: " + response.code());
                    displaySellerInfo();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading seller info", t);
                displaySellerInfo();
            }
        });
    }

    private void displaySellerInfo() {
        if (listing != null) {
            Log.d(TAG, "userDisplayName from listing: " + listing.getUserDisplayName() + 
                   ", userId: " + listing.getUserId());
        }
        
        if (seller != null && seller.getDisplayName() != null && !seller.getDisplayName().isEmpty()) {
            Log.d(TAG, "Displaying seller from API data: " + seller.getDisplayName());
            tvSellerName.setText(seller.getDisplayName());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
            if (seller.getCreatedAt() != null) {
                tvSellerJoinDate.setText("Thành viên từ " + dateFormat.format(seller.getCreatedAt()));
            } else {
                tvSellerJoinDate.setText("Thành viên từ 2024");
            }
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
        } else if (listing != null && listing.getUserDisplayName() != null && !listing.getUserDisplayName().isEmpty()) {
            tvSellerName.setText(listing.getUserDisplayName());
            tvSellerJoinDate.setText("Thành viên");
            ivSellerAvatar.setImageResource(R.drawable.user);
        } else {
            // Fallback to local user data if we're looking at our own listing
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            long currentUserId = prefs.getLong("userId", -1);
            
            if (listing != null && listing.getUserId() != null && currentUserId == listing.getUserId()) {
                String currentUserDisplayName = prefs.getString("displayName", "");
                if (!currentUserDisplayName.isEmpty()) {
                    tvSellerName.setText(currentUserDisplayName);
                    tvSellerJoinDate.setText("Thành viên");
                    
                    String avatarUrl = prefs.getString("avatarUrl", "");
                    if (!avatarUrl.isEmpty()) {
                        Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .circleCrop()
                            .into(ivSellerAvatar);
                    } else {
                        ivSellerAvatar.setImageResource(R.drawable.user);
                    }
                } else {
                    tvSellerName.setText("Bạn");
                    tvSellerJoinDate.setText("");
                    ivSellerAvatar.setImageResource(R.drawable.user);
                }
            } else {
                tvSellerName.setText("Không rõ người đăng");
                tvSellerJoinDate.setText("");
                ivSellerAvatar.setImageResource(R.drawable.user);
            }
        }

        rbSellerRating.setRating(4.5f);
    }    private void checkOwnership() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long currentUserId = prefs.getLong("userId", -1);

        if (listing != null && listing.getUserId() != null) {
            isCurrentUserListing = (currentUserId == listing.getUserId());

            if (isCurrentUserListing) {
                btnContact.setVisibility(View.GONE);
                btnBuy.setVisibility(View.GONE);
                btnMakeOffer.setVisibility(View.GONE);
                fabEdit.setVisibility(View.VISIBLE);
                layoutSellerInfo.setVisibility(View.GONE);
            } else {
                btnContact.setVisibility(View.VISIBLE);
                fabEdit.setVisibility(View.GONE);
                layoutSellerInfo.setVisibility(View.VISIBLE);
                btnBuy.setVisibility(View.VISIBLE);
                if (listing.getIsNegotiable() != null && listing.getIsNegotiable()) {
                    btnMakeOffer.setVisibility(View.VISIBLE);
                } else {
                    btnMakeOffer.setVisibility(View.GONE);
                    addNonNegotiableInfo();
                }
            }
        }
    }

    private void addNonNegotiableInfo() {
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

                    SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    long currentUserId = prefs.getLong("userId", -1);
                    
                    if (currentUserId == -1) {
                        Toast.makeText(requireContext(), "Vui lòng đăng nhập để liên hệ người bán", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Navigate to chat fragment
                    Bundle args = new Bundle();
                    args.putLong("myId", currentUserId);
                    args.putLong("otherId", seller.getId());
                    args.putString("otherName", seller.getDisplayName());
                    args.putLong("listingId", listingId);
                    
                    ChatFragment chatFragment = ChatFragment.newInstance(
                            -1,
                            currentUserId,
                            seller.getId(),
                            seller.getDisplayName(),
                            listingId
                    );
                    
                    ((MainMenu) requireActivity()).replaceFragment(chatFragment);
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
    }    private void reportListing() {
        if (listing == null) {
            Toast.makeText(requireContext(), "Không thể báo cáo tin đăng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user ID from preferences
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long currentUserId = prefs.getLong("user_id", -1);
        
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showReportListingDialog(currentUserId);
    }
    
    private void showReportListingDialog(long reporterId) {
        String[] reasons = {
            "Lừa đảo/Gian lận",
            "Nội dung không phù hợp", 
            "Spam/Quảng cáo",
            "Hàng giả/Không như mô tả",
            "Thao túng giá cả",
            "Tin đăng trùng lặp",
            "Khác"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo tin đăng")
                .setMessage("Tin đăng: " + listing.getTitle())
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];
                    if (which == reasons.length - 1) {
                        // "Khác" - show input dialog
                        showCustomReportListingDialog(reporterId, "Khác");
                    } else {
                        showReportDescriptionDialog(reporterId, reason);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showReportDescriptionDialog(long reporterId, String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etDescription = dialogView.findViewById(R.id.et_custom_reason);
        etDescription.setHint("Mô tả thêm về vấn đề (tùy chọn)");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo: " + reason)
                .setMessage("Tin đăng: " + listing.getTitle())
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    submitListingReport(reporterId, reason, description);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showCustomReportListingDialog(long reporterId, String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etCustomReason = dialogView.findViewById(R.id.et_custom_reason);
        etCustomReason.setHint("Mô tả chi tiết lý do báo cáo");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Lý do báo cáo khác")
                .setMessage("Tin đăng: " + listing.getTitle())
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String customReason = etCustomReason.getText().toString().trim();
                    if (customReason.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập lý do báo cáo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitListingReport(reporterId, reason, customReason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void submitListingReport(long reporterId, String reason, String description) {
        Call<ApiResponse> call = apiService.reportListing(listing.getId(), reporterId, reason, description);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded() || getContext() == null) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã gửi báo cáo thành công", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Không thể gửi báo cáo", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi kết nối: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editListing() {
        if (listing == null) return;

        // TODO: Navigate to edit listing fragment with data
        Toast.makeText(requireContext(), "Tính năng chỉnh sửa đang phát triển", Toast.LENGTH_SHORT).show();
    }    private void viewSellerProfile() {
        if (seller == null) {
            Toast.makeText(requireContext(), "Không thể xem profile người bán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if viewing own profile
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        long currentUserId = prefs.getLong("userId", -1);
        
        if (currentUserId == seller.getId()) {
            // Navigate to own profile (if you have one)
            Toast.makeText(requireContext(), "Đây là profile của bạn", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Navigate to other user profile
        Bundle args = new Bundle();
        args.putLong("userId", seller.getId());
        args.putString("displayName", seller.getDisplayName());
          try {
            // Use a generic navigation or fall back to activity method
            if (getActivity() instanceof MainMenu) {
                MainMenu mainMenu = (MainMenu) getActivity();
                mainMenu.navigateToOtherUserProfile(seller.getId(), seller.getDisplayName());
            } else {
                Toast.makeText(requireContext(), "Không thể mở profile người bán", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            
            // Fallback: Use activity navigation if fragment navigation fails
            if (getActivity() instanceof MainMenu) {
                MainMenu mainMenu = (MainMenu) getActivity();
                mainMenu.navigateToOtherUserProfile(seller.getId(), seller.getDisplayName());
            } else {
                Toast.makeText(requireContext(), "Không thể mở profile người bán", Toast.LENGTH_SHORT).show();
            }
        }
    }    private void buyListing() {
        if (listing == null) {
            Toast.makeText(requireContext(), "Không thể mua sản phẩm này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if user is trying to buy their own listing
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long currentUserId = prefs.getLong("userId", -1);
        
        if (listing.getUserId() != null && currentUserId == listing.getUserId()) {
            Toast.makeText(requireContext(), "Bạn không thể mua sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if user already has a pending payment for this listing
        checkPendingPaymentAndProceed(currentUserId);
    }
    
    private void checkPendingPaymentAndProceed(long userId) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<com.example.ok.model.Payment> call = apiService.getPendingPaymentForListing(userId, listing.getId());
        
        call.enqueue(new Callback<com.example.ok.model.Payment>() {
            @Override
            public void onResponse(Call<com.example.ok.model.Payment> call, Response<com.example.ok.model.Payment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // User has a pending payment, redirect to payment screen with existing payment
                    com.example.ok.model.Payment existingPayment = response.body();
                    showPendingPaymentDialog(existingPayment);
                } else {
                    // No pending payment found, create new payment
                    openPaymentScreen();
                }
            }
            
            @Override
            public void onFailure(Call<com.example.ok.model.Payment> call, Throwable t) {
                Log.e(TAG, "Error checking pending payment", t);
                // On error, proceed with normal flow
                openPaymentScreen();
            }
        });
    }
    
    private void showPendingPaymentDialog(com.example.ok.model.Payment existingPayment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Đơn hàng chưa thanh toán")
                .setMessage("Bạn đã có đơn hàng chưa thanh toán cho sản phẩm này. " +
                           "Bạn muốn tiếp tục thanh toán hay tạo đơn hàng mới?")
                .setPositiveButton("Thanh toán đơn cũ", (dialog, which) -> {
                    // Open payment screen with existing payment
                    openPaymentScreenWithExistingPayment(existingPayment);
                })
                .setNegativeButton("Tạo đơn mới", (dialog, which) -> {
                    // Create new payment
                    openPaymentScreen();
                })
                .setNeutralButton("Hủy", null)
                .show();
    }
    
    private void openPaymentScreenWithExistingPayment(com.example.ok.model.Payment existingPayment) {
        Intent paymentIntent = new Intent(requireActivity(), com.example.ok.ui.PaymentActivity.class);
        
        // Pass existing payment information
        paymentIntent.putExtra("existingPaymentId", existingPayment.getId());
        paymentIntent.putExtra("listingId", listing.getId());
        paymentIntent.putExtra("listingTitle", listing.getTitle());
        paymentIntent.putExtra("listingPrice", listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0);
        paymentIntent.putExtra("listingImageUrl", 
                listing.getImageUrls() != null && !listing.getImageUrls().isEmpty() ? 
                listing.getImageUrls().get(0) : "");
        
        // Pass seller information
        if (seller != null) {
            paymentIntent.putExtra("sellerName", seller.getDisplayName());
        } else if (listing.getUserDisplayName() != null) {
            paymentIntent.putExtra("sellerName", listing.getUserDisplayName());
        } else {
            paymentIntent.putExtra("sellerName", "Người bán");
        }
        
        // Pass existing payment details
        paymentIntent.putExtra("paymentAmount", existingPayment.getAmount());
        paymentIntent.putExtra("paymentStatus", existingPayment.getStatus());
        paymentIntent.putExtra("paymentMethod", existingPayment.getPaymentMethodType());
        
        startActivity(paymentIntent);
    }
    
    private void openPaymentScreen() {
        Intent paymentIntent = new Intent(requireActivity(), com.example.ok.ui.PaymentActivity.class);
          // Pass listing information to payment screen
        paymentIntent.putExtra("listingId", listing.getId());
        paymentIntent.putExtra("listingTitle", listing.getTitle());
        paymentIntent.putExtra("listingPrice", listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0);
        paymentIntent.putExtra("listingImageUrl", 
                listing.getImageUrls() != null && !listing.getImageUrls().isEmpty() ? 
                listing.getImageUrls().get(0) : "");
        
        // Pass seller information
        if (seller != null) {
            paymentIntent.putExtra("sellerName", seller.getDisplayName());
        } else if (listing.getUserDisplayName() != null) {
            paymentIntent.putExtra("sellerName", listing.getUserDisplayName());
        } else {
            paymentIntent.putExtra("sellerName", "Người bán");
        }
        
        startActivityForResult(paymentIntent, 1001);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) { // Payment activity result
            if (resultCode == android.app.Activity.RESULT_OK) {
                // Payment successful
                Toast.makeText(requireContext(), "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                
                // Optionally update listing status or navigate somewhere
                // For now, we'll just show a success message
                showPaymentSuccessDialog();
            }
        }
    }
    
    private void showPaymentSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Thanh toán thành công")
                .setMessage("Đơn hàng của bạn đã được xử lý. Vui lòng liên hệ người bán để sắp xếp giao hàng.")
                .setPositiveButton("Liên hệ người bán", (dialog, which) -> contactSeller())
                .setNegativeButton("Đóng", null)
                .show();
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

    // ========== OFFER FUNCTIONALITY ==========
      private void showMakeOfferDialog() {
        if (listing == null) {
            Toast.makeText(requireContext(), "Không thể tạo offer cho sản phẩm này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if user is trying to offer on their own listing
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long currentUserId = prefs.getLong("userId", -1);
        
        if (listing.getUserId() != null && currentUserId == listing.getUserId()) {
            Toast.makeText(requireContext(), "Bạn không thể đặt giá cho sản phẩm của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create offer dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_make_offer, null);
        
        EditText etOfferAmount = dialogView.findViewById(R.id.etOfferAmount);
        EditText etOfferMessage = dialogView.findViewById(R.id.etOfferMessage);
        TextView tvListingPrice = dialogView.findViewById(R.id.tvListingPrice);
        Button btnDiscount5 = dialogView.findViewById(R.id.btnDiscount5);
        Button btnDiscount10 = dialogView.findViewById(R.id.btnDiscount10);
        Button btnDiscount15 = dialogView.findViewById(R.id.btnDiscount15);
        
        // Show current listing price
        DecimalFormat formatter = new DecimalFormat("#,###");
        double originalPrice = listing.getPrice().doubleValue();
        tvListingPrice.setText("Giá hiện tại: " + formatter.format(originalPrice) + " VNĐ");
        
        // Setup quick discount buttons
        btnDiscount5.setOnClickListener(v -> {
            double discountedPrice = originalPrice * 0.95; // 5% discount
            etOfferAmount.setText(String.valueOf((int)discountedPrice));
            etOfferMessage.setText("Mong bạn có thể giảm 5% giá. Cảm ơn!");
        });
        
        btnDiscount10.setOnClickListener(v -> {
            double discountedPrice = originalPrice * 0.90; // 10% discount
            etOfferAmount.setText(String.valueOf((int)discountedPrice));
            etOfferMessage.setText("Hi vọng bạn có thể giảm 10% giá để tôi có thể mua ngay. Cảm ơn bạn!");
        });
        
        btnDiscount15.setOnClickListener(v -> {
            double discountedPrice = originalPrice * 0.85; // 15% discount
            etOfferAmount.setText(String.valueOf((int)discountedPrice));
            etOfferMessage.setText("Tôi rất thích sản phẩm này. Bạn có thể giảm 15% không? Tôi sẽ mua ngay!");
        });
        
        builder.setView(dialogView)
                .setTitle("Yêu cầu giảm giá")
                .setPositiveButton("Gửi yêu cầu", null) // Set to null to override later
                .setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        
        // Override positive button to validate input
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String amountStr = etOfferAmount.getText().toString().trim();
                String message = etOfferMessage.getText().toString().trim();
                
                if (amountStr.isEmpty()) {
                    etOfferAmount.setError("Vui lòng nhập số tiền hoặc chọn tùy chọn giảm giá");
                    return;
                }
                
                try {
                    double offerAmount = Double.parseDouble(amountStr);
                    if (offerAmount <= 0) {
                        etOfferAmount.setError("Số tiền phải lớn hơn 0");
                        return;
                    }
                    
                    if (offerAmount >= originalPrice) {
                        etOfferAmount.setError("Giá đề xuất phải thấp hơn giá hiện tại");
                        return;
                    }
                    
                    // Add default message if empty
                    if (message.trim().isEmpty()) {
                        message = "Tôi muốn mua sản phẩm này với giá " + formatter.format(offerAmount) + " VNĐ.";
                    }
                    
                    // Send offer
                    sendOffer(currentUserId, offerAmount, message);
                    dialog.dismiss();
                    
                } catch (NumberFormatException e) {
                    etOfferAmount.setError("Số tiền không hợp lệ");
                }
            });
        });
        
        dialog.show();
    }
      private void sendOffer(long buyerId, double amount, String message) {
        CreateOfferRequest request = new CreateOfferRequest();
        request.setListingId(listing.getId());
        request.setOfferAmount(new java.math.BigDecimal(amount));
        request.setMessage(message);
        
        Call<ApiResponse> call = apiService.createOffer(buyerId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "✅ Đã gửi yêu cầu giảm giá thành công!", Toast.LENGTH_SHORT).show();
                        showOfferSentTip();
                    } else {
                        Toast.makeText(requireContext(), 
                            apiResponse.getMessage() != null ? apiResponse.getMessage() : "Gửi yêu cầu thất bại", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Gửi yêu cầu thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Error sending offer", t);
                Toast.makeText(requireContext(), "Lỗi kết nối khi gửi yêu cầu", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showOfferSentTip() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("🎉 Yêu cầu đã được gửi!")
                .setMessage("Người bán sẽ nhận được thông báo về yêu cầu giảm giá của bạn. " +
                           "Họ có thể:\n" +
                           "• ✅ Chấp nhận giá của bạn\n" +
                           "• ❌ Từ chối yêu cầu\n" +
                           "• 🔄 Đề xuất giá khác\n\n" +
                           "Bạn sẽ nhận được thông báo khi có phản hồi!")
                .setPositiveButton("Hiểu rồi", null)
                .setNeutralButton("Xem yêu cầu của tôi", (dialog, which) -> {
                    // Navigate to offers fragment - could implement this later
                    Toast.makeText(requireContext(), "Tính năng sẽ có trong bản cập nhật tiếp theo", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}