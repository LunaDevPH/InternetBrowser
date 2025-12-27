package com.android5.internetbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.misc);

        ImageView logo = findViewById(R.id.imageView);
        
        // Load and start animation
        Animation fadeInZoom = AnimationUtils.loadAnimation(this, R.anim.fade_in_zoom);
        logo.startAnimation(fadeInZoom);

        // Transition to MainActivity after 2.5 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            // Add a smooth cross-fade transition between activities
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}