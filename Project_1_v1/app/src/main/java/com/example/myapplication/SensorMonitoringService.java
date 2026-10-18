package com.example.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class SensorMonitoringService extends Service {

    private SensorMonitor sensorMonitor;

    // Starts or stops the background service based on Notification Mode preference
    public static void sync(Context context) {
        Preferences prefs = new Preferences(context);
        Intent serviceIntent = new Intent(context, SensorMonitoringService.class);

        if (prefs.isContinuousMode()) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.stopService(serviceIntent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(
                Notifications.MONITORING_NOTIFICATION_ID,
                Notifications.createMonitoringNotification(this)
        );
        sensorMonitor = new SensorMonitor(this, null);
        sensorMonitor.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Preferences prefs = new Preferences(this);
        if (!prefs.isContinuousMode()) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorMonitor != null) {
            sensorMonitor.close();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}