package com.miniproject.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.miniproject.myapplication.database.DatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {

    private static final String TEST_DB_NAME = "test_migration.db";
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB_NAME);
    }

    @Test
    public void testUpgradeV2toV3_preservesData_andCreatesNewSchema() {
        // Step 1: Create a V2 Database manually
        SQLiteDatabase dbV2 = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath(TEST_DB_NAME), null);
        dbV2.setVersion(2);

        String CREATE_V2_TABLE = "CREATE TABLE " + DatabaseHelper.TABLE_COOLDOWN + " ("
                + DatabaseHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DatabaseHelper.COLUMN_TITLE + " TEXT NOT NULL, "
                + DatabaseHelper.COLUMN_IMAGE_URL + " TEXT, "
                + DatabaseHelper.COLUMN_PRICE + " REAL NOT NULL, "
                + DatabaseHelper.COLUMN_SOURCE_PLATFORM + " TEXT, "
                + DatabaseHelper.COLUMN_ORIGINAL_URL + " TEXT NOT NULL, "
                + DatabaseHelper.COLUMN_TIMESTAMP_ADDED + " INTEGER, "
                + DatabaseHelper.COLUMN_UNLOCK_TIMESTAMP + " INTEGER, "
                + DatabaseHelper.COLUMN_STATUS + " TEXT NOT NULL, "
                + DatabaseHelper.COLUMN_LATITUDE + " REAL DEFAULT 0.0, "
                + DatabaseHelper.COLUMN_LONGITUDE + " REAL DEFAULT 0.0)";
        dbV2.execSQL(CREATE_V2_TABLE);

        // Insert Item 1: default 0.0 coordinates (simulating old no-location bug)
        ContentValues v1 = new ContentValues();
        v1.put(DatabaseHelper.COLUMN_TITLE, "Old Ungeotagged Item");
        v1.put(DatabaseHelper.COLUMN_PRICE, 999.0);
        v1.put(DatabaseHelper.COLUMN_ORIGINAL_URL, "https://amazon.in/dp/test1");
        v1.put(DatabaseHelper.COLUMN_STATUS, "LOCKED");
        v1.put(DatabaseHelper.COLUMN_LATITUDE, 0.0);
        v1.put(DatabaseHelper.COLUMN_LONGITUDE, 0.0);
        long id1 = dbV2.insert(DatabaseHelper.TABLE_COOLDOWN, null, v1);
        assertTrue(id1 > 0);

        // Insert Item 2: valid coordinates
        ContentValues v2 = new ContentValues();
        v2.put(DatabaseHelper.COLUMN_TITLE, "Old Geotagged Item");
        v2.put(DatabaseHelper.COLUMN_PRICE, 1499.0);
        v2.put(DatabaseHelper.COLUMN_ORIGINAL_URL, "https://amazon.in/dp/test2");
        v2.put(DatabaseHelper.COLUMN_STATUS, "LOCKED");
        v2.put(DatabaseHelper.COLUMN_LATITUDE, 12.9716);
        v2.put(DatabaseHelper.COLUMN_LONGITUDE, 77.5946);
        long id2 = dbV2.insert(DatabaseHelper.TABLE_COOLDOWN, null, v2);
        assertTrue(id2 > 0);

        // Step 2: Run onUpgrade(2, 3)
        DatabaseHelper helper = new DatabaseHelper(context) {
            // We can invoke onUpgrade directly on our test database
        };
        helper.onUpgrade(dbV2, 2, 3);
        dbV2.setVersion(3);

        // Step 3: Verify Item 1 has NULL for lat/lng (0.0 cleaned up)
        Cursor c1 = dbV2.query(DatabaseHelper.TABLE_COOLDOWN, null,
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id1)},
                null, null, null);
        assertNotNull(c1);
        assertTrue(c1.moveToFirst());
        int latIdx = c1.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE);
        int lngIdx = c1.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE);
        int accIdx = c1.getColumnIndex(DatabaseHelper.COLUMN_LOCATION_ACCURACY);
        int labelIdx = c1.getColumnIndex(DatabaseHelper.COLUMN_PLACE_LABEL);

        assertTrue("Old 0.0 latitude should be migrated to NULL", c1.isNull(latIdx));
        assertTrue("Old 0.0 longitude should be migrated to NULL", c1.isNull(lngIdx));
        assertTrue("location_accuracy should exist and be NULL", c1.isNull(accIdx));
        assertTrue("place_label should exist and be NULL", c1.isNull(labelIdx));
        c1.close();

        // Step 4: Verify Item 2 preserved its actual coordinates
        Cursor c2 = dbV2.query(DatabaseHelper.TABLE_COOLDOWN, null,
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id2)},
                null, null, null);
        assertNotNull(c2);
        assertTrue(c2.moveToFirst());
        assertFalse(c2.isNull(latIdx));
        assertFalse(c2.isNull(lngIdx));
        assertEquals(12.9716, c2.getDouble(latIdx), 0.0001);
        assertEquals(77.5946, c2.getDouble(lngIdx), 0.0001);
        c2.close();

        // Step 5: Verify places table exists and works
        ContentValues placeValues = new ContentValues();
        placeValues.put(DatabaseHelper.COLUMN_PLACE_LAT_ROUNDED, 12.972);
        placeValues.put(DatabaseHelper.COLUMN_PLACE_LNG_ROUNDED, 77.595);
        placeValues.put(DatabaseHelper.COLUMN_PLACE_LABEL_NAME, "Coffee Shop");
        long placeId = dbV2.insert(DatabaseHelper.TABLE_PLACES, null, placeValues);
        assertTrue("places table should accept inserts", placeId > 0);

        // Step 6: Run onUpgrade(3, 4)
        helper.onUpgrade(dbV2, 3, 4);
        dbV2.setVersion(4);

        // Step 7: Verify nearby_stores_cache table exists and works
        ContentValues storeValues = new ContentValues();
        storeValues.put(DatabaseHelper.COLUMN_STORE_ITEM_ID, 1);
        storeValues.put(DatabaseHelper.COLUMN_STORE_QUERY, "Apple store");
        storeValues.put(DatabaseHelper.COLUMN_STORE_TIMESTAMP, System.currentTimeMillis());
        storeValues.put(DatabaseHelper.COLUMN_STORE_JSON, "[]");
        storeValues.put(DatabaseHelper.COLUMN_STORE_LAT, 12.9716);
        storeValues.put(DatabaseHelper.COLUMN_STORE_LNG, 77.5946);
        long storeCacheId = dbV2.insert(DatabaseHelper.TABLE_NEARBY_STORES, null, storeValues);
        assertTrue("nearby_stores_cache table should accept inserts", storeCacheId > 0);

        dbV2.close();
    }
}
