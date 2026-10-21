package com.project.norushcart.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class CooldownWorker extends Worker {

    public static final String KEY_ITEM_ID = "ITEM_ID";
    private static final String TAG = "CooldownWorker";

    public CooldownWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int itemId = getInputData().getInt(KEY_ITEM_ID, -1);
        Log.d(TAG, "Executing CooldownWorker for ITEM_ID: " + itemId);

        if (itemId == -1) {
            return Result.failure();
        }

        Context context = getApplicationContext();

        // Send explicit Intent to CooldownReceiver
        Intent intent = new Intent(context, CooldownReceiver.class);
        intent.putExtra(KEY_ITEM_ID, itemId);
        context.sendBroadcast(intent);

        return Result.success();
    }
}
