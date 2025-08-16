package com.zafar.ichatai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this); // must run BEFORE setContentView
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.logoImage);
        TextView appName = findViewById(R.id.appName);
        TextView tagline = findViewById(R.id.tagline);

        // Logo scale-in
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.85f, 1.0f,
                0.85f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(700);
        scaleAnim.setFillAfter(true);
        logo.startAnimation(scaleAnim);

        // Fade-in texts
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(900);
        fadeIn.setStartOffset(150);
        fadeIn.setFillAfter(true);
        appName.startAnimation(fadeIn);
        tagline.startAnimation(fadeIn);

        // Navigate after a short delay (so the splash is visible)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashScreen.this, MainActivity.class));
            finish();
        }, 1500);
    }
}
