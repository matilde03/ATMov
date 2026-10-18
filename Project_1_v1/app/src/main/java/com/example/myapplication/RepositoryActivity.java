package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class RepositoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvTempHistory;
    private TextView tvHumHistory;
    private TextView tvLumHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_repository);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        tvTempHistory = findViewById(R.id.tvTempHistory);
        tvHumHistory = findViewById(R.id.tvHumHistory);
        tvLumHistory = findViewById(R.id.tvLumHistory);

        Toolbar toolbar = findViewById(R.id.toolbarRepository);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actionRefresh) {
                loadReadings();
                Toast.makeText(this, "Repository refreshed", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        findViewById(R.id.btnClearTemp).setOnClickListener(v ->
                confirmClear(DatabaseHelper.TEMPERATURE, getString(R.string.sensor_temperature)));
        findViewById(R.id.btnClearHum).setOnClickListener(v ->
                confirmClear(DatabaseHelper.HUMIDITY, getString(R.string.sensor_humidity)));
        findViewById(R.id.btnClearLum).setOnClickListener(v ->
                confirmClear(DatabaseHelper.LUMINOSITY, getString(R.string.sensor_luminosity)));
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> confirmClearAll());

        loadReadings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReadings();
    }

    private void loadReadings() {
        tvTempHistory.setText(dbHelper.getFormattedReadings(DatabaseHelper.TEMPERATURE, "°C", 1));
        tvHumHistory.setText(dbHelper.getFormattedReadings(DatabaseHelper.HUMIDITY, "%", 0));
        tvLumHistory.setText(dbHelper.getFormattedReadings(DatabaseHelper.LUMINOSITY, "lx", 0));
    }



    private void confirmClear(String sensorType, String sensorName) {
        new AlertDialog.Builder(this)
                .setTitle("Clear " + sensorName)
                .setMessage("Are you sure you want to delete all " + sensorName + " readings?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    dbHelper.deleteReadings(sensorType);
                    loadReadings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Are you sure you want to delete all sensor readings?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    dbHelper.clearReadings();
                    loadReadings();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}