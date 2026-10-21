package com.project.norushcart.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.R;
import com.project.norushcart.data.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class CooldownAdapter extends RecyclerView.Adapter<CooldownAdapter.ViewHolder> {

    public interface OnFastForwardListener {
        void onFastForward(ProductItem item);
    }

    private final Context context;
    private List<ProductItem> items = new ArrayList<>();
    private final OnFastForwardListener listener;

    public CooldownAdapter(Context context, OnFastForwardListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ProductItem> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cooldown_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivProductImage;
        TextView tvPlatformBadge;
        TextView tvProductTitle;
        TextView tvProductPrice;
        TextView tvCountdownText;
        MaterialButton btnDebugFastForward;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvPlatformBadge = itemView.findViewById(R.id.tvPlatformBadge);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCountdownText = itemView.findViewById(R.id.tvCountdownText);
            btnDebugFastForward = itemView.findViewById(R.id.btnDebugFastForward);
        }

        public void bind(ProductItem item) {
            tvProductTitle.setText(item.getTitle());
            tvProductPrice.setText(item.getFormattedPrice());
            tvPlatformBadge.setText(item.getSourcePlatform());

            if ("Flipkart".equalsIgnoreCase(item.getSourcePlatform())) {
                tvPlatformBadge.setBackgroundColor(Color.parseColor("#FFF3E0"));
                tvPlatformBadge.setTextColor(Color.parseColor("#E65100"));
            } else {
                tvPlatformBadge.setBackgroundColor(Color.parseColor("#E8F0FE"));
                tvPlatformBadge.setTextColor(Color.parseColor("#1A73E8"));
            }

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.ic_launcher_background);
            }

            tvCountdownText.setText("⏳ Unlocks in: " + item.getRemainingTimeFormatted());

            btnDebugFastForward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFastForward(item);
                }
            });
        }
    }
}
