package com.example.ok.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ok.R;
import com.example.ok.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<Category> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(Context context) {
        this.context = context;
        this.categories = new ArrayList<>();
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_with_icon, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCategoryIcon;
        private TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCategoryClick(categories.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Category category) {
            tvCategoryName.setText(category.getName());

            // Load category icon
            if (category.getIconUrl() != null && !category.getIconUrl().isEmpty()) {
                Glide.with(context)
                        .load(category.getIconUrl())
                        .placeholder(R.drawable.categories)
                        .error(R.drawable.categories)
                        .into(ivCategoryIcon);
            } else {
                ivCategoryIcon.setImageResource(getCategoryIcon(category.getName()));
            }
        }

        private int getCategoryIcon(String categoryName) {
            // Return appropriate icon based on category name
            switch (categoryName.toLowerCase()) {
                case "điện tử": return R.drawable.ic_electronics;
                case "thời trang": return R.drawable.ic_fashion;
                case "gia dụng": return R.drawable.furniture;
                case "xe cộ": return R.drawable.car;
                case "sách & văn phòng phẩm": return R.drawable.book;
                case "thể thao & giải trí": return R.drawable.sports;
                default: return R.drawable.categories;
            }
        }
    }
}