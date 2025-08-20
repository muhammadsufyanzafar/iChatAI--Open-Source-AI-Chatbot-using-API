package com.zafar.ichatai;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_SETTINGS = "app_settings";
    private static final String KEY_PUSH_ENABLED = "push_enabled";

    private MaterialSwitch pushSwitch;
    private MaterialButtonToggleGroup themeToggle;
    private MaterialButton btnSystem, btnLight, btnDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Make sure the saved theme is applied before inflating the layout
        ThemeUtils.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Toolbar back
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Push notifications (placeholder, just saves local state)
        pushSwitch = findViewById(R.id.switchPush);
        SharedPreferences sp = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        boolean pushEnabled = sp.getBoolean(KEY_PUSH_ENABLED, true);
        pushSwitch.setChecked(pushEnabled);
        pushSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(KEY_PUSH_ENABLED, isChecked).apply();
            Toast.makeText(
                    this,
                    isChecked ? "Push notifications enabled (coming soon)" : "Push notifications disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Theme toggle
        themeToggle = findViewById(R.id.themeToggleGroup);
        btnSystem = findViewById(R.id.btnSystem);
        btnLight = findViewById(R.id.btnLight);
        btnDark = findViewById(R.id.btnDark);

        // Preselect current theme
        selectCurrentThemeButton(ThemeUtils.getSavedThemeMode(this));

        themeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.btnLight) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.btnDark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            // Save + apply immediately
            ThemeUtils.setThemeMode(this, mode);
            // Refresh this screen to apply colors without requiring app restart
            recreate();
        });

        // Version
        TextView versionText = findViewById(R.id.txtVersion);
        versionText.setText(getVersionNameSafe());

        // About links (replace URLs with your live pages)
        findViewById(R.id.rowTerms).setOnClickListener(v ->
                openLink("https://onlineshoppingdealofficial.blogspot.com/p/ichatais-terms-conditions.html"));
        findViewById(R.id.rowPrivacy).setOnClickListener(v ->
                openLink("https://onlineshoppingdealofficial.blogspot.com/p/ichatais-privacy-policy.html"));
        findViewById(R.id.rowContact).setOnClickListener(v ->
                openLink("https://muhammadsufyanzafar.github.io/portfolio/#contact"));
    }

    private void selectCurrentThemeButton(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeToggle.check(R.id.btnLight);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeToggle.check(R.id.btnDark);
        } else {
            themeToggle.check(R.id.btnSystem);
        }
    }

    private String getVersionNameSafe() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "—";
        }
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No browser found to open link", Toast.LENGTH_SHORT).show();
        }
    }
}
