package com.zafar.ichatai;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage and apply the app's theme mode.
 * Supports MODE_NIGHT_NO (light), MODE_NIGHT_YES (dark), and MODE_NIGHT_FOLLOW_SYSTEM.
 */
public class ThemeUtils {

    private static final String PREFS_NAME = "app_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    /**
     * Apply the saved theme mode to the given Context.
     * Call this BEFORE setContentView() in every Activity you want themed at launch.
     */
    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int savedMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    /**
     * Save and apply a new theme mode.
     *
     * @param context  Context for SharedPreferences
     * @param nightMode One of:
     *                  AppCompatDelegate.MODE_NIGHT_NO          → Light
     *                  AppCompatDelegate.MODE_NIGHT_YES         → Dark
     *                  AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM → Follow system setting
     */
    public static void setThemeMode(Context context, int nightMode) {
        // Save
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, nightMode).apply();
        // Apply immediately
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /**
     * Get the currently saved theme mode.
     */
    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
