package com.android5.internetbrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<HistoryActivity.HistoryItem> historyList;

    public HistoryAdapter(List<HistoryActivity.HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryActivity.HistoryItem item = historyList.get(position);
        holder.titleText.setText(item.title);
        holder.urlText.setText(item.url);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView urlText;

        HistoryViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.history_title);
            urlText = itemView.findViewById(R.id.history_url);
        }
    }
}