package com.timeyaa.flutternordicdfu;

import android.app.Activity;

import no.nordicsemi.android.dfu.DfuBaseService;

public class DfuService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }

    @Override
    protected boolean isDebug() {
        // Override this method and return true if you need more logs in LogCat
        // Note: BuildConfig.DEBUG always returns false in library projects, so please use
        // your app package BuildConfig
        return true;
    }
}