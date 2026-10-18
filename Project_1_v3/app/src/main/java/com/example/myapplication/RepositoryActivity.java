package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class RepositoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private TextView tvTempHistory;
    private TextView tvHumHistory;
    private TextView tvLumHistory;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm:ss",
                    Locale.getDefault()
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_repository);

        configureSystemInsets();

        dbHelper = new DatabaseHelper(this);

        initializeViews();
        configureToolbar();
        configureClearButtons();

        loadReadings();
    }


    private void configureSystemInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }


    private void initializeViews() {

        tvTempHistory =
                findViewById(R.id.tvTempHistory);

        tvHumHistory =
                findViewById(R.id.tvHumHistory);

        tvLumHistory =
                findViewById(R.id.tvLumHistory);
    }


    private void configureToolbar() {

        Toolbar toolbar =
                findViewById(R.id.toolbarRepository);

        // Back button.
        toolbar.setNavigationOnClickListener(
                view -> finish()
        );

        // Refresh button.
        toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.actionRefresh) {

                refreshRepository();

                return true;
            }

            return false;
        });
    }


    private void configureClearButtons() {

        Button btnClearTemp =
                findViewById(R.id.btnClearTemp);

        Button btnClearHum =
                findViewById(R.id.btnClearHum);

        Button btnClearLum =
                findViewById(R.id.btnClearLum);

        Button btnClearAll =
                findViewById(R.id.btnClearHistory);


        btnClearTemp.setOnClickListener(view ->
                confirmClear(
                        DatabaseHelper.TEMPERATURE,
                        getString(R.string.sensor_temperature)
                )
        );


        btnClearHum.setOnClickListener(view ->
                confirmClear(
                        DatabaseHelper.HUMIDITY,
                        getString(R.string.sensor_humidity)
                )
        );


        btnClearLum.setOnClickListener(view ->
                confirmClear(
                        DatabaseHelper.LUMINOSITY,
                        getString(R.string.sensor_luminosity)
                )
        );


        btnClearAll.setOnClickListener(
                view -> confirmClearAll()
        );
    }


    @Override
    protected void onResume() {
        super.onResume();


        MonitoringModeManager.apply(this);

        // Reload stored readings.
        loadReadings();
    }


    private void refreshRepository() {


        loadReadings();

        Toast.makeText(
                this,
                "Repository refreshed",
                Toast.LENGTH_SHORT
        ).show();
    }


    private void loadReadings() {

        List<SensorReading> tempReadings =
                dbHelper.getLastReadings(
                        DatabaseHelper.TEMPERATURE
                );

        List<SensorReading> humReadings =
                dbHelper.getLastReadings(
                        DatabaseHelper.HUMIDITY
                );

        List<SensorReading> lumReadings =
                dbHelper.getLastReadings(
                        DatabaseHelper.LUMINOSITY
                );


        tvTempHistory.setText(
                formatReadings(
                        tempReadings,
                        "°C",
                        1
                )
        );


        tvHumHistory.setText(
                formatReadings(
                        humReadings,
                        "%",
                        0
                )
        );


        tvLumHistory.setText(
                formatReadings(
                        lumReadings,
                        "lx",
                        0
                )
        );
    }


    private String formatReadings(
            List<SensorReading> readings,
            String unit,
            int decimals) {

        if (readings.isEmpty()) {
            return "No readings available";
        }


        StringBuilder result =
                new StringBuilder();


        String format =
                decimals == 1
                        ? "%.1f"
                        : "%.0f";


        for (SensorReading reading : readings) {

            String value =
                    String.format(
                            Locale.getDefault(),
                            format,
                            reading.getValue()
                    );


            String date =
                    dateFormat.format(
                            new Date(
                                    reading.getTimestamp()
                            )
                    );


            result.append(value)
                    .append(" ")
                    .append(unit)
                    .append("  |  ")
                    .append(date)
                    .append("\n");
        }


        return result.toString().trim();
    }


    private void confirmClear(
            String sensorType,
            String sensorName) {

        new AlertDialog.Builder(this)

                .setTitle(
                        "Clear " + sensorName
                )

                .setMessage(
                        "Are you sure you want to delete all "
                                + sensorName
                                + " readings?"
                )

                .setPositiveButton(
                        "Clear",
                        (dialog, which) -> {

                            dbHelper.deleteReadings(
                                    sensorType
                            );

                            loadReadings();
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }



    private void confirmClearAll() {

        new AlertDialog.Builder(this)

                .setTitle(
                        "Clear All History"
                )

                .setMessage(
                        "Are you sure you want to delete all sensor readings?"
                )

                .setPositiveButton(
                        "Clear All",
                        (dialog, which) -> {

                            dbHelper.clearReadings();

                            loadReadings();
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }


    @Override
    protected void onDestroy() {

        // Close the database connection.
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}