package com.miniproject.myapplication.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.project.norushcart.data.ProductItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "norush_cart.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_COOLDOWN = "cooldown_items";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_SOURCE_PLATFORM = "source_platform";
    public static final String COLUMN_ORIGINAL_URL = "original_url";
    public static final String COLUMN_TIMESTAMP_ADDED = "timestamp_added";
    public static final String COLUMN_UNLOCK_TIMESTAMP = "unlock_timestamp";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_COOLDOWN + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_IMAGE_URL + " TEXT, "
                + COLUMN_PRICE + " REAL NOT NULL, "
                + COLUMN_SOURCE_PLATFORM + " TEXT, "
                + COLUMN_ORIGINAL_URL + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP_ADDED + " INTEGER, "
                + COLUMN_UNLOCK_TIMESTAMP + " INTEGER, "
                + COLUMN_STATUS + " TEXT NOT NULL, "
                + COLUMN_LATITUDE + " REAL DEFAULT 0.0, "
                + COLUMN_LONGITUDE + " REAL DEFAULT 0.0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_LATITUDE + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_COOLDOWN + " ADD COLUMN " + COLUMN_LONGITUDE + " REAL DEFAULT 0.0");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_COOLDOWN);
            onCreate(db);
        }
    }

    public long insertItem(ProductItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, item.getTitle());
        values.put(COLUMN_IMAGE_URL, item.getImageUrl());
        values.put(COLUMN_PRICE, item.getPrice());
        values.put(COLUMN_SOURCE_PLATFORM, item.getSourcePlatform());
        values.put(COLUMN_ORIGINAL_URL, item.getOriginalUrl());
        values.put(COLUMN_TIMESTAMP_ADDED, item.getTimestampAdded());
        values.put(COLUMN_UNLOCK_TIMESTAMP, item.getUnlockTimestamp());
        values.put(COLUMN_STATUS, item.getStatus());
        values.put(COLUMN_LATITUDE, item.getLatitude());
        values.put(COLUMN_LONGITUDE, item.getLongitude());

        long id = db.insert(TABLE_COOLDOWN, null, values);
        if (id != -1) {
            item.setId((int) id);
        }
        return id;
    }

    public List<ProductItem> getItemsByStatus(String status) {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Automatically auto-promote expired LOCKED items to UNLOCKED if time has passed
        if (ProductItem.STATUS_UNLOCKED.equals(status) || ProductItem.STATUS_LOCKED.equals(status)) {
            checkAndAutoUnlockExpiredItems();
        }

        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                COLUMN_STATUS + " = ?",
                new String[]{status},
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                items.add(cursorToProductItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return items;
    }

    public List<ProductItem> getAllItems() {
        List<ProductItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                null,
                null,
                null, null,
                COLUMN_TIMESTAMP_ADDED + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                items.add(cursorToProductItem(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return items;
    }

    public ProductItem getItemById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_COOLDOWN,
                null,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        ProductItem item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = cursorToProductItem(cursor);
            cursor.close();
        }
        return item;
    }

    public void updateItemStatus(int id, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, newStatus);
        db.update(TABLE_COOLDOWN, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void checkAndAutoUnlockExpiredItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, ProductItem.STATUS_UNLOCKED);
        db.update(
                TABLE_COOLDOWN,
                values,
                COLUMN_STATUS + " = ? AND " + COLUMN_UNLOCK_TIMESTAMP + " <= ?",
                new String[]{ProductItem.STATUS_LOCKED, String.valueOf(now)}
        );
    }

    public double getTotalMoneySaved() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + COLUMN_PRICE + ") FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED});
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public int getDiscardedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TABLE_COOLDOWN + " WHERE " + COLUMN_STATUS + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{ProductItem.STATUS_DISCARDED});
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public void deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_COOLDOWN, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public long setItemUnlockTimestamp(int itemId, long unlockTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_UNLOCK_TIMESTAMP, unlockTimestamp);
        values.put(COLUMN_STATUS, ProductItem.STATUS_LOCKED);
        db.update(TABLE_COOLDOWN, values, COLUMN_ID + " = ?", new String[]{String.valueOf(itemId)});
        return unlockTimestamp;
    }

    /**
     * Developer Testing Utility:
     * Sets unlock_timestamp for the item to 10 seconds from now.
     * Keeps status as LOCKED.
     */
    public long debugFastForwardCooldown(int itemId) {
        return setItemUnlockTimestamp(itemId, System.currentTimeMillis() + 10000L);
    }

    private ProductItem cursorToProductItem(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE));
        String sourcePlatform = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOURCE_PLATFORM));
        String originalUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_URL));
        long timestampAdded = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP_ADDED));
        long unlockTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UNLOCK_TIMESTAMP));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));

        int latIndex = cursor.getColumnIndex(COLUMN_LATITUDE);
        int lngIndex = cursor.getColumnIndex(COLUMN_LONGITUDE);
        double latitude = (latIndex != -1) ? cursor.getDouble(latIndex) : 0.0;
        double longitude = (lngIndex != -1) ? cursor.getDouble(lngIndex) : 0.0;

        return new ProductItem(id, title, imageUrl, price, sourcePlatform, originalUrl, timestampAdded, unlockTimestamp, status, latitude, longitude);
    }
}
