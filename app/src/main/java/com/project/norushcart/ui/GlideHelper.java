package com.project.norushcart.ui;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.miniproject.myapplication.R;

public class GlideHelper {

    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    public static void loadProductImage(Context context, String imageUrl, ImageView imageView) {
        if (context == null || imageView == null) return;

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                GlideUrl glideUrl = new GlideUrl(
                        imageUrl.trim(),
                        new LazyHeaders.Builder()
                                .addHeader("User-Agent", DESKTOP_USER_AGENT)
                                .build()
                );

                Glide.with(context)
                        .load(glideUrl)
                        .placeholder(R.drawable.ic_product_placeholder)
                        .error(R.drawable.ic_product_placeholder)
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_product_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_product_placeholder);
        }
    }
}
