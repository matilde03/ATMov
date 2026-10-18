package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class ConfigActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private Preferences prefs;
    private SwitchCompat switchNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbarConfig);
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = new Preferences(this);

        // 1. Operating Mode Switch (Continuous background service vs Activity only)
        SwitchCompat switchOperatingMode = findViewById(R.id.switchOperatingMode);
        switchOperatingMode.setChecked(prefs.isContinuousMode());
        switchOperatingMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setContinuousMode(isChecked);
            SensorMonitoringService.sync(this);
        });

        // 2. Notifications Master Switch
        switchNotifications = findViewById(R.id.switchNotifications);
        switchNotifications.setChecked(prefs.isEnableNotifications() && Notifications.hasPermission(this));
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !Notifications.hasPermission(this)) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            } else {
                prefs.setEnableNotifications(isChecked);
            }
        });

        SwitchCompat switchTempAlarm = findViewById(R.id.switchTempAlarm);
        switchTempAlarm.setChecked(prefs.isTempAlarmEnabled());
        switchTempAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setTempAlarmEnabled(isChecked));

        SwitchCompat switchHumidityAlarm = findViewById(R.id.switchHumidityAlarm);
        switchHumidityAlarm.setChecked(prefs.isHumidityAlarmEnabled());
        switchHumidityAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setHumidityAlarmEnabled(isChecked));

        SwitchCompat switchLuminosityAlarm = findViewById(R.id.switchLuminosityAlarm);
        switchLuminosityAlarm.setChecked(prefs.isLuminosityAlarmEnabled());
        switchLuminosityAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setLuminosityAlarmEnabled(isChecked));

        TextInputEditText etTempHigh = findViewById(R.id.etTempHigh);
        TextInputEditText etTempLow = findViewById(R.id.etTempLow);
        TextInputEditText etHumidityHigh = findViewById(R.id.etHumidityHigh);
        TextInputEditText etHumidityLow = findViewById(R.id.etHumidityLow);
        TextInputEditText etLuminosityHigh = findViewById(R.id.etLuminosityHigh);
        TextInputEditText etLuminosityLow = findViewById(R.id.etLuminosityLow);

        etTempHigh.setText(String.valueOf(prefs.getTempHigh()));
        etTempLow.setText(String.valueOf(prefs.getTempLow()));
        etHumidityHigh.setText(String.valueOf(prefs.getHumidityHigh()));
        etHumidityLow.setText(String.valueOf(prefs.getHumidityLow()));
        etLuminosityHigh.setText(String.valueOf(prefs.getLuminosityHigh()));
        etLuminosityLow.setText(String.valueOf(prefs.getLuminosityLow()));

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            try {
                float tempLow = readNumber(etTempLow);
                float tempHigh = readNumber(etTempHigh);
                float humLow = readNumber(etHumidityLow);
                float humHigh = readNumber(etHumidityHigh);
                float lumLow = readNumber(etLuminosityLow);
                float lumHigh = readNumber(etLuminosityHigh);

                etTempLow.setError(null);
                etHumidityLow.setError(null);
                etLuminosityLow.setError(null);

                if (tempLow >= tempHigh) {
                    String message = "Temperature: Low (" + tempLow + ") must be lower than High (" + tempHigh + ").";
                    etTempLow.setError(message);
                    etTempLow.requestFocus();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    return;
                }

                if (humLow >= humHigh) {
                    String message = "Humidity: Low (" + humLow + ") must be lower than High (" + humHigh + ").";
                    etHumidityLow.setError(message);
                    etHumidityLow.requestFocus();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    return;
                }

                if (lumLow >= lumHigh) {
                    String message = "Luminosity: Low (" + lumLow + ") must be lower than High (" + lumHigh + ").";
                    etLuminosityLow.setError(message);
                    etLuminosityLow.requestFocus();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    return;
                }

                if (humLow < 0 || humHigh > 100) {
                    Toast.makeText(this, "Humidity must be between 0 and 100%.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (lumLow < 0) {
                    Toast.makeText(this, "Luminosity cannot be negative.", Toast.LENGTH_LONG).show();
                    return;
                }

                prefs.setTempLow(tempLow);
                prefs.setTempHigh(tempHigh);
                prefs.setHumidityLow(humLow);
                prefs.setHumidityHigh(humHigh);
                prefs.setLuminosityLow(lumLow);
                prefs.setLuminosityHigh(lumHigh);

                Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Fill the limits with 6 valid numbers.", Toast.LENGTH_LONG).show();
            }
        });

        Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(v -> {
            prefs.resetMinMax();

            prefs.setTempLow(15.0f);
            prefs.setTempHigh(30.0f);
            prefs.setHumidityLow(30.0f);
            prefs.setHumidityHigh(70.0f);
            prefs.setLuminosityLow(50.0f);
            prefs.setLuminosityHigh(1000.0f);

            etTempLow.setText(String.valueOf(prefs.getTempLow()));
            etTempHigh.setText(String.valueOf(prefs.getTempHigh()));
            etHumidityLow.setText(String.valueOf(prefs.getHumidityLow()));
            etHumidityHigh.setText(String.valueOf(prefs.getHumidityHigh()));
            etLuminosityLow.setText(String.valueOf(prefs.getLuminosityLow()));
            etLuminosityHigh.setText(String.valueOf(prefs.getLuminosityHigh()));

            etTempLow.setError(null);
            etTempHigh.setError(null);
            etHumidityLow.setError(null);
            etHumidityHigh.setError(null);
            etLuminosityLow.setError(null);
            etLuminosityHigh.setError(null);

            Toast.makeText(this, "Min/max reset and default limits restored.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                prefs.setEnableNotifications(true);
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();
            } else {
                prefs.setEnableNotifications(false);
                switchNotifications.setChecked(false);
                Toast.makeText(this, "Notifications disabled! Please enable them in the app settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private float readNumber(TextInputEditText field) {
        if (field.getText() == null) {
            throw new NumberFormatException();
        }
        String text = field.getText().toString().trim().replace(',', '.');
        float valor = Float.parseFloat(text);
        if (Float.isNaN(valor) || Float.isInfinite(valor)) {
            throw new NumberFormatException();
        }
        return valor;
    }
}