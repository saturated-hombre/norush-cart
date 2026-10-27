package com.project.norushcart.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.miniproject.myapplication.R;
import com.project.norushcart.data.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.ViewHolder> {

    private final Context context;
    private List<ProductItem> items = new ArrayList<>();

    public SavedAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<ProductItem> newItems) {
        this.items = (newItems != null) ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView ivSavedImage;
        TextView tvSavedPlatform;
        TextView tvSavedTitle;
        TextView tvSavedPrice;
        View layoutSavedLocation;
        TextView tvSavedLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSavedImage = itemView.findViewById(R.id.ivSavedImage);
            tvSavedPlatform = itemView.findViewById(R.id.tvSavedPlatform);
            tvSavedTitle = itemView.findViewById(R.id.tvSavedTitle);
            tvSavedPrice = itemView.findViewById(R.id.tvSavedPrice);
            layoutSavedLocation = itemView.findViewById(R.id.layoutSavedLocation);
            tvSavedLocation = itemView.findViewById(R.id.tvSavedLocation);
        }

        public void bind(ProductItem item) {
            tvSavedTitle.setText(item.getTitle());
            tvSavedPrice.setText("+ " + item.getFormattedPrice() + " saved");
            tvSavedPlatform.setText(item.getSourcePlatform() != null ? item.getSourcePlatform() : "Online Store");

            GlideHelper.loadProductImage(context, item.getImageUrl(), ivSavedImage);

            if (item.hasLocation()) {
                layoutSavedLocation.setVisibility(View.VISIBLE);
                if (item.getPlaceLabel() != null && !item.getPlaceLabel().isEmpty()) {
                    tvSavedLocation.setText("📍 " + item.getPlaceLabel());
                } else {
                    tvSavedLocation.setText("📍 Geotagged");
                }
            } else {
                layoutSavedLocation.setVisibility(View.GONE);
            }
        }
    }
}
