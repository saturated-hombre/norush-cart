package com.project.norushcart.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.miniproject.myapplication.MainActivity;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

public class CooldownReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "norush_cooldown_channel";
    private static final String TAG = "CooldownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int itemId = intent.getIntExtra(CooldownWorker.KEY_ITEM_ID, -1);
        Log.d(TAG, "onReceive triggered for item_id: " + itemId);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        if (itemId != -1) {
            dbHelper.updateItemStatus(itemId, ProductItem.STATUS_UNLOCKED);
        }

        ProductItem item = (itemId != -1) ? dbHelper.getItemById(itemId) : null;

        createNotificationChannel(context);
        sendNotification(context, item, itemId);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Cooldown Alerts";
            String description = "Notifies when 48-hour item quarantine expires";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification(Context context, ProductItem item, int itemId) {
        // Notification permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping notification.");
                return;
            }
        }

        Intent targetIntent = new Intent(context, MainActivity.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        targetIntent.putExtra("SELECT_TAB", 1); // Open "Ready for Review" tab

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                itemId > 0 ? itemId : (int) System.currentTimeMillis(),
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Cooldown Complete: Re-evaluate Purchase";
        String contentText = (item != null && item.getTitle() != null)
                ? "Your 48-hour buffer has ended for " + item.getTitle() + ". Do you still need this item?"
                : "Your 48-hour buffer has ended. Do you still need this item?";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(itemId > 0 ? itemId : (int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException sending notification: " + e.getMessage());
        }
    }
}
