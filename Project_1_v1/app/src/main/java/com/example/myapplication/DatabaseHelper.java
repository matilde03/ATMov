package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sensor_repository.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "sensor_readings";

    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String LUMINOSITY = "luminosity";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void insertReading(String type, float value, long timestamp) {
        validateSensorType(type);
        SQLiteDatabase db = getWritableDatabase();

        Float lastValue = getLastStoredValue(db, type);
        if (lastValue != null && hasSameDisplayedValue(type, lastValue, value)) {
            return;
        }

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("sensor_type", type);
            values.put("value", value);
            values.put("timestamp", timestamp);

            db.insertOrThrow(TABLE, null, values);

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

    private Float getLastStoredValue(SQLiteDatabase db, String type) {
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

    private boolean hasSameDisplayedValue(String type, float oldValue, float newValue) {
        String format = TEMPERATURE.equals(type) ? "%.1f" : "%.0f";
        String oldFormatted = String.format(Locale.ROOT, format, oldValue);
        String newFormatted = String.format(Locale.ROOT, format, newValue);
        return oldFormatted.equals(newFormatted);
    }

    // Directly returns the formatted String for the UI instead of a List<SensorReading>
    public String getFormattedReadings(String type, String unit, int decimals) {
        validateSensorType(type);
        StringBuilder result = new StringBuilder();
        String format = (decimals == 1) ? "%.1f" : "%.0f";
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE,
                new String[]{"value", "timestamp"},
                "sensor_type = ?",
                new String[]{type},
                null,
                null,
                "timestamp DESC, id DESC",
                "10")) {

            if (!cursor.moveToFirst()) {
                return "No readings available";
            }

            do {
                String valueStr = String.format(Locale.getDefault(), format, cursor.getFloat(0));
                String dateStr = dateFormat.format(new Date(cursor.getLong(1)));
                result.append(valueStr)
                        .append(" ")
                        .append(unit)
                        .append("  |  ")
                        .append(dateStr)
                        .append("\n");
            } while (cursor.moveToNext());
        }

        return result.toString().trim();
    }

    public void clearReadings() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    public void deleteReadings(String sensorType) {
        validateSensorType(sensorType);
        getWritableDatabase().delete(TABLE, "sensor_type = ?", new String[]{sensorType});
    }

    private void validateSensorType(String type) {
        if (!TEMPERATURE.equals(type) && !HUMIDITY.equals(type) && !LUMINOSITY.equals(type)) {
            throw new IllegalArgumentException("Invalid sensor type");
        }
    }
}