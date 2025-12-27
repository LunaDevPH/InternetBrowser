package com.android5.internetbrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    private final List<DownloadItem> downloadList;
    private final OnDownloadActionListener listener;

    public interface OnDownloadActionListener {
        void onCancel(long id);
        void onDelete(long id);
    }

    public static class DownloadItem {
        public long id;
        public String title;
        public String status;
        public int progress;
        public int statusCode;

        public DownloadItem(long id, String title, String status, int progress, int statusCode) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.progress = progress;
            this.statusCode = statusCode;
        }
    }

    public DownloadAdapter(List<DownloadItem> downloadList, OnDownloadActionListener listener) {
        this.downloadList = downloadList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = downloadList.get(position);
        holder.titleText.setText(item.title);
        holder.statusText.setText(item.status);
        holder.progressBar.setProgress(item.progress);
        holder.percentageText.setText(item.progress + "%");
        
        if (item.progress < 0) {
            holder.progressBar.setIndeterminate(true);
            holder.percentageText.setVisibility(View.GONE);
        } else {
            holder.progressBar.setIndeterminate(false);
            holder.percentageText.setVisibility(View.VISIBLE);
        }

        // Show Cancel only if running/pending
        if (item.statusCode == 1 || item.statusCode == 2 || item.statusCode == 4) { // PENDING, RUNNING, PAUSED
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        holder.btnCancel.setOnClickListener(v -> listener.onCancel(item.id));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item.id));
    }

    @Override
    public int getItemCount() {
        return downloadList.size();
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView statusText;
        TextView percentageText;
        LinearProgressIndicator progressBar;
        Button btnCancel, btnDelete;

        DownloadViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.download_title);
            statusText = itemView.findViewById(R.id.download_status);
            percentageText = itemView.findViewById(R.id.download_percentage);
            progressBar = itemView.findViewById(R.id.download_progress_bar);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}