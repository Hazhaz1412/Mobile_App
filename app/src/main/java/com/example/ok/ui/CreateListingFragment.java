package com.example.ok.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.adapter.ImagePreviewAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;
import com.example.ok.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2; // Thêm import này

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.adapter.ImagePreviewAdapter;
import com.example.ok.adapter.PreviewImageAdapter; // Thêm import này
import com.example.ok.api.ApiService;
import com.example.ok.api.ListingApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;
import com.example.ok.util.FileUtil;
import com.google.android.flexbox.FlexboxLayout; // Thêm import này
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.hdodenhof.circleimageview.CircleImageView; // Thêm import này

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat; // Thêm import này
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ...existing code...
public class CreateListingFragment extends Fragment implements LocationListener {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_LOCATION_PERMISSION = 1002;    private EditText etTitle, etDescription, etPrice, etLocation, etTags;
    private Spinner spCategory, spCondition;
    private CheckBox cbNegotiable; // Add negotiable checkbox
    private Button btnSelectImages, btnGetLocation, btnPreview, btnSubmit;
    private RecyclerView rvImagePreview;
    private ImagePreviewAdapter imageAdapter;

    private ApiService apiService;
    private ListingApiService listingApiService;
    private List<Category> categories = new ArrayList<>();
    private List<ItemCondition> conditions = new ArrayList<>();
    private List<Uri> selectedImages = new ArrayList<>();

    private LocationManager locationManager;
    private Geocoder geocoder;
    private BigDecimal currentLatitude, currentLongitude;

