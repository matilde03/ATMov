package com.example.project_1v2;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class RepositoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ReadingAdapter adapter;
    private List<ReadingItem> readingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_repository);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.repository_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarRepository);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rvReadings);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadReadings(recyclerView);

        MaterialButton btnClear = findViewById(R.id.btnClearRepo);
        btnClear.setOnClickListener(v -> {
            dbHelper.clearRepository();
            readingsList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            Toast.makeText(this, R.string.repository_cleared, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecyclerView recyclerView = findViewById(R.id.rvReadings);
        loadReadings(recyclerView);
    }

    private void loadReadings(RecyclerView recyclerView) {
        readingsList = dbHelper.getRecentReadings();
        adapter = new ReadingAdapter(readingsList);
        recyclerView.setAdapter(adapter);
    }
}
