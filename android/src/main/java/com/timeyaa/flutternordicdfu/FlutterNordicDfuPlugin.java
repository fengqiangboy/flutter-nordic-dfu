package com.timeyaa.flutternordicdfu;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
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
     * hold Registrar
     */
    private Registrar registrar;

    /**
     * hold result
     */
    private Result pendingResult;

    /**
     * Method Channel
     */
    private MethodChannel channel;

    private DfuServiceController controller;

    private boolean hasCreateNotification = false;

    private FlutterNordicDfuPlugin(Registrar registrar) {
        this.mContext = registrar.context();
        this.channel = new MethodChannel(registrar.messenger(), NAMESPACE + "/method");
        this.registrar = registrar;
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
            Boolean fileInAsset = call.argument("fileInAsset");
            Boolean forceDfu = call.argument("forceDfu");
            Boolean enableUnsafeExperimentalButtonlessServiceInSecureDfu = call.argument("enableUnsafeExperimentalButtonlessServiceInSecureDfu");
            Boolean disableNotification = call.argument("disableNotification");
            Boolean keepBond = call.argument("keepBond");
            Boolean packetReceiptNotificationsEnabled = call.argument("packetReceiptNotificationsEnabled");
            Boolean restoreBond = call.argument("restoreBond");
            Boolean startAsForegroundService = call.argument("startAsForegroundService");

            if (fileInAsset == null) {
                fileInAsset = false;
            }

            if (address == null || filePath == null) {
                result.error("Abnormal parameter", "address and filePath are required", null);
                return;
            }

            if (fileInAsset) {
                filePath = registrar.lookupKeyForAsset(filePath);
                String tempFileName = PathUtils.getExternalAppCachePath(mContext)
                        + UUID.randomUUID().toString();
                // copy asset file to temp path
                ResourceUtils.copyFileFromAssets(filePath, tempFileName, mContext);
                // now, the path is an absolute path, and can pass it to nordic dfu libarary
                filePath = tempFileName;
            }

            pendingResult = result;
            startDfu(address, name, filePath, forceDfu, enableUnsafeExperimentalButtonlessServiceInSecureDfu, disableNotification, keepBond, packetReceiptNotificationsEnabled, restoreBond, startAsForegroundService, result);
        } else {
            result.notImplemented();
        }
    }

    /**
     * Start Dfu
     */
    private void startDfu(String address, @Nullable String name, String filePath, Boolean forceDfu, Boolean enableUnsafeExperimentalButtonlessServiceInSecureDfu, Boolean disableNotification, Boolean keepBond, Boolean packetReceiptNotificationsEnabled, Boolean restoreBond, Boolean startAsForegroundService, Result result) {
        DfuServiceInitiator starter = new DfuServiceInitiator(address)
                .setZip(filePath)
                .setKeepBond(true);
        if (name != null) {
            starter.setDeviceName(name);
        }

        pendingResult = result;

        if (enableUnsafeExperimentalButtonlessServiceInSecureDfu != null) {
            starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(enableUnsafeExperimentalButtonlessServiceInSecureDfu);
        }

        if (forceDfu != null) {
            starter.setForceDfu(forceDfu);
        }

        if (disableNotification != null) {
            starter.setDisableNotification(disableNotification);
        }

        if (startAsForegroundService != null) {
            starter.setForeground(startAsForegroundService);
        }

        if (keepBond != null) {
            starter.setKeepBond(keepBond);
        }

        if (restoreBond != null) {
            starter.setRestoreBond(restoreBond);
        }

        if (packetReceiptNotificationsEnabled != null) {
            starter.setPacketsReceiptNotificationsEnabled(packetReceiptNotificationsEnabled);
        }

        // fix notification on android 8 and above
        if (startAsForegroundService == null || startAsForegroundService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasCreateNotification) {
                DfuServiceInitiator.createDfuNotificationChannel(mContext);
                hasCreateNotification = true;
            }
        }

        controller = starter.start(mContext, DfuService.class);
    }

    private DfuProgressListenerAdapter mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnected(@NonNull String deviceAddress) {
            super.onDeviceConnected(deviceAddress);
            channel.invokeMethod("onDeviceConnected", deviceAddress);
        }

        @Override
        public void onError(@NonNull String deviceAddress, int error, int errorType, String message) {
            super.onError(deviceAddress, error, errorType, message);
            cancelNotification();

            channel.invokeMethod("onError", deviceAddress);

            if (pendingResult != null) {
                pendingResult.error("2", "DFU FAILED", "device address: " + deviceAddress);
                pendingResult = null;
            }
        }

        @Override
        public void onDeviceConnecting(@NonNull String deviceAddress) {
            super.onDeviceConnecting(deviceAddress);
            channel.invokeMethod("onDeviceConnecting", deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(@NonNull String deviceAddress) {
            super.onDeviceDisconnected(deviceAddress);
            channel.invokeMethod("onDeviceDisconnected", deviceAddress);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            super.onDeviceDisconnecting(deviceAddress);
            channel.invokeMethod("onDeviceDisconnecting", deviceAddress);
        }

        @Override
        public void onDfuAborted(@NonNull String deviceAddress) {
            super.onDfuAborted(deviceAddress);
            cancelNotification();

            if (pendingResult != null) {
                pendingResult.error("2", "DFU ABORTED", "device address: " + deviceAddress);
                pendingResult = null;
            }


            channel.invokeMethod("onDfuAborted", deviceAddress);
        }

        @Override
        public void onDfuCompleted(@NonNull String deviceAddress) {
            super.onDfuCompleted(deviceAddress);
            cancelNotification();

            if (pendingResult != null) {
                pendingResult.success(deviceAddress);
                pendingResult = null;
            }


            channel.invokeMethod("onDfuCompleted", deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(@NonNull String deviceAddress) {
            super.onDfuProcessStarted(deviceAddress);
            channel.invokeMethod("onDfuProcessStarted", deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(@NonNull String deviceAddress) {
            super.onDfuProcessStarting(deviceAddress);
            channel.invokeMethod("onDfuProcessStarting", deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(@NonNull String deviceAddress) {
            super.onEnablingDfuMode(deviceAddress);
            channel.invokeMethod("onEnablingDfuMode", deviceAddress);
        }

        @Override
        public void onFirmwareValidating(@NonNull String deviceAddress) {
            super.onFirmwareValidating(deviceAddress);
            channel.invokeMethod("onFirmwareValidating", deviceAddress);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            super.onProgressChanged(deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);

            Map<String, Object> paras = new HashMap<String, Object>() {{
                put("percent", percent);
                put("speed", speed);
                put("avgSpeed", avgSpeed);
                put("currentPart", currentPart);
                put("partsTotal", partsTotal);
                put("deviceAddress", deviceAddress);
            }};

            channel.invokeMethod("onProgressChanged", paras);
        }
    };

    private void cancelNotification() {
        // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final NotificationManager manager = (NotificationManager) registrar.activity().getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null)
                    manager.cancel(DfuService.NOTIFICATION_ID);
            }
        }, 200);
    }

}

