package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SensorMonitoringService
        extends Service
        implements SensorEventListener {


    private static final String MONITORING_CHANNEL_ID =
            "sensor_monitoring_channel";

    private static final String ALARM_CHANNEL_ID =
            "sensor_alarm_channel";

    private static final int MONITORING_NOTIFICATION_ID = 1001;

    private static final int TEMP_ALARM_NOTIFICATION_ID = 2001;
    private static final int HUMIDITY_ALARM_NOTIFICATION_ID = 2002;
    private static final int LIGHT_ALARM_NOTIFICATION_ID = 2003;

    private SensorManager sensorManager;

    private Sensor temperatureSensor;
    private Sensor humiditySensor;
    private Sensor lightSensor;


    private DatabaseHelper dbHelper;
    private Preferences prefsManager;


    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
            );


    @Override
    public void onCreate() {
        super.onCreate();

        prefsManager =
                new Preferences(this);

        dbHelper =
                new DatabaseHelper(this);

        createNotificationChannels();


        startForeground(
                MONITORING_NOTIFICATION_ID,
                createMonitoringNotification()
        );


        if (!prefsManager.isContinuousMode()) {

            stopForegroundService();

            return;
        }

        sensorManager =
                (SensorManager) getSystemService(
                        SENSOR_SERVICE
                );

        initializeSensors();
        registerSensors();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {


        if (!prefsManager.isContinuousMode()) {

            stopForegroundService();

            return START_NOT_STICKY;
        }


        return START_STICKY;
    }


    private void initializeSensors() {

        if (sensorManager == null) {
            return;
        }

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
    }


    private void registerSensors() {

        if (sensorManager == null) {
            return;
        }

        if (temperatureSensor != null) {

            sensorManager.registerListener(
                    this,
                    temperatureSensor,
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


    @Override
    public void onSensorChanged(SensorEvent event) {


        if (!prefsManager.isContinuousMode()) {

            stopForegroundService();

            return;
        }

        int sensorType =
                event.sensor.getType();

        float value =
                event.values[0];

        long timestamp =
                System.currentTimeMillis();

        String formattedTimestamp =
                dateFormat.format(
                        new Date(timestamp)
                );

        if (sensorType ==
                Sensor.TYPE_AMBIENT_TEMPERATURE) {

            processTemperature(
                    value,
                    timestamp,
                    formattedTimestamp
            );

        } else if (sensorType ==
                Sensor.TYPE_RELATIVE_HUMIDITY) {

            processHumidity(
                    value,
                    timestamp,
                    formattedTimestamp
            );

        } else if (sensorType ==
                Sensor.TYPE_LIGHT) {

            processLight(
                    value,
                    timestamp,
                    formattedTimestamp
            );
        }
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy) {

        // No action required.
    }


    private void processTemperature(
            float temperature,
            long timestamp,
            String formattedTimestamp) {

        if (temperature == 0.0f
                || temperature < -30.0f
                || temperature > 80.0f) {

            return;
        }

        dbHelper.insertReading(
                DatabaseHelper.TEMPERATURE,
                temperature,
                timestamp
        );

        updateTemperatureStatistics(
                temperature,
                formattedTimestamp
        );

        checkTemperatureAlarm(
                temperature
        );
    }


    private void updateTemperatureStatistics(
            float temperature,
            String timestamp) {

        if (temperature >
                prefsManager.getTempMaxVal()) {

            prefsManager.updateTempMax(
                    temperature,
                    timestamp
            );
        }

        if (temperature <
                prefsManager.getTempMinVal()) {

            prefsManager.updateTempMin(
                    temperature,
                    timestamp
            );
        }
    }


    private void checkTemperatureAlarm(
            float temperature) {

        if (!prefsManager.isNotificationMode()
                || !prefsManager.isTempAlarmEnabled()) {

            cancelAlarmNotification(
                    TEMP_ALARM_NOTIFICATION_ID
            );

            return;
        }

        if (temperature >
                prefsManager.getTempHigh()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Temperature is too high: %.1f °C",
                            temperature
                    );

            sendAlarmNotification(
                    TEMP_ALARM_NOTIFICATION_ID,
                    "Temperature alarm",
                    message
            );

        } else if (temperature <
                prefsManager.getTempLow()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Temperature is too low: %.1f °C",
                            temperature
                    );

            sendAlarmNotification(
                    TEMP_ALARM_NOTIFICATION_ID,
                    "Temperature alarm",
                    message
            );

        } else {

            cancelAlarmNotification(
                    TEMP_ALARM_NOTIFICATION_ID
            );
        }
    }


    private void processHumidity(
            float humidity,
            long timestamp,
            String formattedTimestamp) {

        if (humidity < 0.0f
                || humidity > 100.0f) {

            return;
        }

        dbHelper.insertReading(
                DatabaseHelper.HUMIDITY,
                humidity,
                timestamp
        );

        updateHumidityStatistics(
                humidity,
                formattedTimestamp
        );

        checkHumidityAlarm(
                humidity
        );
    }


    private void updateHumidityStatistics(
            float humidity,
            String timestamp) {

        if (humidity >
                prefsManager.getHumidityMaxVal()) {

            prefsManager.updateHumidityMax(
                    humidity,
                    timestamp
            );
        }

        if (humidity <
                prefsManager.getHumidityMinVal()) {

            prefsManager.updateHumidityMin(
                    humidity,
                    timestamp
            );
        }
    }


    private void checkHumidityAlarm(
            float humidity) {

        if (!prefsManager.isNotificationMode()
                || !prefsManager.isHumidityAlarmEnabled()) {

            cancelAlarmNotification(
                    HUMIDITY_ALARM_NOTIFICATION_ID
            );

            return;
        }

        if (humidity >
                prefsManager.getHumidityHigh()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Humidity is too high: %.0f %%",
                            humidity
                    );

            sendAlarmNotification(
                    HUMIDITY_ALARM_NOTIFICATION_ID,
                    "Humidity alarm",
                    message
            );

        } else if (humidity <
                prefsManager.getHumidityLow()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Humidity is too low: %.0f %%",
                            humidity
                    );

            sendAlarmNotification(
                    HUMIDITY_ALARM_NOTIFICATION_ID,
                    "Humidity alarm",
                    message
            );

        } else {

            cancelAlarmNotification(
                    HUMIDITY_ALARM_NOTIFICATION_ID
            );
        }
    }

    private void processLight(
            float light,
            long timestamp,
            String formattedTimestamp) {

        if (light < 0.0f) {
            return;
        }

        dbHelper.insertReading(
                DatabaseHelper.LUMINOSITY,
                light,
                timestamp
        );

        updateLightStatistics(
                light,
                formattedTimestamp
        );

        checkLightAlarm(
                light
        );
    }


    private void updateLightStatistics(
            float light,
            String timestamp) {

        if (light >
                prefsManager.getLuminosityMaxVal()) {

            prefsManager.updateLuminosityMax(
                    light,
                    timestamp
            );
        }

        if (light <
                prefsManager.getLuminosityMinVal()) {

            prefsManager.updateLuminosityMin(
                    light,
                    timestamp
            );
        }
    }


    private void checkLightAlarm(
            float light) {

        if (!prefsManager.isNotificationMode()
                || !prefsManager.isLuminosityAlarmEnabled()) {

            cancelAlarmNotification(
                    LIGHT_ALARM_NOTIFICATION_ID
            );

            return;
        }

        if (light >
                prefsManager.getLuminosityHigh()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Luminosity is too high: %.0f lx",
                            light
                    );

            sendAlarmNotification(
                    LIGHT_ALARM_NOTIFICATION_ID,
                    "Luminosity alarm",
                    message
            );

        } else if (light <
                prefsManager.getLuminosityLow()) {

            String message =
                    String.format(
                            Locale.getDefault(),
                            "Luminosity is too low: %.0f lx",
                            light
                    );

            sendAlarmNotification(
                    LIGHT_ALARM_NOTIFICATION_ID,
                    "Luminosity alarm",
                    message
            );

        } else {

            cancelAlarmNotification(
                    LIGHT_ALARM_NOTIFICATION_ID
            );
        }
    }


    private void createNotificationChannels() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (manager == null) {
            return;
        }

        NotificationChannel monitoringChannel =
                new NotificationChannel(
                        MONITORING_CHANNEL_ID,
                        "Sensor Monitoring",
                        NotificationManager.IMPORTANCE_LOW
                );

        monitoringChannel.setDescription(
                "Continuous environmental sensor monitoring"
        );

        manager.createNotificationChannel(
                monitoringChannel
        );

        NotificationChannel alarmChannel =
                new NotificationChannel(
                        ALARM_CHANNEL_ID,
                        "Sensor Alarms",
                        NotificationManager.IMPORTANCE_HIGH
                );

        alarmChannel.setDescription(
                "Environmental sensor alarm notifications"
        );

        manager.createNotificationChannel(
                alarmChannel
        );
    }


    private Notification createMonitoringNotification() {

        return new NotificationCompat.Builder(
                this,
                MONITORING_CHANNEL_ID
        )
                .setContentTitle(
                        "Monitoring sensors"
                )
                .setContentText(
                        "Temperature, humidity and light monitoring is active"
                )
                .setSmallIcon(
                        R.drawable.ic_sensors
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }


    private void sendAlarmNotification(
            int notificationId,
            String title,
            String message) {

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        ALARM_CHANNEL_ID
                )
                        .setSmallIcon(
                                R.drawable.ic_sensors
                        )
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .build();

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        NOTIFICATION_SERVICE
                );

        if (manager != null) {

            manager.notify(
                    notificationId,
                    notification
            );
        }
    }


    private void cancelAlarmNotification(
            int notificationId) {

        NotificationManager manager =
                (NotificationManager) getSystemService(
                        NOTIFICATION_SERVICE
                );

        if (manager != null) {

            manager.cancel(
                    notificationId
            );
        }
    }


    private void stopForegroundService() {

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        stopForeground(
                STOP_FOREGROUND_REMOVE
        );

        stopSelf();
    }

    @Override
    public void onDestroy() {

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }
}