package com.android5.internetbrowser;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "browser_history";
    private static final String KEY_HISTORY = "history_list";
    private final SharedPreferences prefs;
    private final Gson gson;

    public HistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void addHistoryItem(String title, String url) {
        List<HistoryActivity.HistoryItem> history = getHistory();
        // Don't add duplicate consecutive items
        if (!history.isEmpty() && history.get(0).url.equals(url)) {
            return;
        }
        history.add(0, new HistoryActivity.HistoryItem(title, url));
        // Keep only last 100 items for performance
        if (history.size() > 100) {
            history = history.subList(0, 100);
        }
        saveHistory(history);
    }

    public List<HistoryActivity.HistoryItem> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<HistoryActivity.HistoryItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveHistory(List<HistoryActivity.HistoryItem> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }
}