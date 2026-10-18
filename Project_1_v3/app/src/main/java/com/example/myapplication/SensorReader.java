package com.example.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import java.util.HashSet;
import java.util.Set;


public class SensorReader implements SensorEventListener {


    public interface Callback {


        void onReading(int sensorType, float value);


        void onFinished(int readingCount);
    }

    // Maximum time to wait for sensor events.
    private static final long READ_TIMEOUT = 3000;

    private final SensorManager sensorManager;

    private final Sensor temperatureSensor;
    private final Sensor humiditySensor;
    private final Sensor lightSensor;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    // Stores the sensor types that are still waiting for a reading.
    private final Set<Integer> waitingSensors =
            new HashSet<>();

    private Callback callback;

    private boolean reading = false;
    private int readingCount = 0;

    private final Runnable timeoutRunnable =
            this::finishReading;


    public SensorReader(Context context) {

        sensorManager =
                (SensorManager) context.getSystemService(
                        Context.SENSOR_SERVICE
                );

        if (sensorManager != null) {

            temperatureSensor =
                    sensorManager.getDefaultSensor(
                            Sensor.TYPE_AMBIENT_TEMPERATURE
                    );

            humiditySensor =
                    sensorManager.getDefaultSensor(
                            Sensor.TYPE_RELATIVE_HUMIDITY
                    );

            lightSensor =
                    sensorManager.getDefaultSensor(
                            Sensor.TYPE_LIGHT
                    );

        } else {

            temperatureSensor = null;
            humiditySensor = null;
            lightSensor = null;
        }
    }


    public void readOnce(Callback callback) {

        // Stop any previous reading operation.
        stop();

        this.callback = callback;
        this.readingCount = 0;
        this.reading = true;

        waitingSensors.clear();

        // Register all available sensors.
        registerSensor(temperatureSensor);
        registerSensor(humiditySensor);
        registerSensor(lightSensor);

        // Finish immediately if no sensors are available.
        if (waitingSensors.isEmpty()) {
            finishReading();
            return;
        }

        // Stop automatically after the timeout.
        handler.postDelayed(
                timeoutRunnable,
                READ_TIMEOUT
        );
    }


    private void registerSensor(Sensor sensor) {

        if (sensor == null || sensorManager == null) {
            return;
        }

        boolean registered =
                sensorManager.registerListener(
                        this,
                        sensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                );

        if (registered) {
            waitingSensors.add(sensor.getType());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!reading) {
            return;
        }

        int sensorType = event.sensor.getType();

        // Ignore sensors that already returned a valid value.
        if (!waitingSensors.contains(sensorType)) {
            return;
        }

        float value = event.values[0];

        if (!isValidReading(sensorType, value)) {
            return;
        }

        waitingSensors.remove(sensorType);
        readingCount++;

        if (callback != null) {
            callback.onReading(
                    sensorType,
                    value
            );
        }

        // Stop when all available sensors have returned a value.
        if (waitingSensors.isEmpty()) {
            finishReading();
        }
    }


    private boolean isValidReading(
            int sensorType,
            float value) {

        if (sensorType ==
                Sensor.TYPE_AMBIENT_TEMPERATURE) {

            return value != 0.0f
                    && value >= -30.0f
                    && value <= 80.0f;
        }

        if (sensorType ==
                Sensor.TYPE_RELATIVE_HUMIDITY) {

            return value >= 0.0f
                    && value <= 100.0f;
        }

        if (sensorType ==
                Sensor.TYPE_LIGHT) {

            return value >= 0.0f;
        }

        return false;
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy) {

        // No action required.
    }

    private void finishReading() {

        if (!reading) {
            return;
        }

        reading = false;

        handler.removeCallbacks(timeoutRunnable);

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        Callback currentCallback = callback;

        callback = null;

        if (currentCallback != null) {
            currentCallback.onFinished(readingCount);
        }

        waitingSensors.clear();
    }


    public void stop() {

        reading = false;

        handler.removeCallbacks(timeoutRunnable);

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        waitingSensors.clear();

        callback = null;
    }
}