package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge; // Import the EdgeToEdge class, that treats about the edge of the screen
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
        super.onCreate(savedInstanceState); // Call the superclass constructor
        EdgeToEdge.enable(this); // Enable edge-to-edge mode
        setContentView(R.layout.activity_config); // Set the layout for the activity, which is the config one
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        }); // Apply edge-to-edge padding to the main layout

        Toolbar toolbar = findViewById(R.id.toolbarConfig);
        toolbar.setNavigationOnClickListener(v -> finish()); // Back button

        prefs = new Preferences(this); // Initialize the Preferences class

        // 1. Operating Mode Switch (Continuous background service vs Activity only)
        SwitchCompat switchOperatingMode = findViewById(R.id.switchOperatingMode);
        switchOperatingMode.setChecked(prefs.isContinuousMode());
        switchOperatingMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setContinuousMode(isChecked);
            SensorMonitoringService.sync(this);
        });

        // 2. Notifications Master Switch
        // Set up the switch buttons and their listeners
        switchNotifications = findViewById(R.id.switchNotifications); // Find the switch by its ID
        switchNotifications.setChecked(prefs.isEnableNotifications() && Notifications.hasPermission(this)); // Set the switch state based on the saved value
        switchNotifications.setOnCheckedChangeListener( // Set the listener for the switch
                (buttonView, isChecked) -> {
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


        SwitchCompat switchTempAlarm = findViewById(R.id.switchTempAlarm); // Find the switch by its ID
        switchTempAlarm.setChecked(prefs.isTempAlarmEnabled());
        switchTempAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setTempAlarmEnabled(isChecked));

        SwitchCompat switchHumidityAlarm = findViewById(R.id.switchHumidityAlarm);
        switchHumidityAlarm.setChecked(prefs.isHumidityAlarmEnabled());
        switchHumidityAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setHumidityAlarmEnabled(isChecked));

        SwitchCompat switchLuminosityAlarm = findViewById(R.id.switchLuminosityAlarm);
        switchLuminosityAlarm.setChecked(prefs.isLuminosityAlarmEnabled());
        switchLuminosityAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setLuminosityAlarmEnabled(isChecked));

        // Set up the text input edit fields
        TextInputEditText etTempHigh = findViewById(R.id.etTempHigh);
        TextInputEditText etTempLow = findViewById(R.id.etTempLow);
        TextInputEditText etHumidityHigh = findViewById(R.id.etHumidityHigh);
        TextInputEditText etHumidityLow = findViewById(R.id.etHumidityLow);
        TextInputEditText etLuminosityHigh = findViewById(R.id.etLuminosityHigh);
        TextInputEditText etLuminosityLow = findViewById(R.id.etLuminosityLow);

        // Set the text fields to the saved values
        etTempHigh.setText(String.valueOf(prefs.getTempHigh()));
        etTempLow.setText(String.valueOf(prefs.getTempLow()));
        etHumidityHigh.setText(String.valueOf(prefs.getHumidityHigh()));
        etHumidityLow.setText(String.valueOf(prefs.getHumidityLow()));
        etLuminosityHigh.setText(String.valueOf(prefs.getLuminosityHigh()));
        etLuminosityLow.setText(String.valueOf(prefs.getLuminosityLow()));

        // Set up the save button and its listener
        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {

            // try-catch to handle exceptions like invalid input
            try {

                // First, we convert the six text fields to numbers.
                // If any of them is invalid, we go to catch block,
                // without executing the rest of the code.
                float tempLow = readNumber(etTempLow);
                float tempHigh = readNumber(etTempHigh);
                float humLow = readNumber(etHumidityLow);
                float humHigh = readNumber(etHumidityHigh);
                float lumLow = readNumber(etLuminosityLow);
                float lumHigh = readNumber(etLuminosityHigh);

                // Clear messages from the previous attempt.
                etTempLow.setError(null);
                etHumidityLow.setError(null);
                etLuminosityLow.setError(null);

                if (tempLow >= tempHigh) {
                    String message = "Temperature: Low (" + tempLow + ") must be lower than High (" + tempHigh + ").";
                    etTempLow.setError(message);
                    etTempLow.requestFocus(); // Moves the focus to the field.
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

                // Only when all the checks pass, we save the values.
                prefs.setTempLow(tempLow);
                prefs.setTempHigh(tempHigh);
                prefs.setHumidityLow(humLow);
                prefs.setHumidityHigh(humHigh);
                prefs.setLuminosityLow(lumLow);
                prefs.setLuminosityHigh(lumHigh);

                // Finally, we show a success message
                Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show();
            }
            // If readNumber can't obtain a number,
            // it throws a NumberFormatException.
            // The written values are still in the fields
            // so we can correct them.
            catch (NumberFormatException e) {
                Toast.makeText(this, "Fill the limits with 6 valid numbers.", Toast.LENGTH_LONG).show();
            }
        });

        // Reset measured extremes and restore the six default alarm thresholds.
        // Alarm switches and notification mode keep their current state.
        Button btnReset = findViewById(R.id.btnReset);
        btnReset.setOnClickListener(v -> {

            // Remove recorded minimums, maximums and their timestamps.
            prefs.resetMinMax();

            // Save the default thresholds,
            // so they remain after reopening the app.
            prefs.setTempLow(15.0f);
            prefs.setTempHigh(30.0f);
            prefs.setHumidityLow(30.0f);
            prefs.setHumidityHigh(70.0f);
            prefs.setLuminosityLow(50.0f);
            prefs.setLuminosityHigh(1000.0f);

            // Refresh the visible fields with the values we have just saved.
            etTempLow.setText(String.valueOf(prefs.getTempLow()));
            etTempHigh.setText(String.valueOf(prefs.getTempHigh()));
            etHumidityLow.setText(String.valueOf(prefs.getHumidityLow()));
            etHumidityHigh.setText(String.valueOf(prefs.getHumidityHigh()));
            etLuminosityLow.setText(String.valueOf(prefs.getLuminosityLow()));
            etLuminosityHigh.setText(String.valueOf(prefs.getLuminosityHigh()));

            // Remove validation errors left by a previous save attempt.
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

    // Auxiliary function to read a number from a text field.
    // It's on the Class but outside onCreate,
    // it's private, it returns a float and field
    // is the parameter to get the text from.
    private float readNumber(TextInputEditText field) {

        // getText obtains the editable content.
        if (field.getText() == null) {

            // Interrupts the method, delivers the error to catch.
            throw new NumberFormatException();
        }

        // It converts the text to a string and replaces commas.
        // trim() removes spaces at the beginning and at the end.
        String text = field.getText().toString().trim().replace(',', '.');

        // It converts the text to a float.
        // If the text is empty or not a number,
        // it throws a NumberFormatException.
        float value = Float.parseFloat(text);
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new NumberFormatException();
        }

        // Returns the converted value.
        return value;
    }
}