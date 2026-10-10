package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Choose older modulo, it's faster - import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001; // Arbitrary request code for notification permission

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkNotificationPermission();

        CardView cardSensor = findViewById(R.id.cwTemp);
        CardView cardConfig = findViewById(R.id.cwConfig);
        CardView cardRepository = findViewById(R.id.cwRepository);

        cardSensor.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SensorActivity.class);
            startActivity(intent);
        });

        cardConfig.setOnClickListener(v -> {
            //Intent intent = new Intent(MainActivity.this, ConfigActivity.class);
            //startActivity(intent);
        });

        cardRepository.setOnClickListener(v -> {
            //Intent intent = new Intent(MainActivity.this, RepositoryActivity.class);
            //startActivity(intent);
        });


    } // end of onCreate()



    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE
                );
            }
        }
    } // end of checkNotificationPermission()

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if this is the answer to specific notification request
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:

                    // Check if the result array has data and if the first item is "GRANTED"
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Show a success message.
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show();

                } else {

                    // Show a failure message.
                    Toast.makeText(this, "Alarm notifications are disabled.", Toast.LENGTH_SHORT).show();

                }

        }
    } // end of onRequestPermissionsResult()

}