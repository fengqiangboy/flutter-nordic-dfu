package com.timeyaa.flutternordicdfu

import android.content.Context
import android.util.Log
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ResourceUtils
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceController
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.util.*


class FlutterNordicDfuPlugin(registrar: Registrar) : MethodCallHandler {

    private val NAMESPACE = "com.timeyaa.flutter_nordic_dfu"

    /**
     * hold context
     */
    private var mContext: Context = registrar.context()

    /**
     * hold result
     */
    private var pendingResult: Result? = null

    /**
     * Method Channel
     */
    private val channel: MethodChannel

    private val registrar: Registrar = registrar

    private var controller: DfuServiceController? = null

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
        when {
            call.method == "startDfu" -> {
                pendingResult = result
                val address = call.argument<String>("address")
                val name = call.argument<String?>("name")
                var filePath = call.argument<String?>("filePath")
                val fileInAsset = call.argument<Boolean>("fileInAsset") ?: false

                if (fileInAsset) {
                    filePath = registrar.lookupKeyForAsset(filePath)

                    val tempFileName = PathUtils.getExternalAppCachePath() + UUID.randomUUID().toString()
                    ResourceUtils.copyFileFromAssets(filePath, tempFileName)
                    filePath = tempFileName
                }

                if (address == null || filePath == null) {
                    result.error("Abnormal parameter", "address and filePath are required", null)
                    return
                }

                startDfu(address, name, filePath, result)
            }
            call.method == "getPlatformVersion" -> {
                result.success("android")
                return
            }
            else -> result.notImplemented()
        }
    }

    /**
     * Start Dfu
     */
    private fun startDfu(address: String, name: String?, filePath: String, result: Result) {
        val starter = DfuServiceInitiator(address)
                .setZip(filePath)
                .setKeepBond(true)
        if (name != null) {
            starter.setDeviceName(name)
        }

        pendingResult = result

        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
        DfuServiceListenerHelper.registerProgressListener(registrar.activity(), mDfuProgressListener)
        controller = starter.start(registrar.activity(), DfuService::class.java)
    }

    private val mDfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDeviceConnected(deviceAddress: String?) {
            super.onDeviceConnected(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDeviceConnected")
        }

        override fun onError(deviceAddress: String?, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            Log.e("FlutterNordicDfuPlugin", message)
        }

        override fun onDeviceConnecting(deviceAddress: String?) {
            super.onDeviceConnecting(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDeviceConnecting")
        }

        override fun onDeviceDisconnected(deviceAddress: String?) {
            super.onDeviceDisconnected(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDeviceDisconnected")
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            super.onDeviceDisconnecting(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDeviceDisconnecting")
        }

        override fun onDfuAborted(deviceAddress: String?) {
            super.onDfuAborted(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDfuAborted")
        }

        override fun onDfuCompleted(deviceAddress: String?) {
            super.onDfuCompleted(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDfuCompleted")
        }

        override fun onDfuProcessStarted(deviceAddress: String?) {
            super.onDfuProcessStarted(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDfuProcessStarted")
        }

        override fun onDfuProcessStarting(deviceAddress: String?) {
            super.onDfuProcessStarting(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onDfuProcessStarting")
        }

        override fun onEnablingDfuMode(deviceAddress: String?) {
            super.onEnablingDfuMode(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onEnablingDfuMode")
        }

        override fun onFirmwareValidating(deviceAddress: String?) {
            super.onFirmwareValidating(deviceAddress)
            Log.i("FlutterNordicDfuPlugin", "onFirmwareValidating")
        }

        override fun onProgressChanged(deviceAddress: String?, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)
            Log.i("FlutterNordicDfuPlugin", "onProgressChanged")
        }
    }
}
