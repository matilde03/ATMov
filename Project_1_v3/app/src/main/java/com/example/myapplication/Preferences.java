package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static final String PREF_NAME = "sensor_monitor_prefs";

    // Configuration keys
    public static final String KEY_TEMP_HIGH = "temp_high";
    public static final String KEY_TEMP_LOW = "temp_low";
    public static final String KEY_TEMP_ALARM_ENABLED = "temp_alarm_enabled";

    public static final String KEY_HUMIDITY_HIGH = "humidity_high";
    public static final String KEY_HUMIDITY_LOW = "humidity_low";
    public static final String KEY_HUMIDITY_ALARM_ENABLED = "humidity_alarm_enabled";

    public static final String KEY_LUMINOSITY_HIGH = "luminosity_high";
    public static final String KEY_LUMINOSITY_LOW = "luminosity_low";
    public static final String KEY_LUMINOSITY_ALARM_ENABLED = "luminosity_alarm_enabled";

    public static final String KEY_NOTIFICATION_MODE = "notification_mode";

    // Operating mode
    public static final String KEY_OPERATING_MODE = "operating_mode";

    public static final int OPERATING_MODE_CONTINUOUS = 0;
    public static final int OPERATING_MODE_ACTIVITY_ONLY = 1;

    // Min/Max statistics keys
    public static final String KEY_TEMP_MAX_VAL = "temp_max_val";
    public static final String KEY_TEMP_MAX_TIME = "temp_max_time";
    public static final String KEY_TEMP_MIN_VAL = "temp_min_val";
    public static final String KEY_TEMP_MIN_TIME = "temp_min_time";

    public static final String KEY_HUM_MAX_VAL = "hum_max_val";
    public static final String KEY_HUM_MAX_TIME = "hum_max_time";
    public static final String KEY_HUM_MIN_VAL = "hum_min_val";
    public static final String KEY_HUM_MIN_TIME = "hum_min_time";

    public static final String KEY_LUM_MAX_VAL = "lum_max_val";
    public static final String KEY_LUM_MAX_TIME = "lum_max_time";
    public static final String KEY_LUM_MIN_VAL = "lum_min_val";
    public static final String KEY_LUM_MIN_TIME = "lum_min_time";

    private final SharedPreferences prefs;

    public Preferences(Context context) {
        this.prefs = context
                .getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }


    // Thresholds & Alarms

    public float getTempHigh() {
        return prefs.getFloat(
                KEY_TEMP_HIGH,
                30.0f
        );
    }

    public void setTempHigh(float val) {
        prefs.edit()
                .putFloat(KEY_TEMP_HIGH, val)
                .apply();
    }


    public float getTempLow() {
        return prefs.getFloat(
                KEY_TEMP_LOW,
                15.0f
        );
    }

    public void setTempLow(float val) {
        prefs.edit()
                .putFloat(KEY_TEMP_LOW, val)
                .apply();
    }


    public boolean isTempAlarmEnabled() {
        return prefs.getBoolean(
                KEY_TEMP_ALARM_ENABLED,
                true
        );
    }

    public void setTempAlarmEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(
                        KEY_TEMP_ALARM_ENABLED,
                        enabled
                )
                .apply();
    }


    public float getHumidityHigh() {
        return prefs.getFloat(
                KEY_HUMIDITY_HIGH,
                70.0f
        );
    }

    public void setHumidityHigh(float val) {
        prefs.edit()
                .putFloat(
                        KEY_HUMIDITY_HIGH,
                        val
                )
                .apply();
    }


    public float getHumidityLow() {
        return prefs.getFloat(
                KEY_HUMIDITY_LOW,
                30.0f
        );
    }

    public void setHumidityLow(float val) {
        prefs.edit()
                .putFloat(
                        KEY_HUMIDITY_LOW,
                        val
                )
                .apply();
    }


    public boolean isHumidityAlarmEnabled() {
        return prefs.getBoolean(
                KEY_HUMIDITY_ALARM_ENABLED,
                true
        );
    }

    public void setHumidityAlarmEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(
                        KEY_HUMIDITY_ALARM_ENABLED,
                        enabled
                )
                .apply();
    }


    public float getLuminosityHigh() {
        return prefs.getFloat(
                KEY_LUMINOSITY_HIGH,
                1000.0f
        );
    }

    public void setLuminosityHigh(float val) {
        prefs.edit()
                .putFloat(
                        KEY_LUMINOSITY_HIGH,
                        val
                )
                .apply();
    }


    public float getLuminosityLow() {
        return prefs.getFloat(
                KEY_LUMINOSITY_LOW,
                50.0f
        );
    }

    public void setLuminosityLow(float val) {
        prefs.edit()
                .putFloat(
                        KEY_LUMINOSITY_LOW,
                        val
                )
                .apply();
    }


    public boolean isLuminosityAlarmEnabled() {
        return prefs.getBoolean(
                KEY_LUMINOSITY_ALARM_ENABLED,
                false
        );
    }

    public void setLuminosityAlarmEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(
                        KEY_LUMINOSITY_ALARM_ENABLED,
                        enabled
                )
                .apply();
    }


    public boolean isNotificationMode() {
        return prefs.getBoolean(
                KEY_NOTIFICATION_MODE,
                false
        );
    }

    public void setNotificationMode(boolean enabled) {
        prefs.edit()
                .putBoolean(
                        KEY_NOTIFICATION_MODE,
                        enabled
                )
                .apply();
    }


    // Operating Mode


    public int getOperatingMode() {

        return prefs.getInt(
                KEY_OPERATING_MODE,
                OPERATING_MODE_ACTIVITY_ONLY
        );
    }



    public void setOperatingMode(int mode) {

        if (mode != OPERATING_MODE_CONTINUOUS
                && mode != OPERATING_MODE_ACTIVITY_ONLY) {

            throw new IllegalArgumentException(
                    "Invalid operating mode"
            );
        }

        prefs.edit()
                .putInt(
                        KEY_OPERATING_MODE,
                        mode
                )
                .apply();
    }



    public boolean isContinuousMode() {

        return getOperatingMode()
                == OPERATING_MODE_CONTINUOUS;
    }



    public boolean isActivityOnlyMode() {

        return getOperatingMode()
                == OPERATING_MODE_ACTIVITY_ONLY;
    }


    // Min / Max Stats

    public float getTempMaxVal() {
        return prefs.getFloat(
                KEY_TEMP_MAX_VAL,
                Float.NEGATIVE_INFINITY
        );
    }

    public String getTempMaxTime() {
        return prefs.getString(
                KEY_TEMP_MAX_TIME,
                "N/A"
        );
    }

    public float getTempMinVal() {
        return prefs.getFloat(
                KEY_TEMP_MIN_VAL,
                Float.POSITIVE_INFINITY
        );
    }

    public String getTempMinTime() {
        return prefs.getString(
                KEY_TEMP_MIN_TIME,
                "N/A"
        );
    }


    public void updateTempMax(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_TEMP_MAX_VAL,
                        val
                )
                .putString(
                        KEY_TEMP_MAX_TIME,
                        timestamp
                )
                .apply();
    }

    public void updateTempMin(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_TEMP_MIN_VAL,
                        val
                )
                .putString(
                        KEY_TEMP_MIN_TIME,
                        timestamp
                )
                .apply();
    }


    public float getHumidityMaxVal() {
        return prefs.getFloat(
                KEY_HUM_MAX_VAL,
                Float.NEGATIVE_INFINITY
        );
    }

    public String getHumidityMaxTime() {
        return prefs.getString(
                KEY_HUM_MAX_TIME,
                "N/A"
        );
    }

    public float getHumidityMinVal() {
        return prefs.getFloat(
                KEY_HUM_MIN_VAL,
                Float.POSITIVE_INFINITY
        );
    }

    public String getHumidityMinTime() {
        return prefs.getString(
                KEY_HUM_MIN_TIME,
                "N/A"
        );
    }


    public void updateHumidityMax(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_HUM_MAX_VAL,
                        val
                )
                .putString(
                        KEY_HUM_MAX_TIME,
                        timestamp
                )
                .apply();
    }

    public void updateHumidityMin(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_HUM_MIN_VAL,
                        val
                )
                .putString(
                        KEY_HUM_MIN_TIME,
                        timestamp
                )
                .apply();
    }


    public float getLuminosityMaxVal() {
        return prefs.getFloat(
                KEY_LUM_MAX_VAL,
                Float.NEGATIVE_INFINITY
        );
    }

    public String getLuminosityMaxTime() {
        return prefs.getString(
                KEY_LUM_MAX_TIME,
                "N/A"
        );
    }

    public float getLuminosityMinVal() {
        return prefs.getFloat(
                KEY_LUM_MIN_VAL,
                Float.POSITIVE_INFINITY
        );
    }

    public String getLuminosityMinTime() {
        return prefs.getString(
                KEY_LUM_MIN_TIME,
                "N/A"
        );
    }


    public void updateLuminosityMax(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_LUM_MAX_VAL,
                        val
                )
                .putString(
                        KEY_LUM_MAX_TIME,
                        timestamp
                )
                .apply();
    }

    public void updateLuminosityMin(float val, String timestamp) {
        prefs.edit()
                .putFloat(
                        KEY_LUM_MIN_VAL,
                        val
                )
                .putString(
                        KEY_LUM_MIN_TIME,
                        timestamp
                )
                .apply();
    }


    public void resetMinMax() {

        prefs.edit()
                .remove(KEY_TEMP_MAX_VAL)
                .remove(KEY_TEMP_MAX_TIME)
                .remove(KEY_TEMP_MIN_VAL)
                .remove(KEY_TEMP_MIN_TIME)

                .remove(KEY_HUM_MAX_VAL)
                .remove(KEY_HUM_MAX_TIME)
                .remove(KEY_HUM_MIN_VAL)
                .remove(KEY_HUM_MIN_TIME)

                .remove(KEY_LUM_MAX_VAL)
                .remove(KEY_LUM_MAX_TIME)
                .remove(KEY_LUM_MIN_VAL)
                .remove(KEY_LUM_MIN_TIME)

                .apply();
    }
}