package com.example.ok.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.ok.R;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.UserProfileRequest;
import com.example.ok.model.UserProfileResponse;
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
    private MaterialButton btnEditProfile, btnDeactivateAccount, btnDeleteAccount;
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
        } else {
            // Viewing someone else's profile
            btnChangePhoto.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.GONE);
            accountActionsCard.setVisibility(View.GONE);
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
                    updateUI(response.body());
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

    private void updateUI(UserProfileResponse profile) {
        // Set user name
        tvDisplayName.setText(profile.getDisplayName());

        // Set bio
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            tvBio.setText(profile.getBio());
        } else {
            tvBio.setText("Chưa có thông tin giới thiệu");
        }

        // Set contact info
        tvEmail.setText(profile.getEmail());
        if (profile.getContactInfo() != null && !profile.getContactInfo().isEmpty()) {
            tvPhone.setText(profile.getContactInfo());
        } else {
            tvPhone.setText("Chưa cập nhật");
        }

        // Set rating
        if (profile.getRatingAvg() != null) {
            ratingBar.setRating(profile.getRatingAvg().floatValue());
            tvRatingCount.setText(String.format("%.1f (%d đánh giá)",
                    profile.getRatingAvg(), profile.getRatingCount()));
        } else {
            ratingBar.setRating(0);
            tvRatingCount.setText("Chưa có đánh giá");
        }

        // Load profile image
        if (profile.getProfilePictureUrl() != null && !profile.getProfilePictureUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(profile.getProfilePictureUrl())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.user);
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        try {
            // Show progress
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Đang tải ảnh lên...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Get file path from URI
            String filePath = getRealPathFromURI(imageUri);
            if (filePath == null) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "Không thể xử lý ảnh này", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create file and request body
            File file = new File(filePath);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

            // Make API call
            Call<ApiResponse> call = apiService.uploadProfileImage(userId, imagePart);
            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.isSuccess()) {
                            // Update image in UI
                            String imageUrl = (String) apiResponse.getData();
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(profileImage);

                            Toast.makeText(requireContext(),
                                    "Cập nhật ảnh đại diện thành công",
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
                    Log.e(TAG, "Error uploading image", t);
                }
            });

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Lỗi: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error processing image", e);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = requireContext().getContentResolver().query(contentUri, proj, null, null, null);

        if (cursor == null) return null;

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();

        return path;
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

    private void logoutUser() {
        // Clear user session
        // Replace this with your actual logout logic
        Toast.makeText(requireContext(), "Đăng xuất...", Toast.LENGTH_SHORT).show();

        // Navigate to login screen
        // Replace with your app's navigation logic
        requireActivity().finish();
    }
}