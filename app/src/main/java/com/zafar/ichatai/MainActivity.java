package com.zafar.ichatai;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetworkUtil.isConnected(context)) {
                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG).show();
            }
        }
    };

    private boolean doubleBackToExitPressedOnce = false;
    private int sendQueryButtonClickCount = 0;
    private RewardedAd rewardedAd;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});

        // Load Rewarded Ad
        loadRewardedAd();

        // Schedule the Rewarded Ad to show after 30 seconds
        handler = new Handler();
        handler.postDelayed(this::showRewardedAd, 30000); // 30,000 ms = 30 seconds

        TextInputEditText queryEditText = findViewById(R.id.queryEditText);
        Button sendQueryButton = findViewById(R.id.sendPromptButton);
        TextView responseTextView = findViewById(R.id.modelResponseTextView);
        ImageButton copyButton = findViewById(R.id.copyButton);
        ProgressBar progressBar = findViewById(R.id.sendPromptProgressBar);

        sendQueryButton.setOnClickListener(v -> {
            if (!NetworkUtil.isConnected(this)) {
                Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
                return;
            }

            String query = queryEditText.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please add your query", Toast.LENGTH_SHORT).show();
                return;
            }

            sendQueryButtonClickCount++;
            if (sendQueryButtonClickCount == 3 && rewardedAd != null) {
                showRewardedAd(queryEditText, responseTextView, copyButton, progressBar);
                sendQueryButtonClickCount = 0; // Reset after showing the ad path
            } else {
                sendQuery(queryEditText, responseTextView, copyButton, progressBar);
            }
        });

        copyButton.setOnClickListener(v -> {
            String textToCopy = responseTextView.getText().toString();
            if (!textToCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", textToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Register the network receiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    // Show rewarded ad, then proceed to send the query once user earns the reward.
    private void showRewardedAd(TextInputEditText queryEditText, TextView responseTextView, ImageButton copyButton, ProgressBar progressBar) {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                // After reward, call sendQuery
                sendQuery(queryEditText, responseTextView, copyButton, progressBar);
                // Preload next ad
                loadRewardedAd();
            });
        } else {
            // Fallback: just send the query if ad isn't ready
            sendQuery(queryEditText, responseTextView, copyButton, progressBar);
            Log.d("MainActivity", "Rewarded ad not ready; proceeding without ad.");
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.interstitial_ad_unit_id), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
            }
        });
    }

    private void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                // Load the next rewarded ad
                loadRewardedAd();
            });
        } else {
            Log.d("MainActivity", "Rewarded ad not ready yet.");
        }
    }

    private void sendQuery(TextInputEditText queryEditText, TextView responseTextView, ImageButton copyButton, ProgressBar progressBar) {
        DeepSeekClient model = new DeepSeekClient();
        String query = queryEditText.getText().toString();
        progressBar.setVisibility(View.VISIBLE);

        responseTextView.setText("");
        queryEditText.setText("");

        try {
            model.getResponse(query, new ResponseCallback() {
                @Override
                public void onResponse(String response) {
                    runOnUiThread(() -> {
                        responseTextView.setText(response);
                        progressBar.setVisibility(View.GONE);
                        copyButton.setVisibility(response.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        copyButton.setVisibility(View.GONE);
                    });
                }
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error sending query", e);
            Toast.makeText(this, "An error occurred while sending the query.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            copyButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the network receiver
        unregisterReceiver(networkReceiver);
        // Remove callbacks to avoid memory leaks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }
}

// Utility class to check network connectivity
class NetworkUtil {
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
