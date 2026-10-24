package com.project.norushcart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates custom, theme-coherent Vector/Canvas Map Markers for Google Maps.
 * Eliminates standard Android default balloons and rainbow cluster circles.
 */
public class MapMarkerHelper {

    private static final Map<String, BitmapDescriptor> markerCache = new HashMap<>();

    public static BitmapDescriptor getHotspotMarker(@NonNull Context context, boolean isLateNight, boolean isUnlocked) {
        String cacheKey = "hotspot_" + isLateNight + "_" + isUnlocked;
        BitmapDescriptor cached = markerCache.get(cacheKey);
        if (cached != null) return cached;

        int accentColor;
        String glyph;
        if (isLateNight) {
            accentColor = 0xFFEC4899; // Electric Fuchsia
            glyph = "🌙";
        } else if (isUnlocked) {
            accentColor = 0xFF34D399; // Mint Green
            glyph = "✨";
        } else {
            accentColor = 0xFF00F0FF; // Electric Cyan
            glyph = "⚡";
        }

        BitmapDescriptor descriptor = createGlowPin(context, accentColor, glyph);
        markerCache.put(cacheKey, descriptor);
        return descriptor;
    }

    public static BitmapDescriptor getClusterMarker(@NonNull Context context, int count) {
        String cacheKey = "cluster_" + (count > 99 ? "99+" : String.valueOf(count));
        BitmapDescriptor cached = markerCache.get(cacheKey);
        if (cached != null) return cached;

        int sizeDp = count < 10 ? 44 : (count < 50 ? 50 : 56);
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float center = sizePx / 2f;
        float outerRadius = center;
        float coreRadius = outerRadius * 0.72f;

        // 1. Ambient Iridescent Glow Ring
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(0xFF00F0FF);
        glowPaint.setAlpha(40);
        canvas.drawCircle(center, center, outerRadius, glowPaint);

        // 2. Sanctuary Dark Canvas Core
        Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(0xFF130C35);
        canvas.drawCircle(center, center, coreRadius, corePaint);

        // 3. Sharp Cyan-to-Fuchsia Border Ring
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * density);
        ringPaint.setColor(count > 10 ? 0xFFEC4899 : 0xFF00F0FF);
        canvas.drawCircle(center, center, coreRadius, ringPaint);

        // 4. Tabular Item Count Text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFF2F0FA);
        textPaint.setTextSize((sizeDp >= 50 ? 15 : 13) * context.getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        String label = count > 99 ? "99+" : String.valueOf(count);
        float yPos = center - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(label, center, yPos, textPaint);

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerCache.put(cacheKey, descriptor);
        return descriptor;
    }

    public static BitmapDescriptor getStoreMarker(@NonNull Context context) {
        String cacheKey = "store_marker";
        BitmapDescriptor cached = markerCache.get(cacheKey);
        if (cached != null) return cached;

        BitmapDescriptor descriptor = createGlowPin(context, 0xFFF59E0B, "🏬");
        markerCache.put(cacheKey, descriptor);
        return descriptor;
    }

    private static BitmapDescriptor createGlowPin(Context context, int accentColor, String glyph) {
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (44 * density);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float center = sizePx / 2f;

        // Outer soft glow
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(accentColor);
        glowPaint.setAlpha(50);
        canvas.drawCircle(center, center, center, glowPaint);

        // Solid surface core
        Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        corePaint.setColor(0xFF130C35);
        canvas.drawCircle(center, center, center * 0.68f, corePaint);

        // Crisp ring
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.2f * density);
        ringPaint.setColor(accentColor);
        canvas.drawCircle(center, center, center * 0.68f, ringPaint);

        // Center glyph
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(13 * context.getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTextAlign(Paint.Align.CENTER);
        float yPos = center - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(glyph, center, yPos, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
