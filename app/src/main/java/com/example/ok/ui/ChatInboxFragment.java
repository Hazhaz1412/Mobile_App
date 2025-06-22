package com.example.ok.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ok.MainMenu;
import com.example.ok.R;
import com.example.ok.adapter.ChatInboxAdapter;
import com.example.ok.model.ApiResponse;
import com.example.ok.api.ChatApiService;
import com.example.ok.api.RetrofitClient;
import com.example.ok.model.ChatRoom;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class ChatInboxFragment extends Fragment implements ChatInboxAdapter.OnChatRoomClickListener, ChatInboxAdapter.OnUserProfileClickListener {

    private static final String TAG = "ChatInboxFragment";
    private static final int POLLING_INTERVAL = 10000; // 10 seconds

    // UI components
    private RecyclerView recyclerChatRooms;
    private EditText etSearch;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    // Data
    private List<ChatRoom> chatRoomList = new ArrayList<>();
    private List<ChatRoom> filteredChatRoomList = new ArrayList<>();
    private ChatInboxAdapter chatInboxAdapter;
    private long currentUserId = -1;
    private boolean showUnreadOnly = false;
    private String searchQuery = "";

    // Services
    private ChatApiService chatApiService;

    // Polling for updates
    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);

        // Ensure RetrofitClient is initialized before using it
        RetrofitClient.init(requireContext());
        chatApiService = RetrofitClient.getChatApiService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();        if (currentUserId != -1) {
            loadChatRooms();
            startChatRoomPolling();
        } else {
            // Check if fragment is still attached before accessing context
            if (!isAdded() || getContext() == null) {
                Log.d(TAG, "Fragment not attached, skipping error message");
                return;
            }
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem tin nhắn", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }

    private void initViews(View view) {
        // Find views
        recyclerChatRooms = view.findViewById(R.id.recyclerChatRooms);
        etSearch = view.findViewById(R.id.etSearch);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerChatRooms.setLayoutManager(layoutManager);
        
        // Add divider
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerChatRooms.getContext(), layoutManager.getOrientation());
        recyclerChatRooms.addItemDecoration(dividerItemDecoration);        // Set up adapter
        chatInboxAdapter = new ChatInboxAdapter(requireContext(), filteredChatRoomList, currentUserId, this);
        chatInboxAdapter.setOnUserProfileClickListener(this);
        recyclerChatRooms.setAdapter(chatInboxAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefresh.setOnRefreshListener(this::refreshChatRooms);

        // Tab selection (All/Unread)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showUnreadOnly = tab.getPosition() == 1;
                filterChatRooms();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Search box
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                filterChatRooms();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }    private void loadChatRooms() {
        showProgress();

        chatApiService.getUserChatRoomsDirect(currentUserId).enqueue(new Callback<List<ChatRoom>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoom>> call, @NonNull Response<List<ChatRoom>> response) {
                // Check if fragment is still attached before accessing context
                if (!isAdded() || getContext() == null) {
                    Log.d(TAG, "Fragment not attached, skipping response handling");
                    return;
                }
                
                hideProgress();
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    chatRoomList = response.body();
                    
                    if (chatRoomList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showChatRooms();
                        filterChatRooms();
                    }
                } else {
                    Toast.makeText(getContext(), "Không thể tải danh sách tin nhắn", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoom>> call, @NonNull Throwable t) {
                // Check if fragment is still attached before accessing context
                if (!isAdded() || getContext() == null) {
                    Log.d(TAG, "Fragment not attached, skipping error handling");
                    return;
                }
                
                hideProgress();
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading chat rooms", t);
                showEmptyState();
            }
        });
    }

    private void refreshChatRooms() {
        swipeRefresh.setRefreshing(true);
        loadChatRooms();
    }

    private void filterChatRooms() {
        filteredChatRoomList.clear();

        for (ChatRoom room : chatRoomList) {
            // Apply unread filter
            if (showUnreadOnly && room.getUnreadCount() <= 0) {
                continue;
            }

            // Apply search filter
            String otherUserName = room.getOtherUserName(currentUserId);
            String listingTitle = room.getListingTitle();

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (otherUserName != null && otherUserName.toLowerCase().contains(searchQuery)) ||
                    (listingTitle != null && listingTitle.toLowerCase().contains(searchQuery)) ||
                    (room.getLastMessageContent() != null && 
                     room.getLastMessageContent().toLowerCase().contains(searchQuery));

            if (matchesSearch) {
                filteredChatRoomList.add(room);
            }
        }

        chatInboxAdapter.notifyDataSetChanged();

        // Show empty state if no results
        if (filteredChatRoomList.isEmpty() && !chatRoomList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerChatRooms.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerChatRooms.setVisibility(View.VISIBLE);
        }
    }

    private void startChatRoomPolling() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {                if (isAdded() && currentUserId != -1) {
                    // Poll for chat room updates - Use same API as loadChatRooms()
                    chatApiService.getUserChatRoomsDirect(currentUserId).enqueue(new Callback<List<ChatRoom>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<ChatRoom>> call, @NonNull Response<List<ChatRoom>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<ChatRoom> updatedRooms = response.body();
                                
                                if (!updatedRooms.isEmpty()) {
                                    chatRoomList = updatedRooms;
                                    filterChatRooms();
                                    
                                    if (layoutEmpty.getVisibility() == View.VISIBLE && !chatRoomList.isEmpty()) {
                                        showChatRooms();
                                    }
                                }
                            }
                            
                            // Schedule next poll
                            updateHandler.postDelayed(updateRunnable, POLLING_INTERVAL);
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<List<ChatRoom>> call, @NonNull Throwable t) {
                            Log.e(TAG, "Error polling for chat room updates", t);
                            
                            // Schedule next poll even if this one failed
                            updateHandler.postDelayed(updateRunnable, POLLING_INTERVAL);
                        }
                    });
                }
            }
        };
        
        // Start polling
        updateHandler.postDelayed(updateRunnable, POLLING_INTERVAL);
    }
    
    private void stopChatRoomPolling() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerChatRooms.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerChatRooms.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void showChatRooms() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerChatRooms.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }    @Override
    public void onChatRoomClick(ChatRoom chatRoom) {
        if (chatRoom == null) return;
        
        // Navigate to chat fragment
        Bundle args = new Bundle();
        args.putLong("roomId", chatRoom.getId());
        args.putLong("myId", currentUserId);
        args.putLong("otherId", chatRoom.getOtherUserId(currentUserId));
        args.putString("otherName", chatRoom.getOtherUserName(currentUserId));
        
        ChatFragment chatFragment = ChatFragment.newInstance(
                chatRoom.getId(),
                currentUserId,
                chatRoom.getOtherUserId(currentUserId),
                chatRoom.getOtherUserName(currentUserId)
        );
          ((MainMenu) requireActivity()).replaceFragment(chatFragment);
    }
    
    @Override
    public void onUserProfileClick(Long userId, String userName) {
        if (userId == null) return;
        
        // Navigate to other user's profile
        OtherUserProfileFragment profileFragment = OtherUserProfileFragment.newInstance(userId, userName);
        ((MainMenu) requireActivity()).replaceFragment(profileFragment);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopChatRoomPolling();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            refreshChatRooms();
            startChatRoomPolling();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopChatRoomPolling();
    }
}
