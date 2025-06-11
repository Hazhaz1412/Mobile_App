package com.example.ok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ok.R;
import com.example.ok.util.NotificationHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsFragment extends Fragment {
    private static final String TAG = "NotificationSettings";

    private NotificationHelper notificationHelper;
    
    // UI Components
    private SwitchMaterial switchMessages, switchOffers, switchListings, switchPromotions;
    private TextView tvSystemNotificationStatus, tvNotificationSummary;
    private MaterialCardView cardSystemSettings;

    public NotificationSettingsFragment() {
        // Required empty public constructor
    }

    public static NotificationSettingsFragment newInstance() {
        return new NotificationSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationHelper = new NotificationHelper(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_settings, container, false);

        initViews(view);
        setupListeners();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        switchMessages = view.findViewById(R.id.switchMessages);
        switchOffers = view.findViewById(R.id.switchOffers);
        switchListings = view.findViewById(R.id.switchListings);
        switchPromotions = view.findViewById(R.id.switchPromotions);
        tvSystemNotificationStatus = view.findViewById(R.id.tvSystemNotificationStatus);
        tvNotificationSummary = view.findViewById(R.id.tvNotificationSummary);
        cardSystemSettings = view.findViewById(R.id.cardSystemSettings);
    }

    private void setupListeners() {
        switchMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notificationHelper.setNotificationEnabled(NotificationHelper.NOTIF_MESSAGES, isChecked);
            updateSummary();
        });

        switchOffers.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notificationHelper.setNotificationEnabled(NotificationHelper.NOTIF_OFFERS, isChecked);
            updateSummary();
        });

        switchListings.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notificationHelper.setNotificationEnabled(NotificationHelper.NOTIF_LISTINGS, isChecked);
            updateSummary();
        });

        switchPromotions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notificationHelper.setNotificationEnabled(NotificationHelper.NOTIF_PROMOTIONS, isChecked);
            updateSummary();
        });

        cardSystemSettings.setOnClickListener(v -> openSystemNotificationSettings());
    }

    private void updateUI() {
        // Update switch states
        switchMessages.setChecked(notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_MESSAGES));
        switchOffers.setChecked(notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_OFFERS));
        switchListings.setChecked(notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_LISTINGS));
        switchPromotions.setChecked(notificationHelper.isNotificationEnabled(NotificationHelper.NOTIF_PROMOTIONS));

        // Update system notification status
        if (notificationHelper.areNotificationsEnabled()) {
            tvSystemNotificationStatus.setText("Đã bật");
            tvSystemNotificationStatus.setTextColor(getResources().getColor(R.color.success_color));
        } else {
            tvSystemNotificationStatus.setText("Đã tắt");
            tvSystemNotificationStatus.setTextColor(getResources().getColor(R.color.error_color));
        }

        updateSummary();
    }

    private void updateSummary() {
        tvNotificationSummary.setText(notificationHelper.getNotificationSettingsSummary());
    }

    private void openSystemNotificationSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update UI when returning from system settings
        updateUI();
    }
}
