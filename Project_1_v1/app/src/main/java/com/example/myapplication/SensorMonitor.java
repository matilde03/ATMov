package com.example.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SensorMonitor implements SensorEventListener {

    public interface Listener {
        void onTempUpdated(float temp);
        void onHumidityUpdated(float humidity);
        void onLuminosityUpdated(float luminosity);
    }

    private final Context context;
    private final Preferences prefsManager;
    private final DatabaseHelper dbHelper;
    private final SensorManager sensorManager;
    private final Sensor tempSensor;
    private final Sensor humiditySensor;
    private final Sensor lightSensor;
    private final Listener uiListener;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public SensorMonitor(Context context, Listener uiListener) {
        this.context = context.getApplicationContext();
        this.uiListener = uiListener;
        this.prefsManager = new Preferences(this.context);
        this.dbHelper = new DatabaseHelper(this.context);

        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        } else {
            tempSensor = null;
            humiditySensor = null;
            lightSensor = null;
        }
    }

    public boolean isSensorManagerAvailable() {
        return sensorManager != null;
    }

    public boolean hasAllSensors() {
        return tempSensor != null && humiditySensor != null && lightSensor != null;
    }

    public void start() {
        if (sensorManager == null) return;

        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (humiditySensor != null) {
            sensorManager.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void close() {
        stop();
        dbHelper.close();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not required for basic functionality
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long nowMillis = System.currentTimeMillis();
        String timestamp = sdf.format(new Date(nowMillis));
        int type = event.sensor.getType();
        float val = event.values[0];

        if (type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            if (val >= -273.1f && val <= 100.0f) {
                processTempUpdate(val, nowMillis, timestamp);
            }
        } else if (type == Sensor.TYPE_RELATIVE_HUMIDITY) {
            if (val >= 0.0f && val <= 100.0f) {
                processHumUpdate(val, nowMillis, timestamp);
            }
        } else if (type == Sensor.TYPE_LIGHT) {
            if (val >= 0.0f && val <= 40000.0f) {
                processLightUpdate(val, nowMillis, timestamp);
            }
        }
    }

    //**************+ Process Updates *************//
    private void processTempUpdate(float temp, long nowMillis, String timestamp) {
        dbHelper.insertReading(DatabaseHelper.TEMPERATURE, temp, nowMillis);

        if (temp > prefsManager.getTempMaxVal()) {
            prefsManager.updateTempMax(temp, timestamp);
        }
        if (temp < prefsManager.getTempMinVal()) {
            prefsManager.updateTempMin(temp, timestamp);
        }

        if (uiListener != null) {
            uiListener.onTempUpdated(temp);
        }

        if (prefsManager.isTempAlarmEnabled()) {
            if (temp > prefsManager.getTempHigh()) {
                String msg = context.getString(R.string.alarm_temp_high, temp, prefsManager.getTempHigh());
                Notifications.sendAlarmNotification(context, 101, context.getString(R.string.title_sensor), msg);
            } else if (temp < prefsManager.getTempLow()) {
                String msg = context.getString(R.string.alarm_temp_low, temp, prefsManager.getTempLow());
                Notifications.sendAlarmNotification(context, 102, context.getString(R.string.title_sensor), msg);
            }
        }
    }

    private void processHumUpdate(float hum, long nowMillis, String timestamp) {
        dbHelper.insertReading(DatabaseHelper.HUMIDITY, hum, nowMillis);

        if (hum > prefsManager.getHumidityMaxVal()) {
            prefsManager.updateHumidityMax(hum, timestamp);
        }
        if (hum < prefsManager.getHumidityMinVal()) {
            prefsManager.updateHumidityMin(hum, timestamp);
        }

        if (uiListener != null) {
            uiListener.onHumidityUpdated(hum);
        }

        if (prefsManager.isHumidityAlarmEnabled()) {
            if (hum > prefsManager.getHumidityHigh()) {
                String msg = context.getString(R.string.alarm_hum_high, hum, prefsManager.getHumidityHigh());
                Notifications.sendAlarmNotification(context, 201, context.getString(R.string.title_sensor), msg);
            } else if (hum < prefsManager.getHumidityLow()) {
                String msg = context.getString(R.string.alarm_hum_low, hum, prefsManager.getHumidityLow());
                Notifications.sendAlarmNotification(context, 202, context.getString(R.string.title_sensor), msg);
            }
        }
    }

    private void processLightUpdate(float light, long nowMillis, String timestamp) {
        dbHelper.insertReading(DatabaseHelper.LUMINOSITY, light, nowMillis);

        if (light > prefsManager.getLuminosityMaxVal()) {
            prefsManager.updateLuminosityMax(light, timestamp);
        }
        if (light < prefsManager.getLuminosityMinVal()) {
            prefsManager.updateLuminosityMin(light, timestamp);
        }

        if (uiListener != null) {
            uiListener.onLuminosityUpdated(light);
        }

        if (prefsManager.isLuminosityAlarmEnabled()) {
            if (light > prefsManager.getLuminosityHigh()) {
                String msg = context.getString(R.string.alarm_lum_high, light, prefsManager.getLuminosityHigh());
                Notifications.sendAlarmNotification(context, 301, context.getString(R.string.title_sensor), msg);
            } else if (light < prefsManager.getLuminosityLow()) {
                String msg = context.getString(R.string.alarm_lum_low, light, prefsManager.getLuminosityLow());
                Notifications.sendAlarmNotification(context, 302, context.getString(R.string.title_sensor), msg);
            }
        }
    }
}