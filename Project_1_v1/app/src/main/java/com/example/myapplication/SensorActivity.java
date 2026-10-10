package com.example.myapplication;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor tempSensor;
    private Sensor humiditySensor;
    private Sensor lightSensor;

    private TextView tvTempLive;
    private TextView tvHumidityLive;
    private TextView tvLuminosityLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        tvTempLive = findViewById(R.id.tvTempLive);
        tvHumidityLive = findViewById(R.id.tvHumidityLive);
        tvLuminosityLive = findViewById(R.id.tvLuminosityLive);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float val = event.values[0];
            if (val != 0.0f && val >= -30.0f && val <= 80.0f) {
                tvTempLive.setText(String.format(Locale.getDefault(), "%.1f °C", val));
            }
        } else if (type == Sensor.TYPE_RELATIVE_HUMIDITY) {
            float val = event.values[0];
            if (val >= 0.0f && val <= 100.0f) {
                tvHumidityLive.setText(String.format(Locale.getDefault(), "%.0f %%", val));
            }
        } else if (type == Sensor.TYPE_LIGHT) {
            float val = event.values[0];
            if (val >= 0.0f) {
                tvLuminosityLive.setText(String.format(Locale.getDefault(), "%.0f lx", val));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not required for basic functionality
    }
}