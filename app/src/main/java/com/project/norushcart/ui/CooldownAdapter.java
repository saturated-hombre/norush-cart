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

    public void tickVisibleCountdowns(RecyclerView recyclerView) {
        if (recyclerView == null || items.isEmpty()) return;
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
            if (holder instanceof ViewHolder) {
                int pos = holder.getBindingAdapterPosition();
                if (pos >= 0 && pos < items.size()) {
                    ProductItem item = items.get(pos);
                    ((ViewHolder) holder).updateCountdownOnly(item);
                }
            }
        }
    }

    public boolean hasAnyExpired() {
        long now = System.currentTimeMillis();
        for (ProductItem item : items) {
            if (item.getUnlockTimestamp() <= now) {
                return true;
            }
        }
        return false;
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
        TextView tvHoursOfWork;
        TextView tvCountdownText;
        View layoutLocationChip;
        TextView tvLocationChip;
        MaterialButton btnDebugFastForward;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvPlatformBadge = itemView.findViewById(R.id.tvPlatformBadge);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvHoursOfWork = itemView.findViewById(R.id.tvHoursOfWork);
            tvCountdownText = itemView.findViewById(R.id.tvCountdownText);
            layoutLocationChip = itemView.findViewById(R.id.layoutLocationChip);
            tvLocationChip = itemView.findViewById(R.id.tvLocationChip);
            btnDebugFastForward = itemView.findViewById(R.id.btnDebugFastForward);
        }

        public void bind(ProductItem item) {
            tvProductTitle.setText(item.getTitle());
            tvProductPrice.setText(item.getFormattedPrice());
            tvPlatformBadge.setText(item.getSourcePlatform() != null ? item.getSourcePlatform() : "Online Store");

            android.content.SharedPreferences prefs = context.getSharedPreferences(com.project.norushcart.location.LocationHelper.PREFS_NAME, Context.MODE_PRIVATE);
            float hourlyWage = prefs.getFloat("hourly_wage", 0f);
            if (hourlyWage > 0 && item.getPrice() > 0 && tvHoursOfWork != null) {
                double hours = item.getPrice() / hourlyWage;
                tvHoursOfWork.setText(String.format(java.util.Locale.getDefault(), "· %.1fh of work", hours));
                tvHoursOfWork.setVisibility(View.VISIBLE);
            } else if (tvHoursOfWork != null) {
                tvHoursOfWork.setVisibility(View.GONE);
            }

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.bg_privacy_pill)
                        .error(R.drawable.bg_privacy_pill)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.bg_privacy_pill);
            }

            tvCountdownText.setText("⏳ " + item.getRemainingTimeFormatted());

            if (item.hasLocation()) {
                layoutLocationChip.setVisibility(View.VISIBLE);
                if (item.getPlaceLabel() != null && !item.getPlaceLabel().isEmpty()) {
                    tvLocationChip.setText("📍 " + item.getPlaceLabel());
                } else {
                    tvLocationChip.setText("📍 Geotagged");
                }
            } else {
                layoutLocationChip.setVisibility(View.GONE);
            }

            btnDebugFastForward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFastForward(item);
                }
            });
        }

        public void updateCountdownOnly(ProductItem item) {
            if (tvCountdownText != null && item != null) {
                tvCountdownText.setText("⏳ " + item.getRemainingTimeFormatted());
            }
        }

    }
}
