package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class SensorActivity extends AppCompatActivity implements SensorMonitor.Listener {

    private TextView tvTempLive;
    private TextView tvTempMinMax;
    private TextView tvHumidityLive;
    private TextView tvHumidityMinMax;
    private TextView tvLuminosityLive;
    private TextView tvLuminosityMinMax;

    private Preferences prefsManager;
    private SensorMonitor sensorMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensor);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbarSensor);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefsManager = new Preferences(this);
        sensorMonitor = new SensorMonitor(this, this);

        Button btnReset = findViewById(R.id.btnResetMinMax);
        btnReset.setOnClickListener(v -> {
            prefsManager.resetMinMax();
            updateTempMinMaxDisplay();
            updateHumMinMaxDisplay();
            updateLumMinMaxDisplay();
            Toast.makeText(this, R.string.min_max_reset_done, Toast.LENGTH_SHORT).show();
        });

        tvTempLive = findViewById(R.id.tvTempLive);
        tvTempMinMax = findViewById(R.id.tvTempMinMax);
        tvHumidityLive = findViewById(R.id.tvHumidityLive);
        tvHumidityMinMax = findViewById(R.id.tvHumidityMinMax);
        tvLuminosityLive = findViewById(R.id.tvLuminosityLive);
        tvLuminosityMinMax = findViewById(R.id.tvLuminosityMinMax);

        if (!sensorMonitor.isSensorManagerAvailable()) {
            Toast.makeText(this, "Error: Sensor Manager is unavailable.", Toast.LENGTH_LONG).show();
        } else if (!sensorMonitor.hasAllSensors()) {
            Toast.makeText(this, "Warning: One or more physical sensors are missing on this device.", Toast.LENGTH_LONG).show();
        }

        updateTempMinMaxDisplay();
        updateHumMinMaxDisplay();
        updateLumMinMaxDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Pause background service while activity is actively reading sensors
        stopService(new Intent(this, SensorMonitoringService.class));
        sensorMonitor.start();

        updateTempMinMaxDisplay();
        updateHumMinMaxDisplay();
        updateLumMinMaxDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMonitor.stop();
        // Hand monitoring back to background service if Notification Mode is enabled
        SensorMonitoringService.sync(this);
    }

    @Override
    protected void onDestroy() {
        sensorMonitor.close();
        super.onDestroy();
    }

    @Override
    public void onTempUpdated(float temp) {
        tvTempLive.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
        updateTempMinMaxDisplay();
    }

    @Override
    public void onHumidityUpdated(float humidity) {
        tvHumidityLive.setText(String.format(Locale.getDefault(), "%.0f %%", humidity));
        updateHumMinMaxDisplay();
    }

    @Override
    public void onLuminosityUpdated(float luminosity) {
        tvLuminosityLive.setText(String.format(Locale.getDefault(), "%.0f lx", luminosity));
        updateLumMinMaxDisplay();
    }

    private void updateTempMinMaxDisplay() {
        float minVal = prefsManager.getTempMinVal();
        float maxVal = prefsManager.getTempMaxVal();
        String minTime = prefsManager.getTempMinTime();
        String maxTime = prefsManager.getTempMaxTime();

        String minStr = (minVal == Float.POSITIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.1f °C (%s)", minVal, minTime);
        String maxStr = (maxVal == Float.NEGATIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.1f °C (%s)", maxVal, maxTime);
        tvTempMinMax.setText(String.format("Min: %s\nMax: %s", minStr, maxStr));
    }

    private void updateHumMinMaxDisplay() {
        float minVal = prefsManager.getHumidityMinVal();
        float maxVal = prefsManager.getHumidityMaxVal();
        String minTime = prefsManager.getHumidityMinTime();
        String maxTime = prefsManager.getHumidityMaxTime();

        String minStr = (minVal == Float.POSITIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.0f %% (%s)", minVal, minTime);
        String maxStr = (maxVal == Float.NEGATIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.0f %% (%s)", maxVal, maxTime);
        tvHumidityMinMax.setText(String.format("Min: %s\nMax: %s", minStr, maxStr));
    }

    private void updateLumMinMaxDisplay() {
        float minVal = prefsManager.getLuminosityMinVal();
        float maxVal = prefsManager.getLuminosityMaxVal();
        String minTime = prefsManager.getLuminosityMinTime();
        String maxTime = prefsManager.getLuminosityMaxTime();

        String minStr = (minVal == Float.POSITIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.0f lx (%s)", minVal, minTime);
        String maxStr = (maxVal == Float.NEGATIVE_INFINITY) ? "N/A" : String.format(Locale.getDefault(), "%.0f lx (%s)", maxVal, maxTime);
        tvLuminosityMinMax.setText(String.format("Min: %s\nMax: %s", minStr, maxStr));
    }
}