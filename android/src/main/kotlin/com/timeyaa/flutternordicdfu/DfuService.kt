package com.timeyaa.flutternordicdfu

import android.app.Activity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService : DfuBaseService() {
    private val TAG = "DfuService"

    override fun getNotificationTarget(): Class<out Activity> {
        return NotificationActivity::class.java
    }
}