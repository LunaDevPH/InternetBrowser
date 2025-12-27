package com.android5.internetbrowser;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity implements DownloadAdapter.OnDownloadActionListener {

    private RecyclerView recyclerView;
    private DownloadAdapter adapter;
    private List<DownloadAdapter.DownloadItem> downloadList;
    private TextView noDownloadsText;
    private DownloadManager downloadManager;
    
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final int UPDATE_INTERVAL = 1000;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            loadDownloads();
            updateHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_downloads);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.downloads_container), (v, insets) -> {
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

        recyclerView = findViewById(R.id.downloads_recycler_view);
        noDownloadsText = findViewById(R.id.no_downloads_text);

        downloadList = new ArrayList<>();
        adapter = new DownloadAdapter(downloadList, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void loadDownloads() {
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query);

        if (cursor != null && cursor.moveToFirst()) {
            List<DownloadAdapter.DownloadItem> newList = new ArrayList<>();
            int idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

            do {
                long id = cursor.getLong(idIdx);
                String title = cursor.getString(titleIdx);
                int status = cursor.getInt(statusIdx);
                long total = cursor.getLong(totalIdx);
                long downloaded = cursor.getLong(downloadedIdx);
                
                int progress = 0;
                if (total > 0) {
                    progress = (int) ((downloaded * 100L) / total);
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    progress = 100;
                }

                String statusText;
                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL: statusText = "Completed"; break;
                    case DownloadManager.STATUS_RUNNING: statusText = "Downloading... " + formatSize(downloaded) + " / " + formatSize(total); break;
                    case DownloadManager.STATUS_PENDING: statusText = "Pending"; break;
                    case DownloadManager.STATUS_PAUSED: statusText = "Paused"; break;
                    case DownloadManager.STATUS_FAILED: statusText = "Failed"; break;
                    default: statusText = "Unknown"; break;
                }
                
                newList.add(new DownloadAdapter.DownloadItem(id, title, statusText, progress, status));
            } while (cursor.moveToNext());

            downloadList.clear();
            downloadList.addAll(newList);
            noDownloadsText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        } else {
            noDownloadsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        if (cursor != null) cursor.close();
    }

    @Override
    public void onCancel(long id) {
        downloadManager.remove(id);
        Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();
        loadDownloads();
    }

    @Override
    public void onDelete(long id) {
        downloadManager.remove(id);
        Toast.makeText(this, "Download removed", Toast.LENGTH_SHORT).show();
        loadDownloads();
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}