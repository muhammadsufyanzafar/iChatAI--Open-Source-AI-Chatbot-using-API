package com.zafar.ichatai;

import android.content.Context;

public class AppContextHelper {
    private static Context appContext;

    // Initialize the context in the Application class
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // Get the global application context
    public static Context getContext() {
        return appContext;
    }
}
