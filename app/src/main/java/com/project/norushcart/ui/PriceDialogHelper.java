package com.project.norushcart.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.material.button.MaterialButton;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

public class PriceDialogHelper {

    public static void showEditPriceDialog(Context context, ProductItem item, Runnable onPriceUpdated) {
        if (context == null || item == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_price, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setAttributes(params);
        }

        final EditText input = view.findViewById(R.id.etEditPriceInput);
        final MaterialButton btnCancel = view.findViewById(R.id.btnCancelPrice);
        final MaterialButton btnSave = view.findViewById(R.id.btnSavePrice);

        if (item.getPrice() > 0) {
            input.setText(String.valueOf((long) item.getPrice()));
            input.setSelection(input.getText().length());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                try {
                    double newPrice = Double.parseDouble(val.replace(",", ""));
                    if (newPrice > 0) {
                        item.setPrice(newPrice);
                        DatabaseHelper.getInstance(context).updateItemPrice(item.getId(), newPrice);
                        if (onPriceUpdated != null) {
                            onPriceUpdated.run();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
