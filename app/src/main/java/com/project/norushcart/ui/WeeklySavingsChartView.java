package com.project.norushcart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeeklySavingsChartView extends View {

    public static class BarData {
        public String label;
        public double amount;
        public boolean isCurrent;

        public BarData(String label, double amount, boolean isCurrent) {
            this.label = label;
            this.amount = amount;
            this.isCurrent = isCurrent;
        }
    }

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private List<BarData> data = new ArrayList<>();

    public WeeklySavingsChartView(Context context) {
        super(context);
        init();
    }

    public WeeklySavingsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeeklySavingsChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(0xFFF2F0FA);
        textPaint.setTextSize(spToPx(10));
        textPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(0xFFA9A3C9);
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Default placeholder data
        for (int i = 1; i <= 6; i++) {
            data.add(new BarData("W" + i, 0.0, i == 6));
        }
    }

    public void setWeeklyData(List<BarData> newData) {
        if (newData != null && !newData.isEmpty()) {
            this.data = new ArrayList<>(newData);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int count = data.size();
        if (count == 0) return;

        float width = getWidth();
        float height = getHeight();

        float paddingBottom = dpToPx(24);
        float paddingTop = dpToPx(24);
        float chartHeight = height - paddingTop - paddingBottom;

        double maxAmount = 1000.0;
        for (BarData b : data) {
            if (b.amount > maxAmount) {
                maxAmount = b.amount;
            }
        }

        float colWidth = width / count;
        float barWidth = Math.min(colWidth * 0.45f, dpToPx(32));
        float cornerRadius = dpToPx(8);

        for (int i = 0; i < count; i++) {
            BarData item = data.get(i);
            float cx = i * colWidth + (colWidth / 2f);

            float barH = (float) (chartHeight * (item.amount / maxAmount));
            if (barH < dpToPx(6)) {
                barH = dpToPx(6); // Minimal baseline height so empty weeks still render subtle bar
            }

            float top = paddingTop + (chartHeight - barH);
            float bottom = paddingTop + chartHeight;
            float left = cx - (barWidth / 2f);
            float right = cx + (barWidth / 2f);

            barRect.set(left, top, right, bottom);

            if (item.isCurrent) {
                LinearGradient gradient = new LinearGradient(
                        left, top, right, bottom,
                        0xFF00F0FF, 0xFF3B82F6, Shader.TileMode.CLAMP
                );
                barPaint.setShader(gradient);
            } else {
                barPaint.setShader(null);
                barPaint.setColor(0xFF322575);
            }

            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);

            // Value text above bar
            String valStr;
            if (item.amount >= 1000) {
                valStr = String.format(Locale.getDefault(), "₹%.1fk", item.amount / 1000.0);
            } else if (item.amount > 0) {
                valStr = String.format(Locale.getDefault(), "₹%.0f", item.amount);
            } else {
                valStr = "—";
            }

            textPaint.setColor(item.isCurrent ? 0xFF00F0FF : 0xFF7F77A8);
            canvas.drawText(valStr, cx, top - dpToPx(6), textPaint);

            // Label text below bar
            labelPaint.setColor(item.isCurrent ? 0xFF00F0FF : 0xFFA9A3C9);
            labelPaint.setTypeface(item.isCurrent ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            canvas.drawText(item.label, cx, bottom + dpToPx(16), labelPaint);

        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
