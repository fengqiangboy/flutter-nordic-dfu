package com.timeyaa.flutternordicdfu

import android.content.Context
import android.support.annotation.Nullable
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
import kotlin.collections.HashMap


class FlutterNordicDfuPlugin(registrar: Registrar) : MethodCallHandler {

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
        controller = starter.start(mContext, DfuService::class.java)
    }

    /**
     * send event to flutter
     * @param eventName 事件名称
     */
    private fun sendEvent(eventName: String, params: HashMap<String, String>?) {

    }

    /**
     * send dfu state to flutter when state change
     */
    private fun sendStateUpdate(state: String, deviceAddress: String?) {
        val map = hashMapOf<String, String>()
        Log.d(TAG, "State: $state")
        map["state"] = state
        if (deviceAddress != null) {
            map["deviceAddress"] = deviceAddress
        }
        sendEvent("DFUStateChanged", map)
    }

    private val mDfuProgressListener = object : DfuProgressListenerAdapter() {
        override fun onDeviceConnected(deviceAddress: String?) {
            super.onDeviceConnected(deviceAddress)
            sendStateUpdate("CONNECTED", deviceAddress)
        }

        override fun onError(deviceAddress: String?, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            sendStateUpdate("DFU_FAILED", deviceAddress)

            pendingResult?.error("2", "DFU FAILED", "device address: $deviceAddress")
            pendingResult = null
        }

        override fun onDeviceConnecting(deviceAddress: String?) {
            super.onDeviceConnecting(deviceAddress)
            sendStateUpdate("CONNECTING", deviceAddress)
        }

        override fun onDeviceDisconnected(deviceAddress: String?) {
            super.onDeviceDisconnected(deviceAddress)
            sendStateUpdate("DISCONNECTED", deviceAddress)
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            super.onDeviceDisconnecting(deviceAddress)
            sendStateUpdate("DEVICE_DISCONNECTING", deviceAddress)
        }

        override fun onDfuAborted(deviceAddress: String?) {
            super.onDfuAborted(deviceAddress)

            pendingResult?.error("2", "DFU ABORTED", "device address: $deviceAddress")
            pendingResult = null

            sendStateUpdate("DFU_ABORTED", deviceAddress)
        }

        override fun onDfuCompleted(deviceAddress: String?) {
            super.onDfuCompleted(deviceAddress)

            pendingResult?.success(deviceAddress)
            pendingResult = null

            sendStateUpdate("DFU_COMPLETED", deviceAddress)
        }

        override fun onDfuProcessStarted(deviceAddress: String?) {
            super.onDfuProcessStarted(deviceAddress)
            sendStateUpdate("DFU_PROCESS_STARTED", deviceAddress)
        }

        override fun onDfuProcessStarting(deviceAddress: String?) {
            super.onDfuProcessStarting(deviceAddress)
            sendStateUpdate("DFU_PROCESS_STARTING", deviceAddress)
        }

        override fun onEnablingDfuMode(deviceAddress: String?) {
            super.onEnablingDfuMode(deviceAddress)
            sendStateUpdate("ENABLING_DFU_MODE", deviceAddress)
        }

        override fun onFirmwareValidating(deviceAddress: String?) {
            super.onFirmwareValidating(deviceAddress)
            sendStateUpdate("FIRMWARE_VALIDATING", deviceAddress)
        }

        override fun onProgressChanged(deviceAddress: String?, percent: Int, speed: Float, avgSpeed: Float, currentPart: Int, partsTotal: Int) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal)
            Log.d("FlutterNordicDfuPlugin", "onProgressChanged - deviceAddress: $deviceAddress, percent: $percent, speed: $speed, avgSpeed: $avgSpeed, currentPart: $currentPart, partsTotal: $partsTotal")
        }
    }
}
