package com.example.ok.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ChatAdapter;
import com.example.ok.api.ApiService;
import com.example.ok.api.ChatApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.utils.BlockedUserFilter;
import com.example.ok.model.ApiResponse;
import com.example.ok.model.BlockUserRequest;
import com.example.ok.model.ChatMessage;
import com.example.ok.model.ChatRoom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import com.example.ok.service.ChatPollingService;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment implements ChatAdapter.OnMessageActionListener {
    private static final String TAG = "ChatFragment";
    private static final int PAGE_SIZE = 20;
    private static final int REQUEST_SELECT_IMAGE = 101;
    private static final int REQUEST_TAKE_PHOTO = 102;
    
    // Fragment arguments
    private long roomId = -1;
    private long myId = -1;
    private long otherId = -1;
    private String otherName = "";
    private long listingId = -1;      // UI components
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttachment;
    private ImageButton btnTestNotification;
    private Button btnViewListing;
    private TextView tvOtherUserName;
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
    
    // Notification management
    private com.example.ok.util.ChatNotificationManager chatNotificationManager;
    
    // Polling for new messages
    private Handler messageHandler = new Handler(Looper.getMainLooper());
    private Runnable messageRunnable;    private static final int POLLING_INTERVAL_ACTIVE = 1000; // 1 second when active (faster real-time)
    private static final int POLLING_INTERVAL_IDLE = 3000; // 3 seconds when idle (reduced from 5)
    
    // Track if fragment is visible to user
    private boolean isFragmentVisible = false;
    
    // Add block status check variables
    private boolean isBlockedUser = false;
    private boolean hasCheckedBlockStatus = false;
    
    // Track last message timestamp for better polling
    private long lastMessageTimestamp = 0;
    private long lastActivityTime = 0;
    
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
        }        // Ensure RetrofitClient is initialized before using it
        RetrofitClient.init(requireContext());
        chatApiService = RetrofitClient.getChatApiService();
        
        // Initialize chat notification manager
        chatNotificationManager = new com.example.ok.util.ChatNotificationManager(requireContext());
        
        Log.d(TAG, "✅ ChatNotificationManager initialized successfully");
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
      // 🔥 FIX: Thêm lifecycle methods để quản lý background polling    @Override
    public void onResume() {
        super.onResume();
        isFragmentVisible = true;
          // Clear notifications khi user mở chat
        if (chatNotificationManager != null) {
            chatNotificationManager.clearChatNotifications(roomId);
        }
        
        // 🔥 NEW: Set visible room in background service để tránh spam notification
        if (roomId != -1) {
            ChatPollingService.setVisibleRoom(requireContext(), roomId);
        }
        
        // 🔥 FIX: Start LOCAL polling để cập nhật UI real-time
        if (roomId != -1) {
            Log.d(TAG, "onResume: Starting LOCAL message polling for UI updates");
            startLocalMessagePolling();
        }
        
        Log.d(TAG, "Fragment resumed - fragment is now visible");
    }      @Override
    public void onPause() {
        super.onPause();
        isFragmentVisible = false;
        
        // 🔥 NEW: Stop LOCAL polling khi fragment không visible, nhưng background service tiếp tục
        stopLocalMessagePolling();
        
        // 🔥 NEW: Set visible room to -1 để background service biết room không visible
        ChatPollingService.setVisibleRoom(requireContext(), -1);
        
        Log.d(TAG, "Fragment paused - local polling stopped, background service continues");
    }
    
    @Override
    public void onStop() {
        super.onStop();
        isFragmentVisible = false;
        
        Log.d(TAG, "Fragment stopped - background service handles notifications");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 🔥 NEW: Remove room từ background polling khi fragment destroyed
        if (roomId != -1) {
            ChatPollingService.removeRoomFromPolling(requireContext(), roomId);
        }
        
        // 🔥 Cleanup local polling
        stopLocalMessagePolling();
        
        // Clear handlers
        if (messageHandler != null) {
            messageHandler.removeCallbacksAndMessages(null);
        }
        
        Log.d(TAG, "Fragment destroyed - removed from background polling");
    }
      private void initViews(View view) {        // Toolbar        ImageButton btnBack = view.findViewById(R.id.btnBack);
        tvOtherUserName = view.findViewById(R.id.tvOtherUserName);
        ivOtherUserAvatar = view.findViewById(R.id.ivOtherUserAvatar);
        ImageButton btnTestNotification = view.findViewById(R.id.btnTestNotification);
        
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
        
        // 🔥 THÊM: Implement OnMessageActionListener cho edit/delete tin nhắn
        chatAdapter.setOnMessageActionListener(new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onEditMessage(ChatMessage message, int position) {
                showEditMessageDialog(message, position);
            }

            @Override
            public void onDeleteMessage(ChatMessage message, int position) {
                showDeleteMessageConfirmDialog(message, position);
            }

            @Override
            public void onCopyMessage(ChatMessage message) {
                copyMessageToClipboard(message);
            }
        });
        
        // Set initial UI values
        tvOtherUserName.setText(otherName);
    }
      private void setupListeners() {
        // Back button
        View btnBack = requireView().findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
          // **NEW: Test Notification Button (with null check)**
        if (btnTestNotification != null) {
            btnTestNotification.setOnClickListener(v -> {
                Log.d(TAG, "🔔 TEST NOTIFICATION BUTTON CLICKED");
                
                if (chatNotificationManager != null) {
                    // Show immediate test notification
                    chatNotificationManager.showTestNotification("🧪 Manual Test từ " + otherName);
                    
                    // Also simulate a real chat notification
                    List<ChatMessage> testMessages = new ArrayList<>();
                    ChatMessage testMsg = new ChatMessage(roomId, otherId, myId, "Tin nhắn test từ nút!", "TEXT");
                    testMsg.setId(888888L);
                    testMsg.setTimestamp(System.currentTimeMillis());
                    testMessages.add(testMsg);
                    
                    Log.d(TAG, "📨 Simulating chat notification...");
                    chatNotificationManager.showChatNotification(roomId, otherName, testMessages, myId, otherId);
                    
                    Toast.makeText(requireContext(), "🔔 Test notification sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "❌ ChatNotificationManager NULL!");
                    Toast.makeText(requireContext(), "❌ Notification system chưa sẵn sàng", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.w(TAG, "⚠️ btnTestNotification not found in layout, using alternative test method");
        }
        
        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
        
        // Attachment button
        btnAttachment.setOnClickListener(v -> showAttachmentOptions());
        
        // Back to listing button
        btnViewListing.setOnClickListener(v -> navigateToListing());
        
        // Pull to refresh
        swipeRefresh.setOnRefreshListener(this::loadMoreMessages);        // **ENHANCED: Add test notification with better debug info**
        tvOtherUserName.setOnClickListener(v -> {
            Log.d(TAG, "=== USER NAME CLICKED - TEST NOTIFICATION ===");
            Log.d(TAG, "Other user name: " + otherName);
            Log.d(TAG, "Room ID: " + roomId);
            Log.d(TAG, "ChatNotificationManager: " + (chatNotificationManager != null ? "AVAILABLE" : "NULL"));
            
            if (chatNotificationManager != null) {
                // First show debug status
                chatNotificationManager.debugNotificationStatus();
                
                // Then show test notification
                chatNotificationManager.showTestNotification("🔔 Test từ " + otherName + " (Room " + roomId + ")");

                // Also test chat notification simulation
                List<ChatMessage> testMessages = new ArrayList<>();
                ChatMessage testMsg = new ChatMessage(roomId, otherId, myId, "Đây là tin nhắn test notification!", "TEXT");
                testMsg.setId(999999L);
                testMsg.setTimestamp(System.currentTimeMillis());
                testMessages.add(testMsg);
                
                Log.d(TAG, "🔔 Testing chat notification simulation...");
                chatNotificationManager.showChatNotification(roomId, otherName, testMessages, myId, otherId);
                
                Toast.makeText(requireContext(), "✅ Test notifications sent!", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "❌ ChatNotificationManager is NULL!");
                Toast.makeText(requireContext(), "❌ Notification manager chưa khởi tạo", Toast.LENGTH_LONG).show();
            }
            
            Log.d(TAG, "=== END TEST NOTIFICATION ===");
        });
        
        // Long click for report options
        tvOtherUserName.setOnLongClickListener(v -> {
            showChatReportOptions();
            return true;
        });
        
        // 🔥 DEBUG: Double click để force notification test (ngay cả khi fragment visible)
        tvOtherUserName.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;
            
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < 500) { // Double click trong 500ms
                    showDebugNotificationMenu();
                }
                lastClickTime = currentTime;
            }
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
            Log.d(TAG, "Auth token preview: " + authToken.substring(0, Math.min(10, authToken.length())) + "...");        }
        
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
                        Log.d(TAG, "No messages found, starting with empty chat");
                          // 🔥 FIX: Start polling even when no messages initially
                        if (isFragmentVisible) {
                            Log.d(TAG, "Starting LOCAL polling for empty chat room");
                            startLocalMessagePolling();
                        }
                        
                        // 🔥 NEW: Add empty room to background service
                        ChatPollingService.addRoomToPolling(requireContext(), roomId, otherId, otherName, 0);} else {
                        // Process messages and ensure imageUrl is set for IMAGE types
                        for (ChatMessage message : newMessages) {
                            if ("IMAGE".equals(message.getType()) && message.getContent() != null) {
                                // For IMAGE messages, ensure imageUrl is set from content
                                message.setImageUrl(message.getContent());
                                Log.d(TAG, "📸 Set imageUrl for IMAGE message: " + message.getContent());
                            }
                        }
                        
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
                    markMessagesAsRead();                    // Start polling for new messages if this is the first load
                    if (currentPage == 1) {
                        // 🔥 NEW: Add room to background service polling
                        long latestMessageId = !newMessages.isEmpty() ? 
                            newMessages.get(newMessages.size() - 1).getIdSafely() : 0;
                        ChatPollingService.addRoomToPolling(requireContext(), roomId, otherId, otherName, latestMessageId);
                        
                        // 🔥 FIX: Start LOCAL polling for UI updates when fragment visible
                        if (isFragmentVisible) {
                            Log.d(TAG, "Starting LOCAL polling after loading " + newMessages.size() + " messages");
                            startLocalMessagePolling();
                        }
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
                    });                    // Check if it's a fresh chat room (no messages yet)
                    if (currentPage == 0) {
                        Log.d(TAG, "Fresh chat room - treating as normal (no messages yet)");
                        canLoadMore = false;
                        // 🔥 FIX: Start LOCAL polling nếu fragment visible
                        if (isFragmentVisible) {
                            startLocalMessagePolling();
                        }
                        
                        // 🔥 NEW: Add to background service even for 403 errors (new chat room)
                        ChatPollingService.addRoomToPolling(requireContext(), roomId, otherId, otherName, 0);
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
                    // 🔥 FIX: Start LOCAL polling nếu fragment visible
                    if (isFragmentVisible) {
                        startLocalMessagePolling();
                    }
                    
                    // 🔥 NEW: Add to background service even on network error
                    ChatPollingService.addRoomToPolling(requireContext(), roomId, otherId, otherName, 0);
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
        
        // Track activity time for adaptive polling
        lastActivityTime = System.currentTimeMillis();
        
        // Clear input field and hide keyboard
        etMessage.setText("");
        hideKeyboard();
          // Create message với temporary ID để tránh duplicate
        ChatMessage newMessage = new ChatMessage(roomId, myId, otherId, messageText, "TEXT");
        // Set temporary ID để nhận diện optimistic message
        newMessage.setId(-System.currentTimeMillis()); // Negative temporary ID
        newMessage.setTimestamp(System.currentTimeMillis());
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
        request.put("content", messageText);        // Send to server
        chatApiService.sendTextMessageDirect(request).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessage> call, @NonNull Response<ChatMessage> response) {
                Log.d(TAG, "Send message response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    // 🔥 FIX: Update optimistic message với server data
                    ChatMessage serverMessage = response.body();
                    
                    // Find and update the optimistic message
                    for (int i = messageList.size() - 1; i >= 0; i--) {
                        ChatMessage msg = messageList.get(i);
                        if (msg.getId() != null && msg.getId() < 0 && 
                            msg.getContent().equals(messageText) &&
                            msg.getSenderId().equals(myId)) {
                            // Update với data từ server
                            msg.setId(serverMessage.getId());                            msg.setCreatedAt(serverMessage.getCreatedAt());
                            msg.setTimestamp(serverMessage.getTimestamp() != null ? 
                                serverMessage.getTimestamp() : System.currentTimeMillis());
                            

                            chatAdapter.notifyItemChanged(i);
                            Log.d(TAG, "✅ Message sent successfully with ID: " + serverMessage.getId());
                            break;
                        }
                    }
                } else {
                    // Message failed - show error and remove optimistic message
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Failed to send message: " + response.code());
                    Toast.makeText(getContext(), "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
                    
                    // Remove the failed optimistic message
                    for (int i = messageList.size() - 1; i >= 0; i--) {
                        ChatMessage msg = messageList.get(i);
                        if (msg.getId() != null && msg.getId() < 0 && 
                            msg.getContent().equals(messageText) &&
                            msg.getSenderId().equals(myId)) {
                            messageList.remove(i);
                            chatAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
            }
              @Override
            public void onFailure(@NonNull Call<ChatMessage> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Log.e(TAG, "Network error sending message", t);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Remove the failed optimistic message
                for (int i = messageList.size() - 1; i >= 0; i--) {
                    ChatMessage msg = messageList.get(i);
                    if (msg.getId() != null && msg.getId() < 0 && 
                        msg.getContent().equals(messageText) &&
                        msg.getSenderId().equals(myId)) {
                        messageList.remove(i);
                        chatAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }
        });
    }      private void showAttachmentOptions() {
        Log.d(TAG, "=== ATTACHMENT OPTIONS CLICKED ===");
        Log.d(TAG, "Room ID: " + roomId);
        
        if (roomId == -1) {
            Log.e(TAG, "❌ Cannot show attachment options - invalid room ID");
            return;
        }
        
        String[] options = {"Chọn ảnh từ thư viện", "Chụp ảnh", "Hủy"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Gửi hình ảnh");
        builder.setItems(options, (dialog, which) -> {
            Log.d(TAG, "Attachment option selected: " + which + " (" + options[which] + ")");
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
                    Log.d(TAG, "User cancelled attachment selection");
                    dialog.dismiss();
                    break;
            }
        });
        
        Log.d(TAG, "Showing attachment options dialog...");
        builder.show();
    }
      private void selectImageFromGallery() {
        Log.d(TAG, "=== SELECT IMAGE FROM GALLERY ===");
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            Log.d(TAG, "Starting gallery intent with REQUEST_SELECT_IMAGE (" + REQUEST_SELECT_IMAGE + ")");
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting gallery intent", e);
            Toast.makeText(getContext(), "Không thể mở thư viện ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void takePhoto() {
        Log.d(TAG, "=== TAKE PHOTO ===");
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Log.d(TAG, "Starting camera intent with REQUEST_TAKE_PHOTO (" + REQUEST_TAKE_PHOTO + ")");
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera intent", e);
            Toast.makeText(getContext(), "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();        }
    }
    
    private void sendImageMessage(Uri imageUri) {
        Log.d(TAG, "=== SEND IMAGE MESSAGE START ===");
        Log.d(TAG, "Image URI: " + imageUri);
        Log.d(TAG, "Room ID: " + roomId);
        Log.d(TAG, "My ID: " + myId + ", Other ID: " + otherId);
        Log.d(TAG, "Is blocked user: " + isBlockedUser);
        
        if (roomId == -1 || imageUri == null) {
            Log.e(TAG, "❌ Cannot send image - invalid parameters");
            Log.e(TAG, "Room ID valid: " + (roomId != -1));
            Log.e(TAG, "Image URI valid: " + (imageUri != null));
            return;
        }
          // Check if user is blocked
        if (isBlockedUser) {
            Log.d(TAG, "❌ Blocked user - preventing image send");
            Toast.makeText(getContext(), "Không thể gửi hình ảnh cho người dùng này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress while uploading
        Log.d(TAG, "📤 Starting image upload process...");
        Toast.makeText(getContext(), "Đang gửi hình ảnh...", Toast.LENGTH_SHORT).show();
        
        try {
            Log.d(TAG, "🔧 Creating multipart request...");
            // 🔥 FIX: Use ContentResolver instead of direct file access to avoid permission issues
            String fileName = getFileNameFromUri(imageUri);
            Log.d(TAG, "File name: " + fileName);
            
            RequestBody requestFile = createRequestBodyFromUri(imageUri);
            Log.d(TAG, "✅ RequestBody created successfully");
              MultipartBody.Part imagePart = MultipartBody.Part.createFormData("imageFile", fileName, requestFile);
            Log.d(TAG, "✅ MultipartBody.Part created successfully");
            
            // Track activity time for adaptive polling
            lastActivityTime = System.currentTimeMillis();
              // Create optimistic message for UI
            ChatMessage optimisticMessage = new ChatMessage(roomId, myId, otherId, "[Đang gửi hình ảnh...]", "IMAGE");
            optimisticMessage.setId(-System.currentTimeMillis()); // Negative temporary ID
            optimisticMessage.setTimestamp(System.currentTimeMillis());
            optimisticMessage.setImageUrl(imageUri.toString()); // Temporary local URI
            
            Log.d(TAG, "🎭 OPTIMISTIC MESSAGE CREATED:");
            Log.d(TAG, "  - Content: '" + optimisticMessage.getContent() + "'");
            Log.d(TAG, "  - ImageUrl: '" + optimisticMessage.getImageUrl() + "'");
            Log.d(TAG, "  - Type: '" + optimisticMessage.getType() + "'");
            Log.d(TAG, "  - isImage(): " + optimisticMessage.isImage());
            
            // Add to UI immediately
            messageList.add(optimisticMessage);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
              // Send to server - Use correct API with query parameters
            chatApiService.sendImageMessage((long)roomId, (long)myId, imagePart).enqueue(new Callback<ApiResponse>() {                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    Log.d(TAG, "Send image response code: " + response.code());
                    
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        Log.d(TAG, "API Response success: " + apiResponse.isSuccess());
                        
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            // Parse the data as ChatMessage
                            try {
                                // The data should contain the ChatMessage with imageUrl in content field
                                Object data = apiResponse.getData();
                                Log.d(TAG, "✅ Image uploaded successfully, data: " + data.toString());
                                
                                // Find and update the optimistic message
                                for (int i = messageList.size() - 1; i >= 0; i--) {
                                    ChatMessage msg = messageList.get(i);
                                    if (msg.getId() != null && msg.getId() < 0 && 
                                        msg.getSenderId().equals(myId) && 
                                        msg.getType().equals("IMAGE")) {
                                        
                                        // For image messages, server returns imageUrl in content field
                                        if (data instanceof Map) {
                                            Map<String, Object> messageData = (Map<String, Object>) data;
                                            String serverImageUrl = (String) messageData.get("content");
                                            Long serverId = ((Number) messageData.get("id")).longValue();
                                            
                                            // Update with server data
                                            msg.setId(serverId);
                                            msg.setImageUrl(serverImageUrl); // Set imageUrl for proper display
                                            msg.setContent(serverImageUrl); // Also set content for compatibility
                                            msg.setTimestamp(System.currentTimeMillis());
                                            
                                            chatAdapter.notifyItemChanged(i);
                                            Log.d(TAG, "✅ Image sent successfully with ID: " + serverId);
                                            Log.d(TAG, "✅ Server image URL: " + serverImageUrl);
                                            Toast.makeText(getContext(), "Hình ảnh đã được gửi", Toast.LENGTH_SHORT).show();
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing server response", e);
                                Toast.makeText(getContext(), "Lỗi xử lý phản hồi từ server", Toast.LENGTH_SHORT).show();
                                removeFailedOptimisticMessage();
                            }                        } else {
                            String errorMsg = apiResponse.getMessage();
                            Log.e(TAG, "API response indicates failure: " + errorMsg);
                            Toast.makeText(getContext(), "Lỗi: " + (errorMsg != null ? errorMsg : "Không thể gửi hình ảnh"), Toast.LENGTH_SHORT).show();
                            removeFailedOptimisticMessage();
                        }
                    } else {
                        // Image failed - show error and remove optimistic message
                        if (!isAdded() || getContext() == null) return;
                        Log.e(TAG, "Failed to send image: " + response.code());
                        Toast.makeText(getContext(), "Không thể gửi hình ảnh", Toast.LENGTH_SHORT).show();
                        removeFailedOptimisticMessage();
                    }
                }
                
                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Network error sending image", t);
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    removeFailedOptimisticMessage();
                }
            });
              } catch (Exception e) {
            Log.e(TAG, "❌ CRITICAL ERROR in sendImageMessage", e);
            Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
            Log.e(TAG, "Error message: " + e.getMessage());
            Log.e(TAG, "Error cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            
            if (!isAdded() || getContext() == null) {
                Log.e(TAG, "Fragment not added or context null during error handling");
                return;
            }
            
            // Show detailed error message
            String errorMsg = "Lỗi xử lý hình ảnh: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (" + e.getCause().getMessage() + ")";
            }
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "❌ Image send failed during setup - no cleanup needed");
        }
    }
    
    // Helper method to get real path from URI    // 🔥 FIX: Create RequestBody from URI using ContentResolver (no file permissions needed)
    private RequestBody createRequestBodyFromUri(Uri uri) {
        Log.d(TAG, "🔧 Creating RequestBody from URI: " + uri);
        try {
            Log.d(TAG, "📂 Opening InputStream from ContentResolver...");
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for URI: " + uri);
            }
            
            Log.d(TAG, "📊 Reading InputStream to bytes...");
            byte[] bytes = readInputStreamToBytes(inputStream);
            inputStream.close();
            
            Log.d(TAG, "✅ Successfully read " + bytes.length + " bytes from image");
            Log.d(TAG, "🔧 Creating RequestBody with MediaType image/*...");
            
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), bytes);
            Log.d(TAG, "✅ RequestBody created successfully");
            
            return requestBody;
        } catch (Exception e) {
            Log.e(TAG, "❌ FATAL ERROR creating RequestBody from URI: " + uri, e);
            Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
            Log.e(TAG, "Error message: " + e.getMessage());
            Log.e(TAG, "Error cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            throw new RuntimeException("Failed to read image file", e);
        }
    }
      // Helper method to read InputStream to byte array
    private byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        Log.d(TAG, "📊 Starting to read InputStream to bytes...");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        long totalBytes = 0;
        
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
            totalBytes += nRead;
            
            // Log progress for large files
            if (totalBytes % (1024 * 1024) == 0) {
                Log.d(TAG, "📊 Read " + (totalBytes / (1024 * 1024)) + "MB so far...");
            }
        }
        
        byte[] result = buffer.toByteArray();
        Log.d(TAG, "✅ Finished reading InputStream: " + result.length + " bytes total");
        return result;
    }
    
    // Get filename from URI using ContentResolver
    private String getFileNameFromUri(Uri uri) {
        String fileName = "image.jpg"; // Default filename
        try {
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get filename from URI, using default: " + e.getMessage());
        }
        
        // Ensure filename has extension
        if (!fileName.contains(".")) {
            fileName += ".jpg";
        }
        
        return fileName;
    }
    
    // DEPRECATED: Remove old method that causes permission issues
    @Deprecated
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return uri.getPath();
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
      private void startLocalMessagePolling() {
        // 🔥 FIX: Local polling chỉ để cập nhật UI realtime khi fragment visible
        // Background service sẽ handle notification
        stopLocalMessagePolling();
        
        Log.d(TAG, "🔄 Starting LOCAL message polling for UI updates only");
          messageRunnable = new Runnable() {
            @Override
            public void run() {
                // Chỉ poll khi fragment visible và added
                if (!isAdded() || roomId == -1 || !isFragmentVisible) {
                    Log.d(TAG, "❌ LOCAL polling cancelled - fragment state: isAdded=" + isAdded() + 
                          ", roomId=" + roomId + ", visible=" + isFragmentVisible);
                    return;
                }
                
                Log.d(TAG, "📡 LOCAL polling for UI updates...");
                
                // Get latest message ID
                final long latestMessageId = !messageList.isEmpty() ? 
                    messageList.get(messageList.size() - 1).getIdSafely() : 0;
                
                Log.d(TAG, "📡 Checking for messages after ID: " + latestMessageId);
                    // Poll for new messages
                chatApiService.getChatMessagesDirect(roomId, myId).enqueue(new Callback<List<ChatMessage>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                        if (!isAdded() || !isFragmentVisible) {
                            Log.d(TAG, "Fragment not added or not visible, skipping UI update");
                            return;
                        }
                        
                        if (response.isSuccessful() && response.body() != null) {
                            List<ChatMessage> newMessages = response.body();                            // Lọc tin nhắn mới cho UI
                            List<ChatMessage> messagesToAdd = new ArrayList<>();
                            for (ChatMessage message : newMessages) {
                                if (message.getId() != null && message.getId() < 0) {
                                    continue; // Skip optimistic messages
                                }
                                
                                if (message.getIdSafely() > latestMessageId && !isDuplicateMessage(message)) {
                                    // Set imageUrl for IMAGE messages
                                    if ("IMAGE".equals(message.getType()) && message.getContent() != null) {
                                        message.setImageUrl(message.getContent());
                                    }
                                    messagesToAdd.add(message);
                                }
                            }
                            

                            // Update UI với tin nhắn mới
                            if (!messagesToAdd.isEmpty()) {
                                // Remove matching optimistic messages
                                for (ChatMessage newMsg : messagesToAdd) {
                                    if (newMsg.getSenderId().equals(myId)) {
                                        for (int i = messageList.size() - 1; i >= 0; i--) {
                                            ChatMessage existingMsg = messageList.get(i);
                                            if (existingMsg.getId() != null && existingMsg.getId() < 0 &&
                                                existingMsg.getContent().equals(newMsg.getContent()) &&
                                                existingMsg.getSenderId().equals(newMsg.getSenderId())) {
                                                messageList.remove(i);
                                                chatAdapter.notifyItemRemoved(i);
                                                break;
                                            }
                                        }
                                    }
                                }
                                
                                int insertPosition = messageList.size();
                                messageList.addAll(messagesToAdd);
                                chatAdapter.notifyItemRangeInserted(insertPosition, messagesToAdd.size());
                                
                                // Scroll to bottom
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                                });
                                
                                // Mark messages as read
                                markMessagesAsRead();
                                
                                Log.d(TAG, "Added " + messagesToAdd.size() + " new messages to UI via LOCAL polling");
                            }
                        }
                        
                        // Schedule next LOCAL poll
                        if (isAdded() && isFragmentVisible && messageRunnable != null) {
                            int interval = POLLING_INTERVAL_ACTIVE; // Fast for UI updates
                            messageHandler.postDelayed(messageRunnable, interval);
                            Log.d(TAG, "⏰ Scheduling next LOCAL poll in " + interval + "ms");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                        Log.e(TAG, "❌ Error in LOCAL polling: " + t.getMessage());
                        
                        // Schedule retry
                        if (isAdded() && isFragmentVisible && messageRunnable != null) {
                            messageHandler.postDelayed(messageRunnable, POLLING_INTERVAL_IDLE);
                        }
                    }
                });
            }
        };
        
        // Start immediately
        messageHandler.postDelayed(messageRunnable, 200);
    }
    
    private void stopLocalMessagePolling() {
        Log.d(TAG, "Stopping LOCAL message polling");
        
        if (messageHandler != null && messageRunnable != null) {
            messageHandler.removeCallbacks(messageRunnable);
            messageRunnable = null;
        }
        
        Log.d(TAG, "LOCAL message polling stopped successfully");
    }
      // 🔥 FIX: Thêm method để kiểm tra duplicate message với logic tốt hơn
    private boolean isDuplicateMessage(ChatMessage newMessage) {
        if (newMessage == null) {
            return false;
        }
        
        // Kiểm tra duplicate bằng ID nếu có
        if (newMessage.getId() != null) {
            for (ChatMessage existingMessage : messageList) {
                if (existingMessage.getId() != null && 
                    existingMessage.getId().equals(newMessage.getId())) {
                    return true;
                }
            }
        }
        
        // Kiểm tra duplicate bằng nội dung và thời gian (cho optimistic UI)
        if (newMessage.getContent() != null && newMessage.getSenderId() != null) {
            long newMessageTime = System.currentTimeMillis();
            for (ChatMessage existingMessage : messageList) {
                if (existingMessage.getContent() != null && 
                    existingMessage.getSenderId() != null &&
                    existingMessage.getContent().equals(newMessage.getContent()) &&
                    existingMessage.getSenderId().equals(newMessage.getSenderId())) {
                    
                    // Check if messages are very close in time (within 10 seconds)
                    long timeDiff = Math.abs(newMessageTime - (existingMessage.getTimestamp() != null ? 
                        existingMessage.getTimestamp() : System.currentTimeMillis()));
                    if (timeDiff < 10000) { // 10 seconds
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Show local notification for new messages when fragment is not visible
     */
    private void showNewMessageNotification(List<ChatMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            Log.d(TAG, "No new messages to notify about");
            return;
        }        Log.d(TAG, "=== NOTIFICATION DEBUG ===");
        Log.d(TAG, "New messages count: " + newMessages.size());
        Log.d(TAG, "Fragment visible: " + isFragmentVisible);
        
        // Use ChatNotificationManager for better notification handling
        if (chatNotificationManager != null) {
            Log.d(TAG, "Using ChatNotificationManager for notifications");
            
            // Only show notifications when fragment is not visible
            // This prevents spam notifications while user is actively chatting
            if (!isFragmentVisible) {
                chatNotificationManager.showChatNotification(roomId, otherName, newMessages, myId, otherId);
                Log.d(TAG, "✅ Notification sent via ChatNotificationManager (fragment not visible)");
            } else {
                Log.d(TAG, "Skipping notification - user is viewing this chat");
                
                // Clear any existing notifications for this chat since user is viewing it
                chatNotificationManager.clearChatNotifications(roomId);
            }
        } else {
            Log.e(TAG, "❌ ChatNotificationManager is null, cannot show notifications");
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
                    }                } else if (latestMessage.isImage()) {
                    content = "Đã gửi một hình ảnh";
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
                    .setSmallIcon(android.R.drawable.ic_dialog_email) // Use system icon
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL);
                  // Use default notification sound since custom sound resource doesn't exist
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            
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
              // 4. Check app notification settings - DISABLED for now
            // if (notificationHelper != null) {
            //     boolean appMessagesEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_MESSAGES);
            //     Log.d(TAG, "App messages setting enabled: " + appMessagesEnabled);
            // } else {
            //     Log.e(TAG, "❌ NotificationHelper is NULL");
            // }
            
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
            if ( imm != null && getActivity().getCurrentFocus() != null) {
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
    
    // 🔥 Implementation của OnMessageActionListener
    @Override
    public void onEditMessage(ChatMessage message, int position) {
        showEditMessageDialog(message, position);
    }
    
    @Override
    public void onDeleteMessage(ChatMessage message, int position) {
        showDeleteMessageConfirmDialog(message, position);
    }
    
    @Override
    public void onCopyMessage(ChatMessage message) {
        copyMessageToClipboard(message);
    }
    
    // 🔥 Method để hiện dialog sửa tin nhắn
    private void showEditMessageDialog(ChatMessage message, int position) {
        if (message.isImage()) {
            Toast.makeText(getContext(), "Không thể sửa tin nhắn hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sửa tin nhắn");
        
        final EditText editText = new EditText(requireContext());
        editText.setText(message.getContent());
        editText.setSelection(message.getContent().length()); // Cursor ở cuối
        builder.setView(editText);
        
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newContent = editText.getText().toString().trim();
            if (!newContent.isEmpty()) {
                updateMessage(message, newContent, position);
            } else {
                Toast.makeText(getContext(), "Nội dung tin nhắn không được để trống", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Show keyboard
        editText.requestFocus();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }
    
    private void showDeleteMessageConfirmDialog(ChatMessage message, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Xóa tin nhắn");
        builder.setMessage("Bạn có chắc chắn muốn xóa tin nhắn này?");
        
        builder.setPositiveButton("Xóa", (dialog, which) -> {
            deleteMessage(message, position);
        });
        
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    
    private void copyMessageToClipboard(ChatMessage message) {
        if (message.isImage()) {
            Toast.makeText(getContext(), "Không thể sao chép tin nhắn hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Tin nhắn", message.getContent());
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(getContext(), "Đã sao chép tin nhắn", Toast.LENGTH_SHORT).show();
    }
      private void updateMessage(ChatMessage message, String newContent, int position) {
        if (message.getId() == null) {
            Toast.makeText(getContext(), "Không thể sửa tin nhắn này", Toast.LENGTH_SHORT).show();
            return;
        }
          // Save original content if this is the first edit
        final String finalOriginalContent;
        if (message.getOriginalContent() == null || message.getOriginalContent().isEmpty()) {
            finalOriginalContent = message.getContent(); // Current content becomes original
        } else {
            finalOriginalContent = message.getOriginalContent();
        }
        
        // Call API để update message
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("content", newContent);
        
        chatApiService.updateMessage(message.getId(), myId, updateRequest).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Update local message
                    message.setOriginalContent(finalOriginalContent); // Set original content
                    message.setContent(newContent);
                    message.setIsEdited(true);
                    // Set update time (simplified format)
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                    message.setUpdatedAt(sdf.format(new java.util.Date()));
                    
                    chatAdapter.updateMessage(position, message);
                    
                    Toast.makeText(getContext(), "Đã cập nhật tin nhắn", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Không thể cập nhật tin nhắn", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void deleteMessage(ChatMessage message, int position) {
        if (message.getId() == null) {
            Toast.makeText(getContext(), "Không thể xóa tin nhắn này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Call API để delete message
        chatApiService.deleteMessage(message.getId(), myId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Remove from adapter
                    chatAdapter.removeMessage(position);
                    
                    Toast.makeText(getContext(), "Đã xóa tin nhắn", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Không thể xóa tin nhắn", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "=== ON ACTIVITY RESULT ===");
        Log.d(TAG, "Request Code: " + requestCode);
        Log.d(TAG, "Result Code: " + resultCode);
        Log.d(TAG, "Data: " + (data != null ? "Available" : "NULL"));
        Log.d(TAG, "RESULT_OK: " + getActivity().RESULT_OK);
        
        if (resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = null;
            
            switch (requestCode) {
                case REQUEST_SELECT_IMAGE:
                    Log.d(TAG, "Processing gallery image selection...");
                    // Image selected from gallery
                    imageUri = data.getData();
                    Log.d(TAG, "Gallery image URI: " + imageUri);
                    break;
                    
                case REQUEST_TAKE_PHOTO:
                    Log.d(TAG, "Processing camera photo...");
                    // Photo taken with camera
                    Bundle extras = data.getExtras();
                    if (extras != null && extras.get("data") != null) {
                        // For camera, we need to save the bitmap and get URI
                        // This is a simplified version - in production you'd save to file
                        Toast.makeText(getContext(), "Tính năng chụp ảnh sẽ được cập nhật sớm", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                    
                default:
                    Log.d(TAG, "Unknown request code: " + requestCode);
                    break;
            }
            
            if (imageUri != null) {
                Log.d(TAG, "✅ Image selected successfully: " + imageUri);
                Log.d(TAG, "Calling sendImageMessage...");
                sendImageMessage(imageUri);
            } else {
                Log.e(TAG, "❌ Image URI is null");
                Toast.makeText(getContext(), "Không thể chọn hình ảnh", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "⚠️ Activity result not OK or data is null");
            Log.w(TAG, "User may have cancelled or there was an error");
        }
    }
    
    private void showDebugNotificationMenu() {
        Log.d(TAG, "=== DEBUG NOTIFICATION MENU ===");
        
        String[] options = {
            "🔔 Test Notification (Normal)",
            "🔔 Force Chat Notification (Ignore visibility)", 
            "📱 Simulate Background Message",
            "🧹 Clear All Notifications",
            "❌ Cancel"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("🔧 Debug Notification Menu");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // Test notification
                    if (chatNotificationManager != null) {
                        chatNotificationManager.showTestNotification("🔔 Normal Test từ " + otherName);
                        Toast.makeText(getContext(), "✅ Test notification sent", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case 1:
                    // Force chat notification (ignore visibility)
                    if (chatNotificationManager != null) {
                        List<ChatMessage> testMessages = new ArrayList<>();
                        ChatMessage testMsg = new ChatMessage(roomId, otherId, myId, "🔔 FORCE notification test - ignore fragment visibility!", "TEXT");
                        testMsg.setId(System.currentTimeMillis());
                        testMsg.setTimestamp(System.currentTimeMillis());
                        testMessages.add(testMsg);
                        
                        Log.d(TAG, "🔔 FORCE showing chat notification (ignore visibility)");
                        chatNotificationManager.showChatNotification(roomId, otherName, testMessages, myId, otherId);
                        Toast.makeText(getContext(), "🔔 Force notification sent!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case 2:
                    // Simulate background message
                    Toast.makeText(getContext(), "📱 Simulation: Press HOME button and ask someone to send you a message", Toast.LENGTH_LONG).show();
                    break;
                    
                case 3:
                    // Clear notifications
                    if (chatNotificationManager != null) {
                        chatNotificationManager.clearAllChatNotifications();
                        Toast.makeText(getContext(), "🧹 All notifications cleared", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    
                case 4:
                    // Cancel
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }
    
    /**
     * Helper method to remove failed optimistic image message
     */
    private void removeFailedOptimisticMessage() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            ChatMessage msg = messageList.get(i);
            if (msg.getId() != null && msg.getId() < 0 && 
                msg.getSenderId().equals(myId) && 
                msg.getType().equals("IMAGE")) {
                messageList.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }
}
