package com.example.project_1v2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sensor_repository.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_READINGS = "readings";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_TEMP = "temperature";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_LUMINOSITY = "luminosity";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_READINGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TIMESTAMP + " TEXT, " +
                    COLUMN_TEMP + " REAL, " +
                    COLUMN_HUMIDITY + " REAL, " +
                    COLUMN_LUMINOSITY + " REAL" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READINGS);
        onCreate(db);
    }

    /**
     * Inserts a new reading and keeps only the last 10 entries in persistent storage.
     */
    public synchronized void addReading(double temp, double humidity, double luminosity, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_TEMP, temp);
        values.put(COLUMN_HUMIDITY, humidity);
        values.put(COLUMN_LUMINOSITY, luminosity);

        db.insert(TABLE_READINGS, null, values);

        // Retain only the 10 most recent readings
        String trimQuery = "DELETE FROM " + TABLE_READINGS +
                " WHERE " + COLUMN_ID + " NOT IN (" +
                " SELECT " + COLUMN_ID + " FROM " + TABLE_READINGS +
                " ORDER BY " + COLUMN_ID + " DESC LIMIT 10" +
                " );";
        db.execSQL(trimQuery);
    }

    /**
     * Retrieves the last 10 sensor readings ordered by most recent first.
     */
    public synchronized List<ReadingItem> getRecentReadings() {
        List<ReadingItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_READINGS + " ORDER BY " + COLUMN_ID + " DESC LIMIT 10",
                null
        );

        if (cursor.moveToFirst()) {
            int timeIdx = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            int tempIdx = cursor.getColumnIndex(COLUMN_TEMP);
            int humIdx = cursor.getColumnIndex(COLUMN_HUMIDITY);
            int lumIdx = cursor.getColumnIndex(COLUMN_LUMINOSITY);

            do {
                String timestamp = cursor.getString(timeIdx);
                double temp = cursor.getDouble(tempIdx);
                double hum = cursor.getDouble(humIdx);
                double lum = cursor.getDouble(lumIdx);
                list.add(new ReadingItem(timestamp, temp, hum, lum));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Clears all saved readings.
     */
    public synchronized void clearRepository() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_READINGS);
    }
}
