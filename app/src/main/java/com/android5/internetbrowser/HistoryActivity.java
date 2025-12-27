package com.android5.internetbrowser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private MaterialButton btnExportCsv;
    private HistoryManager historyManager;

    public static class HistoryItem {
        public String title;
        public String url;

        public HistoryItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    private final ActivityResultLauncher<String> createFileLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    saveCsvToFile(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        historyManager = new HistoryManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.history_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.history_recycler_view);
        btnExportCsv = findViewById(R.id.btn_export_csv);

        // Load real history data
        historyList = historyManager.getHistory();

        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        btnExportCsv.setOnClickListener(v -> {
            if (historyList.isEmpty()) {
                Toast.makeText(this, "No history to export", Toast.LENGTH_SHORT).show();
            } else {
                createFileLauncher.launch("browser_history.csv");
            }
        });
    }

    private void saveCsvToFile(Uri uri) {
        try {
            StringBuilder csv = new StringBuilder("Title,URL\n");
            for (HistoryItem item : historyList) {
                // Escape commas in titles
                String escapedTitle = item.title.replace(",", " ");
                csv.append(escapedTitle).append(",").append(item.url).append("\n");
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(csv.toString().getBytes());
                outputStream.close();
                Toast.makeText(this, R.string.msg_history_exported, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}