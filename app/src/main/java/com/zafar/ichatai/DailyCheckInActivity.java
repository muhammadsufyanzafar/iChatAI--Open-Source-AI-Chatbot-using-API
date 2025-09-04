package com.zafar.ichatai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.chip.Chip;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DailyCheckInActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "DailyCheckInPrefs";
    private static final String LAST_CHECK_IN_KEY = "lastCheckInTimestamp";
    private static final String STREAK_KEY = "currentStreak";

    private SharedPreferences sharedPrefs;
    private CreditsManager creditsManager;

    private Button checkInButton;
    private Chip chipCredits;
    private View[] dayViews;
    private TextView[] dayStatusTextViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_check_in);

        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        creditsManager = new CreditsManager(this);

        ImageButton btnClose = findViewById(R.id.btnClose);
        chipCredits = findViewById(R.id.chipCredits);
        checkInButton = findViewById(R.id.check_in_button);

        dayViews = new View[]{
                findViewById(R.id.day1), findViewById(R.id.day2), findViewById(R.id.day3),
                findViewById(R.id.day4), findViewById(R.id.day5), findViewById(R.id.day6),
                findViewById(R.id.day7)
        };

        dayStatusTextViews = new TextView[]{
                findViewById(R.id.day1_status), findViewById(R.id.day2_status), findViewById(R.id.day3_status),
                findViewById(R.id.day4_status), findViewById(R.id.day5_status), findViewById(R.id.day6_status),
                findViewById(R.id.day7_status)
        };

        btnClose.setOnClickListener(v -> finish());

        chipCredits.setOnClickListener(v -> {
            startActivity(new Intent(DailyCheckInActivity.this, CreditsActivity.class));
        });

        checkInButton.setOnClickListener(v -> checkIn());

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCreditsUI();
        updateUI();
    }

    private void updateCreditsUI() {
        chipCredits.setText(String.valueOf(creditsManager.get()));
    }

    private void updateUI() {
        long lastCheckIn = sharedPrefs.getLong(LAST_CHECK_IN_KEY, 0);
        int streak = sharedPrefs.getInt(STREAK_KEY, 0);
        long currentTime = System.currentTimeMillis();

        // Check if streak is broken
        if (lastCheckIn > 0) {
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTimeInMillis(lastCheckIn);
            Calendar currentCal = Calendar.getInstance();
            currentCal.setTimeInMillis(currentTime);

            long daysBetween = TimeUnit.MILLISECONDS.toDays(currentCal.getTimeInMillis()) - TimeUnit.MILLISECONDS.toDays(lastCal.getTimeInMillis());

            if (daysBetween > 1) {
                resetStreak();
                streak = 0; // Update local variable after reset
            }
        }

        boolean canCheckInToday = true;
        if(lastCheckIn > 0){
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTimeInMillis(lastCheckIn);
            Calendar currentCal = Calendar.getInstance();
            if(lastCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && lastCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)){
                canCheckInToday = false;
            }
        }


        // Update day views based on streak
        for (int i = 0; i < dayViews.length; i++) {
            CardView card = (CardView) dayViews[i];
            TextView status = dayStatusTextViews[i];

            if (i < streak) {
                // Already claimed
                card.setCardBackgroundColor(getResources().getColor(R.color.green_200));
                status.setText("Claimed");
                status.setTextColor(getResources().getColor(R.color.green_700));
            } else {
                // Not claimed yet
                card.setCardBackgroundColor(getResources().getColor(R.color.colorSurfaceVariant));
                status.setText("Unclaimed");
                status.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        }

        // Update button state
        if (canCheckInToday) {
            checkInButton.setEnabled(true);
            checkInButton.setText("Check In Now");
        } else {
            checkInButton.setEnabled(false);
            checkInButton.setText("Checked In Today");
        }
    }


    private void checkIn() {
        int streak = sharedPrefs.getInt(STREAK_KEY, 0);

        // If streak is 7, it should have been reset. But as a safeguard:
        if (streak >= 7) {
            resetStreak();
            streak = 0;
        }

        streak++;

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(LAST_CHECK_IN_KEY, System.currentTimeMillis());
        editor.putInt(STREAK_KEY, streak);
        editor.apply();

        earnCredits(streak);
        updateUI();
        Toast.makeText(this, "Checked in! Streak: " + streak, Toast.LENGTH_SHORT).show();

        if (streak >= 7) {
            resetStreak();
        }
    }


    private void resetStreak() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(STREAK_KEY, 0);
        editor.apply();
    }

    private void earnCredits(int streak) {
        int credits = 0;
        switch (streak) {
            case 1: credits = 5; break;
            case 2: credits = 10; break;
            case 3: credits = 15; break;
            case 4: credits = 20; break;
            case 5: credits = 25; break;
            case 6: credits = 30; break;
            case 7: credits = 50; break; // Jackpot
        }

        if (credits > 0) {
            creditsManager.add(credits);
            sendBroadcast(new Intent(MainActivity.ACTION_CREDITS_CHANGED));
            updateCreditsUI();
            Toast.makeText(this, "You earned " + credits + " credits!", Toast.LENGTH_SHORT).show();
        }
    }
}
