package com.example.project_1v2;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class ConfigActivity extends AppCompatActivity {

    private PreferencesManager prefsManager;

    private MaterialSwitch switchNotificationMode;
    private MaterialSwitch switchTempAlarm, switchHumidityAlarm, switchLuminosityAlarm;
    private TextInputEditText etTempHigh, etTempLow;
    private TextInputEditText etHumidityHigh, etHumidityLow;
    private TextInputEditText etLuminosityHigh, etLuminosityLow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.config_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefsManager = new PreferencesManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarConfig);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchNotificationMode = findViewById(R.id.switchNotificationMode);

        switchTempAlarm = findViewById(R.id.switchTempAlarm);
        etTempHigh = findViewById(R.id.etTempHigh);
        etTempLow = findViewById(R.id.etTempLow);

        switchHumidityAlarm = findViewById(R.id.switchHumidityAlarm);
        etHumidityHigh = findViewById(R.id.etHumidityHigh);
        etHumidityLow = findViewById(R.id.etHumidityLow);

        switchLuminosityAlarm = findViewById(R.id.switchLuminosityAlarm);
        etLuminosityHigh = findViewById(R.id.etLuminosityHigh);
        etLuminosityLow = findViewById(R.id.etLuminosityLow);

        loadCurrentConfig();

        MaterialButton btnSave = findViewById(R.id.btnSaveConfig);
        btnSave.setOnClickListener(v -> saveConfig());

        MaterialButton btnReset = findViewById(R.id.btnResetMinMaxConfig);
        btnReset.setOnClickListener(v -> {
            prefsManager.resetMinMax();
            Toast.makeText(this, R.string.min_max_reset_done, Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCurrentConfig() {
        switchNotificationMode.setChecked(prefsManager.isNotificationMode());

        switchTempAlarm.setChecked(prefsManager.isTempAlarmEnabled());
        etTempHigh.setText(String.format(Locale.US, "%.1f", prefsManager.getTempHigh()));
        etTempLow.setText(String.format(Locale.US, "%.1f", prefsManager.getTempLow()));

        switchHumidityAlarm.setChecked(prefsManager.isHumidityAlarmEnabled());
        etHumidityHigh.setText(String.format(Locale.US, "%.1f", prefsManager.getHumidityHigh()));
        etHumidityLow.setText(String.format(Locale.US, "%.1f", prefsManager.getHumidityLow()));

        switchLuminosityAlarm.setChecked(prefsManager.isLuminosityAlarmEnabled());
        etLuminosityHigh.setText(String.format(Locale.US, "%.1f", prefsManager.getLuminosityHigh()));
        etLuminosityLow.setText(String.format(Locale.US, "%.1f", prefsManager.getLuminosityLow()));
    }

    private void saveConfig() {
        float tempHigh = parseFloatSafe(etTempHigh, prefsManager.getTempHigh());
        float tempLow = parseFloatSafe(etTempLow, prefsManager.getTempLow());

        float humHigh = parseFloatSafe(etHumidityHigh, prefsManager.getHumidityHigh());
        float humLow = parseFloatSafe(etHumidityLow, prefsManager.getHumidityLow());

        float lumHigh = parseFloatSafe(etLuminosityHigh, prefsManager.getLuminosityHigh());
        float lumLow = parseFloatSafe(etLuminosityLow, prefsManager.getLuminosityLow());

        prefsManager.setNotificationMode(switchNotificationMode.isChecked());

        prefsManager.setTempAlarmEnabled(switchTempAlarm.isChecked());
        prefsManager.setTempHigh(tempHigh);
        prefsManager.setTempLow(tempLow);

        prefsManager.setHumidityAlarmEnabled(switchHumidityAlarm.isChecked());
        prefsManager.setHumidityHigh(humHigh);
        prefsManager.setHumidityLow(humLow);

        prefsManager.setLuminosityAlarmEnabled(switchLuminosityAlarm.isChecked());
        prefsManager.setLuminosityHigh(lumHigh);
        prefsManager.setLuminosityLow(lumLow);

        Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private float parseFloatSafe(TextInputEditText editText, float fallback) {
        if (editText == null || editText.getText() == null) {
            return fallback;
        }
        String input = editText.getText().toString().trim().replace(',', '.');
        if (input.isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
