package com.example.ok.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;

import java.util.List;

public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewViewHolder> {

    private Context context;
    private List<Uri> imageUris;
    private OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public ImagePreviewAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    public void setOnImageRemoveListener(OnImageRemoveListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ImagePreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_preview, parent, false);
        return new ImagePreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImagePreviewViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.bind(imageUri, position);
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public class ImagePreviewViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPreview;
        private ImageButton btnRemove;

        public ImagePreviewViewHolder(@NonNull View itemView) {
            super(itemView);

            ivPreview = itemView.findViewById(R.id.iv_preview);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }

        public void bind(Uri imageUri, int position) {
            // Load image using Glide
            Glide.with(context)
                    .load(imageUri)
                    .centerCrop()
                    .placeholder(R.drawable.image)
                    .error(R.drawable.photo)
                    .into(ivPreview);

            // Set remove button click listener
            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onImageRemove(position);
                }
            });
        }
    }
}