    // Executor for background tasks
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_listing, container, false);

        initViews(view);
        setupImagePreview();
        setupSpinners();
        setupClickListeners();

        try {
            // Initialize RetrofitClient before using any API services
            RetrofitClient.init(requireContext());
            apiService = RetrofitClient.getApiService();
            listingApiService = RetrofitClient.getListingApiService();

            Log.d("CreateListing", "Services initialized:");
            Log.d("CreateListing", "- apiService: " + (apiService != null ? "OK" : "NULL"));
            Log.d("CreateListing", "- listingApiService: " + (listingApiService != null ? "OK" : "NULL"));

        } catch (Exception e) {
            Log.e("CreateListing", "Error initializing services", e);
            Toast.makeText(getContext(), "Lỗi khởi tạo dịch vụ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        loadCategories();
        loadConditions();

        return view;
    }

    // THÊM CÁC METHOD BẮT BUỘC CỦA LocationListener
    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("CreateListing", "Location received: " + location.getLatitude() + ", " + location.getLongitude());

        currentLatitude = new BigDecimal(location.getLatitude());
        currentLongitude = new BigDecimal(location.getLongitude());

        // Stop location updates
        locationManager.removeUpdates(this);

        // Get address from coordinates
        executorService.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1
                );

                String locationText = "";
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();

                    // Build address string
                    if (address.getThoroughfare() != null) {
                        sb.append(address.getThoroughfare()).append(", ");
                    }
                    if (address.getSubAdminArea() != null) {
                        sb.append(address.getSubAdminArea()).append(", ");
                    }
                    if (address.getAdminArea() != null) {
                        sb.append(address.getAdminArea());
                    }

                    locationText = sb.toString();
                    if (locationText.endsWith(", ")) {
                        locationText = locationText.substring(0, locationText.length() - 2);
                    }
                } else {
                    locationText = "Lat: " + String.format("%.6f", location.getLatitude()) +
                            ", Lng: " + String.format("%.6f", location.getLongitude());
                }

                final String finalLocationText = locationText;

                // Update UI on main thread
                mainHandler.post(() -> {
                    etLocation.setText(finalLocationText);
                    resetLocationButton();
                    Toast.makeText(getContext(), "✓ Đã lấy vị trí thành công", Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                Log.e("CreateListing", "Error getting address from location", e);
                mainHandler.post(() -> {
                    String locationText = "Lat: " + String.format("%.6f", location.getLatitude()) +
                            ", Lng: " + String.format("%.6f", location.getLongitude());
                    etLocation.setText(locationText);
                    resetLocationButton();
                    Toast.makeText(getContext(), "✓ Đã lấy tọa độ (không thể lấy địa chỉ)", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("CreateListing", "Location provider status changed: " + provider + " status: " + status);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.d("CreateListing", "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.d("CreateListing", "Location provider disabled: " + provider);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Cần quyền truy cập vị trí để lấy địa chỉ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count && selectedImages.size() < 10; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                }
            } else if (data.getData() != null) {
                // Single image selected
                selectedImages.add(data.getData());
            }

            imageAdapter.notifyDataSetChanged();
            updateImageCountText();
        }
    }    private void initViews(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etLocation = view.findViewById(R.id.et_location);
        etTags = view.findViewById(R.id.et_tags);

        spCategory = view.findViewById(R.id.sp_category);
        spCondition = view.findViewById(R.id.sp_condition);
        cbNegotiable = view.findViewById(R.id.cb_negotiable); // Add this line

        btnSelectImages = view.findViewById(R.id.btn_select_images);
        btnGetLocation = view.findViewById(R.id.btn_get_location);
        btnPreview = view.findViewById(R.id.btn_preview);
        btnSubmit = view.findViewById(R.id.btn_submit);

        rvImagePreview = view.findViewById(R.id.rv_image_preview);
    }

    private void setupImagePreview() {
        imageAdapter = new ImagePreviewAdapter(getContext(), selectedImages);
        rvImagePreview.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        rvImagePreview.setAdapter(imageAdapter);

        imageAdapter.setOnImageRemoveListener(position -> {
            selectedImages.remove(position);
            imageAdapter.notifyItemRemoved(position);
            updateImageCountText();
        });
    }

    private void updateImageCountText() {
        btnSelectImages.setText("Chọn hình ảnh (" + selectedImages.size() + "/10)");
    }

    private void setupSpinners() {
        // Category spinner adapter will be set when data is loaded
        // Condition spinner adapter will be set when data is loaded
    }

    private void setupClickListeners() {
        btnSelectImages.setOnClickListener(v -> selectImages());
        btnGetLocation.setOnClickListener(v -> getCurrentLocation());
        btnPreview.setOnClickListener(v -> previewListing());
        btnSubmit.setOnClickListener(v -> submitListing());
    }

    private void loadCategories() {
        Call<ApiResponse> call = listingApiService.getAllCategories();
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            java.lang.reflect.Type listType = new TypeToken<List<Category>>(){}.getType();
                            categories = gson.fromJson(json, listType);

                            setupCategorySpinner();
                        } catch (Exception e) {
                            Log.e("CreateListing", "Error parsing categories", e);
                            setupDefaultCategories();
                        }
                    } else {
                        Log.e("CreateListing", "API error: " + apiResponse.getMessage());
                        setupDefaultCategories();
                    }
                } else {
                    Log.e("CreateListing", "Response not successful: " + response.code());
                    setupDefaultCategories();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("CreateListing", "Network error", t);
                setupDefaultCategories();
            }
        });
    }

    private void setupDefaultCategories() {
        categories.clear();

        Category cat1 = new Category();
        cat1.setId(1L);
        cat1.setName("Điện tử");
        categories.add(cat1);

        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Thời trang");
        categories.add(cat2);

        Category cat3 = new Category();
        cat3.setId(3L);
        cat3.setName("Gia dụng");
        categories.add(cat3);

        Category cat4 = new Category();
        cat4.setId(4L);
        cat4.setName("Xe cộ");
        categories.add(cat4);

        Category cat5 = new Category();
        cat5.setId(5L);
        cat5.setName("Sách & Văn phòng phẩm");
        categories.add(cat5);

        setupCategorySpinner();
        Toast.makeText(getContext(), "Sử dụng dữ liệu danh mục mặc định", Toast.LENGTH_SHORT).show();
    }

    private void loadConditions() {
        Call<ApiResponse> call = listingApiService.getAllConditions();
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            java.lang.reflect.Type listType = new TypeToken<List<ItemCondition>>(){}.getType();
                            conditions = gson.fromJson(json, listType);

                            setupConditionSpinner();
                        } catch (Exception e) {
                            Log.e("CreateListing", "Error parsing conditions", e);
                            setupDefaultConditions();
                        }
                    } else {
                        Log.e("CreateListing", "API error: " + apiResponse.getMessage());
                        setupDefaultConditions();
                    }
                } else {
                    Log.e("CreateListing", "Response not successful: " + response.code());
                    setupDefaultConditions();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e("CreateListing", "Network error", t);
                setupDefaultConditions();
            }
        });
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Chọn danh mục");
        for (Category category : categories) {
            categoryNames.add(category.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
    }

    private void setupConditionSpinner() {
        List<String> conditionNames = new ArrayList<>();
        conditionNames.add("Chọn tình trạng");
        for (ItemCondition condition : conditions) {
            conditionNames.add(condition.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, conditionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCondition.setAdapter(adapter);
    }

    private void setupDefaultConditions() {
        conditions.clear();

        ItemCondition condition1 = new ItemCondition();
        condition1.setId(1L);
        condition1.setName("Mới");
        conditions.add(condition1);

        ItemCondition condition2 = new ItemCondition();
        condition2.setId(2L);
        condition2.setName("Như mới");
        conditions.add(condition2);

        ItemCondition condition3 = new ItemCondition();
        condition3.setId(3L);
        condition3.setName("Tốt");
        conditions.add(condition3);

        ItemCondition condition4 = new ItemCondition();
        condition4.setId(4L);
        condition4.setName("Khá tốt");
        conditions.add(condition4);

        ItemCondition condition5 = new ItemCondition();
        condition5.setId(5L);
        condition5.setName("Cũ");
        conditions.add(condition5);

        setupConditionSpinner();
        Toast.makeText(getContext(), "Sử dụng dữ liệu tình trạng mặc định", Toast.LENGTH_SHORT).show();
    }

    private void selectImages() {
        if (selectedImages.size() >= 10) {
            Toast.makeText(getContext(), "Tối đa 10 hình ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        btnGetLocation.setText("Đang lấy vị trí...");
        btnGetLocation.setEnabled(false);

        try {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!gpsEnabled && !networkEnabled) {
                Toast.makeText(getContext(), "Vui lòng bật GPS hoặc dữ liệu di động", Toast.LENGTH_SHORT).show();
                resetLocationButton();
                return;
            }

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
            if (networkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }

            mainHandler.postDelayed(() -> {
                locationManager.removeUpdates(this);
                if (btnGetLocation.getText().equals("Đang lấy vị trí...")) {
                    Toast.makeText(getContext(), "Không thể lấy vị trí. Hãy thử lại.", Toast.LENGTH_SHORT).show();
                    resetLocationButton();
                }
            }, 15000);

        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Lỗi quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            resetLocationButton();
        }
    }

    private void resetLocationButton() {
        btnGetLocation.setText("Lấy vị trí");
        btnGetLocation.setEnabled(true);
    }

    private boolean validateFields() {
        if (TextUtils.isEmpty(etTitle.getText().toString().trim())) {
            etTitle.setError("Tiêu đề không được để trống");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etDescription.getText().toString().trim())) {
            etDescription.setError("Mô tả không được để trống");
            etDescription.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etPrice.getText().toString().trim())) {
            etPrice.setError("Giá không được để trống");
            etPrice.requestFocus();
            return false;
        }

        try {
            BigDecimal price = new BigDecimal(etPrice.getText().toString().trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                etPrice.setError("Giá phải lớn hơn 0");
                etPrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etPrice.setError("Giá không hợp lệ");
            etPrice.requestFocus();
            return false;
        }

        if (spCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spCondition.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Vui lòng chọn tình trạng", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void previewListing() {
        if (!validateFields()) {
            return;
        }
        showPreviewDialog();
    }

    private void showPreviewDialog() {
        // Tạo dialog full screen
        Dialog previewDialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        previewDialog.setContentView(R.layout.dialog_listing_preview);

        // Get views
        ViewPager2 vpPreviewImages = previewDialog.findViewById(R.id.vpPreviewImages);
        TextView tvImageCounter = previewDialog.findViewById(R.id.tvImageCounter);
        LinearLayout layoutNoImages = previewDialog.findViewById(R.id.layoutNoImages);
        TextView tvPreviewTitle = previewDialog.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewPrice = previewDialog.findViewById(R.id.tvPreviewPrice);
        TextView tvPreviewCategory = previewDialog.findViewById(R.id.tvPreviewCategory);
        TextView tvPreviewCondition = previewDialog.findViewById(R.id.tvPreviewCondition);
        TextView tvPreviewDescription = previewDialog.findViewById(R.id.tvPreviewDescription);
        TextView tvPreviewLocation = previewDialog.findViewById(R.id.tvPreviewLocation);
        TextView tvTagsLabel = previewDialog.findViewById(R.id.tvTagsLabel);
        FlexboxLayout flexboxTags = previewDialog.findViewById(R.id.flexboxTags);
        CircleImageView ivSellerAvatar = previewDialog.findViewById(R.id.ivSellerAvatar);
        TextView tvSellerName = previewDialog.findViewById(R.id.tvSellerName);
        RatingBar ratingBar = previewDialog.findViewById(R.id.ratingBar);

        ImageButton btnClosePreview = previewDialog.findViewById(R.id.btnClosePreview);
        Button btnEditPreview = previewDialog.findViewById(R.id.btnEditPreview);
        Button btnPublishPreview = previewDialog.findViewById(R.id.btnPublishPreview);

        // Setup image gallery
        if (selectedImages.isEmpty()) {
            vpPreviewImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
            layoutNoImages.setVisibility(View.VISIBLE);
        } else {
            vpPreviewImages.setVisibility(View.VISIBLE);
            tvImageCounter.setVisibility(View.VISIBLE);
            layoutNoImages.setVisibility(View.GONE);

            PreviewImageAdapter imageAdapter = new PreviewImageAdapter(getContext(), selectedImages);
            vpPreviewImages.setAdapter(imageAdapter);

            // Update image counter
            updateImageCounter(vpPreviewImages, tvImageCounter);
        }

        // Fill content
        tvPreviewTitle.setText(etTitle.getText().toString().trim());

        // Format price
        try {
            BigDecimal price = new BigDecimal(etPrice.getText().toString().trim());
            DecimalFormat formatter = new DecimalFormat("#,###");
            tvPreviewPrice.setText(formatter.format(price) + " VNĐ");
        } catch (Exception e) {
            tvPreviewPrice.setText(etPrice.getText().toString().trim() + " VNĐ");
        }

        // Category
        int categoryPosition = spCategory.getSelectedItemPosition();
        if (categoryPosition > 0) {
            tvPreviewCategory.setText(categories.get(categoryPosition - 1).getName());
        } else {
            tvPreviewCategory.setText("Không xác định");
        }

        // Condition
        int conditionPosition = spCondition.getSelectedItemPosition();
        if (conditionPosition > 0) {
            tvPreviewCondition.setText(conditions.get(conditionPosition - 1).getName());
        } else {
            tvPreviewCondition.setText("Không xác định");
        }

        tvPreviewDescription.setText(etDescription.getText().toString().trim());

        // Location
        String locationText = etLocation.getText().toString().trim();
        if (locationText.isEmpty()) {
            tvPreviewLocation.setText("Chưa xác định vị trí");
        } else {
            tvPreviewLocation.setText(locationText);
        }

        // Tags
        setupPreviewTags(flexboxTags, tvTagsLabel);

        // Seller info (get from SharedPreferences)
        setupSellerInfo(ivSellerAvatar, tvSellerName, ratingBar);

        // Button listeners
        btnClosePreview.setOnClickListener(v -> previewDialog.dismiss());

        btnEditPreview.setOnClickListener(v -> {
            previewDialog.dismiss();
            // Focus on first field or scroll to top
            etTitle.requestFocus();
        });

        btnPublishPreview.setOnClickListener(v -> {
            previewDialog.dismiss();
            submitListing(); // Call existing submit method
        });

        previewDialog.show();
    }

    private void updateImageCounter(ViewPager2 viewPager, TextView tvCounter) {
        tvCounter.setText("1/" + selectedImages.size());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvCounter.setText((position + 1) + "/" + selectedImages.size());
            }
        });
    }

    private void setupPreviewTags(FlexboxLayout flexboxTags, TextView tvTagsLabel) {
        String tagsText = etTags.getText().toString().trim();

        if (tagsText.isEmpty()) {
            flexboxTags.setVisibility(View.GONE);
            tvTagsLabel.setVisibility(View.GONE);
            return;
        }

        flexboxTags.setVisibility(View.VISIBLE);
        tvTagsLabel.setVisibility(View.VISIBLE);
        flexboxTags.removeAllViews();

        String[] tags = tagsText.split(",");
        for (String tag : tags) {
            String trimmedTag = tag.trim();
            if (!trimmedTag.isEmpty()) {
                TextView tagView = createTagView(trimmedTag);
                flexboxTags.addView(tagView);
            }
        }
    }

    private TextView createTagView(String tagText) {
        TextView tagView = new TextView(getContext());
        tagView.setText("#" + tagText);
        tagView.setTextSize(12);
        tagView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        tagView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_tag_item));

        // Set padding
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        tagView.setPadding(padding * 2, padding, padding * 2, padding);

        // Set margin
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, padding, padding);
        tagView.setLayoutParams(params);

        return tagView;
    }

    private void setupSellerInfo(CircleImageView ivAvatar, TextView tvName, RatingBar ratingBar) {
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String displayName = prefs.getString("displayName", "Người dùng");
        String avatarUrl = prefs.getString("avatarUrl", "");

        tvName.setText(displayName);

        // Load avatar
        if (!avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.drawable.user);
        }

        // Set default rating (you can get this from user profile)
        ratingBar.setRating(4.5f);
    }

    private void submitListing() {
        if (!validateFields()) {
            return;
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn ít nhất một hình ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setText("Đang tạo...");
        btnSubmit.setEnabled(false);

        CreateListingRequest request = new CreateListingRequest();
        request.setTitle(etTitle.getText().toString().trim());
        request.setDescription(etDescription.getText().toString().trim());
        request.setPrice(new BigDecimal(etPrice.getText().toString().trim()));

        int categoryPosition = spCategory.getSelectedItemPosition();
        if (categoryPosition > 0) {
            request.setCategoryId(categories.get(categoryPosition - 1).getId());
        }

        int conditionPosition = spCondition.getSelectedItemPosition();
        if (conditionPosition > 0) {
            request.setConditionId(conditions.get(conditionPosition - 1).getId());
        }        request.setLocationText(etLocation.getText().toString().trim());
        request.setLatitude(currentLatitude);
        request.setLongitude(currentLongitude);
        request.setIsNegotiable(cbNegotiable.isChecked()); // Add negotiable field

        String tagsText = etTags.getText().toString().trim();
        if (!tagsText.isEmpty()) {
            List<String> tagsList = Arrays.asList(tagsText.split(","));
            List<String> trimmedTags = new ArrayList<>();
            for (String tag : tagsList) {
                String trimmedTag = tag.trim();
                if (!trimmedTag.isEmpty()) {
                    trimmedTags.add(trimmedTag);
                }
            }
            request.setTags(trimmedTags);
        }

        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        Call<ApiResponse> call = listingApiService.createListing(userId, request);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Object dataObj = apiResponse.getData();
                        Long listingId = null;

                        if (dataObj instanceof Integer) {
                            listingId = ((Integer) dataObj).longValue();
                        } else if (dataObj instanceof Long) {
                            listingId = (Long) dataObj;
                        } else if (dataObj instanceof Double) {
                            listingId = ((Double) dataObj).longValue();
                        }

                        if (listingId != null) {
                            uploadImages(listingId, userId);
                        } else {
                            btnSubmit.setText("Đăng tin");
                            btnSubmit.setEnabled(true);
                            Toast.makeText(getContext(), "Lỗi tạo listing", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        btnSubmit.setText("Đăng tin");
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    btnSubmit.setText("Đăng tin");
                    btnSubmit.setEnabled(true);
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnSubmit.setText("Đăng tin");
                btnSubmit.setEnabled(true);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImages(Long listingId, Long userId) {
        Log.d("CreateListing", "=== STARTING IMAGE UPLOAD ===");
        Log.d("CreateListing", "Listing ID: " + listingId);
        Log.d("CreateListing", "User ID: " + userId);
        Log.d("CreateListing", "Selected images count: " + selectedImages.size());

        if (selectedImages.isEmpty()) {
            btnSubmit.setText("Đăng tin");
            btnSubmit.setEnabled(true);
            Toast.makeText(getContext(), "Không có hình ảnh để upload", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadAllImagesSequentially(listingId, userId);
    }

    private void uploadAllImages(Long listingId, Long userId) {
        Log.d("CreateListing", "=== UPLOADING ALL IMAGES ===");

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        List<File> tempFiles = new ArrayList<>();

        for (int i = 0; i < selectedImages.size(); i++) {
            Uri imageUri = selectedImages.get(i);
            Log.d("CreateListing", "Processing image " + (i + 1) + "/" + selectedImages.size() + ": " + imageUri);

            try {
                File imageFile = FileUtil.getFileFromUri(getContext(), imageUri);
                tempFiles.add(imageFile);

                if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                    RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);

                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                            "images",
                            imageFile.getName(),
                            requestFile
                    );

                    imageParts.add(imagePart);

                    Log.d("CreateListing", "✓ Added image " + (i + 1) + ": " + imageFile.getName() +
                            " (" + imageFile.length() + " bytes)");
                } else {
                    Log.e("CreateListing", "✗ Image " + (i + 1) + " is empty or doesn't exist");
                }

            } catch (IOException e) {
                Log.e("CreateListing", "✗ Error processing image " + (i + 1), e);
            }
        }

        if (imageParts.isEmpty()) {
            btnSubmit.setText("Đăng tin");
            btnSubmit.setEnabled(true);
            Toast.makeText(getContext(), "Không thể xử lý hình ảnh nào", Toast.LENGTH_SHORT).show();
            cleanupTempFiles(tempFiles);
            return;
        }

        Log.d("CreateListing", "Uploading " + imageParts.size() + " image parts to server...");

        Call<ApiResponse> call = listingApiService.uploadImages(listingId, userId, imageParts);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                btnSubmit.setText("Đăng tin");
                btnSubmit.setEnabled(true);

                Log.d("CreateListing", "=== UPLOAD ALL IMAGES RESPONSE ===");
                Log.d("CreateListing", "Response code: " + response.code());

                cleanupTempFiles(tempFiles);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    Log.d("CreateListing", "Success: " + apiResponse.isSuccess());
                    Log.d("CreateListing", "Message: " + apiResponse.getMessage());

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(getContext(), "🎉 Đăng tin thành công!", Toast.LENGTH_LONG).show();
                        clearForm();
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } else {
                        Toast.makeText(getContext(), "❌ Lỗi upload: " + apiResponse.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("CreateListing", "Upload response not successful: " + response.code());
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                btnSubmit.setText("Đăng tin");
                btnSubmit.setEnabled(true);
                cleanupTempFiles(tempFiles);
                Log.e("CreateListing", "Upload network error", t);
                Toast.makeText(getContext(), "❌ Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAllImagesSequentially(Long listingId, Long userId) {
        Log.d("CreateListing", "=== UPLOADING IMAGES SEQUENTIALLY ===");

        if (selectedImages.isEmpty()) {
            btnSubmit.setText("Đăng tin");
            btnSubmit.setEnabled(true);
            Toast.makeText(getContext(), "Không có hình ảnh để upload", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImageAtIndex(listingId, userId, 0, new ArrayList<>());
    }

    private void uploadImageAtIndex(Long listingId, Long userId, int index, List<File> tempFiles) {
        if (index >= selectedImages.size()) {
            btnSubmit.setText("Đăng tin");
            btnSubmit.setEnabled(true);
            cleanupTempFiles(tempFiles);
            Toast.makeText(getContext(), "🎉 Đăng tin thành công!", Toast.LENGTH_LONG).show();
            clearForm();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return;
        }

        Uri imageUri = selectedImages.get(index);
        Log.d("CreateListing", "Uploading image " + (index + 1) + "/" + selectedImages.size());

        try {
            File imageFile = FileUtil.getFileFromUri(getContext(), imageUri);
            tempFiles.add(imageFile);

            if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("images", imageFile.getName(), requestFile);

                // SỬA: Cần thêm method uploadSingleImage trong ListingApiService hoặc dùng uploadImages với 1 phần tử
                List<MultipartBody.Part> singleImageList = new ArrayList<>();
                singleImageList.add(imagePart);

                Call<ApiResponse> call = listingApiService.uploadImages(listingId, userId, singleImageList);
                call.enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse apiResponse = response.body();
                            if (apiResponse.isSuccess()) {
                                Log.d("CreateListing", "✓ Image " + (index + 1) + " uploaded successfully");
                                uploadImageAtIndex(listingId, userId, index + 1, tempFiles);
                            } else {
                                btnSubmit.setText("Đăng tin");
                                btnSubmit.setEnabled(true);
                                cleanupTempFiles(tempFiles);
                                Toast.makeText(getContext(), "❌ Lỗi upload ảnh " + (index + 1) + ": " + apiResponse.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            btnSubmit.setText("Đăng tin");
                            btnSubmit.setEnabled(true);
                            cleanupTempFiles(tempFiles);
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        btnSubmit.setText("Đăng tin");
                        btnSubmit.setEnabled(true);
                        cleanupTempFiles(tempFiles);
                        Log.e("CreateListing", "Upload image " + (index + 1) + " network error", t);
                        Toast.makeText(getContext(), "❌ Lỗi upload ảnh " + (index + 1) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("CreateListing", "✗ Image " + (index + 1) + " is empty or doesn't exist");
                uploadImageAtIndex(listingId, userId, index + 1, tempFiles);
            }

        } catch (Exception e) {
            Log.e("CreateListing", "Error processing image " + (index + 1), e);
            uploadImageAtIndex(listingId, userId, index + 1, tempFiles);
        }
    }

    private void cleanupTempFiles(List<File> tempFiles) {
        for (File file : tempFiles) {
            if (file != null && file.exists()) {
                try {
                    file.delete();
                    Log.d("CreateListing", "Deleted temp file: " + file.getName());
                } catch (Exception e) {
                    Log.e("CreateListing", "Error deleting temp file", e);
                }
            }
        }
    }

    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        etPrice.setText("");
        etLocation.setText("");
        etTags.setText("");

        spCategory.setSelection(0);
        spCondition.setSelection(0);

        selectedImages.clear();
        imageAdapter.notifyDataSetChanged();
        updateImageCountText();

        currentLatitude = null;
        currentLongitude = null;
    }

    private void handleErrorResponse(Response<ApiResponse> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e("CreateListing", "Error response: " + errorBody);
            Toast.makeText(getContext(), "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("CreateListing", "Error reading error response", e);
            Toast.makeText(getContext(), "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}