package com.android5.internetbrowser;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class InternetBrowserApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // This applies dynamic colors to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}