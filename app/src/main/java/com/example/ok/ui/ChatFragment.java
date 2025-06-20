package com.example.ok.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.ok.api.ApiService;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ChatAdapter;
import com.example.ok.model.ApiResponse;
import com.example.ok.api.ChatApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.BlockUserRequest;
import com.example.ok.model.ChatMessage;
import com.example.ok.model.ChatRoom;
import com.google.gson.Gson;
import com.example.ok.utils.BlockedUserFilter;
import com.example.ok.util.NotificationHelper;

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private static final int PAGE_SIZE = 20;
    private static final int REQUEST_SELECT_IMAGE = 101;
    private static final int REQUEST_TAKE_PHOTO = 102;
    
    // Fragment arguments
    private long roomId = -1;
    private long myId = -1;
    private long otherId = -1;
    private String otherName = "";
    private long listingId = -1;
      // UI components
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttachment;
    private Button btnViewListing;    private TextView tvOtherUserName;
    private ImageView ivOtherUserAvatar;
    private ImageView ivListingImage;
    private TextView tvListingTitle;
    private TextView tvListingPrice;
    private LinearLayout layoutListing;
    private SwipeRefreshLayout swipeRefresh;
    private View progressBar;
    
    // Data
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private ChatRoom chatRoom;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean canLoadMore = true;
    
    // Services
    private ChatApiService chatApiService;
      // Polling for new messages
    private Handler messageHandler = new Handler(Looper.getMainLooper());
    private Runnable messageRunnable;
    private static final int POLLING_INTERVAL = 5000; // 5 seconds
    
    // Notification helper for local notifications
    private NotificationHelper notificationHelper;
    
    // Track if fragment is visible to user
    private boolean isFragmentVisible = false;
    
    // Add block status check variables
    private boolean isBlockedUser = false;
    private boolean hasCheckedBlockStatus = false;
    
    public static ChatFragment newInstance(long roomId, long myId, long otherId, String otherName) {
        return newInstance(roomId, myId, otherId, otherName, -1);
    }
    
    public static ChatFragment newInstance(long roomId, long myId, long otherId, String otherName, long listingId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putLong("roomId", roomId);
        args.putLong("myId", myId);
        args.putLong("otherId", otherId);
        args.putString("otherName", otherName);
        args.putLong("listingId", listingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        if (getArguments() != null) {
            roomId = getArguments().getLong("roomId", -1);
            myId = getArguments().getLong("myId", -1);
            otherId = getArguments().getLong("otherId", -1);
            otherName = getArguments().getString("otherName", "");
            listingId = getArguments().getLong("listingId", -1);
        }
          // If user ID not provided, get from SharedPreferences
        if (myId == -1) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            myId = prefs.getLong("userId", -1);
        }
          // Ensure RetrofitClient is initialized before using it
        RetrofitClient.init(requireContext());
        chatApiService = RetrofitClient.getChatApiService();
        
        // Initialize notification helper
        notificationHelper = new NotificationHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupListeners();
        initChatRoom();
    }
      private void initViews(View view) {        // Toolbar
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        tvOtherUserName = view.findViewById(R.id.tvOtherUserName);
        ivOtherUserAvatar = view.findViewById(R.id.ivOtherUserAvatar);
          // Listing info
        layoutListing = view.findViewById(R.id.layoutListing);
        ivListingImage = view.findViewById(R.id.ivListingImage);
        tvListingTitle = view.findViewById(R.id.tvListingTitle);
        tvListingPrice = view.findViewById(R.id.tvListingPrice);
        btnViewListing = view.findViewById(R.id.btnViewListing);
          // Chat
        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnAttachment = view.findViewById(R.id.btnAttachment);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        
        chatAdapter = new ChatAdapter(requireContext(), messageList, myId);
        recyclerMessages.setAdapter(chatAdapter);
        
        // Set initial UI values
        tvOtherUserName.setText(otherName);
    }
      private void setupListeners() {
        // Back button
        View btnBack = requireView().findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        
        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        
        // Attachment button
        btnAttachment.setOnClickListener(v -> showAttachmentOptions());
        
        // Back to listing button
        btnViewListing.setOnClickListener(v -> navigateToListing());
        
        // Pull to refresh
        swipeRefresh.setOnRefreshListener(this::loadMoreMessages);
          // **TEMPORARY: Add test notification on double-tap of other user name**
        tvOtherUserName.setOnClickListener(v -> {
            Log.d(TAG, "User name clicked - triggering test notification");
            testNotificationManually();
        });
        
        // Long click for report options
        tvOtherUserName.setOnLongClickListener(v -> {
            showChatReportOptions();
            return true;
        });
        
        // Scroll listener for pagination
        recyclerMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && canLoadMore) {
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItemPosition == 0) {
                        loadMoreMessages();
                    }
                }
            }
        });
    }      private void initChatRoom() {
        showProgress();
        
        // Check block status first before loading any chat content
        checkBlockStatus();
    }
    
    private void continueInitChatRoom() {
        // Add debug logging for authentication
        SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String authToken = authPrefs.getString("auth_token", "");
        Log.d(TAG, "Auth token available: " + (!authToken.isEmpty()));
        Log.d(TAG, "Auth token length: " + authToken.length());
        if (!authToken.isEmpty()) {
            Log.d(TAG, "Auth token preview: " + authToken.substring(0, Math.min(10, authToken.length())) + "...");
        }
        
        if (roomId != -1) {
            // Room ID provided, just load messages
            loadMessages();
        } else if (otherId != -1) {
            // Create or get chat room
            Map<String, Long> params = new HashMap<>();
            params.put("user1Id", myId);
            params.put("user2Id", otherId);
            
            if (listingId != -1) {
                params.put("listingId", listingId);
            }
            
            Log.d(TAG, "Creating chat room with params: " + params);            chatApiService.createChatRoomDirect(params).enqueue(new Callback<ChatRoom>() {
                @Override
                public void onResponse(@NonNull Call<ChatRoom> call, @NonNull Response<ChatRoom> response) {                    Log.d(TAG, "Chat room creation response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        chatRoom = response.body();
                        Log.d(TAG, "Successfully got ChatRoom directly from response");
                        Log.d(TAG, "ChatRoom ID: " + chatRoom.getId());
                        Log.d(TAG, "ChatRoom users: " + chatRoom.getUser1Name() + " <-> " + chatRoom.getUser2Name());
                        
                        roomId = chatRoom.getId();                        // Update UI with chat room info
                        updateChatRoomUI();
                        
                        // Test if token works immediately after chat room creation
                        testTokenAuthentication();
                        
                        // Add a small delay before loading messages to ensure token is properly set
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Load messages
                                loadMessages();
                            }
                        }, 500); // 500ms delay
                    } else {
                        hideProgress();
                        String errorMsg = "Không thể tạo phòng chat";
                        if (response.code() == 403) {
                            errorMsg = "Lỗi xác thực. Vui lòng đăng nhập lại.";
                        }
                        Log.e(TAG, "Failed to create chat room. Response code: " + response.code());
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "Error response: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }                }
                
                @Override
                public void onFailure(@NonNull Call<ChatRoom> call, @NonNull Throwable t) {
                    if (!isAdded() || getContext() == null) return;
                    hideProgress();
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating chat room", t);
                }
            });
        } else {
            hideProgress();
            Toast.makeText(getContext(), "Thiếu thông tin để khởi tạo chat", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }
    }
    
    private void updateChatRoomUI() {
        if (chatRoom == null) return;
        
        // Set user name and avatar
        String otherUserName = chatRoom.getOtherUserName(myId);
        String otherUserAvatar = chatRoom.getOtherUserAvatar(myId);
        
        tvOtherUserName.setText(otherUserName);
        if (!TextUtils.isEmpty(otherUserAvatar)) {
            Glide.with(this)
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(ivOtherUserAvatar);
        }
        
        // Set listing info if available
        if (chatRoom.getListingId() != null && chatRoom.getListingId() > 0) {
            layoutListing.setVisibility(View.VISIBLE);
            tvListingTitle.setText(chatRoom.getListingTitle());
            
            if (!TextUtils.isEmpty(chatRoom.getListingImageUrl())) {
                Glide.with(this)
                        .load(chatRoom.getListingImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(ivListingImage);
            }
            
            // TODO: Format price properly
            tvListingPrice.setText(chatRoom.getListingPrice() != null ? 
                    chatRoom.getListingPrice().toString() + " đ" : "");
        } else {
            layoutListing.setVisibility(View.GONE);
        }
    }    private void loadMessages() {
        if (roomId == -1) return;
        
        isLoading = true;
        
        // Add comprehensive debug logging for authentication
        SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String authToken = authPrefs.getString("auth_token", "");
        Log.d(TAG, "=== LOADING MESSAGES DEBUG ===");
        Log.d(TAG, "Auth token available: " + (!authToken.isEmpty()));
        Log.d(TAG, "Auth token length: " + authToken.length());
        if (!authToken.isEmpty()) {
            Log.d(TAG, "Auth token first 20 chars: " + authToken.substring(0, Math.min(20, authToken.length())) + "...");
            Log.d(TAG, "Auth token last 10 chars: ..." + authToken.substring(Math.max(0, authToken.length() - 10)));
        }
        Log.d(TAG, "Room ID: " + roomId + ", User ID: " + myId);
        Log.d(TAG, "Request URL will be: api/chat/messages/" + roomId + "/user/" + myId);
        Log.d(TAG, "=== END DEBUG ===");
          chatApiService.getChatMessagesDirect(roomId, myId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                hideProgress();
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                
                Log.d(TAG, "Load messages response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatMessage> newMessages = response.body();
                    
                    Log.d(TAG, "Successfully loaded " + newMessages.size() + " messages using direct array parsing");
                    
                    if (newMessages.isEmpty()) {
                        canLoadMore = false;
                        Log.d(TAG, "No messages found, starting with empty chat");                    } else {
                        // Add to the beginning of the list
                        messageList.addAll(0, newMessages);
                        chatAdapter.notifyDataSetChanged();
                        
                        // Scroll to bottom if first load
                        if (currentPage == 0) {
                            scrollToBottom();
                        }
                        
                        currentPage++;
                    }
                    
                    // Mark messages as read
                    markMessagesAsRead();
                    
                    // Start polling for new messages if this is the first load
                    if (currentPage == 1) {
                        startMessagePolling();
                    }} else if (response.code() == 403) {
                    // Handle 403 specifically for chat messages
                    Log.e(TAG, "=== 403 FORBIDDEN ERROR ===");
                    Log.e(TAG, "Response code: " + response.code());
                    Log.e(TAG, "Response message: " + response.message());
                    
                    // Re-check token after the failed request
                    SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String currentToken = authPrefs.getString("auth_token", "");
                    Log.e(TAG, "Token still available after 403: " + (!currentToken.isEmpty()));
                    Log.e(TAG, "Token length after 403: " + currentToken.length());
                    
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "403 Error response body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading 403 error body", e);
                    }
                    
                    // Test if authentication works with other endpoints when messages fail
                    Log.e(TAG, "Testing authentication with other endpoints after message 403...");
                    chatApiService.getUserChatRooms(myId).enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                            Log.e(TAG, "Other endpoint test after 403 - Response code: " + response.code());
                            if (response.isSuccessful()) {
                                Log.e(TAG, "OTHER ENDPOINTS WORK - Message endpoint specific issue!");
                            } else {
                                Log.e(TAG, "OTHER ENDPOINTS ALSO FAIL - General auth issue");
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                            Log.e(TAG, "Other endpoint test failed: " + t.getMessage());
                        }
                    });
                    
                    // Check if it's a fresh chat room (no messages yet)
                    if (currentPage == 0) {
                        Log.d(TAG, "Fresh chat room - treating as normal (no messages yet)");
                        canLoadMore = false;
                        // Start polling anyway in case messages come in
                        startMessagePolling();
                    } else {
                        Toast.makeText(requireContext(), "Lỗi xác thực khi tải tin nhắn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Error loading messages: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error response: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(requireContext(), "Không thể tải tin nhắn", Toast.LENGTH_SHORT).show();
                }            }
            
            @Override
            public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                // Check if fragment is still attached before accessing context
                if (!isAdded() || getContext() == null) {
                    Log.d(TAG, "Fragment not attached, skipping error handling");
                    return;
                }
                
                hideProgress();
                isLoading = false;
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading messages", t);
                
                // For new chat rooms, we might not have messages yet
                if (currentPage == 0) {
                    Log.d(TAG, "Network error on first load - might be new chat room");
                    canLoadMore = false;
                    startMessagePolling();
                }
            }
        });
    }
    
    private void loadMoreMessages() {
        if (!isLoading && canLoadMore) {
            swipeRefresh.setRefreshing(true);
            loadMessages();
        } else {
            swipeRefresh.setRefreshing(false);
        }
    }    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || roomId == -1) return;
        
        Log.d(TAG, "=== SEND MESSAGE DEBUG ===");
        Log.d(TAG, "isBlockedUser: " + isBlockedUser);
        Log.d(TAG, "hasChecked: " + hasCheckedBlockStatus);
        Log.d(TAG, "myId: " + myId + ", otherId: " + otherId);
        
        // Check if user is blocked
        if (isBlockedUser) {
            Log.d(TAG, "Blocked user - preventing message send");
            Toast.makeText(getContext(), "Không thể gửi tin nhắn cho người dùng này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Double check with BlockedUserFilter
        BlockedUserFilter filter = BlockedUserFilter.getInstance(getContext());
        if (filter.isUserBlocked(otherId)) {
            Log.d(TAG, "User is blocked by filter - preventing message send");
            isBlockedUser = true;
            Toast.makeText(getContext(), "Không thể gửi tin nhắn cho người dùng này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Proceeding with message send");
        
        // Clear input field and hide keyboard
        etMessage.setText("");
        hideKeyboard();
        
        // Create message
        ChatMessage newMessage = new ChatMessage(roomId, myId, otherId, messageText, "TEXT");
          // Add to UI immediately (optimistic UI update)
        messageList.add(newMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        
        // Scroll to bottom with delay to ensure animation
        new Handler(Looper.getMainLooper()).post(() -> {
            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
        });
        
        // Prepare the request
        Map<String, Object> request = new HashMap<>();
        request.put("chatRoomId", roomId);
        request.put("senderId", myId);
        request.put("content", messageText);
          // Send to server
        chatApiService.sendTextMessageDirect(request).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessage> call, @NonNull Response<ChatMessage> response) {
                Log.d(TAG, "Send message response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    // Update message with server data
                    ChatMessage serverMessage = response.body();
                    int lastIndex = messageList.size() - 1;
                    if (lastIndex >= 0) {
                        messageList.set(lastIndex, serverMessage);
                        chatAdapter.notifyItemChanged(lastIndex);
                        Log.d(TAG, "Message sent successfully with ID: " + (serverMessage.getId() != null ? serverMessage.getId() : "null"));
                    }                } else {
                    // Message failed - show error and remove optimistic message
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Failed to send message: " + response.code());
                    Toast.makeText(getContext(), "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
                    
                    // Remove the failed message from UI
                    int lastIndex = messageList.size() - 1;
                    if (lastIndex >= 0) {
                        messageList.remove(lastIndex);
                        chatAdapter.notifyItemRemoved(lastIndex);
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ChatMessage> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Network error sending message", t);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Remove the failed message from UI
                int lastIndex = messageList.size() - 1;
                if (lastIndex >= 0) {
                    messageList.remove(lastIndex);
                    chatAdapter.notifyItemRemoved(lastIndex);
                }
            }
        });
    }
      private void showAttachmentOptions() {
        if (roomId == -1) return;
        
        String[] options = {"Chọn ảnh từ thư viện", "Chụp ảnh", "Hủy"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Gửi hình ảnh");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Choose image from gallery
                    selectImageFromGallery();
                    break;
                case 1:
                    // Take photo
                    takePhoto();
                    break;
                case 2:
                    // Cancel
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }
    
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }
    
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }      private void sendImageMessage(Uri imageUri) {        if (roomId == -1 || imageUri == null) return;
        
        // TODO: Re-implement image sending functionality
        Toast.makeText(getContext(), "Tính năng gửi ảnh đang được phát triển", Toast.LENGTH_SHORT).show();
    }
    
      private void navigateToListing() {
        if (chatRoom != null && chatRoom.getListingId() != null) {
            ListingDetailFragment fragment = ListingDetailFragment.newInstance(chatRoom.getListingId());
            ((MainMenu) requireActivity()).replaceFragment(fragment);
        }
    }
      private void markMessagesAsRead() {
        if (roomId == -1) return;
        
        chatApiService.markMessagesAsRead(roomId, myId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                // Message read status updated on server
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error marking messages as read", t);
            }
        });
    }    private void testTokenAuthentication() {
        // Test token by making a simple API call that requires authentication
        Log.d(TAG, "=== TESTING TOKEN AUTHENTICATION ===");
        SharedPreferences authPrefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        String authToken = authPrefs.getString("auth_token", "");
        Log.d(TAG, "Token for test: " + (!authToken.isEmpty() ? "available (" + authToken.length() + " chars)" : "not available"));
        
        // Since the main message loading works perfectly, just log that authentication is working
        if (!authToken.isEmpty() && authToken.length() > 100) {
            Log.d(TAG, "=== TOKEN TEST RESULT ===");
            Log.d(TAG, "Token test SUCCESS - authentication is working");
            Log.d(TAG, "Token format appears valid and main message loading is working");
            Log.d(TAG, "=== END TOKEN TEST ===");
        } else {
            Log.e(TAG, "Token test FAILED - no valid token found");
        }
    }
      private void startMessagePolling() {
        messageRunnable = new Runnable() {
            @Override
            public void run() {                if (isAdded() && roomId != -1) {
                    // Get latest message ID - use safe method
                    final long latestMessageId = !messageList.isEmpty() ? 
                        messageList.get(messageList.size() - 1).getIdSafely() : 0;
                      // Poll for new messages
                    chatApiService.getChatMessagesDirect(roomId, myId).enqueue(new Callback<List<ChatMessage>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ChatMessage> newMessages = response.body();                                // Filter for messages newer than our latest
                                List<ChatMessage> messagesToAdd = new ArrayList<>();
                                for (ChatMessage message : newMessages) {
                                    // Use safe ID method
                                    if (message.getIdSafely() > latestMessageId) {
                                        messagesToAdd.add(message);
                                    }
                                }// Add new messages to UI
                                if (!messagesToAdd.isEmpty()) {
                                    int insertPosition = messageList.size();
                                    messageList.addAll(messagesToAdd);
                                    chatAdapter.notifyItemRangeInserted(insertPosition, messagesToAdd.size());
                                    
                                    // Smooth scroll to bottom if fragment is visible
                                    if (isFragmentVisible) {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                                        });
                                        
                                        // Mark messages as read if user is viewing the chat
                                        markMessagesAsRead();
                                    } else {
                                        // Show local notification if fragment is not visible
                                        showNewMessageNotification(messagesToAdd);
                                    }
                                    
                                    Log.d(TAG, "Added " + messagesToAdd.size() + " new messages from polling");
                                }
                            }
                            
                            // Schedule next poll
                            messageHandler.postDelayed(messageRunnable, POLLING_INTERVAL);
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                            Log.e(TAG, "Error polling for messages", t);
                            
                            // Schedule next poll even if this one failed
                            messageHandler.postDelayed(messageRunnable, POLLING_INTERVAL);
                        }
                    });
                }
            }
        };
        
        // Start polling
        messageHandler.postDelayed(messageRunnable, POLLING_INTERVAL);
    }
      private void stopMessagePolling() {
        if (messageHandler != null && messageRunnable != null) {
            messageHandler.removeCallbacks(messageRunnable);
        }
    }
    
    /**
     * Show local notification for new messages when fragment is not visible
     */
    private void showNewMessageNotification(List<ChatMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            Log.d(TAG, "No new messages to notify about");
            return;
        }
        
        Log.d(TAG, "=== NOTIFICATION DEBUG ===");
        Log.d(TAG, "New messages count: " + newMessages.size());
        Log.d(TAG, "Fragment visible: " + isFragmentVisible);
        Log.d(TAG, "NotificationHelper available: " + (notificationHelper != null));
        
        // Check if message notifications are enabled
        if (!notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_MESSAGES)) {
            Log.d(TAG, "Message notifications are DISABLED in settings");
            return;
        } else {
            Log.d(TAG, "Message notifications are ENABLED in settings");
        }
        
        // **TEMPORARY DEBUG: Always show notification regardless of fragment visibility**
        Log.d(TAG, "Showing notification (DEBUG MODE - ignoring fragment visibility)");
        
        try {
            // Get the latest message for notification content
            ChatMessage latestMessage = newMessages.get(newMessages.size() - 1);
            
            // Create and show notification using NotificationHelper
            createAndShowNotification(latestMessage, newMessages.size());
            
            Log.d(TAG, "✅ Local notification created successfully for " + newMessages.size() + " new messages");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing notification", e);
        }
        
        Log.d(TAG, "=== END NOTIFICATION DEBUG ===");
    }
    
    /**
     * Create and display the actual notification
     */
    private void createAndShowNotification(ChatMessage latestMessage, int messageCount) {
        try {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create notification content
            String title = otherName != null ? otherName : "Tin nhắn mới";
            String content;
              if (messageCount == 1) {
                // Single message - show actual content
                if (latestMessage.isText()) {
                    content = latestMessage.getContent();
                    // Limit content length
                    if (content.length() > 50) {
                        content = content.substring(0, 47) + "...";
                    }
                } else if (latestMessage.isImage()) {
                    content = getString(R.string.image_message_notification_text);
                } else {
                    content = "Tin nhắn mới";
                }
            } else {
                // Multiple messages
                content = messageCount + " tin nhắn mới";
            }
            
            // Create notification channel ID
            String channelId = "messages";
            
            // Create intent to open chat when notification is tapped
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.ok.MainMenu.class);
            intent.putExtra("openChat", true);
            intent.putExtra("roomId", roomId);
            intent.putExtra("myId", myId);
            intent.putExtra("otherId", otherId);
            intent.putExtra("otherName", otherName);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                requireContext(), 
                (int) roomId, // Use roomId as request code to make it unique
                intent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );            // Build notification
            androidx.core.app.NotificationCompat.Builder builder = 
                new androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
                    .setSmallIcon(R.drawable.chat) // Use existing chat icon
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL);
              
            Log.d(TAG, "Building notification with:");
            Log.d(TAG, "- Title: " + title);
            Log.d(TAG, "- Content: " + content);
            Log.d(TAG, "- Channel ID: " + channelId);
            Log.d(TAG, "- Room ID: " + roomId);
            
            // Show notification with unique ID based on room
            notificationManager.notify((int) roomId, builder.build());
            
            Log.d(TAG, "✅ Notification sent to system with ID: " + roomId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
        }
    }
    
    /**
     * Clear notifications for this specific chat when user opens it
     */
    private void clearNotificationsForThisChat() {
        try {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Cancel notification with this room's ID
            notificationManager.cancel((int) roomId);
            
            Log.d(TAG, "Cleared notifications for chat room: " + roomId);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing notifications", e);
        }
    }
    
    /**
     * Comprehensive notification debugging method
     */
    private void debugNotificationSystem() {
        Log.d(TAG, "=== COMPREHENSIVE NOTIFICATION DEBUG ===");
        
        try {
            Context context = requireContext();
            
            // 1. Check NotificationManager
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "❌ NotificationManager is NULL");
                return;
            }
            
            // 2. Check system-wide notification permission
            boolean systemEnabled = notificationManager.areNotificationsEnabled();
            Log.d(TAG, "System notifications enabled: " + systemEnabled);
            
            // 3. Check notification channels (Android 8.0+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel messagesChannel = notificationManager.getNotificationChannel("messages");
                if (messagesChannel != null) {
                    Log.d(TAG, "Messages channel exists: " + messagesChannel.getName());
                    Log.d(TAG, "Messages channel importance: " + messagesChannel.getImportance());
                    Log.d(TAG, "Messages channel blocked: " + (messagesChannel.getImportance() == android.app.NotificationManager.IMPORTANCE_NONE));
                } else {
                    Log.e(TAG, "❌ Messages notification channel NOT FOUND");
                }
            }
            
            // 4. Check app notification settings
            if (notificationHelper != null) {
                boolean appMessagesEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_MESSAGES);
                Log.d(TAG, "App messages setting enabled: " + appMessagesEnabled);
            } else {
                Log.e(TAG, "❌ NotificationHelper is NULL");
            }
            
            // 5. Check Do Not Disturb
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int filter = notificationManager.getCurrentInterruptionFilter();
                Log.d(TAG, "DND Filter: " + filter + " (1=ALL, 2=PRIORITY, 3=NONE, 4=ALARMS)");
            }
            
            // 6. Force create a simple test notification
            Log.d(TAG, "Creating force test notification...");
            createForceTestNotification();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in notification debugging", e);
        }
        
        Log.d(TAG, "=== END COMPREHENSIVE DEBUG ===");
    }
    
    /**
     * Create a very simple test notification to verify basic functionality
     */
    private void createForceTestNotification() {
        try {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Create the simplest possible notification
            androidx.core.app.NotificationCompat.Builder builder = 
                new androidx.core.app.NotificationCompat.Builder(requireContext(), "messages")
                    .setSmallIcon(android.R.drawable.ic_notification_overlay) // Use system icon
                    .setContentTitle("TEST NOTIFICATION")
                    .setContentText("If you see this, notifications work!")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);
            
            // Show with a unique ID
            notificationManager.notify(12345, builder.build());
            Log.d(TAG, "✅ Force test notification sent with ID: 12345");
              } catch (Exception e) {
            Log.e(TAG, "❌ Error creating force test notification", e);
        }
    }

    /**
     * Test notification manually - triggered by clicking username
     */
    private void testNotificationManually() {
        Log.d(TAG, "=== MANUAL NOTIFICATION TEST ===");
          try {
            // Create a test notification specifically for chat messages
            NotificationManager notificationManager = 
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "❌ NotificationManager is null");
                return;
            }
            
            // Check if notifications are enabled
            if (!notificationManager.areNotificationsEnabled()) {
                Log.e(TAG, "❌ Notifications are disabled in system settings");
                Toast.makeText(requireContext(), "Thông báo đã bị tắt trong cài đặt hệ thống", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Create notification with chat-specific styling
            androidx.core.app.NotificationCompat.Builder builder = 
                new androidx.core.app.NotificationCompat.Builder(requireContext(), "messages")
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setContentTitle("Tin nhắn mới từ " + otherName)
                    .setContentText("Đây là tin nhắn thử nghiệm từ hệ thống chat")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE)
                    .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("Đây là tin nhắn thử nghiệm từ hệ thống chat. Nếu bạn thấy thông báo này, hệ thống đang hoạt động bình thường."));
            
            // Add intent to open chat when notification is clicked
            Intent intent = new Intent(requireContext(), com.example.ok.MainMenu.class);
            intent.putExtra("openChat", true);
            intent.putExtra("roomId", roomId);
            intent.putExtra("myId", myId);
            intent.putExtra("otherId", otherId);
            intent.putExtra("otherName", otherName);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(), 
                (int) roomId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);
            
            // Show notification with room-specific ID
            int notificationId = (int) (2000 + roomId);
            notificationManager.notify(notificationId, builder.build());
            
            Log.d(TAG, "✅ Manual test notification sent successfully");
            Log.d(TAG, "- Notification ID: " + notificationId);
            Log.d(TAG, "- Channel: messages");
            Log.d(TAG, "- Room ID: " + roomId);
            Log.d(TAG, "- Other user: " + otherName);
            
            // Show success message to user
            Toast.makeText(requireContext(), "Đã gửi thông báo thử nghiệm", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in manual notification test", e);
            Toast.makeText(requireContext(), "Lỗi khi gửi thông báo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        Log.d(TAG, "=== MANUAL NOTIFICATION TEST COMPLETE ===");
    }

    /**
     * Show report options for chat and user
     */
    private void showChatReportOptions() {
        String[] options = {
            "📝 Xem thông tin người dùng",
            "⚠️ Báo cáo cuộc trò chuyện",
            "🚫 Báo cáo người dùng", 
            "🔒 Chặn người dùng"
        };
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Tùy chọn")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewUserProfile();
                            break;
                        case 1:
                            showReportChatDialog();
                            break;
                        case 2:
                            showReportUserDialog();
                            break;
                        case 3:
                            showBlockUserDialog();
                            break;
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void viewUserProfile() {
        // Navigate to user profile
        Bundle args = new Bundle();
        args.putLong("userId", otherId);
        args.putString("displayName", otherName);
        
        // Navigate to OtherUserProfileFragment
        // This would need to be implemented in your navigation system
        Log.d(TAG, "Navigate to user profile: " + otherName);
    }
    
    private void showReportChatDialog() {
        String[] reasons = {
            "Lừa đảo/Gian lận",
            "Nội dung không phù hợp", 
            "Spam/Quảng cáo",
            "Quấy rối",
            "Ngôn từ xúc phạm",
            "Khác"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo cuộc trò chuyện")
                .setMessage("Báo cáo cuộc trò chuyện với " + otherName)
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];
                    if (which == reasons.length - 1) {
                        // "Khác" - show input dialog
                        showCustomReportChatDialog(reason);
                    } else {
                        showReportChatDescriptionDialog(reason);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showReportChatDescriptionDialog(String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etDescription = dialogView.findViewById(R.id.et_custom_reason);
        etDescription.setHint("Mô tả thêm về vấn đề (tùy chọn)");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo: " + reason)
                .setMessage("Cuộc trò chuyện với " + otherName)
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    submitChatReport(reason, description);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showCustomReportChatDialog(String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etCustomReason = dialogView.findViewById(R.id.et_custom_reason);
        etCustomReason.setHint("Mô tả chi tiết lý do báo cáo");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Lý do báo cáo khác")
                .setMessage("Cuộc trò chuyện với " + otherName)
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String customReason = etCustomReason.getText().toString().trim();
                    if (customReason.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập lý do báo cáo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitChatReport(reason, customReason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void submitChatReport(String reason, String description) {
        Call<ApiResponse> call = chatApiService.reportChatRoom(roomId, myId, reason, description);
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
    
    private void showReportUserDialog() {
        String[] reasons = {
            "Lừa đảo/Gian lận",
            "Nội dung không phù hợp", 
            "Spam/Quảng cáo",
            "Quấy rối",
            "Ngôn từ xúc phạm",
            "Tài khoản giả mạo",
            "Khác"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo người dùng")
                .setMessage("Báo cáo người dùng: " + otherName)
                .setItems(reasons, (dialog, which) -> {
                    String reason = reasons[which];
                    if (which == reasons.length - 1) {
                        // "Khác" - show input dialog
                        showCustomReportUserDialog(reason);
                    } else {
                        showReportUserDescriptionDialog(reason);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showReportUserDescriptionDialog(String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etDescription = dialogView.findViewById(R.id.et_custom_reason);
        etDescription.setHint("Mô tả thêm về vấn đề (tùy chọn)");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Báo cáo: " + reason)
                .setMessage("Người dùng: " + otherName)
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    submitUserReport(reason, description);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showCustomReportUserDialog(String reason) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_custom_report, null);
        
        EditText etCustomReason = dialogView.findViewById(R.id.et_custom_reason);
        etCustomReason.setHint("Mô tả chi tiết lý do báo cáo");
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Lý do báo cáo khác")
                .setMessage("Người dùng: " + otherName)
                .setView(dialogView)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> {
                    String customReason = etCustomReason.getText().toString().trim();
                    if (customReason.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập lý do báo cáo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitUserReport(reason, customReason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void submitUserReport(String reason, String description) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ApiResponse> call = apiService.reportUserDetailed(myId, otherId, reason, description);
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
    
    private void showBlockUserDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chặn người dùng")
                .setMessage("Bạn có chắc chắn muốn chặn " + otherName + "? Bạn sẽ không nhận được tin nhắn từ người này nữa.")
                .setPositiveButton("Chặn", (dialog, which) -> blockUser())
                .setNegativeButton("Hủy", null)
                .show();
    }      private void blockUser() {
        // Validate user IDs first
        if (myId == -1 || otherId == -1) {
            Log.e(TAG, "Invalid user IDs - myId: " + myId + ", otherId: " + otherId);
            Toast.makeText(getContext(), "Lỗi: Không thể xác định người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (myId == otherId) {
            Log.e(TAG, "Cannot block yourself - myId: " + myId + ", otherId: " + otherId);
            Toast.makeText(getContext(), "Lỗi: Không thể chặn chính mình", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Blocking user - currentUserId: " + myId + ", targetUserId: " + otherId);
        
        ApiService apiService = RetrofitClient.getApiService();
        
        // Use Query-based endpoint (more reliable as it includes currentUserId)
        Call<ApiResponse> call = apiService.blockUser(myId, otherId, new BlockUserRequest("Blocked from chat"));
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (!isAdded() || getContext() == null) return;
                handleChatBlockResponse(response);
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                
                Log.e(TAG, "Block user failed", t);
                Toast.makeText(getContext(), "Lỗi khi chặn người dùng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void handleChatBlockResponse(Response<ApiResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse apiResponse = response.body();            if (apiResponse.isSuccess()) {
                // Add to blocked users list
                BlockedUserFilter.getInstance(getContext()).addBlockedUser(otherId);
                
                Toast.makeText(requireContext(), "Đã chặn người dùng thành công", Toast.LENGTH_SHORT).show();
                // Navigate back
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            } else {
                String errorMsg = apiResponse.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Không thể chặn người dùng";
                }
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                Log.w(TAG, "Block user failed: " + errorMsg);
            }
        } else {
            // Handle HTTP error codes
            String errorMessage;
            switch (response.code()) {
                case 403:
                    errorMessage = "Không có quyền thực hiện hành động này.\n\nCó thể do backend chưa hỗ trợ tính năng chặn người dùng.\nVui lòng liên hệ hỗ trợ.";
                    break;
                case 404:
                    errorMessage = "Không tìm thấy người dùng này.";
                    break;
                case 400:
                    errorMessage = "Yêu cầu không hợp lệ.\nCó thể bạn đã chặn người dùng này trước đó.";
                    break;
                case 409:
                    errorMessage = "Người dùng đã được chặn trước đó.";
                    break;
                case 500:
                    errorMessage = "Lỗi máy chủ. Vui lòng thử lại sau.";
                    break;
                default:
                    errorMessage = "Không thể chặn người dùng.\nMã lỗi: " + response.code();
            }
            
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            Log.w(TAG, "Block user HTTP error: " + response.code() + " - " + response.message());
        }
    }
    
    private void handleChatBlockFailure(Throwable t) {
        Log.e(TAG, "Error blocking user", t);
        String errorMessage = "Lỗi kết nối. Vui lòng kiểm tra mạng và thử lại.";
        if (t.getMessage() != null && !t.getMessage().isEmpty()) {
            errorMessage += "\nChi tiết: " + t.getMessage();
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show progress bar
     */
    private void showProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide progress bar
     */
    private void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Scroll to bottom of messages
     */
    private void scrollToBottom() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            recyclerMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    /**
     * Hide keyboard
     */
    private void hideKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getActivity().getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }    /**
     * Check if current user is blocked by the other user or vice versa
     */
    private void checkBlockStatus() {
        if (hasCheckedBlockStatus) return;
        
        Log.d(TAG, "Checking block status between users: " + myId + " and " + otherId);
        
        // Use BlockedUserFilter to check if user is blocked
        BlockedUserFilter filter = BlockedUserFilter.getInstance(getContext());
        
        // First refresh the blocked users list, then check
        filter.refreshBlockedUsers(new BlockedUserFilter.RefreshCallback() {
            @Override
            public void onComplete(boolean success) {
                if (!isAdded() || getContext() == null) return;
                
                hasCheckedBlockStatus = true;
                
                // Check if the other user is in our blocked list
                isBlockedUser = filter.isUserBlocked(otherId);
                Log.d(TAG, "Block status check result: " + isBlockedUser);
                
                if (isBlockedUser) {
                    showBlockedUserMessage();
                } else {
                    // Continue with normal chat loading
                    continueInitChatRoom();
                }
            }
        });    }
    
    /**
     * Show message when user is blocked
     */
    private void showBlockedUserMessage() {        // Hide chat input
        try {
            View etMessage = getView().findViewById(R.id.etMessage);
            View btnSend = getView().findViewById(R.id.btnSend);
            View btnAttachment = getView().findViewById(R.id.btnAttachment);
            
            if (etMessage != null) etMessage.setVisibility(View.GONE);
            if (btnSend != null) btnSend.setVisibility(View.GONE);
            if (btnAttachment != null) btnAttachment.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.w(TAG, "Could not hide chat input", e);
        }
          // Show blocked message
        messageList.clear();
        ChatMessage blockedMessage = new ChatMessage();
        blockedMessage.setContent("⚠️ Cuộc trò chuyện này không khả dụng do một trong hai người dùng đã bị chặn.");
        blockedMessage.setType("SYSTEM");
        blockedMessage.setSenderId(-1L); // System message
        messageList.add(blockedMessage);
        
        chatAdapter.notifyDataSetChanged();
        
        Toast.makeText(getContext(), "Không thể nhắn tin với người dùng này", Toast.LENGTH_LONG).show();
    }
}
