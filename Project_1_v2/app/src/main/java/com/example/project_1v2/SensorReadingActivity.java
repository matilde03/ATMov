package com.example.project_1v2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class SensorReadingActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor tempSensor;
    private Sensor humiditySensor;
    private Sensor lightSensor;

    private PreferencesManager prefsManager;
    private DatabaseHelper dbHelper;

    private TextView tvTempLive, tvTempMinMax;
    private TextView tvHumidityLive, tvHumidityMinMax;
    private TextView tvLuminosityLive, tvLuminosityMinMax;

    private float currentTemp = 22.5f;
    private float currentHumidity = 48.0f;
    private float currentLuminosity = 350.0f;

    private boolean isSimulatingTemp = false;
    private boolean isSimulatingHum = false;
    private boolean isSimulatingLight = false;

    private float simStep = 0.0f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final Runnable simulationRunnable = new Runnable() {
        @Override
        public void run() {
            String timestamp = getCurrentTimestamp();
            simStep += 0.15f;

            if (isSimulatingTemp) {
                float targetTemp = 22.5f + (float) Math.sin(simStep) * 2.5f + (random.nextFloat() - 0.5f) * 0.3f;
                currentTemp = Math.max(18.0f, Math.min(28.0f, targetTemp));
                processTempUpdate(currentTemp, timestamp);
            }
            if (isSimulatingHum) {
                float targetHum = 48.0f + (float) Math.cos(simStep) * 6.0f + (random.nextFloat() - 0.5f) * 1.0f;
                currentHumidity = Math.max(30.0f, Math.min(70.0f, targetHum));
                processHumUpdate(currentHumidity, timestamp);
            }
            if (isSimulatingLight) {
                float targetLight = 350.0f + (float) Math.sin(simStep * 0.5f) * 100.0f + (random.nextFloat() - 0.5f) * 15.0f;
                currentLuminosity = Math.max(100.0f, Math.min(800.0f, targetLight));
                processLightUpdate(currentLuminosity, timestamp);
            }
            handler.postDelayed(this, 2000);
        }
    };

    private final Runnable repositoryLoggerRunnable = new Runnable() {
        @Override
        public void run() {
            String timestamp = getCurrentTimestamp();
            dbHelper.addReading(currentTemp, currentHumidity, currentLuminosity, timestamp);
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensor_reading);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sensor_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbarSensor);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefsManager = new PreferencesManager(this);
        dbHelper = new DatabaseHelper(this);

        tvTempLive = findViewById(R.id.tvTempLive);
        tvTempMinMax = findViewById(R.id.tvTempMinMax);

        tvHumidityLive = findViewById(R.id.tvHumidityLive);
        tvHumidityMinMax = findViewById(R.id.tvHumidityMinMax);

        tvLuminosityLive = findViewById(R.id.tvLuminosityLive);
        tvLuminosityMinMax = findViewById(R.id.tvLuminosityMinMax);

        MaterialButton btnReset = findViewById(R.id.btnResetMinMax);
        btnReset.setOnClickListener(v -> {
            prefsManager.resetMinMax();
            updateMinMaxDisplays();
            Toast.makeText(this, R.string.min_max_reset_done, Toast.LENGTH_SHORT).show();
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        isSimulatingTemp = (tempSensor == null);
        isSimulatingHum = (humiditySensor == null);
        isSimulatingLight = (lightSensor == null);

        // Immediate UI initialization
        String initTimestamp = getCurrentTimestamp();
        processTempUpdate(currentTemp, initTimestamp);
        processHumUpdate(currentHumidity, initTimestamp);
        processLightUpdate(currentLuminosity, initTimestamp);
        updateMinMaxDisplays();
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

        if (isSimulatingTemp || isSimulatingHum || isSimulatingLight) {
            handler.post(simulationRunnable);
        }
        handler.post(repositoryLoggerRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        handler.removeCallbacks(simulationRunnable);
        handler.removeCallbacks(repositoryLoggerRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String timestamp = getCurrentTimestamp();
        int type = event.sensor.getType();

        if (type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            float val = event.values[0];
            // Validate sensor reading range (ignore uninitialized 0.0 or extreme outliers on emulators)
            if (val != 0.0f && val >= -30.0f && val <= 80.0f) {
                currentTemp = val;
                processTempUpdate(currentTemp, timestamp);
            }
        } else if (type == Sensor.TYPE_RELATIVE_HUMIDITY) {
            float val = event.values[0];
            if (val >= 0.0f && val <= 100.0f) {
                currentHumidity = val;
                processHumUpdate(currentHumidity, timestamp);
            }
        } else if (type == Sensor.TYPE_LIGHT) {
            float val = event.values[0];
            if (val >= 0.0f) {
                currentLuminosity = val;
                processLightUpdate(currentLuminosity, timestamp);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not required for basic functionality
    }

    private void processTempUpdate(float temp, String timestamp) {
        tvTempLive.setText(String.format(Locale.getDefault(), "%.1f °C", temp));

        if (temp > prefsManager.getTempMaxVal()) {
            prefsManager.updateTempMax(temp, timestamp);
        }
        if (temp < prefsManager.getTempMinVal()) {
            prefsManager.updateTempMin(temp, timestamp);
        }
        updateTempMinMaxDisplay();

        if (prefsManager.isTempAlarmEnabled()) {
            if (temp > prefsManager.getTempHigh()) {
                String msg = getString(R.string.alarm_temp_high, temp, prefsManager.getTempHigh());
                NotificationHelper.sendAlarmNotification(this, 101, getString(R.string.title_sensor), msg);
            } else if (temp < prefsManager.getTempLow()) {
                String msg = getString(R.string.alarm_temp_low, temp, prefsManager.getTempLow());
                NotificationHelper.sendAlarmNotification(this, 102, getString(R.string.title_sensor), msg);
            }
        }
    }

    private void processHumUpdate(float hum, String timestamp) {
        tvHumidityLive.setText(String.format(Locale.getDefault(), "%.0f %%", hum));

        if (hum > prefsManager.getHumidityMaxVal()) {
            prefsManager.updateHumidityMax(hum, timestamp);
        }
        if (hum < prefsManager.getHumidityMinVal()) {
            prefsManager.updateHumidityMin(hum, timestamp);
        }
        updateHumMinMaxDisplay();

        if (prefsManager.isHumidityAlarmEnabled()) {
            if (hum > prefsManager.getHumidityHigh()) {
                String msg = getString(R.string.alarm_hum_high, hum, prefsManager.getHumidityHigh());
                NotificationHelper.sendAlarmNotification(this, 201, getString(R.string.title_sensor), msg);
            } else if (hum < prefsManager.getHumidityLow()) {
                String msg = getString(R.string.alarm_hum_low, hum, prefsManager.getHumidityLow());
                NotificationHelper.sendAlarmNotification(this, 202, getString(R.string.title_sensor), msg);
            }
        }
    }

    private void processLightUpdate(float light, String timestamp) {
        tvLuminosityLive.setText(String.format(Locale.getDefault(), "%.0f lx", light));

        if (light > prefsManager.getLuminosityMaxVal()) {
            prefsManager.updateLuminosityMax(light, timestamp);
        }
        if (light < prefsManager.getLuminosityMinVal()) {
            prefsManager.updateLuminosityMin(light, timestamp);
        }
        updateLumMinMaxDisplay();

        if (prefsManager.isLuminosityAlarmEnabled()) {
            if (light > prefsManager.getLuminosityHigh()) {
                String msg = getString(R.string.alarm_lum_high, light, prefsManager.getLuminosityHigh());
                NotificationHelper.sendAlarmNotification(this, 301, getString(R.string.title_sensor), msg);
            } else if (light < prefsManager.getLuminosityLow()) {
                String msg = getString(R.string.alarm_lum_low, light, prefsManager.getLuminosityLow());
                NotificationHelper.sendAlarmNotification(this, 302, getString(R.string.title_sensor), msg);
            }
        }
    }

    private void updateMinMaxDisplays() {
        updateTempMinMaxDisplay();
        updateHumMinMaxDisplay();
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

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
