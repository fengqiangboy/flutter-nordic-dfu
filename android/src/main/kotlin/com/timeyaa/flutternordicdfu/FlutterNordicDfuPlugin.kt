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
            val instance = FlutterNordicDfuPlugin(registrar)
            DfuServiceListenerHelper.registerProgressListener(registrar.context(), instance.mDfuProgressListener)
        }
    }

    init {
        this.channel = MethodChannel(registrar.messenger(), "$NAMESPACE/method")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when {
            call.method == "startDfu" -> {
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

                pendingResult = result
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
        controller = starter.start(registrar.activity(), DfuService::class.java)
    }

    private val mDfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDeviceConnected(deviceAddress: String?) {
            super.onDeviceConnected(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDeviceConnected -  deviceAddress: $deviceAddress")
        }

        override fun onError(deviceAddress: String?, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            Log.e("FlutterNordicDfuPlugin", "onError -  deviceAddress: $deviceAddress, error: $error, errorType: $errorType, message: $message")

            pendingResult?.error("2", "DFU FAILED", "device address: $deviceAddress")
            pendingResult = null
        }

        override fun onDeviceConnecting(deviceAddress: String?) {
            super.onDeviceConnecting(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDeviceConnecting - deviceAddress: $deviceAddress")
        }

        override fun onDeviceDisconnected(deviceAddress: String?) {
            super.onDeviceDisconnected(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDeviceDisconnected - deviceAddress: $deviceAddress")
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            super.onDeviceDisconnecting(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDeviceDisconnecting - deviceAddress: $deviceAddress")
        }

        override fun onDfuAborted(deviceAddress: String?) {
            super.onDfuAborted(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDfuAborted - deviceAddress: $deviceAddress")

            pendingResult?.error("2", "DFU ABORTED", "device address: $deviceAddress")
            pendingResult = null
        }

        override fun onDfuCompleted(deviceAddress: String?) {
            super.onDfuCompleted(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDfuCompleted - deviceAddress: $deviceAddress")

            pendingResult?.success(deviceAddress)
            pendingResult = null
        }

        override fun onDfuProcessStarted(deviceAddress: String?) {
            super.onDfuProcessStarted(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDfuProcessStarted - deviceAddress: $deviceAddress")
        }

        override fun onDfuProcessStarting(deviceAddress: String?) {
            super.onDfuProcessStarting(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onDfuProcessStarting - deviceAddress: $deviceAddress")
        }

        override fun onEnablingDfuMode(deviceAddress: String?) {
            super.onEnablingDfuMode(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onEnablingDfuMode - deviceAddress: $deviceAddress")
        }

        override fun onFirmwareValidating(deviceAddress: String?) {
            super.onFirmwareValidating(deviceAddress)
            Log.d("FlutterNordicDfuPlugin", "onFirmwareValidating - deviceAddress: $deviceAddress")
        }

        override fun onProgressChanged(deviceAddress: String?, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)
            Log.d("FlutterNordicDfuPlugin", "onProgressChanged - deviceAddress: $deviceAddress, percent: $percent, speed: $speed, avgSpeed: $avgSpeed, currentPart: $currentPart, partsTotal: $partsTotal")
        }
    }
}
