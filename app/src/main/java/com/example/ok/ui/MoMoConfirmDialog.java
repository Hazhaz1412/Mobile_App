package com.example.ok.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.ok.R;

/**
 * Dialog hiển thị khi người dùng muốn xác nhận thanh toán thủ công MoMo
 */
public class MoMoConfirmDialog extends Dialog {
    
    private static final String TAG = "MoMoConfirmDialog";
    
    public interface MoMoConfirmListener {
        void onConfirmPayment();
        void onCancelPayment();
    }
    
    private final MoMoConfirmListener listener;
    private final String transactionId;
    
    public MoMoConfirmDialog(@NonNull Context context, String transactionId, MoMoConfirmListener listener) {
        super(context);
        this.listener = listener;
        this.transactionId = transactionId;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_momo_confirm);
        
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        TextView tvTransactionId = findViewById(R.id.tvTransactionId);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnCancel = findViewById(R.id.btnCancel);
        
        tvTransactionId.setText("Mã giao dịch: " + transactionId);
        
        btnConfirm.setOnClickListener(v -> {
            Log.d(TAG, "User confirmed MoMo payment: " + transactionId);
            if (listener != null) {
                listener.onConfirmPayment();
            }
            dismiss();
        });
        
        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "User cancelled MoMo payment: " + transactionId);
            if (listener != null) {
                listener.onCancelPayment();
            }
            dismiss();
        });
        
        setCancelable(true);
        setOnCancelListener(dialog -> {
            Log.d(TAG, "Dialog cancelled by user");
        });
    }
}
