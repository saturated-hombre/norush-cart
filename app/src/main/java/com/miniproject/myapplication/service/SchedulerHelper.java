package com.miniproject.myapplication.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.model.ProductItem;

import java.util.concurrent.TimeUnit;

public class SchedulerHelper {

    private static final String TAG = "SchedulerHelper";

    public static void scheduleCooldown(Context context, ProductItem item) {
        if (item == null || item.getId() <= 0) return;

        long delayMs = item.getUnlockTimestamp() - System.currentTimeMillis();
        if (delayMs < 0) {
            delayMs = 0;
        }

        Data inputData = new Data.Builder()
                .putInt(CooldownWorker.KEY_ITEM_ID, item.getId())
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CooldownWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("cooldown_work_" + item.getId())
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                "cooldown_work_" + item.getId(),
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Scheduled cooldown for item " + item.getId() + " in " + (delayMs / 1000) + " seconds.");
    }

    public static void scheduleDebugFastForward(Context context, int itemId) {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        long newUnlockTimestamp = dbHelper.debugFastForwardCooldown(itemId);

        long delayMs = newUnlockTimestamp - System.currentTimeMillis();
        if (delayMs < 0) delayMs = 0;

        Data inputData = new Data.Builder()
                .putInt(CooldownWorker.KEY_ITEM_ID, itemId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CooldownWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("cooldown_work_" + itemId)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                "cooldown_work_" + itemId,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Debug fast-forward scheduled for item " + itemId + " in " + (delayMs / 1000) + " seconds.");
    }
}
