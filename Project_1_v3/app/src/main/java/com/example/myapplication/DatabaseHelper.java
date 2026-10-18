package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sensor_repository.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE = "sensor_readings";

    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String LUMINOSITY = "luminosity";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "sensor_type TEXT NOT NULL, " +
                        "value REAL NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        // Reserved for future database structure changes.
    }


    public void insertReading(
            String type,
            float value,
            long timestamp) {

        validateSensorType(type);

        SQLiteDatabase db = getWritableDatabase();

        // Ignore consecutive readings with the same displayed value.
        Float lastValue = getLastStoredValue(db, type);

        if (lastValue != null &&
                hasSameDisplayedValue(type, lastValue, value)) {

            return;
        }

        db.beginTransaction();

        try {

            ContentValues values = new ContentValues();

            values.put("sensor_type", type);
            values.put("value", value);
            values.put("timestamp", timestamp);

            db.insertOrThrow(
                    TABLE,
                    null,
                    values
            );

            db.execSQL(
                    "DELETE FROM " + TABLE +
                            " WHERE sensor_type = ? AND id NOT IN (" +
                            "SELECT id FROM " + TABLE +
                            " WHERE sensor_type = ?" +
                            " ORDER BY timestamp DESC, id DESC LIMIT 10)",
                    new Object[]{type, type}
            );

            db.setTransactionSuccessful();

        } finally {

            db.endTransaction();
        }
    }

    private Float getLastStoredValue(
            SQLiteDatabase db,
            String type) {

        try (Cursor cursor = db.query(
                TABLE,
                new String[]{"value"},
                "sensor_type = ?",
                new String[]{type},
                null,
                null,
                "timestamp DESC, id DESC",
                "1")) {

            if (cursor.moveToFirst()) {
                return cursor.getFloat(0);
            }
        }

        return null;
    }


    private boolean hasSameDisplayedValue(
            String type,
            float oldValue,
            float newValue) {

        if (TEMPERATURE.equals(type)) {

            // Temperature is displayed with one decimal place.
            String oldFormatted = String.format(
                    Locale.ROOT,
                    "%.1f",
                    oldValue
            );

            String newFormatted = String.format(
                    Locale.ROOT,
                    "%.1f",
                    newValue
            );

            return oldFormatted.equals(newFormatted);
        }

        // Humidity and luminosity are displayed
        // without decimal places.
        String oldFormatted = String.format(
                Locale.ROOT,
                "%.0f",
                oldValue
        );

        String newFormatted = String.format(
                Locale.ROOT,
                "%.0f",
                newValue
        );

        return oldFormatted.equals(newFormatted);
    }


    public List<SensorReading> getLastReadings(String type) {

        validateSensorType(type);

        List<SensorReading> readings =
                new ArrayList<>();

        SQLiteDatabase db =
                getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE,
                new String[]{
                        "value",
                        "timestamp"
                },
                "sensor_type = ?",
                new String[]{type},
                null,
                null,
                "timestamp DESC, id DESC",
                "10")) {

            while (cursor.moveToNext()) {

                float value =
                        cursor.getFloat(0);

                long timestamp =
                        cursor.getLong(1);

                readings.add(
                        new SensorReading(
                                value,
                                timestamp
                        )
                );
            }
        }

        return readings;
    }


    public void clearReadings() {

        SQLiteDatabase db =
                getWritableDatabase();

        db.delete(
                TABLE,
                null,
                null
        );
    }


    public void deleteReadings(String sensorType) {

        validateSensorType(sensorType);

        SQLiteDatabase db =
                getWritableDatabase();

        db.delete(
                TABLE,
                "sensor_type = ?",
                new String[]{sensorType}
        );
    }


    private void validateSensorType(String type) {

        if (!TEMPERATURE.equals(type)
                && !HUMIDITY.equals(type)
                && !LUMINOSITY.equals(type)) {

            throw new IllegalArgumentException(
                    "Invalid sensor type"
            );
        }
    }
}