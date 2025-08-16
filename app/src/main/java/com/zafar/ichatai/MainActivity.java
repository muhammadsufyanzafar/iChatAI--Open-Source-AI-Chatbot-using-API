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
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
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

    private MessageAdapter adapter;
    private RecyclerView recyclerView;
    private TextInputEditText queryEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ThemeUtils.applySavedTheme(this);
        setContentView(R.layout.activity_main);

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        // Views
        recyclerView = findViewById(R.id.recyclerView);
        queryEditText = findViewById(R.id.queryEditText);
        sendButton = findViewById(R.id.sendPromptButton);
        progressBar = findViewById(R.id.sendPromptProgressBar);

        // RecyclerView setup
        adapter = new MessageAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Welcome message
        adapter.addMessage(new Message("Hi, I’m iChatAI. Ask me anything!", false));
        scrollToBottom();

        // AdMob init
        MobileAds.initialize(this, initializationStatus -> {});
        loadRewardedAd();

        // Show a rewarded ad after 30 seconds (optional, as before)
        handler = new Handler();
        handler.postDelayed(this::showRewardedAd, 30000);

        // Send click
        sendButton.setOnClickListener(v -> {
            if (!NetworkUtil.isConnected(this)) {
                Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
                return;
            }
            String query = queryEditText.getText() != null ? queryEditText.getText().toString().trim() : "";
            if (query.isEmpty()) {
                Toast.makeText(this, "Please add your query", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add user message immediately
            adapter.addMessage(new Message(query, true));
            scrollToBottom();

            // Ad every 3rd send
            sendQueryButtonClickCount++;
            if (sendQueryButtonClickCount == 3 && rewardedAd != null) {
                showRewardedAdThenSend(query);
                sendQueryButtonClickCount = 0;
            } else {
                sendQuery(query);
            }
        });

        // Register network receiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);

        // Auto-focus + show keyboard on launch
        queryEditText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Ensure the bottom bar lifts above the keyboard (works even on quirky devices)
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            int bottom = Math.max(ime.bottom, sysBars.bottom);
            int top = sysBars.top;
            int left = sysBars.left;
            int right = sysBars.right;

            v.setPadding(left, top, right, bottom);
            return insets;
        });
    }

    private boolean onMenuItemClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
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
            rewardedAd.show(this, rewardItem -> loadRewardedAd());
        } else {
            Log.d("MainActivity", "Rewarded ad not ready yet.");
        }
    }

    private void showRewardedAdThenSend(String query) {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                loadRewardedAd();
                sendQuery(query);
            });
        } else {
            Log.d("MainActivity", "Rewarded ad not ready; sending query anyway.");
            sendQuery(query);
        }
    }

    private void sendQuery(String query) {
        queryEditText.setText("");
        progressBar.setVisibility(View.VISIBLE);

        // Local identity check so “who are you” always answers as iChatAI
        String normalized = query.toLowerCase();
        if (normalized.equals("who are you") || normalized.equals("who are u")
                || normalized.equals("what is your name") || normalized.equals("tell me your name")) {
            adapter.addMessage(new Message("I am iChatAI — your friendly AI companion.", false));
            progressBar.setVisibility(View.GONE);
            scrollToBottom();
            return;
        }

        // Temporary typing indicator
        adapter.addMessage(new Message("Thinking…", false));
        scrollToBottom();

        DeepSeekClient model = new DeepSeekClient();
        try {
            model.getResponse(query, new ResponseCallback() {
                @Override
                public void onResponse(String response) {
                    runOnUiThread(() -> {
                        adapter.replaceLastAiMessage(response == null || response.isEmpty() ? "..." : response);
                        progressBar.setVisibility(View.GONE);
                        scrollToBottom();
                        copyLastToClipboardIfLong(response);
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    runOnUiThread(() -> {
                        adapter.replaceLastAiMessage("Sorry, I ran into an error: " + throwable.getMessage());
                        progressBar.setVisibility(View.GONE);
                        scrollToBottom();
                    });
                }
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error sending query", e);
            adapter.replaceLastAiMessage("An error occurred while sending the query.");
            progressBar.setVisibility(View.GONE);
            scrollToBottom();
        }
    }

    private void copyLastToClipboardIfLong(String text) {
        if (text != null && text.length() > 800) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Response", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Long response copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        recyclerView.post(() -> recyclerView.scrollToPosition(Math.max(0, adapter.getItemCount() - 1)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkReceiver);
        if (handler != null) handler.removeCallbacksAndMessages(null);
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
