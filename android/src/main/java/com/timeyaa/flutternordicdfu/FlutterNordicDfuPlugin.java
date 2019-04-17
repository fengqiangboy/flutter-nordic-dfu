package com.timeyaa.flutternordicdfu;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class FlutterNordicDfuPlugin implements MethodCallHandler {

    private String TAG = "FlutterNordicDfuPlugin";

    private String NAMESPACE = "com.timeyaa.flutter_nordic_dfu";

    /**
     * hold context
     */
    private Context mContext;

    /**
     * hold result
     */
    private Result pendingResult;

    /**
     * Method Channel
     */
    private MethodChannel channel;

    private DfuServiceController controller;

    public FlutterNordicDfuPlugin(Registrar registrar) {
        this.mContext = registrar.context();
        this.channel = new MethodChannel(registrar.messenger(), "$NAMESPACE/method");
        channel.setMethodCallHandler(this);
    }

    public static void registerWith(Registrar registrar) {
        FlutterNordicDfuPlugin instance = new FlutterNordicDfuPlugin(registrar);
        DfuServiceListenerHelper.registerProgressListener(registrar.context(), instance.mDfuProgressListener);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("startDfu")) {
            String address = call.argument("address");
            String name = call.argument("name");
            String filePath = call.argument("filePath");

            if (address == null || filePath == null) {
                result.error("Abnormal parameter", "address and filePath are required", null);
                return;
            }

            pendingResult = result;
            startDfu(address, name, filePath, result);
        } else {
            result.notImplemented();
        }
    }

    /**
     * Start Dfu
     */
    private void startDfu(String address, @Nullable String name, String filePath, Result result) {
        DfuServiceInitiator starter = new DfuServiceInitiator(address)
                .setZip(filePath)
                .setKeepBond(true);
        if (name != null) {
            starter.setDeviceName(name);
        }

        pendingResult = result;

        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        controller = starter.start(mContext, DfuService.class);
    }

    private DfuProgressListenerAdapter mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnected(String deviceAddress) {
            super.onDeviceConnected(deviceAddress);
            channel.invokeMethod("onDeviceConnected", deviceAddress);
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            super.onError(deviceAddress, error, errorType, message);
            channel.invokeMethod("onError", deviceAddress);

            if (pendingResult != null) {
                pendingResult.error("2", "DFU FAILED", "device address: $deviceAddress");
                pendingResult = null;
            }
        }

        @Override
        public void onDeviceConnecting(String deviceAddress) {
            super.onDeviceConnecting(deviceAddress);
            channel.invokeMethod("onDeviceConnecting", deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            super.onDeviceDisconnected(deviceAddress);
            channel.invokeMethod("onDeviceDisconnected", deviceAddress);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            super.onDeviceDisconnecting(deviceAddress);
            channel.invokeMethod("onDeviceDisconnecting", deviceAddress);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            super.onDfuAborted(deviceAddress);

            if (pendingResult != null) {
                pendingResult.error("2", "DFU ABORTED", "device address: $deviceAddress");
                pendingResult = null;
            }


            channel.invokeMethod("onDfuAborted", deviceAddress);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            super.onDfuCompleted(deviceAddress);

            if (pendingResult != null) {
                pendingResult.success(deviceAddress);
                pendingResult = null;
            }


            channel.invokeMethod("onDfuCompleted", deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            super.onDfuProcessStarted(deviceAddress);
            channel.invokeMethod("onDfuProcessStarted", deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            super.onDfuProcessStarting(deviceAddress);
            channel.invokeMethod("onDfuProcessStarting", deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            super.onEnablingDfuMode(deviceAddress);
            channel.invokeMethod("onEnablingDfuMode", deviceAddress);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            super.onFirmwareValidating(deviceAddress);
            channel.invokeMethod("onFirmwareValidating", deviceAddress);
        }

        @Override
        public void onProgressChanged(String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);

            Map<String, Object> paras = new HashMap<String, Object>() {{
                put("percent", percent);
                put("speed", speed);
                put("avgSpeed", avgSpeed);
                put("currentPart", currentPart);
                put("partsTotal", partsTotal);
            }};

            if (deviceAddress != null) {
                paras.put("deviceAddress", deviceAddress);
            }

            channel.invokeMethod("onProgressChanged", paras);
        }
    };
}

