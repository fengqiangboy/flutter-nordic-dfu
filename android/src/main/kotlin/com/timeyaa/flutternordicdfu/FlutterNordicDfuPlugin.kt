package com.timeyaa.flutternordicdfu

import android.content.Context
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import no.nordicsemi.android.dfu.DfuServiceInitiator


class FlutterNordicDfuPlugin : MethodCallHandler {

    /**
     * hold context
     */
    private var mContext: Context

    /**
     * hole result
     */
    private var pendingResult: Result? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_nordic_dfu")
            channel.setMethodCallHandler(FlutterNordicDfuPlugin(registrar.context()))
        }
    }

    constructor(context: Context) {
        mContext = context
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "startDfu") {
            pendingResult = result
            val address = call.argument<String>("address")
            val name = call.argument<String?>("name")
            val filePath = call.argument<String?>("filePath")
            if (address == null || filePath == null) {
                result.error("Abnormal parameter", "address and filePath are required", null)
                return
            }

            startDfu(address, name, filePath, result)
        } else {
            result.notImplemented()
        }
    }

    /**
     * Start Dfu
     */
    fun startDfu(address: String, name: String?, filePath: String, result: Result) {
        val starter = DfuServiceInitiator(address)
                .setKeepBond(false)
        if (name != null) {
            starter.setDeviceName(name)
        }

        pendingResult = result

        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
        starter.setZip(filePath)
        val controller = starter.start(this.mContext, DfuService::class.java)
    }
}
