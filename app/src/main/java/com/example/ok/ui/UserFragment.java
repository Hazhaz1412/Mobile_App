package com.example.ok.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ok.Login;
import com.example.ok.MainActivity;
import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.User;
import com.example.ok.model.UserProfileRequest;
import com.example.ok.model.UserProfileResponse;
import com.example.ok.model.EmailVerificationRequest;
import com.example.ok.model.VerifyCodeRequest;
import com.example.ok.util.FileUtil;
import com.example.ok.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserFragment extends Fragment {
    private static final String TAG = "UserFragment";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_IS_CURRENT_USER = "isCurrentUser";    private Long userId;
    private boolean isCurrentUser;
    private String currentUserEmail; // 🔐 Store email for verification// UI Components
    private ImageView profileImage;
    private ImageButton btnChangePhoto;
    private TextView tvDisplayName, tvBio, tvEmail, tvPhone, tvRatingCount;
    private RatingBar ratingBar;
    private MaterialButton btnEditProfile, btnDeactivateAccount, btnDeleteAccount, btnLogout, btnNotificationSettings, btnPaymentHistory, btnFavorites, btnOfferManagement, btnMyOffers;
    private View accountActionsCard;

    // API Service
    private ApiService apiService;

    // Activity Result Launcher for image picking
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public UserFragment() {
        // Required empty public constructor
    }

    public static UserFragment newInstance(Long userId, boolean isCurrentUser) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putBoolean(ARG_IS_CURRENT_USER, isCurrentUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
            isCurrentUser = getArguments().getBoolean(ARG_IS_CURRENT_USER, false);
        }

        // Initialize API service
        RetrofitClient.init(requireContext());
        apiService = RetrofitClient.getApiService();

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadProfileImage(selectedImageUri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Initialize views
        initViews(view);

        // Set up click listeners
        setupListeners();

        // Load user profile data
        loadUserProfile();

        return view;
    }    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvBio = view.findViewById(R.id.tvBio);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvRatingCount = view.findViewById(R.id.tvRatingCount);
        ratingBar = view.findViewById(R.id.ratingBar);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnDeactivateAccount = view.findViewById(R.id.btnDeactivateAccount);        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);        btnLogout = view.findViewById(R.id.btnLogout);
        btnNotificationSettings = view.findViewById(R.id.btnNotificationSettings);
        btnPaymentHistory = view.findViewById(R.id.btnPaymentHistory);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        btnOfferManagement = view.findViewById(R.id.btnOfferManagement);
        btnMyOffers = view.findViewById(R.id.btnMyOffers);
        accountActionsCard = view.findViewById(R.id.accountActionsCard);

        // Update UI based on whether viewing own profile or other user's profile
        updateUIForViewMode();
    }    private void updateUIForViewMode() {        if (isCurrentUser) {
            // Viewing own profile
            btnChangePhoto.setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.VISIBLE);
            btnPaymentHistory.setVisibility(View.VISIBLE);
            btnFavorites.setVisibility(View.VISIBLE);
            accountActionsCard.setVisibility(View.VISIBLE);
            if (btnLogout != null) {
                btnLogout.setVisibility(View.VISIBLE);
            }
            if (btnNotificationSettings != null) {
                btnNotificationSettings.setVisibility(View.VISIBLE);
            }
        } else {
            // Viewing someone else's profile
            btnChangePhoto.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.GONE);
            btnPaymentHistory.setVisibility(View.GONE);
            btnFavorites.setVisibility(View.GONE);
            accountActionsCard.setVisibility(View.GONE);
            if (btnLogout != null) {
                btnLogout.setVisibility(View.GONE);
            }
            if (btnNotificationSettings != null) {
                btnNotificationSettings.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        // Profile image change
        btnChangePhoto.setOnClickListener(v -> selectImage());

        // Edit profile
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Account management
        if (btnDeactivateAccount != null) {
            btnDeactivateAccount.setOnClickListener(v -> showDeactivateConfirmation());
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmation());
        }

        // Logout button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
          // Notification settings button
        if (btnNotificationSettings != null) {
            btnNotificationSettings.setOnClickListener(v -> navigateToNotificationSettings());
        }          // Payment history button
        if (btnPaymentHistory != null) {
            btnPaymentHistory.setOnClickListener(v -> navigateToPaymentHistory());
        }
        
        // Favorites button
        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> navigateToFavorites());
        }
          // Offer management button
        if (btnOfferManagement != null) {
            btnOfferManagement.setOnClickListener(v -> navigateToOfferManagement());
        }
        
        // My offers button
        if (btnMyOffers != null) {
            btnMyOffers.setOnClickListener(v -> navigateToMyOffers());
        }
    }    private void navigateToNotificationSettings() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToNotificationSettings();
        }
    }      private void navigateToPaymentHistory() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToPaymentHistory();
        }
    }
    
    private void navigateToFavorites() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToFavorites();
        }
    }
      private void navigateToOfferManagement() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToOfferManagement();
        }
    }
    
    private void navigateToMyOffers() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToMyOffers();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logoutUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logoutUser() {
        // Hiển thị dialog tiến trình
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang đăng xuất...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Gọi API đăng xuất (nếu có)
        Call<ApiResponse> call = apiService.logout();
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                // Xử lý khi đã nhận phản hồi từ server (thành công hoặc thất bại)
                clearLocalData();
                progressDialog.dismiss();
                navigateToLogin();
                Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                // Vẫn đăng xuất khỏi ứng dụng ngay cả khi API thất bại
                Log.e(TAG, "Logout API call failed", t);
                clearLocalData();
                progressDialog.dismiss();
                navigateToLogin();
                Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearLocalData() {
        // Xóa token và dữ liệu người dùng từ SharedPreferences
        SharedPreferences preferences = requireActivity().getSharedPreferences(
                "UserPrefs", Context.MODE_PRIVATE); // Sửa từ "user_prefs" thành "UserPrefs"
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Xóa các biến session khác (nếu có)
        SessionManager sessionManager = new SessionManager(requireContext());
        sessionManager.clearSession();
    }

    private void navigateToLogin() {
        // Chuyển đến LoginActivity
        Intent intent = new Intent(requireActivity(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void loadUserProfile() {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tải thông tin...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<UserProfileResponse> call = apiService.getUserProfile(userId);
        call.enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse userResponse = response.body();
                    
                    // Mặc định là thành công nếu response có HTTP 200
                    userResponse.setSuccess(true);
                    
                    Log.d(TAG, "Loaded profile data: " + 
                          userResponse.getDisplayName() + ", " + 
                          userResponse.getProfilePictureUrl());
                    
                    updateUI(userResponse);
                } else {
                    Toast.makeText(requireContext(),
                            "Lỗi khi tải thông tin: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading profile", t);
            }
        });
    }

    private void updateUI(UserProfileResponse response) {
        try {
            if (response == null || !response.isSuccess()) {
                Toast.makeText(requireContext(), "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy dữ liệu user từ response
            User user = response.getData();
            if (user == null) {
                Log.e(TAG, "User data is null in response");
                Toast.makeText(requireContext(), "Dữ liệu người dùng trống", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log để debug
            Log.d(TAG, "User data loaded: " + user.getDisplayName() + ", avatar: " + user.getAvatarUrl());            // Hiển thị thông tin cơ bản
            tvDisplayName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());
            
            // 🔐 Store email for verification purposes
            currentUserEmail = user.getEmail();

            // Hiển thị thông tin bio và contact nếu có
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                tvBio.setText(user.getBio());
            } else {
                tvBio.setText("Chưa có thông tin giới thiệu");
            }

            if (user.getContactInfo() != null && !user.getContactInfo().isEmpty()) {
                tvPhone.setText(user.getContactInfo());
            } else {
                tvPhone.setText("Chưa có thông tin liên hệ");
            }

            // Hiển thị avatar
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // Lưu URL avatar vào SharedPreferences để sử dụng ở nơi khác
                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                prefs.edit().putString("avatarUrl", avatarUrl).apply();

                // Hiển thị avatar với Glide
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(profileImage);            } else {
                profileImage.setImageResource(R.drawable.user);
            }            // Update rating bar and count
            Log.d(TAG, "Rating data - Avg: " + response.getRatingAvg() + ", Count: " + response.getRatingCount());
            
            if (response.getRatingAvg() != null) {
                float rating = response.getRatingAvg().floatValue();
                ratingBar.setRating(rating);
                Log.d(TAG, "Set rating bar to: " + rating);
            } else {
                ratingBar.setRating(0.0f);
                Log.d(TAG, "Set rating bar to: 0.0 (null avg)");
            }
            
            if (response.getRatingCount() != null && response.getRatingCount() > 0) {
                String ratingText = response.getRatingCount() + " đánh giá";
                tvRatingCount.setText(ratingText);
                Log.d(TAG, "Set rating count to: " + ratingText);
            } else {
                tvRatingCount.setText("Chưa có đánh giá");
                Log.d(TAG, "Set rating count to: Chưa có đánh giá");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with user data", e);
            Toast.makeText(requireContext(), "Lỗi hiển thị dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        try {
            // Hiển thị tiến trình
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Đang tải ảnh lên...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Log.d(TAG, "Uploading image from URI: " + imageUri.toString());

            // Sử dụng FileUtil thay vì getRealPathFromURI
            File file = FileUtil.getFileFromUri(requireContext(), imageUri);

            if (file == null) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Không thể xử lý ảnh này", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Created file: " + file.getAbsolutePath() + ", size: " + file.length());

            // Tạo request body
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            // Gọi API
            Call<ApiResponse> call = apiService.uploadProfileImage(userId, imagePart);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    progressDialog.dismiss();

                    Log.d(TAG, "Upload response code: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // Cập nhật UI với URL mới
                            String imageUrl = (String) apiResponse.getData();
                            Log.d(TAG, "Upload successful, new image URL: " + imageUrl);

                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.user)
                                    .circleCrop()
                                    .into(profileImage);

                            Toast.makeText(requireContext(),
                                    "Cập nhật ảnh đại diện thành công",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Upload failed: " + apiResponse.getMessage());
                            Toast.makeText(requireContext(),
                                    apiResponse.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        try {
                            String errorBody = response.errorBody() != null ?
                                    response.errorBody().string() : "Unknown error";
                            Log.e(TAG, "Error response: " + errorBody);
                        } catch (IOException e) {
                            Log.e(TAG, "Could not read error body", e);
                        }

                        Toast.makeText(requireContext(),
                                "Lỗi: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Network error during upload", t);
                    Toast.makeText(requireContext(),
                            "Lỗi kết nối: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(requireContext(),
                    "Lỗi xử lý ảnh: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);

        EditText etDisplayName = dialogView.findViewById(R.id.etDisplayName);
        EditText etBio = dialogView.findViewById(R.id.etBio);
        EditText etContactInfo = dialogView.findViewById(R.id.etContactInfo);

        // Pre-fill with current values
        etDisplayName.setText(tvDisplayName.getText());
        etBio.setText(tvBio.getText());
        etContactInfo.setText(tvPhone.getText());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa hồ sơ")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    // Get updated values
                    String displayName = etDisplayName.getText().toString().trim();
                    String bio = etBio.getText().toString().trim();
                    String contactInfo = etContactInfo.getText().toString().trim();

                    if (displayName.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Tên hiển thị không được để trống",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateUserProfile(displayName, bio, contactInfo);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateUserProfile(String displayName, String bio, String contactInfo) {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        UserProfileRequest request = new UserProfileRequest(displayName, bio, contactInfo);
        Call<ApiResponse> call = apiService.updateUserProfile(userId, request);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        // Update UI
                        tvDisplayName.setText(displayName);
                        tvBio.setText(bio);
                        tvPhone.setText(contactInfo);

                        Toast.makeText(requireContext(),
                                "Cập nhật hồ sơ thành công",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Lỗi: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating profile", t);
            }
        });
    }    private void showDeactivateConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Tạm ngưng tài khoản")
                .setMessage("Bạn có chắc chắn muốn tạm ngưng tài khoản?\n\n" +
                           "• Tài khoản sẽ bị vô hiệu hóa tạm thời\n" +
                           "• Bạn có thể kích hoạt lại sau\n" +
                           "• Để đảm bảo an toàn, chúng tôi sẽ gửi mã xác thực qua email")
                .setPositiveButton("Tiếp tục", (dialog, which) -> requestEmailVerification("DEACTIVATE"))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🚨 Xóa tài khoản vĩnh viễn")
                .setMessage("CẢNH BÁO: Hành động này KHÔNG THỂ HOÀN TÁC!\n\n" +
                           "Sẽ bị xóa vĩnh viễn:\n" +
                           "• Tất cả thông tin cá nhân\n" +
                           "• Lịch sử giao dịch\n" +
                           "• Tin đăng và hình ảnh\n" +
                           "• Tin nhắn và đánh giá\n\n" +
                           "Để đảm bảo an toàn, chúng tôi sẽ gửi mã xác thực qua email")
                .setPositiveButton("Tôi hiểu, tiếp tục", (dialog, which) -> requestEmailVerification("DELETE"))
                .setNegativeButton("Hủy", null)
                .show();    }    // 🔐 NEW: Email verification system for secure account management
    private void requestEmailVerification(String action) {
        // Get user email from loaded profile data
        String userEmail = currentUserEmail;
        
        // Fallback: get from TextView if currentUserEmail is null
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = tvEmail.getText().toString().trim();
        }
        
        // Final fallback: try SharedPreferences
        if (userEmail == null || userEmail.isEmpty()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            userEmail = prefs.getString("email", "");
        }
        
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(requireContext(), 
                "❌ Không tìm thấy email. Vui lòng tải lại trang hoặc đăng nhập lại.", 
                Toast.LENGTH_LONG).show();
            return;
        }        Log.d(TAG, "🔐 Using email for verification: " + maskEmail(userEmail));

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang gửi mã xác thực...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        EmailVerificationRequest request = new EmailVerificationRequest(userEmail, action);
        Call<ApiResponse> call = apiService.sendAccountVerificationCode(request);
        
        final String finalUserEmail = userEmail; // Make final for inner class
          call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        showEmailVerificationDialog(finalUserEmail, action);
                        Toast.makeText(requireContext(), 
                                "Mã xác thực đã được gửi đến " + maskEmail(finalUserEmail), 
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else if (response.code() == 403) {
                    // 🔧 Handle 403 - Backend endpoint not implemented yet
                    Log.w(TAG, "❌ Backend endpoint not implemented yet (403). Showing demo mode.");
                    showBackendNotReadyDialog(finalUserEmail, action);
                } else if (response.code() == 404) {
                    // 🔧 Handle 404 - Endpoint not found
                    Log.w(TAG, "❌ Backend endpoint not found (404). Showing demo mode.");
                    showBackendNotReadyDialog(finalUserEmail, action);
                } else {
                    Toast.makeText(requireContext(), 
                        "❌ Lỗi server: " + response.code() + ". Vui lòng thử lại sau.", 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "❌ Network error sending verification code", t);
                
                // Show demo mode for network errors too
                showBackendNotReadyDialog(finalUserEmail, action);
            }
        });    }

    // 🔧 Handle case when backend is not ready yet
    private void showBackendNotReadyDialog(String email, String action) {
        String actionText = action.equals("DEACTIVATE") ? "tạm ngưng" : "xóa vĩnh viễn";
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Tính năng đang phát triển")
                .setMessage("Hệ thống email verification cho việc " + actionText + " tài khoản đang được phát triển.\n\n" +
                           "📧 Email sẽ được gửi đến: " + maskEmail(email) + "\n\n" +
                           "Bạn có muốn:\n" +
                           "• Xem demo UI verification?\n" +
                           "• Hoặc sử dụng phương thức trực tiếp (không an toàn)?")
                .setPositiveButton("🎯 Demo UI", (dialog, which) -> {
                    // Show demo verification dialog
                    showDemoVerificationDialog(email, action);
                })
                .setNeutralButton("⚡ Trực tiếp", (dialog, which) -> {
                    // Show confirmation for direct action
                    showDirectActionConfirmation(action);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDemoVerificationDialog(String email, String action) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email_verification, null);
        
        EditText etVerificationCode = dialogView.findViewById(R.id.etVerificationCode);
        TextView tvMessage = dialogView.findViewById(R.id.tvVerificationMessage);
        MaterialButton btnVerify = dialogView.findViewById(R.id.btnVerify);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnResendCode = dialogView.findViewById(R.id.btnResendCode);

        String actionText = action.equals("DEACTIVATE") ? "tạm ngưng" : "xóa vĩnh viễn";
        tvMessage.setText("🎯 DEMO MODE 🎯\n\n" +
                         "Giả lập gửi mã xác thực đến " + maskEmail(email) + 
                         " để " + actionText + " tài khoản.\n\n" +
                         "Nhập bất kỳ mã 6 chữ số nào để test UI:");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnVerify.setOnClickListener(v -> {
            String code = etVerificationCode.getText().toString().trim();
            if (code.length() != 6) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            
            // Demo verification always succeeds
            String successMessage = action.equals("DEACTIVATE") 
                ? "🎯 DEMO: Tài khoản sẽ được tạm ngưng (thực tế chưa thực hiện)" 
                : "🎯 DEMO: Tài khoản sẽ được xóa vĩnh viễn (thực tế chưa thực hiện)";
                
            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnResendCode.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "🎯 DEMO: Đã gửi lại mã xác thực", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
        etVerificationCode.requestFocus();
    }

    private void showDirectActionConfirmation(String action) {
        String actionText = action.equals("DEACTIVATE") ? "tạm ngưng" : "xóa vĩnh viễn";
        String warningText = action.equals("DEACTIVATE") 
            ? "Tài khoản sẽ bị tạm ngưng ngay lập tức mà KHÔNG CẦN xác thực email."
            : "Tài khoản sẽ bị xóa vĩnh viễn ngay lập tức mà KHÔNG CẦN xác thực email.\n\n⚠️ NGUY HIỂM: Hành động này không thể hoàn tác!";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Xác nhận " + actionText + " trực tiếp")
                .setMessage("CẢNH BÁO: " + warningText + "\n\n" +
                           "Phương thức này KHÔNG AN TOÀN vì thiếu email verification.\n\n" +
                           "Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Có, tiếp tục", (dialog, which) -> {
                    if (action.equals("DEACTIVATE")) {
                        deactivateAccount(); // Use legacy method
                    } else {
                        deleteAccount(); // Use legacy method
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEmailVerificationDialog(String email, String action) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email_verification, null);
        
        EditText etVerificationCode = dialogView.findViewById(R.id.etVerificationCode);
        TextView tvMessage = dialogView.findViewById(R.id.tvVerificationMessage);
        MaterialButton btnVerify = dialogView.findViewById(R.id.btnVerify);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnResendCode = dialogView.findViewById(R.id.btnResendCode);

        String actionText = action.equals("DEACTIVATE") ? "tạm ngưng" : "xóa vĩnh viễn";
        tvMessage.setText("Chúng tôi đã gửi mã xác thực 6 chữ số đến email " + maskEmail(email) + 
                         " để xác nhận việc " + actionText + " tài khoản.");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnVerify.setOnClickListener(v -> {
            String code = etVerificationCode.getText().toString().trim();
            if (code.length() != 6) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ 6 chữ số", Toast.LENGTH_SHORT).show();
                return;
            }
            
            dialog.dismiss();
            verifyCodeAndExecuteAction(email, code, action);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnResendCode.setOnClickListener(v -> {
            dialog.dismiss();
            requestEmailVerification(action);
        });

        dialog.show();
        
        // Auto focus on input field
        etVerificationCode.requestFocus();
    }

    private void verifyCodeAndExecuteAction(String email, String code, String action) {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang xác thực...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        VerifyCodeRequest request = new VerifyCodeRequest(email, code, action);
        
        Call<ApiResponse> call;
        if (action.equals("DEACTIVATE")) {
            call = apiService.deactivateAccountWithCode(userId, request);
        } else {
            call = apiService.deleteAccountWithCode(userId, request);
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        String successMessage = action.equals("DEACTIVATE") 
                            ? "✅ Tài khoản đã được tạm ngưng thành công" 
                            : "✅ Tài khoản đã được xóa vĩnh viễn";
                            
                        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
                        logoutUser();
                    } else {
                        Toast.makeText(requireContext(), "❌ " + apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else if (response.code() == 400) {
                    Toast.makeText(requireContext(), "❌ Mã xác thực không đúng hoặc đã hết hạn", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "❌ Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "❌ Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error verifying code and executing action", t);
            }
        });
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return email; // Too short to mask
        }
        
        String masked = localPart.substring(0, 2) + "***" + localPart.substring(localPart.length() - 1);
        return masked + "@" + domain;
    }

    // Legacy methods (keep for backward compatibility)
    private void deactivateAccount() {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<ApiResponse> call = apiService.deactivateAccount(userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(),
                                "Tài khoản đã được tạm ngưng",
                                Toast.LENGTH_SHORT).show();

                        // Log out user
                        logoutUser();
                    } else {
                        Toast.makeText(requireContext(),
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Lỗi: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error deactivating account", t);
            }
        });
    }

    private void deleteAccount() {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<ApiResponse> call = apiService.deleteAccount(userId);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(requireContext(),
                                "Tài khoản đã được xóa vĩnh viễn",
                                Toast.LENGTH_SHORT).show();

                        // Log out user
                        logoutUser();
                    } else {
                        Toast.makeText(requireContext(),
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Lỗi: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error deleting account", t);
            }
        });
    }
}