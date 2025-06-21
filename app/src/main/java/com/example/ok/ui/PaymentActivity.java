package com.example.ok.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.adapter.PaymentMethodAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    private static final boolean USE_MOCK_PAYMENT = false;  // Disable mock to use real MoMo  
    private TextView tvListingTitle, tvListingPrice, tvSellerName;
    private ImageView ivListingImage;
    private RecyclerView rvPaymentMethods;
    private Button btnAddPaymentMethod, btnProceedPayment;
    private RadioGroup rgPaymentMethods;
    private LinearLayout layoutEscrow;    private CheckBox cbUseEscrow;
    private TextView tvEscrowInfo;
    
    // Data
    private Long listingId;
    private Long existingPaymentId; // New field for existing payment
    private Long currentPaymentId; // Track current payment being processed
    private Listing listing;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private PaymentMethod selectedPaymentMethod;
    private PaymentMethodAdapter adapter;
    private ApiService apiService;
    private Long currentUserId;
    private DecimalFormat priceFormat = new DecimalFormat("#,###");
    
    // Store MoMo URLs for later use
    private String currentPayUrl;
    private String currentQrCodeUrl;
    private String currentDeeplink;
    
    // Store transaction ID for manual confirmation
    private String currentTransactionId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        
        initViews();
        initData();
        loadListingInfo();
        loadPaymentMethods();
        setupClickListeners();
    }
    
    private void initViews() {
        tvListingTitle = findViewById(R.id.tvListingTitle);
        tvListingPrice = findViewById(R.id.tvListingPrice);
        tvSellerName = findViewById(R.id.tvSellerName);
        ivListingImage = findViewById(R.id.ivListingImage);
        rvPaymentMethods = findViewById(R.id.rvPaymentMethods);
        btnAddPaymentMethod = findViewById(R.id.btnAddPaymentMethod);
        btnProceedPayment = findViewById(R.id.btnProceedPayment);
        layoutEscrow = findViewById(R.id.layoutEscrow);
        cbUseEscrow = findViewById(R.id.cbUseEscrow);
        tvEscrowInfo = findViewById(R.id.tvEscrowInfo);
        
        // Setup RecyclerView
        adapter = new PaymentMethodAdapter(paymentMethods, this::onPaymentMethodSelected);
        rvPaymentMethods.setLayoutManager(new LinearLayoutManager(this));
        rvPaymentMethods.setAdapter(adapter);
        
        // Setup escrow info
        tvEscrowInfo.setText("Escrow giúp bảo vệ giao dịch của bạn. Tiền sẽ được giữ an toàn cho đến khi xác nhận nhận hàng.");
    }
      private void initData() {
        apiService = RetrofitClient.getApiService();
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
        
        // Check if this is payment from accepted offer
        boolean isOfferPayment = getIntent().getBooleanExtra("IS_OFFER_PAYMENT", false);
        
        if (isOfferPayment) {
            // Payment from accepted offer
            listingId = getIntent().getLongExtra("LISTING_ID", -1);
            Long offerId = getIntent().getLongExtra("OFFER_ID", -1);
            double offerPrice = getIntent().getDoubleExtra("PRICE", 0);
            String listingTitle = getIntent().getStringExtra("LISTING_TITLE");
            
            Log.d(TAG, "Payment from accepted offer - Offer ID: " + offerId + 
                       ", Price: " + offerPrice + ", Listing: " + listingTitle);
            
            // Set the offer price as the payment amount
            if (offerPrice > 0) {
                getIntent().putExtra("listingPrice", offerPrice);
                getIntent().putExtra("listingTitle", listingTitle);
            }
        } else {
            // Normal payment flow
            listingId = getIntent().getLongExtra("listingId", -1);
        }
        
        existingPaymentId = getIntent().getLongExtra("existingPaymentId", -1);
        
        if (listingId == -1) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Log if we have an existing payment
        if (existingPaymentId != -1) {
            Log.d(TAG, "Processing existing payment ID: " + existingPaymentId);
        }
    }    private void loadListingInfo() {
        // Get listing info from intent
        String title = getIntent().getStringExtra("listingTitle");
        double price = getIntent().getDoubleExtra("listingPrice", 0);
        String sellerName = getIntent().getStringExtra("sellerName");
        String imageUrl = getIntent().getStringExtra("listingImageUrl");
        boolean isOfferPayment = getIntent().getBooleanExtra("IS_OFFER_PAYMENT", false);
        
        if (title != null) {
            // If this is payment from accepted offer, show special title
            if (isOfferPayment) {
                tvListingTitle.setText(title + " (Thanh toán theo offer đã chấp nhận)");
                Log.d(TAG, "Displaying offer payment for: " + title + ", price: " + price);
            } else {
                tvListingTitle.setText(title);
            }
            
            // If we have existing payment, show that amount instead of listing price
            if (existingPaymentId != -1) {
                double paymentAmount = getIntent().getDoubleExtra("paymentAmount", price);
                String paymentStatus = getIntent().getStringExtra("paymentStatus");
                String paymentMethod = getIntent().getStringExtra("paymentMethod");
                
                tvListingPrice.setText(priceFormat.format(paymentAmount) + " VNĐ");
                
                // Add status indicator
                if (paymentStatus != null) {
                    String statusText = getStatusText(paymentStatus);
                    tvListingTitle.setText(title + " (" + statusText + ")");
                }
                // Pre-select payment method if available
                if (paymentMethod != null) {
                    // This will be handled in loadPaymentMethods
                }
            } else {
                tvListingPrice.setText(priceFormat.format(price) + " VNĐ");
            }
            
            tvSellerName.setText("Người bán: " + sellerName);
            
            // Load image using Glide if available
            // For now, use placeholder
            ivListingImage.setImageResource(R.drawable.ic_placeholder_image);
        }
    }
    
    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "Chờ thanh toán";
            case "PROCESSING":
                return "Đang xử lý";
            case "COMPLETED":
                return "Hoàn thành";
            case "CANCELLED":
                return "Đã hủy";
            case "FAILED":
                return "Thất bại";
            default:
                return status;
        }
    }private void loadPaymentMethods() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải phương thức thanh toán...");
        progressDialog.show();
        
        Call<List<PaymentMethod>> call = apiService.getUserPaymentMethods(currentUserId);
        call.enqueue(new Callback<List<PaymentMethod>>() {
            @Override
            public void onResponse(@NonNull Call<List<PaymentMethod>> call, @NonNull Response<List<PaymentMethod>> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    List<PaymentMethod> loadedMethods = response.body();
                    if (loadedMethods != null && !loadedMethods.isEmpty()) {
                        paymentMethods.clear();
                        paymentMethods.addAll(loadedMethods);
                        setupPaymentMethodsUI();
                        Log.d(TAG, "Loaded " + loadedMethods.size() + " payment methods from API");
                    } else {
                        Log.d(TAG, "No payment methods found, using defaults");
                        setupDefaultPaymentMethods();
                    }
                } else {
                    Log.w(TAG, "Failed to load payment methods: " + response.code());
                    setupDefaultPaymentMethods();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<PaymentMethod>> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error loading payment methods", t);
                setupDefaultPaymentMethods();
            }
        });
    }
      private void setupDefaultPaymentMethods() {
        paymentMethods.clear();
        
        // Add default payment methods
        paymentMethods.add(new PaymentMethod(PaymentMethod.MOMO, "Ví MoMo", "Thanh toán qua ví MoMo"));
        paymentMethods.add(new PaymentMethod(PaymentMethod.VISA, "Thẻ Visa", "Thanh toán bằng thẻ Visa"));
        paymentMethods.add(new PaymentMethod("STRIPE", "Stripe", "Thanh toán quốc tế qua Stripe"));
        paymentMethods.add(new PaymentMethod(PaymentMethod.CASH, "Tiền mặt", "Thanh toán khi giao hàng (COD)"));
        
        adapter.notifyDataSetChanged();
        
        // Select first method by default
        if (!paymentMethods.isEmpty()) {
            selectedPaymentMethod = paymentMethods.get(0);
            adapter.setSelectedMethod(selectedPaymentMethod);
        }
    }
    
    private void setupPaymentMethodsUI() {
        adapter.notifyDataSetChanged();
        
        // Select first method by default
        if (!paymentMethods.isEmpty()) {
            selectedPaymentMethod = paymentMethods.get(0);
            adapter.setSelectedMethod(selectedPaymentMethod);
        }
    }
    
    private void onPaymentMethodSelected(PaymentMethod method) {
        selectedPaymentMethod = method;
        Log.d(TAG, "Selected payment method: " + method.getType());
        
        // Show/hide escrow option based on payment method
        if (PaymentMethod.CASH.equals(method.getType())) {
            layoutEscrow.setVisibility(View.GONE);
        } else {
            layoutEscrow.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupClickListeners() {
        btnAddPaymentMethod.setOnClickListener(v -> showAddPaymentMethodDialog());
        btnProceedPayment.setOnClickListener(v -> proceedWithPayment());
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
    
    private void showAddPaymentMethodDialog() {
        String[] options = {"Thêm ví MoMo", "Thêm thẻ Visa/Mastercard"};
        
        new AlertDialog.Builder(this)
                .setTitle("Thêm phương thức thanh toán")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddMoMoDialog();
                    } else {
                        showAddCardDialog();
                    }
                })
                .show();
    }
    
    private void showAddMoMoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_momo, null);
        EditText etPhoneNumber = dialogView.findViewById(R.id.etPhoneNumber);
        
        new AlertDialog.Builder(this)
                .setTitle("Thêm ví MoMo")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String phoneNumber = etPhoneNumber.getText().toString().trim();
                    if (validatePhoneNumber(phoneNumber)) {
                        addMoMoPaymentMethod(phoneNumber);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showAddCardDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_card, null);
        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etExpiryDate = dialogView.findViewById(R.id.etExpiryDate);
        EditText etHolderName = dialogView.findViewById(R.id.etHolderName);
        
        new AlertDialog.Builder(this)
                .setTitle("Thêm thẻ thanh toán")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String cardNumber = etCardNumber.getText().toString().trim();
                    String expiryDate = etExpiryDate.getText().toString().trim();
                    String holderName = etHolderName.getText().toString().trim();
                    
                    if (validateCardInfo(cardNumber, expiryDate, holderName)) {
                        addCardPaymentMethod(cardNumber, expiryDate, holderName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!phoneNumber.matches("^0[0-9]{9}$")) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    private boolean validateCardInfo(String cardNumber, String expiryDate, String holderName) {
        if (cardNumber.isEmpty() || expiryDate.isEmpty() || holderName.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin thẻ", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Basic card number validation (should be improved)
        cardNumber = cardNumber.replaceAll("\\s", "");
        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            Toast.makeText(this, "Số thẻ không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void addMoMoPaymentMethod(String phoneNumber) {
        PaymentMethod method = new PaymentMethod(PaymentMethod.MOMO, "MoMo " + phoneNumber, 
                phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7));
        method.setPhoneNumber(phoneNumber);
        
        // Add to API and local list
        paymentMethods.add(method);
        adapter.notifyDataSetChanged();
        
        Toast.makeText(this, "Đã thêm ví MoMo", Toast.LENGTH_SHORT).show();
    }
    
    private void addCardPaymentMethod(String cardNumber, String expiryDate, String holderName) {
        String maskedNumber = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        String cardType = getCardType(cardNumber);
        
        PaymentMethod method = new PaymentMethod(cardType, cardType + " " + maskedNumber, maskedNumber);
        method.setCardNumber(cardNumber);
        method.setExpiryDate(expiryDate);
        method.setHolderName(holderName);
        
        paymentMethods.add(method);
        adapter.notifyDataSetChanged();
        
        Toast.makeText(this, "Đã thêm thẻ " + cardType, Toast.LENGTH_SHORT).show();
    }
    
    private String getCardType(String cardNumber) {
        cardNumber = cardNumber.replaceAll("\\s", "");
        if (cardNumber.startsWith("4")) {
            return PaymentMethod.VISA;
        } else if (cardNumber.startsWith("5")) {
            return PaymentMethod.MASTERCARD;
        }
        return PaymentMethod.VISA; // Default
    }
    
    private void proceedWithPayment() {
        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // First check if there's an existing pending payment for this listing
        checkPendingPayment();
    }
      private void checkPendingPayment() {
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang kiểm tra giao dịch...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
        
        // Call API to check for pending payments
        Call<Payment> call = apiService.getPendingPaymentForListing(userId, listingId);
        call.enqueue(new Callback<Payment>() {
            @Override
            public void onResponse(@NonNull Call<Payment> call, @NonNull Response<Payment> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null && response.body().getId() != null) {
                    // Found existing pending payment
                    Payment existingPayment = response.body();
                    Log.d(TAG, "Found pending payment: " + existingPayment.getId() + 
                          " with status: " + existingPayment.getStatus());
                    
                    // Show dialog to handle the existing payment
                    showExistingPaymentDialog(existingPayment);
                } else {
                    // No pending payment, proceed with new payment
                    showPaymentConfirmationDialog();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Payment> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error checking pending payment", t);
                // If we can't check, just proceed with normal flow
                showPaymentConfirmationDialog();
            }
        });
    }
      private void showExistingPaymentDialog(Payment existingPayment) {
        // Get formatted information
        String amount = priceFormat.format(existingPayment.getAmount());
        String paymentMethod = existingPayment.getPaymentMethodType();
        String status = getStatusText(existingPayment.getStatus());
        String formattedDate = "";
        
        try {
            // Format created date if available
            if (existingPayment.getCreatedAt() != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                formattedDate = outputFormat.format(existingPayment.getCreatedAt());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            formattedDate = existingPayment.getCreatedAt() != null ? existingPayment.getCreatedAt().toString() : "";
        }
        
        // Create alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Giao dịch đang chờ xử lý");
        
        // Create message with details
        String message = String.format(
            "Đã tồn tại giao dịch cho sản phẩm này:\n\n" +
            "• Mã giao dịch: %s\n" +
            "• Số tiền: %s VNĐ\n" +
            "• Phương thức: %s\n" +
            "• Trạng thái: %s\n" +
            "• Ngày tạo: %s\n\n" +
            "Bạn muốn làm gì với giao dịch này?",
            existingPayment.getTransactionId(),
            amount,
            getPaymentMethodText(paymentMethod),
            status,
            formattedDate
        );
        
        builder.setMessage(message);
        
        // Continue with existing payment
        builder.setPositiveButton("Tiếp tục thanh toán", (dialog, which) -> {
            // Store the existing payment ID
            existingPaymentId = existingPayment.getId();
            currentTransactionId = existingPayment.getTransactionId();
            
            // Proceed based on payment method
            if ("MOMO".equals(existingPayment.getPaymentMethodType())) {
                showMoMoOptionsForExistingPayment(existingPayment);
            } else if ("VISA".equals(existingPayment.getPaymentMethodType()) || 
                       "MASTERCARD".equals(existingPayment.getPaymentMethodType())) {
                showStripeOptionsForExistingPayment(existingPayment);
            } else {
                // For other methods, just show manual confirm
                showManualConfirmPaymentDialog();
            }
        });
        
        // Cancel existing payment and create new one
        builder.setNegativeButton("Hủy và tạo mới", (dialog, which) -> {
            // Cancel the existing payment first
            cancelExistingPayment(existingPayment.getId(), () -> {
                // Then show confirmation for new payment
                showPaymentConfirmationDialog();
            });
        });
        
        // Just go back
        builder.setNeutralButton("Đóng", null);
        
        builder.show();
    }
      private String getPaymentMethodText(String method) {
        switch (method) {
            case "MOMO": return "Ví MoMo";
            case "VISA": return "Thẻ Visa";
            case "MASTERCARD": return "Thẻ Mastercard";
            case "CASH": return "Tiền mặt (COD)";
            case "STRIPE": return "Stripe";
            default: return method;
        }
    }
    
    private void cancelExistingPayment(Long paymentId, Runnable onComplete) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang hủy giao dịch...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
          Call<java.util.Map<String, Object>> call = apiService.cancelPayment(paymentId, userId);
        call.enqueue(new Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> result = response.body();
                    Boolean success = (Boolean) result.get("success");
                    if (success != null && success) {
                        Toast.makeText(PaymentActivity.this, "Đã hủy giao dịch cũ", Toast.LENGTH_SHORT).show();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } else {
                        Toast.makeText(PaymentActivity.this, 
                            "Không thể hủy giao dịch. Vui lòng thử lại sau.", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Không thể hủy giao dịch. Vui lòng thử lại sau.", 
                        Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error cancelling payment", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
      private void showMoMoOptionsForExistingPayment(Payment existingPayment) {
        // Create options for continuing with existing MoMo payment
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🌟 Tiếp tục thanh toán MoMo");
        
        // Create list of options
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        
        // Option 1: Open MoMo app (if we have the transaction ID)
        options.add("📱 Mở ứng dụng MoMo");
        actions.add(() -> {
            if (existingPayment.getTransactionId() != null) {
                // Try to open MoMo app with the transaction ID
                currentTransactionId = existingPayment.getTransactionId();
                // openMoMoApp("momo://app?action=payment&tranid=" + currentTransactionId);
                Toast.makeText(this, "Sẽ mở ứng dụng MoMo với mã giao dịch: " + currentTransactionId, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có mã giao dịch MoMo", Toast.LENGTH_SHORT).show();
                // showManualConfirmPaymentDialog();
                Toast.makeText(this, "Vui lòng xác nhận thanh toán thủ công", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Option 2: Manual confirmation
        options.add("✅ Xác nhận đã thanh toán");
        actions.add(() -> {
            // showManualConfirmPaymentDialog();
            Toast.makeText(this, "Chức năng xác nhận thủ công sẽ được triển khai", Toast.LENGTH_SHORT).show();
        });
        
        // Option 3: Cancel payment
        options.add("❌ Hủy thanh toán");
        actions.add(() -> {
            cancelExistingPayment(existingPayment.getId(), null);
        });
        
        // Create the dialog
        CharSequence[] optionsArray = options.toArray(new CharSequence[0]);
        builder.setItems(optionsArray, (dialog, which) -> {
            actions.get(which).run();
        });
        
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }    private void showStripeOptionsForExistingPayment(Payment existingPayment) {
        // Create options for continuing with existing Stripe payment
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🌟 Tiếp tục thanh toán Stripe");
        
        // Create list of options
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        
        // Option 1: Re-create Stripe session if needed
        options.add("🔄 Tạo lại phiên thanh toán");
        actions.add(() -> {
            recreateStripePayment(existingPayment);
        });
        
        // Option 2: Manual confirmation
        options.add("✅ Xác nhận đã thanh toán");
        actions.add(() -> {
            showStripeManualConfirmDialog();
        });
          // Option 3: Cancel payment
        options.add("❌ Hủy thanh toán");
        actions.add(() -> {
            cancelExistingPayment(existingPayment.getId(), null);
        });
        
        // Create the dialog
        CharSequence[] optionsArray = options.toArray(new CharSequence[0]);
        builder.setItems(optionsArray, (dialog, which) -> {
            actions.get(which).run();
        });
        
        builder.setNegativeButton("Đóng", null);
        builder.show();
    }
    
    private void recreateStripePayment(Payment existingPayment) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo lại phiên thanh toán Stripe...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Create new payment request based on existing payment
        com.example.ok.model.PaymentRequest paymentRequest = new com.example.ok.model.PaymentRequest();
        paymentRequest.setListingId(existingPayment.getListingId());
        paymentRequest.setAmount(existingPayment.getAmount());
        paymentRequest.setDescription(existingPayment.getDescription());
        paymentRequest.setBuyerId(existingPayment.getBuyerId());
        paymentRequest.setSellerId(existingPayment.getSellerId());
        paymentRequest.setPaymentMethodType("STRIPE");
        
        // Process new Stripe payment
        processStripePayment(paymentRequest, progressDialog);
    }
    
    private void showPaymentConfirmationDialog() {
        double amount = getIntent().getDoubleExtra("listingPrice", 0);
        String message = String.format("Xác nhận thanh toán %s VNĐ bằng %s?", 
                priceFormat.format(amount), selectedPaymentMethod.getDisplayName());

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> processPayment())
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showManualConfirmPaymentDialog() {
        // Placeholder method for manual payment confirmation
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán thủ công")
                .setMessage("Bạn đã hoàn thành thanh toán chưa?")
                .setPositiveButton("Đã thanh toán", (dialog, which) -> {
                    // Handle manual confirmation
                    Toast.makeText(this, "Đã xác nhận thanh toán", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Chưa", null)
                .show();
    }
    
    private void processPayment() {
        if (selectedPaymentMethod == null) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý thanh toán...");
        progressDialog.setCancelable(false);
        progressDialog.show();
          // Get payment details from intent
        double amount = getIntent().getDoubleExtra("listingPrice", 0);
        String listingTitle = getIntent().getStringExtra("listingTitle");
        Long sellerId = getIntent().getLongExtra("sellerId", -1);
        Long offerId = getIntent().getLongExtra("OFFER_ID", -1); // Get offer ID if this is offer payment
        
        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        Long userId = prefs.getLong("userId", -1);
        
        // Create payment request
        com.example.ok.model.PaymentRequest paymentRequest = new com.example.ok.model.PaymentRequest();
        paymentRequest.setListingId(listingId);
        
        // Set offer ID if this is an offer payment
        if (offerId != null && offerId != -1) {
            paymentRequest.setOfferId(offerId);
            Log.d(TAG, "Setting offer ID in payment request: " + offerId);
        }
        
        paymentRequest.setPaymentMethodType(selectedPaymentMethod.getType());
        paymentRequest.setAmount(amount);
        paymentRequest.setDescription("Thanh toán cho: " + listingTitle);
        paymentRequest.setBuyerId(userId);
        paymentRequest.setSellerId(sellerId);
        paymentRequest.setUseEscrow(cbUseEscrow != null && cbUseEscrow.isChecked());
        
        // Set additional fields based on payment method
        if (PaymentMethod.MOMO.equals(selectedPaymentMethod.getType())) {
            paymentRequest.setPhoneNumber(selectedPaymentMethod.getPhoneNumber());
        } else if (PaymentMethod.VISA.equals(selectedPaymentMethod.getType()) || 
                   PaymentMethod.MASTERCARD.equals(selectedPaymentMethod.getType())) {
            paymentRequest.setCardNumber(selectedPaymentMethod.getCardNumber());
            paymentRequest.setExpiryDate(selectedPaymentMethod.getExpiryDate());
            paymentRequest.setHolderName(selectedPaymentMethod.getHolderName());
            // Note: CVV should be collected from user input for security
        }
          // Process payment based on method type
        if (PaymentMethod.MOMO.equals(selectedPaymentMethod.getType())) {
            processMoMoPayment(paymentRequest, progressDialog);
        } else if (PaymentMethod.VISA.equals(selectedPaymentMethod.getType()) || 
                   PaymentMethod.MASTERCARD.equals(selectedPaymentMethod.getType())) {
            processCardPayment(paymentRequest, progressDialog);
        } else if (PaymentMethod.STRIPE.equals(selectedPaymentMethod.getType())) {
            processStripePayment(paymentRequest, progressDialog);
        } else if (PaymentMethod.CASH.equals(selectedPaymentMethod.getType())) {
            processCashPayment(paymentRequest, progressDialog);
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Phương thức thanh toán không được hỗ trợ", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void processMoMoPayment(com.example.ok.model.PaymentRequest paymentRequest, ProgressDialog progressDialog) {
        Call<com.example.ok.model.MoMoPaymentResponse> call = apiService.processMoMoPayment(paymentRequest);
        call.enqueue(new Callback<com.example.ok.model.MoMoPaymentResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.ok.model.MoMoPaymentResponse> call, 
                                 @NonNull Response<com.example.ok.model.MoMoPaymentResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    com.example.ok.model.MoMoPaymentResponse momoResponse = response.body();
                    
                    if (momoResponse.isSuccess() && momoResponse.getPayment() != null) {
                        // Store payment info for later reference
                        existingPaymentId = momoResponse.getPayment().getId();
                        currentTransactionId = momoResponse.getPayment().getTransactionId();
                        
                        // Show MoMo payment options
                        showMoMoPaymentOptions(momoResponse);
                    } else {
                        Toast.makeText(PaymentActivity.this, 
                            "Lỗi MoMo: " + momoResponse.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Không thể kết nối với MoMo. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<com.example.ok.model.MoMoPaymentResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "MoMo payment error", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void processCardPayment(com.example.ok.model.PaymentRequest paymentRequest, ProgressDialog progressDialog) {
        Call<ApiResponse> call = apiService.processCardPayment(paymentRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                      if (apiResponse.isSuccess()) {
                        Toast.makeText(PaymentActivity.this, 
                            "Thanh toán thẻ thành công!", 
                            Toast.LENGTH_SHORT).show();
                        // Navigate to rating activity
                        navigateToRatingActivity();
                    } else {
                        Toast.makeText(PaymentActivity.this, 
                            "Lỗi thanh toán thẻ: " + apiResponse.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Không thể xử lý thanh toán thẻ. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Card payment error", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void processCashPayment(com.example.ok.model.PaymentRequest paymentRequest, ProgressDialog progressDialog) {
        Call<ApiResponse> call = apiService.createPayment(paymentRequest);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                      if (apiResponse.isSuccess()) {
                        Toast.makeText(PaymentActivity.this, 
                            "Đã tạo đơn hàng COD thành công!", 
                            Toast.LENGTH_SHORT).show();
                        // Navigate to rating activity
                        navigateToRatingActivity();
                    } else {
                        Toast.makeText(PaymentActivity.this, 
                            "Lỗi tạo đơn COD: " + apiResponse.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Không thể tạo đơn COD. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Cash payment error", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showMoMoPaymentOptions(com.example.ok.model.MoMoPaymentResponse momoResponse) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🌟 Thanh toán MoMo");
        
        // Create list of options
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        
        // Option 1: Open MoMo app with deeplink
        if (momoResponse.getDeeplink() != null && !momoResponse.getDeeplink().isEmpty()) {
            options.add("📱 Mở ứng dụng MoMo");
            actions.add(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(momoResponse.getDeeplink()));
                    startActivity(intent);
                    // Start monitoring payment status
                    startPaymentStatusMonitoring();
                } catch (Exception e) {
                    Log.e(TAG, "Cannot open MoMo app", e);
                    Toast.makeText(this, "Không thể mở ứng dụng MoMo", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Option 2: Open payment URL in browser
        if (momoResponse.getPaymentUrl() != null && !momoResponse.getPaymentUrl().isEmpty()) {
            options.add("🌐 Thanh toán trên web");
            actions.add(() -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(momoResponse.getPaymentUrl()));
                    startActivity(intent);
                    // Start monitoring payment status
                    startPaymentStatusMonitoring();
                } catch (Exception e) {
                    Log.e(TAG, "Cannot open payment URL", e);
                    Toast.makeText(this, "Không thể mở trang thanh toán", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Option 3: Manual confirmation
        options.add("✅ Xác nhận đã thanh toán");
        actions.add(() -> {
            showManualConfirmPaymentDialog();
        });
        
        // Option 4: Cancel payment
        options.add("❌ Hủy thanh toán");
        actions.add(() -> {
            if (existingPaymentId != null) {
                cancelExistingPayment(existingPaymentId, null);
            }
        });
        
        // Create the dialog
        CharSequence[] optionsArray = options.toArray(new CharSequence[0]);
        builder.setItems(optionsArray, (dialog, which) -> {
            actions.get(which).run();
        });
        
        builder.setCancelable(false);
        builder.show();
    }    private void processStripePayment(com.example.ok.model.PaymentRequest paymentRequest, ProgressDialog progressDialog) {
        Call<Map<String, Object>> call = apiService.createStripePayment(paymentRequest);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, 
                                 @NonNull Response<Map<String, Object>> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> stripeResponse = response.body();
                    Boolean success = (Boolean) stripeResponse.get("success");
                    
                    if (success != null && success) {
                        // Store payment info for later reference
                        Object paymentIdObj = stripeResponse.get("paymentId");
                        if (paymentIdObj instanceof Number) {
                            existingPaymentId = ((Number) paymentIdObj).longValue();
                        }
                        
                        String sessionId = (String) stripeResponse.get("sessionId");
                        String checkoutUrl = (String) stripeResponse.get("checkoutUrl");
                        
                        // Show Stripe payment options
                        showStripePaymentOptions(stripeResponse);
                    } else {
                        String message = (String) stripeResponse.get("message");
                        Toast.makeText(PaymentActivity.this, 
                            "Lỗi Stripe: " + (message != null ? message : "Không xác định"), 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Không thể kết nối với Stripe. Vui lòng thử lại sau.", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Stripe payment error", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối Stripe: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }    private void showStripePaymentOptions(Map<String, Object> stripeResponse) {
        Log.d(TAG, "=== SHOWING STRIPE PAYMENT OPTIONS ===");
        
        String sessionId = (String) stripeResponse.get("sessionId");
        String checkoutUrl = (String) stripeResponse.get("checkoutUrl");
        Boolean mockMode = (Boolean) stripeResponse.get("mockMode");
        Long paymentId = existingPaymentId != null ? existingPaymentId : currentPaymentId;
        
        Log.d(TAG, "sessionId: " + sessionId);
        Log.d(TAG, "checkoutUrl: " + checkoutUrl);
        Log.d(TAG, "mockMode: " + mockMode);
        Log.d(TAG, "paymentId: " + paymentId);
        
        // Dialog with different options based on mode
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        if (Boolean.TRUE.equals(mockMode)) {
            // Mock mode dialog
            builder.setTitle("💳 Stripe Payment (Mock Mode)");
            builder.setMessage("Chế độ test - Vui lòng chọn:");
            
            // Mock checkout page
            builder.setPositiveButton("🌐 Mock Checkout", (dialog, which) -> {
                Log.d(TAG, "User chose: Mock checkout page");
                openMockCheckoutPage(paymentId);
            });
            
            // Quick success
            builder.setNeutralButton("✅ Quick Success", (dialog, which) -> {
                Log.d(TAG, "User chose: Quick success");
                showStripeManualConfirmDialog();
            });
        } else {
            // Real mode dialog
            builder.setTitle("💳 Stripe Payment (Real Mode)");
            builder.setMessage("Thanh toán thật - Nhập thông tin thẻ:");
            
            // Real checkout with card form
            builder.setPositiveButton("💳 Nhập thẻ", (dialog, which) -> {
                Log.d(TAG, "User chose: Real card form checkout");
                openRealCheckoutPage(paymentId);
            });
            
            // Mock option still available
            builder.setNeutralButton("🧪 Test Mock", (dialog, which) -> {
                Log.d(TAG, "User chose: Switch to mock mode");
                openMockCheckoutPage(paymentId);
            });
        }
        
        // Cancel button for both modes
        builder.setNegativeButton("❌ Hủy", (dialog, which) -> {
            Log.d(TAG, "User chose: Cancel payment");
            if (paymentId != null) {
                cancelExistingPayment(paymentId, null);
            } else {
                Toast.makeText(this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setCancelable(true);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        Log.d(TAG, "=== STRIPE DIALOG SHOWN ===");
    }
      private void openMockCheckoutPage(Long paymentId) {
        if (paymentId == null) {
            Toast.makeText(this, "Lỗi: Không có payment ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Fix double slash issue
            String baseUrl = RetrofitClient.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String mockUrl = baseUrl + "/api/v1/payments/stripe/mock-checkout?paymentId=" + paymentId;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mockUrl));
            startActivity(intent);
            Toast.makeText(this, "Đang mở trang mock checkout...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Cannot open mock checkout", e);
            Toast.makeText(this, "Lỗi mở trang mock: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }private void openRealCheckoutPage(Long paymentId) {
        if (paymentId == null) {
            Toast.makeText(this, "Lỗi: Không có payment ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Use our custom checkout page with the payment ID - fix double slash
            String baseUrl = RetrofitClient.getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String realUrl = baseUrl + "/api/v1/payments/stripe/checkout/" + paymentId;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(realUrl));
            startActivity(intent);
            Toast.makeText(this, "Đang mở trang nhập thẻ...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Opening real checkout URL: " + realUrl);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open real checkout", e);
            Toast.makeText(this, "Lỗi mở trang thật: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startStripePaymentStatusMonitoring() {
        if (existingPaymentId == null) {
            Log.w(TAG, "Cannot monitor Stripe payment status: no payment ID");
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đang theo dõi thanh toán Stripe");
        builder.setMessage("Hệ thống đang theo dõi trạng thái thanh toán của bạn...\n\nVui lòng hoàn tất thanh toán trên trang Stripe.");
        builder.setPositiveButton("Đã thanh toán", (dialog, which) -> {
            showStripeManualConfirmDialog();
        });        builder.setNegativeButton("Hủy", (dialog, which) -> {
            if (existingPaymentId != null) {
                cancelExistingPayment(existingPaymentId, null);
            }
        });
        builder.setCancelable(false);
        
        builder.show();
    }
    
    private void showStripeManualConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận thanh toán Stripe");
        builder.setMessage("Bạn đã hoàn tất thanh toán trên trang Stripe chưa?");
        
        builder.setPositiveButton("✅ Đã thanh toán", (dialog, which) -> {
            // Call API to confirm payment success
            confirmStripePayment();
        });
        
        builder.setNegativeButton("❌ Chưa thanh toán", null);
        builder.show();
    }
    
    private void confirmStripePayment() {
        if (existingPaymentId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xác nhận thanh toán Stripe...");
        progressDialog.setCancelable(false);
        progressDialog.show();
          // For mock mode, we can simulate success by updating payment status
        Call<Object> call = apiService.updatePaymentStatus(existingPaymentId, "COMPLETED", null);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful()) {
                    Toast.makeText(PaymentActivity.this, 
                        "✅ Thanh toán Stripe thành công!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Set result and finish
                    Intent result = new Intent();
                    result.putExtra("payment_success", true);
                    result.putExtra("payment_method", "STRIPE");
                    result.putExtra("payment_id", existingPaymentId);
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Toast.makeText(PaymentActivity.this, 
                        "Lỗi xác nhận thanh toán", 
                        Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Error confirming Stripe payment", t);
                Toast.makeText(PaymentActivity.this, 
                    "Lỗi kết nối: " + t.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startPaymentStatusMonitoring() {
        if (existingPaymentId == null) {
            Log.w(TAG, "Cannot monitor payment status: no payment ID");
            return;
        }
        
        // Show a monitoring dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đang theo dõi thanh toán");
        builder.setMessage("Hệ thống đang theo dõi trạng thái thanh toán của bạn...\n\nVui lòng hoàn tất thanh toán trên MoMo.");
        builder.setPositiveButton("Đã thanh toán", (dialog, which) -> {
            // User confirms payment is done
            showManualConfirmPaymentDialog();
        });
        builder.setNegativeButton("Hủy", null);
        builder.setCancelable(false);
        
        builder.show();
    }    private void navigateToRatingActivity() {
        // Get necessary data for rating
        Long sellerId = getIntent().getLongExtra("sellerId", -1);
        String sellerName = getIntent().getStringExtra("sellerName");
        String listingTitle = getIntent().getStringExtra("title");
        
        // Create intent for RatingActivity
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("listingId", listingId); // Pass listing ID instead of transaction ID
        intent.putExtra("ratedUserId", sellerId);
        intent.putExtra("ratedUserName", sellerName);
        intent.putExtra("listingTitle", listingTitle);
        intent.putExtra("isRatingBuyer", false); // We're rating the seller
        
        startActivity(intent);
        finish();
    }
}
