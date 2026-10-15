package com.miniproject.myapplication.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.miniproject.myapplication.MainActivity;
import com.miniproject.myapplication.R;
import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.model.ProductItem;

public class CooldownReceiver extends BroadcastReceiver {

    public static final String ACTION_COOLDOWN_EXPIRED = "com.miniproject.myapplication.ACTION_COOLDOWN_EXPIRED";
    public static final String CHANNEL_ID = "norush_cart_cooldown_channel";
    private static final String TAG = "CooldownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int itemId = intent.getIntExtra(CooldownWorker.KEY_ITEM_ID, -1);
        Log.d(TAG, "onReceive triggered for itemId: " + itemId);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        if (itemId != -1) {
            dbHelper.updateItemStatus(itemId, ProductItem.STATUS_UNLOCKED);
        }

        ProductItem item = dbHelper.getItemById(itemId);

        createNotificationChannel(context);
        sendNotification(context, item, itemId);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NoRush Cart Cooldown Alerts";
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
        Intent targetIntent = new Intent(context, MainActivity.class);
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        targetIntent.putExtra("SELECT_TAB", 1); // Open Tab 2 (Ready for Review)

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                itemId > 0 ? itemId : (int) System.currentTimeMillis(),
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Cooldown Ended: Time to Decide!";
        String contentText = (item != null && item.getTitle() != null)
                ? "Quarantine expired for: " + item.getTitle()
                : "An item in your queue is ready for review!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                notificationManager.notify(itemId > 0 ? itemId : (int) System.currentTimeMillis(), builder.build());
            } catch (SecurityException e) {
                Log.e(TAG, "Notification permission missing: " + e.getMessage());
            }
        }
    }
}
