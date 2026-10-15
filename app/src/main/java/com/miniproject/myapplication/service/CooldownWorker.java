package com.miniproject.myapplication.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.miniproject.myapplication.database.DatabaseHelper;
import com.miniproject.myapplication.model.ProductItem;

public class CooldownWorker extends Worker {

    public static final String KEY_ITEM_ID = "item_id";
    private static final String TAG = "CooldownWorker";

    public CooldownWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int itemId = getInputData().getInt(KEY_ITEM_ID, -1);
        Log.d(TAG, "Executing CooldownWorker for itemId: " + itemId);

        if (itemId == -1) {
            return Result.failure();
        }

        Context context = getApplicationContext();

        // 1. Update database status
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        dbHelper.updateItemStatus(itemId, ProductItem.STATUS_UNLOCKED);

        // 2. Send explicit Broadcast to trigger Notification
        Intent broadcastIntent = new Intent(context, CooldownReceiver.class);
        broadcastIntent.setAction(CooldownReceiver.ACTION_COOLDOWN_EXPIRED);
        broadcastIntent.putExtra(KEY_ITEM_ID, itemId);
        context.sendBroadcast(broadcastIntent);

        return Result.success();
    }
}
