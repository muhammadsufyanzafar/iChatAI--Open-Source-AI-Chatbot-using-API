package com.zafar.ichatai;

import android.content.Context;
import android.content.SharedPreferences;

public class CreditsManager {
    private static final String PREF = "credits_pref";
    private static final String KEY_CREDITS = "credits";
    private static final String KEY_INIT = "initialized";
    private static final int INITIAL = 25;

    private final SharedPreferences sp;

    public CreditsManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean(KEY_INIT, false)) {
            sp.edit()
                    .putInt(KEY_CREDITS, INITIAL)
                    .putBoolean(KEY_INIT, true)
                    .apply();
        }
    }

    public int get() { return sp.getInt(KEY_CREDITS, 0); }

    public void add(int amount) {
        sp.edit().putInt(KEY_CREDITS, Math.max(0, get() + amount)).apply();
    }

    public boolean spend(int amount) {
        int cur = get();
        if (cur < amount) return false;
        sp.edit().putInt(KEY_CREDITS, cur - amount).apply();
        return true;
    }
}
