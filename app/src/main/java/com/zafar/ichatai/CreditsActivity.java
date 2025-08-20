package com.zafar.ichatai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CreditsActivity extends AppCompatActivity {

    private CreditsManager credits;
    private TextView tvCreditsAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_credits);

        credits = new CreditsManager(this);
        tvCreditsAmount = findViewById(R.id.tvCreditsAmount);
        ImageButton btnClose = findViewById(R.id.btnCloseCredits);
        MaterialButton btnWatchAd = findViewById(R.id.btnWatchAd);

        btnClose.setOnClickListener(v -> finish());

        btnWatchAd.setOnClickListener(v -> {
            AdHelper.showRewarded(this, rewarded -> {
                if (rewarded) {
                    credits.add(5);
                    updateCredits();
                    sendBroadcast(new Intent(MainActivity.ACTION_CREDITS_CHANGED));
                    Toast.makeText(this, "+5 credits added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Ad not completed.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        updateCredits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCredits();
    }

    private void updateCredits() {
        tvCreditsAmount.setText(String.valueOf(credits.get()));
    }
}
