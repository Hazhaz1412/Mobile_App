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
    private static final String ARG_IS_CURRENT_USER = "isCurrentUser";

    private Long userId;
    private boolean isCurrentUser;

    // UI Components
    private ImageView profileImage;
    private ImageButton btnChangePhoto;
    private TextView tvDisplayName, tvBio, tvEmail, tvPhone, tvRatingCount;
    private RatingBar ratingBar;
    private MaterialButton btnEditProfile, btnDeactivateAccount, btnDeleteAccount, btnLogout, btnNotificationSettings;
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
    }

    private void initViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvBio = view.findViewById(R.id.tvBio);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvRatingCount = view.findViewById(R.id.tvRatingCount);
        ratingBar = view.findViewById(R.id.ratingBar);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnDeactivateAccount = view.findViewById(R.id.btnDeactivateAccount);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnNotificationSettings = view.findViewById(R.id.btnNotificationSettings);
        accountActionsCard = view.findViewById(R.id.accountActionsCard);

        // Update UI based on whether viewing own profile or other user's profile
        updateUIForViewMode();
    }

    private void updateUIForViewMode() {
        if (isCurrentUser) {
            // Viewing own profile
            btnChangePhoto.setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.VISIBLE);
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
        }
    }

    private void navigateToNotificationSettings() {
        if (getActivity() instanceof MainMenu) {
            ((MainMenu) getActivity()).navigateToNotificationSettings();
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
            Log.d(TAG, "User data loaded: " + user.getDisplayName() + ", avatar: " + user.getAvatarUrl());

            // Hiển thị thông tin cơ bản
            tvDisplayName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());

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
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.user);
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
    }

    private void showDeactivateConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạm ngưng tài khoản")
                .setMessage("Bạn có chắc chắn muốn tạm ngưng tài khoản? Bạn có thể kích hoạt lại sau.")
                .setPositiveButton("Tạm ngưng", (dialog, which) -> deactivateAccount())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tài khoản vĩnh viễn")
                .setMessage("Cảnh báo: Hành động này không thể hoàn tác. Tất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn. Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> deleteAccount())
                .setNegativeButton("Hủy", null)
                .show();
    }

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