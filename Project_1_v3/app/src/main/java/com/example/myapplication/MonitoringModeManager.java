package com.example.myapplication;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public final class MonitoringModeManager {

    private MonitoringModeManager() {
    }

    public static void apply(Context context) {

        Preferences preferences =
                new Preferences(context);

        Intent serviceIntent =
                new Intent(
                        context,
                        SensorMonitoringService.class
                );

        if (preferences.isContinuousMode()) {

            ContextCompat.startForegroundService(
                    context,
                    serviceIntent
            );

        } else {

            context.stopService(serviceIntent);
        }
    }
}