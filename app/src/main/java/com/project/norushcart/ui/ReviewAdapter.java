package com.project.norushcart.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    public interface OnReviewActionListener {
        void onDiscard(ProductItem item, int position);
        void onBuyNow(ProductItem item);
        void onFindInStore(ProductItem item);
    }

    private final Context context;
    private List<ProductItem> items = new ArrayList<>();
    private final OnReviewActionListener listener;

    public ReviewAdapter(Context context, OnReviewActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ProductItem> newItems) {
        this.items = (newItems != null) ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem item = items.get(position);
        holder.bind(item, position);
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
        View layoutLocationChipReview;
        TextView tvLocationChipReview;
        MaterialButton btnFindInStore;
        MaterialButton btnDiscard;
        MaterialButton btnBuyNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvPlatformBadge = itemView.findViewById(R.id.tvPlatformBadge);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            layoutLocationChipReview = itemView.findViewById(R.id.layoutLocationChipReview);
            tvLocationChipReview = itemView.findViewById(R.id.tvLocationChipReview);
            btnFindInStore = itemView.findViewById(R.id.btnFindInStore);
            btnDiscard = itemView.findViewById(R.id.btnDiscard);
            btnBuyNow = itemView.findViewById(R.id.btnBuyNow);
        }

        public void bind(ProductItem item, int position) {
            tvProductTitle.setText(item.getTitle());
            tvProductPrice.setText(item.getFormattedPrice());
            tvProductPrice.setOnClickListener(v -> {
                PriceDialogHelper.showEditPriceDialog(context, item, () -> {
                    tvProductPrice.setText(item.getFormattedPrice());
                });
            });
            tvPlatformBadge.setText(item.getSourcePlatform() != null ? item.getSourcePlatform() : "Online Store");

            GlideHelper.loadProductImage(context, item.getImageUrl(), ivProductImage);

            if (item.hasLocation()) {
                layoutLocationChipReview.setVisibility(View.VISIBLE);
                if (item.getPlaceLabel() != null && !item.getPlaceLabel().isEmpty()) {
                    tvLocationChipReview.setText("📍 " + item.getPlaceLabel());
                } else {
                    tvLocationChipReview.setText("📍 Geotagged");
                }
            } else {
                layoutLocationChipReview.setVisibility(View.GONE);
            }

            btnFindInStore.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFindInStore(item);
                }
            });

            btnDiscard.setOnClickListener(v -> {
                DatabaseHelper.getInstance(context).updateItemStatus(item.getId(), ProductItem.STATUS_DISCARDED);
                if (listener != null) {
                    listener.onDiscard(item, getBindingAdapterPosition());
                }
            });

            btnBuyNow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBuyNow(item);
                } else {
                    Intent intent = new Intent(context, CheckoutWebViewActivity.class);
                    intent.putExtra(CheckoutWebViewActivity.EXTRA_ITEM_ID, item.getId());
                    intent.putExtra(CheckoutWebViewActivity.EXTRA_URL, item.getOriginalUrl());
                    intent.putExtra(CheckoutWebViewActivity.EXTRA_TITLE, item.getTitle());
                    context.startActivity(intent);
                }
            });
        }
    }
}

