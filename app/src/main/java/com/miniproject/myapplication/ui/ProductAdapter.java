package com.miniproject.myapplication.ui;

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
import com.miniproject.myapplication.model.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnItemActionListener {
        void onBuyNow(ProductItem item);
        void onDiscard(ProductItem item);
        void onDebugFastForward(ProductItem item);
    }

    private final Context context;
    private List<ProductItem> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public ProductAdapter(Context context, OnItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ProductItem> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {

        ImageView ivProductImage;
        TextView tvPlatform;
        TextView tvStatusBadge;
        TextView tvProductTitle;
        TextView tvProductPrice;
        TextView tvCooldownTime;
        MaterialButton btnDebugFastForward;
        MaterialButton btnDiscard;
        MaterialButton btnBuyNow;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCooldownTime = itemView.findViewById(R.id.tvCooldownTime);
            btnDebugFastForward = itemView.findViewById(R.id.btnDebugFastForward);
            btnDiscard = itemView.findViewById(R.id.btnDiscard);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
        }

        public void bind(ProductItem item) {
            tvProductTitle.setText(item.getTitle());
            tvProductPrice.setText(item.getFormattedPrice());
            tvPlatform.setText(item.getSourcePlatform());

            // Image loading via Glide
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.ic_launcher_background);
            }

            // Configure state views based on item status
            if (item.isLocked()) {
                tvStatusBadge.setText("LOCKED (48H QUEUE)");
                tvStatusBadge.setTextColor(Color.parseColor("#D93025"));

                tvCooldownTime.setVisibility(View.VISIBLE);
                tvCooldownTime.setText("⏳ " + item.getRemainingTimeFormatted());

                btnDebugFastForward.setVisibility(View.VISIBLE);
                btnDiscard.setVisibility(View.GONE);
                btnBuyNow.setVisibility(View.GONE);

                btnDebugFastForward.setOnClickListener(v -> {
                    if (listener != null) listener.onDebugFastForward(item);
                });

            } else if (ProductItem.STATUS_UNLOCKED.equalsIgnoreCase(item.getStatus()) || !item.isLocked() && ProductItem.STATUS_LOCKED.equalsIgnoreCase(item.getStatus())) {
                tvStatusBadge.setText("READY FOR REVIEW");
                tvStatusBadge.setTextColor(Color.parseColor("#188038"));

                tvCooldownTime.setVisibility(View.VISIBLE);
                tvCooldownTime.setText("✅ 48h Cooldown Expired! Make your decision.");
                tvCooldownTime.setBackgroundColor(Color.parseColor("#E6F4EA"));
                tvCooldownTime.setTextColor(Color.parseColor("#137333"));

                btnDebugFastForward.setVisibility(View.GONE);
                btnDiscard.setVisibility(View.VISIBLE);
                btnBuyNow.setVisibility(View.VISIBLE);

                btnBuyNow.setOnClickListener(v -> {
                    if (listener != null) listener.onBuyNow(item);
                });

                btnDiscard.setOnClickListener(v -> {
                    if (listener != null) listener.onDiscard(item);
                });

            } else if (ProductItem.STATUS_DISCARDED.equalsIgnoreCase(item.getStatus())) {
                tvStatusBadge.setText("DISCARDED (SAVED)");
                tvStatusBadge.setTextColor(Color.parseColor("#1A73E8"));

                tvCooldownTime.setVisibility(View.VISIBLE);
                tvCooldownTime.setText("🎉 Saved " + item.getFormattedPrice() + " by avoiding impulse purchase!");
                tvCooldownTime.setBackgroundColor(Color.parseColor("#E8F0FE"));
                tvCooldownTime.setTextColor(Color.parseColor("#1A73E8"));

                btnDebugFastForward.setVisibility(View.GONE);
                btnDiscard.setVisibility(View.GONE);
                btnBuyNow.setVisibility(View.GONE);
            } else {
                tvStatusBadge.setText(item.getStatus());
                tvCooldownTime.setVisibility(View.GONE);
                btnDebugFastForward.setVisibility(View.GONE);
                btnDiscard.setVisibility(View.GONE);
                btnBuyNow.setVisibility(View.GONE);
            }
        }
    }
}
