package com.timeyaa.flutternordicdfu

import android.app.Activity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService : DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val TAG = "DfuService"
}