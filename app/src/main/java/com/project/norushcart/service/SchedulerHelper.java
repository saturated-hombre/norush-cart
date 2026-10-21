package com.project.norushcart.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.miniproject.myapplication.database.DatabaseHelper;
import com.project.norushcart.data.ProductItem;

import java.util.concurrent.TimeUnit;

public class SchedulerHelper {

    private static final String TAG = "SchedulerHelper";

    public static void scheduleCooldownWork(Context context, int itemId, long unlockTimestamp) {
        if (context == null || itemId <= 0) return;

        long delayMillis = unlockTimestamp - System.currentTimeMillis();
        if (delayMillis < 0) {
            delayMillis = 0;
        }

        Data inputData = new Data.Builder()
                .putInt(CooldownWorker.KEY_ITEM_ID, itemId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CooldownWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("COOLDOWN_TAG_" + itemId)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                "COOLDOWN_WORK_" + itemId,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Scheduled cooldown work for item " + itemId + " with delay " + (delayMillis / 1000) + " seconds.");
    }

    public static void scheduleCooldown(Context context, ProductItem item) {
        if (item != null) {
            scheduleCooldownWork(context, item.getId(), item.getUnlockTimestamp());
        }
    }

    public static void debugFastForward(Context context, int itemId, long seconds) {
        if (context == null || itemId <= 0) return;

        long newUnlockTimestamp = System.currentTimeMillis() + (seconds * 1000L);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        dbHelper.setItemUnlockTimestamp(itemId, newUnlockTimestamp);

        Data inputData = new Data.Builder()
                .putInt(CooldownWorker.KEY_ITEM_ID, itemId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CooldownWorker.class)
                .setInitialDelay(seconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("COOLDOWN_TAG_" + itemId)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                "COOLDOWN_WORK_" + itemId,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Debug fast-forward scheduled for item " + itemId + " in " + seconds + " seconds.");
    }

    public static void scheduleDebugFastForward(Context context, int itemId) {
        debugFastForward(context, itemId, 10L);
    }
}
