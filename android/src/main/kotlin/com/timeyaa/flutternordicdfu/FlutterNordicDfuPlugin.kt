package com.timeyaa.flutternordicdfu

import android.content.Context
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


class FlutterNordicDfuPlugin(private val registrar: Registrar) : MethodCallHandler {

    private val TAG = "FlutterNordicDfuPlugin"

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
        controller = starter.start(mContext, DfuService::class.java)
    }

    private val mDfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDeviceConnected(deviceAddress: String?) {
            super.onDeviceConnected(deviceAddress)
            channel.invokeMethod("onDeviceConnected", deviceAddress)
        }

        override fun onError(deviceAddress: String?, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            channel.invokeMethod("onError", deviceAddress)

            pendingResult?.error("2", "DFU FAILED", "device address: $deviceAddress")
            pendingResult = null
        }

        override fun onDeviceConnecting(deviceAddress: String?) {
            super.onDeviceConnecting(deviceAddress)
            channel.invokeMethod("onDeviceConnecting", deviceAddress)
        }

        override fun onDeviceDisconnected(deviceAddress: String?) {
            super.onDeviceDisconnected(deviceAddress)
            channel.invokeMethod("onDeviceDisconnected", deviceAddress)
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            super.onDeviceDisconnecting(deviceAddress)
            channel.invokeMethod("onDeviceDisconnecting", deviceAddress)
        }

        override fun onDfuAborted(deviceAddress: String?) {
            super.onDfuAborted(deviceAddress)

            pendingResult?.error("2", "DFU ABORTED", "device address: $deviceAddress")
            pendingResult = null

            channel.invokeMethod("onDfuAborted", deviceAddress)
        }

        override fun onDfuCompleted(deviceAddress: String?) {
            super.onDfuCompleted(deviceAddress)

            pendingResult?.success(deviceAddress)
            pendingResult = null

            channel.invokeMethod("onDfuCompleted", deviceAddress)
        }

        override fun onDfuProcessStarted(deviceAddress: String?) {
            super.onDfuProcessStarted(deviceAddress)
            channel.invokeMethod("onDfuProcessStarted", deviceAddress)
        }

        override fun onDfuProcessStarting(deviceAddress: String?) {
            super.onDfuProcessStarting(deviceAddress)
            channel.invokeMethod("onDfuProcessStarting", deviceAddress)
        }

        override fun onEnablingDfuMode(deviceAddress: String?) {
            super.onEnablingDfuMode(deviceAddress)
            channel.invokeMethod("onEnablingDfuMode", deviceAddress)
        }

        override fun onFirmwareValidating(deviceAddress: String?) {
            super.onFirmwareValidating(deviceAddress)
            channel.invokeMethod("onFirmwareValidating", deviceAddress)
        }

        override fun onProgressChanged(deviceAddress: String?, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)

            val paras = hashMapOf("percent" to percent, "speed" to speed, "avgSpeed" to avgSpeed, "currentPart" to currentPart, "partsTotal" to partsTotal)

            if (deviceAddress != null) {
                paras["deviceAddress"] = deviceAddress
            }

            channel.invokeMethod("onProgressChanged", paras)
        }
    }
}
