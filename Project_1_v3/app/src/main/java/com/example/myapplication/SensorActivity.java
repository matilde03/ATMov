package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {

    // Sensors and manager
    private SensorManager sensorManager;
    private Sensor tempSensor;
    private Sensor humiditySensor;
    private Sensor lightSensor;

    // Text Views to update
    private TextView tvTempLive;
    private TextView tvTempMinMax;
    private TextView tvHumidityLive;
    private TextView tvHumidityMinMax;
    private TextView tvLuminosityLive;
    private TextView tvLuminosityMinMax;

    // Sensor value variables
    private float currentTemp = 0.0f;
    private float currentHumidity = 0.0f;
    private float currentLuminosity = 0.0f;

    // Save interval for activity-only mode
    private static final long SAVE_INTERVAL = 10000;

    private long lastTempSave = 0;
    private long lastHumSave = 0;
    private long lastLightSave = 0;

    private Preferences prefsManager;
    private DatabaseHelper dbHelper;

    // Format for displaying dates
    private final SimpleDateFormat sdf =
            new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_sensor);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );


        Toolbar toolbar =
                findViewById(R.id.toolbarSensor);

        toolbar.setNavigationOnClickListener(
                v -> finish()
        );


        prefsManager =
                new Preferences(this);

        dbHelper =
                new DatabaseHelper(this);


        Button btnReset =
                findViewById(R.id.btnResetMinMax);

        btnReset.setOnClickListener(v -> {

            prefsManager.resetMinMax();

            updateTempMinMaxDisplay();
            updateHumMinMaxDisplay();
            updateLumMinMaxDisplay();

            Toast.makeText(
                    this,
                    R.string.min_max_reset_done,
                    Toast.LENGTH_SHORT
            ).show();
        });


        tvTempLive =
                findViewById(R.id.tvTempLive);

        tvTempMinMax =
                findViewById(R.id.tvTempMinMax);

        tvHumidityLive =
                findViewById(R.id.tvHumidityLive);

        tvHumidityMinMax =
                findViewById(R.id.tvHumidityMinMax);

        tvLuminosityLive =
                findViewById(R.id.tvLuminosityLive);

        tvLuminosityMinMax =
                findViewById(R.id.tvLuminosityMinMax);


        sensorManager =
                (SensorManager) getSystemService(
                        SENSOR_SERVICE
                );

        if (sensorManager != null) {

            tempSensor =
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


            if (tempSensor == null
                    || humiditySensor == null
                    || lightSensor == null) {

                Toast.makeText(
                        this,
                        "Warning: One or more physical sensors are missing on this device.",
                        Toast.LENGTH_LONG
                ).show();
            }

        } else {

            Toast.makeText(
                    this,
                    "Error: Sensor Manager is unavailable.",
                    Toast.LENGTH_LONG
            ).show();
        }


        updateTempMinMaxDisplay();
        updateHumMinMaxDisplay();
        updateLumMinMaxDisplay();
    }


    @Override
    protected void onResume() {
        super.onResume();


        MonitoringModeManager.apply(this);

        if (sensorManager != null) {

            if (tempSensor != null) {

                sensorManager.registerListener(
                        this,
                        tempSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                );
            }

            if (humiditySensor != null) {

                sensorManager.registerListener(
                        this,
                        humiditySensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                );
            }

            if (lightSensor != null) {

                sensorManager.registerListener(
                        this,
                        lightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                );
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

        String timestamp =
                sdf.format(
                        new Date()
                );

        int type =
                event.sensor.getType();


        if (type == Sensor.TYPE_AMBIENT_TEMPERATURE) {

            float val =
                    event.values[0];

            if (val >= -30.0f
                    && val <= 80.0f) {

                currentTemp = val;

                processTempUpdate(
                        currentTemp,
                        timestamp
                );
            }

        } else if (type == Sensor.TYPE_RELATIVE_HUMIDITY) {

            float val =
                    event.values[0];

            if (val >= 0.0f
                    && val <= 100.0f) {

                currentHumidity = val;

                processHumUpdate(
                        currentHumidity,
                        timestamp
                );
            }

        } else if (type == Sensor.TYPE_LIGHT) {

            float val =
                    event.values[0];

            if (val >= 0.0f) {

                currentLuminosity = val;

                processLightUpdate(
                        currentLuminosity,
                        timestamp
                );
            }
        }
    }


    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy) {

        // Not required for basic functionality
    }


    //**************+******************************//
    //**************+ Extra Functions *************//
    //**************+******************************//


    //**************+ Process Updates *************//

    private void processTempUpdate(
            float temp,
            String timestamp) {

        tvTempLive.setText(
                String.format(
                        Locale.getDefault(),
                        "%.1f °C",
                        temp
                )
        );



        if (prefsManager.isActivityOnlyMode()) {

            long now =
                    System.currentTimeMillis();


            if (now - lastTempSave >= SAVE_INTERVAL) {

                dbHelper.insertReading(
                        DatabaseHelper.TEMPERATURE,
                        temp,
                        now
                );

                lastTempSave = now;
            }


            if (temp > prefsManager.getTempMaxVal()) {

                prefsManager.updateTempMax(
                        temp,
                        timestamp
                );
            }


            if (temp < prefsManager.getTempMinVal()) {

                prefsManager.updateTempMin(
                        temp,
                        timestamp
                );
            }


            if (prefsManager.isTempAlarmEnabled()) {

                if (temp > prefsManager.getTempHigh()) {

                    String msg =
                            getString(
                                    R.string.alarm_temp_high,
                                    temp,
                                    prefsManager.getTempHigh()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            101,
                            getString(R.string.title_sensor),
                            msg
                    );

                } else if (temp < prefsManager.getTempLow()) {

                    String msg =
                            getString(
                                    R.string.alarm_temp_low,
                                    temp,
                                    prefsManager.getTempLow()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            102,
                            getString(R.string.title_sensor),
                            msg
                    );
                }
            }
        }


        updateTempMinMaxDisplay();
    }


    private void processHumUpdate(
            float hum,
            String timestamp) {

        tvHumidityLive.setText(
                String.format(
                        Locale.getDefault(),
                        "%.0f %%",
                        hum
                )
        );


        if (prefsManager.isActivityOnlyMode()) {

            long now =
                    System.currentTimeMillis();


            if (now - lastHumSave >= SAVE_INTERVAL) {

                dbHelper.insertReading(
                        DatabaseHelper.HUMIDITY,
                        hum,
                        now
                );

                lastHumSave = now;
            }


            if (hum > prefsManager.getHumidityMaxVal()) {

                prefsManager.updateHumidityMax(
                        hum,
                        timestamp
                );
            }


            if (hum < prefsManager.getHumidityMinVal()) {

                prefsManager.updateHumidityMin(
                        hum,
                        timestamp
                );
            }


            if (prefsManager.isHumidityAlarmEnabled()) {

                if (hum > prefsManager.getHumidityHigh()) {

                    String msg =
                            getString(
                                    R.string.alarm_hum_high,
                                    hum,
                                    prefsManager.getHumidityHigh()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            201,
                            getString(R.string.title_sensor),
                            msg
                    );

                } else if (hum < prefsManager.getHumidityLow()) {

                    String msg =
                            getString(
                                    R.string.alarm_hum_low,
                                    hum,
                                    prefsManager.getHumidityLow()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            202,
                            getString(R.string.title_sensor),
                            msg
                    );
                }
            }
        }


        updateHumMinMaxDisplay();
    }


    private void processLightUpdate(
            float light,
            String timestamp) {

        tvLuminosityLive.setText(
                String.format(
                        Locale.getDefault(),
                        "%.0f lx",
                        light
                )
        );


        if (prefsManager.isActivityOnlyMode()) {

            long now =
                    System.currentTimeMillis();


            if (now - lastLightSave >= SAVE_INTERVAL) {

                dbHelper.insertReading(
                        DatabaseHelper.LUMINOSITY,
                        light,
                        now
                );

                lastLightSave = now;
            }


            if (light > prefsManager.getLuminosityMaxVal()) {

                prefsManager.updateLuminosityMax(
                        light,
                        timestamp
                );
            }


            if (light < prefsManager.getLuminosityMinVal()) {

                prefsManager.updateLuminosityMin(
                        light,
                        timestamp
                );
            }


            if (prefsManager.isLuminosityAlarmEnabled()) {

                if (light > prefsManager.getLuminosityHigh()) {

                    String msg =
                            getString(
                                    R.string.alarm_lum_high,
                                    light,
                                    prefsManager.getLuminosityHigh()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            301,
                            getString(R.string.title_sensor),
                            msg
                    );

                } else if (light < prefsManager.getLuminosityLow()) {

                    String msg =
                            getString(
                                    R.string.alarm_lum_low,
                                    light,
                                    prefsManager.getLuminosityLow()
                            );

                    Notifications.sendAlarmNotification(
                            this,
                            302,
                            getString(R.string.title_sensor),
                            msg
                    );
                }
            }
        }


        updateLumMinMaxDisplay();
    }


    //**************+ Update Min Max Display *************//

    private void updateTempMinMaxDisplay() {

        float minVal =
                prefsManager.getTempMinVal();

        float maxVal =
                prefsManager.getTempMaxVal();

        String minTime =
                prefsManager.getTempMinTime();

        String maxTime =
                prefsManager.getTempMaxTime();


        String minStr =
                (minVal == Float.POSITIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.1f °C (%s)",
                        minVal,
                        minTime
                );


        String maxStr =
                (maxVal == Float.NEGATIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.1f °C (%s)",
                        maxVal,
                        maxTime
                );


        tvTempMinMax.setText(
                String.format(
                        Locale.getDefault(),
                        "Min: %s\nMax: %s",
                        minStr,
                        maxStr
                )
        );
    }


    private void updateHumMinMaxDisplay() {

        float minVal =
                prefsManager.getHumidityMinVal();

        float maxVal =
                prefsManager.getHumidityMaxVal();

        String minTime =
                prefsManager.getHumidityMinTime();

        String maxTime =
                prefsManager.getHumidityMaxTime();


        String minStr =
                (minVal == Float.POSITIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.0f %% (%s)",
                        minVal,
                        minTime
                );


        String maxStr =
                (maxVal == Float.NEGATIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.0f %% (%s)",
                        maxVal,
                        maxTime
                );


        tvHumidityMinMax.setText(
                String.format(
                        Locale.getDefault(),
                        "Min: %s\nMax: %s",
                        minStr,
                        maxStr
                )
        );
    }


    private void updateLumMinMaxDisplay() {

        float minVal =
                prefsManager.getLuminosityMinVal();

        float maxVal =
                prefsManager.getLuminosityMaxVal();

        String minTime =
                prefsManager.getLuminosityMinTime();

        String maxTime =
                prefsManager.getLuminosityMaxTime();


        String minStr =
                (minVal == Float.POSITIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.0f lx (%s)",
                        minVal,
                        minTime
                );


        String maxStr =
                (maxVal == Float.NEGATIVE_INFINITY)
                        ? "N/A"
                        : String.format(
                        Locale.getDefault(),
                        "%.0f lx (%s)",
                        maxVal,
                        maxTime
                );


        tvLuminosityMinMax.setText(
                String.format(
                        Locale.getDefault(),
                        "Min: %s\nMax: %s",
                        minStr,
                        maxStr
                )
        );
    }


    private String getCurrentTimestamp() {

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                );

        return sdf.format(
                new Date()
        );
    }


    @Override
    protected void onDestroy() {

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}