package com.timeyaa.flutternordicdfu

import android.content.Context
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import no.nordicsemi.android.dfu.DfuServiceInitiator


class FlutterNordicDfuPlugin(registrar: Registrar) : MethodCallHandler {

    private val NAMESPACE = "com.timeyaa.flutter_nordic_dfu"

    /**
     * hold context
     */
    private var mContext: Context = registrar.context()

    /**
     * hole result
     */
    private var pendingResult: Result? = null

    /**
     * Method Channel
     */
    private val channel: MethodChannel

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            FlutterNordicDfuPlugin(registrar)
        }
    }

    init {
        this.channel = MethodChannel(registrar.messenger(), "$NAMESPACE/method")
        channel.setMethodCallHandler(this)
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
    private fun startDfu(address: String, name: String?, filePath: String, result: Result) {
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
