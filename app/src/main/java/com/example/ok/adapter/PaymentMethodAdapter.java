package com.example.ok.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ok.R;
import com.example.ok.model.PaymentMethod;

import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {
    
    private List<PaymentMethod> paymentMethods;
    private PaymentMethod selectedMethod;
    private OnPaymentMethodSelectedListener listener;
    
    public interface OnPaymentMethodSelectedListener {
        void onPaymentMethodSelected(PaymentMethod method);
    }
    
    public PaymentMethodAdapter(List<PaymentMethod> paymentMethods, OnPaymentMethodSelectedListener listener) {
        this.paymentMethods = paymentMethods;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod method = paymentMethods.get(position);
        holder.bind(method);
    }
    
    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }
    
    public void setSelectedMethod(PaymentMethod method) {
        this.selectedMethod = method;
        notifyDataSetChanged();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivIcon;
        private TextView tvDisplayName;
        private TextView tvMaskedNumber;
        private RadioButton rbSelected;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivPaymentIcon);
            tvDisplayName = itemView.findViewById(R.id.tvPaymentDisplayName);
            tvMaskedNumber = itemView.findViewById(R.id.tvPaymentMaskedNumber);
            rbSelected = itemView.findViewById(R.id.rbPaymentSelected);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    PaymentMethod method = paymentMethods.get(position);
                    setSelectedMethod(method);
                    if (listener != null) {
                        listener.onPaymentMethodSelected(method);
                    }
                }
            });
        }
        
        public void bind(PaymentMethod method) {
            tvDisplayName.setText(method.getDisplayName());
            tvMaskedNumber.setText(method.getMaskedNumber());
            rbSelected.setChecked(method.equals(selectedMethod));
            
            // Set appropriate icon based on payment method type
            switch (method.getType()) {
                case PaymentMethod.MOMO:
                    ivIcon.setImageResource(R.drawable.ic_momo);
                    break;
                case PaymentMethod.VISA:
                    ivIcon.setImageResource(R.drawable.ic_visa);
                    break;
                case PaymentMethod.MASTERCARD:
                    ivIcon.setImageResource(R.drawable.ic_mastercard);
                    break;
                case PaymentMethod.CASH:
                    ivIcon.setImageResource(R.drawable.ic_cash);
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_payment);
                    break;
            }
        }
    }
}
