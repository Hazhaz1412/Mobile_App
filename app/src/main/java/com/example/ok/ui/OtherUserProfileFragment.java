package com.example.ok.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.User;
import com.example.ok.model.UserProfileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtherUserProfileFragment extends Fragment {
    private static final String TAG = "OtherUserProfileFragment";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_USER_NAME = "userName";

    private Long userId;
    private String userName;
    private Long currentUserId;

    // UI Components
    private ImageView profileImage;
    private TextView tvDisplayName, tvBio, tvEmail, tvPhone, tvRatingCount, tvJoinDate;
    private RatingBar ratingBar;
    private MaterialButton btnSendMessage, btnBlockUser, btnReportUser;
    private MaterialCardView cardContactInfo;

    // API Service
    private ApiService apiService;

    public OtherUserProfileFragment() {
        // Required empty public constructor
    }

    public static OtherUserProfileFragment newInstance(Long userId, String userName) {
        OtherUserProfileFragment fragment = new OtherUserProfileFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
            userName = getArguments().getString(ARG_USER_NAME);
        }

        // Get current user ID
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);

        // Initialize API service
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other_user_profile, container, false);

        initViews(view);
        setupListeners();
        loadUserProfile();

        return view;
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvBio = view.findViewById(R.id.tvBio);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvRatingCount = view.findViewById(R.id.tvRatingCount);
        tvJoinDate = view.findViewById(R.id.tvJoinDate);
        ratingBar = view.findViewById(R.id.ratingBar);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        btnBlockUser = view.findViewById(R.id.btnBlockUser);
        btnReportUser = view.findViewById(R.id.btnReportUser);
        cardContactInfo = view.findViewById(R.id.cardContactInfo);
    }

    private void setupListeners() {
        btnSendMessage.setOnClickListener(v -> openChatWithUser());
        btnBlockUser.setOnClickListener(v -> showBlockUserDialog());
        btnReportUser.setOnClickListener(v -> showReportUserDialog());
    }

    private void loadUserProfile() {
        if (userId == null || userId <= 0) {
            Toast.makeText(requireContext(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<UserProfileResponse> call = apiService.getUserProfile(userId);
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse userResponse = response.body();
                    if (userResponse.isSuccess()) {
                        updateUI(userResponse);
                    } else {
                        Toast.makeText(requireContext(), 
                            userResponse.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), 
                        "Lỗi khi tải thông tin người dùng", 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading user profile", t);
                Toast.makeText(requireContext(), 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(UserProfileResponse response) {
        try {
            User user = response.getData();
            if (user == null) return;

            // Set display name
            tvDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Người dùng");

            // Set bio
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                tvBio.setText(user.getBio());
            } else {
                tvBio.setText("Chưa có thông tin giới thiệu");
            }

            // Set email - chỉ hiển thị domain
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String email = user.getEmail();
                String maskedEmail = email.replaceAll("(.{2}).*(@.*)", "$1***$2");
                tvEmail.setText(maskedEmail);
            } else {
                tvEmail.setText("Chưa có email");
            }

            // Set contact info - ẩn một phần số điện thoại
            if (user.getContactInfo() != null && !user.getContactInfo().isEmpty()) {
                String phone = user.getContactInfo();
                String maskedPhone = phone.replaceAll("(\\d{3})\\d{4}(\\d{3})", "$1****$2");
                tvPhone.setText(maskedPhone);
            } else {
                tvPhone.setText("Chưa có thông tin liên hệ");
            }

            // Set avatar
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.user);
            }

            // Set rating (default values for now)
            ratingBar.setRating(4.2f);
            tvRatingCount.setText("15 đánh giá");

            // Set join date
            tvJoinDate.setText("Thành viên từ 2024");

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with user data", e);
            Toast.makeText(requireContext(), "Lỗi hiển thị dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatWithUser() {
        if (userId == null || currentUserId == null) {
            Toast.makeText(requireContext(), "Không thể mở chat", Toast.LENGTH_SHORT).show();
            return;
        }        // Navigate to ChatFragment
        ChatFragment chatFragment = ChatFragment.newInstance(
                -1, // roomId will be created
                currentUserId,
                userId,
                userName != null ? userName : "Người dùng",
                -1 // no listing
        );
        
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showBlockUserDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn chặn " + tvDisplayName.getText() + "? Bạn sẽ không nhận được tin nhắn từ người này nữa.")
                .setPositiveButton("Chặn", (dialog, which) -> blockUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showReportUserDialog() {
        String[] reasons = {
            "Spam hoặc quảng cáo",
            "Ngôn từ không phù hợp",
            "Lừa đảo",
            "Bán hàng giả",
            "Quấy rối",
            "Khác"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo người dùng")
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];
                    if (which == reasons.length - 1) {
                        // "Khác" - show input dialog
                        showCustomReportDialog();
                    } else {
                        reportUser(reason);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCustomReportDialog() {
        // Create custom input dialog for "Other" reason
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        // You'll need to create this layout
        new AlertDialog.Builder(requireContext())
                .setTitle("Lý do báo cáo")
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    // Get custom reason from EditText in dialogView
                    reportUser("Lý do khác");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void blockUser() {
        // Implement block user API call
        Call<ApiResponse> call = apiService.blockUser(currentUserId, userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã chặn người dùng", Toast.LENGTH_SHORT).show();
                        // Navigate back
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi khi chặn người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error blocking user", t);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reportUser(String reason) {
        // Implement report user API call
        Call<ApiResponse> call = apiService.reportUser(currentUserId, userId, reason);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(), "Đã gửi báo cáo", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Lỗi khi gửi báo cáo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error reporting user", t);
                Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
