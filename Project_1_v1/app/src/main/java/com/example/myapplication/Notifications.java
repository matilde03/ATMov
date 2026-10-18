package com.example.myapplication;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class Notifications {

    public static final String CHANNEL_ID = "sensor_alarms_channel";
    public static final String CHANNEL_NAME = "Sensor Alarm Alerts";

    public static final String MONITORING_CHANNEL_ID = "sensor_monitoring_channel";
    public static final String MONITORING_CHANNEL_NAME = "Sensor Background Monitoring";
    public static final int MONITORING_NOTIFICATION_ID = 10;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel alarmChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            alarmChannel.setDescription("Alerts when sensor thresholds are breached");
            manager.createNotificationChannel(alarmChannel);

            NotificationChannel monitoringChannel = new NotificationChannel(
                    MONITORING_CHANNEL_ID,
                    MONITORING_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            monitoringChannel.setDescription("Ongoing background sensor monitoring");
            manager.createNotificationChannel(monitoringChannel);
        }
    }

    public static Notification createMonitoringNotification(Context context) {
        createNotificationChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sensors)
                .setContentTitle("Notification Mode Active")
                .setContentText("Monitoring temperature, humidity, and luminosity in background")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    public static void sendAlarmNotification(Context context, int notificationId, String title, String message) {
        Preferences prefs = new Preferences(context);
        if (!prefs.isEnableNotifications()) {
            return;
        }
        createNotificationChannels(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, SensorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_sensors)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }
}