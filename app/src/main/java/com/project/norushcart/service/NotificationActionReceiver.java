package com.project.norushcart.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_DISCARD = "com.project.norushcart.action.DISCARD";
    public static final String EXTRA_ITEM_ID = "extra_item_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_DISCARD.equals(intent.getAction())) return;

        int itemId = intent.getIntExtra(EXTRA_ITEM_ID, -1);
        if (itemId != -1) {
            DatabaseHelper db = DatabaseHelper.getInstance(context);
            ProductItem item = db.getItemById(itemId);
            db.updateItemStatus(itemId, ProductItem.STATUS_DISCARDED);

            // Dismiss the notification
            NotificationManagerCompat.from(context).cancel(itemId);

            String message = (item != null)
                    ? "Resisted & Saved " + item.getFormattedPrice() + "! High five."
                    : "Item discarded and saved to dashboard.";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
