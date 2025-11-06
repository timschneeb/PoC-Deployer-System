package com.wqry085.deployesystem.next;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.google.android.material.color.DynamicColors;

public class init_kernel extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        init_kernel.context = getApplicationContext();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    public static Context getAppContext() {
        return init_kernel.context;
    }
}