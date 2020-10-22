import Flutter
import UIKit
import iOSDFULibrary
import CoreBluetooth

public class SwiftFlutterNordicDfuPlugin: NSObject, FlutterPlugin, DFUServiceDelegate, DFUProgressDelegate, LoggerDelegate {
    
    let registrar: FlutterPluginRegistrar
    let channel: FlutterMethodChannel
    var pendingResult: FlutterResult?
    var deviceAddress: String?
    private var dfuController    : DFUServiceController!
    
    init(_ registrar: FlutterPluginRegistrar, _ channel: FlutterMethodChannel) {
        self.registrar = registrar
        self.channel = channel
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.timeyaa.flutter_nordic_dfu/method", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterNordicDfuPlugin(registrar, channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if (call.method == "startDfu") {
            guard let arguments = call.arguments as? Dictionary<String, AnyObject> else {
                result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
                return
            }
            let name = arguments["name"] as? String
            guard let address = arguments["address"] as? String,
                var filePath = arguments["filePath"] as? String else {
                    result(FlutterError(code: "ABNORMAL_PARAMETER", message: "address and filePath are required", details: nil))
                    return
            }
            
            let forceDfu = arguments["forceDfu"] as? Bool
            
            let enableUnsafeExperimentalButtonlessServiceInSecureDfu = arguments["enableUnsafeExperimentalButtonlessServiceInSecureDfu"] as? Bool
            
            let fileInAsset = (arguments["fileInAsset"] as? Bool) ?? false
            
            if (fileInAsset) {
                let key = registrar.lookupKey(forAsset: filePath)
                guard let pathInAsset = Bundle.main.path(forResource: key, ofType: nil) else {
                    result(FlutterError(code: "ABNORMAL_PARAMETER", message: "file in asset not found \(filePath)", details: nil))
                    return
                }
                
                filePath = pathInAsset
            }
            
            let alternativeAdvertisingNameEnabled = arguments["alternativeAdvertisingNameEnabled"] as? Bool
            
            startDfu(address,
                     name: name,
                     filePath: filePath,
                     forceDfu: forceDfu,
                     enableUnsafeExperimentalButtonlessServiceInSecureDfu: enableUnsafeExperimentalButtonlessServiceInSecureDfu,
                     alternativeAdvertisingNameEnabled: alternativeAdvertisingNameEnabled,
                     result: result)
        } else if (call.method == "abortDfu") {
            _ = dfuController?.abort()
            dfuController = nil
        }
    }
    
    private func startDfu(
        _ address: String,
        name: String?,
        filePath: String,
        forceDfu: Bool?,
        enableUnsafeExperimentalButtonlessServiceInSecureDfu: Bool?,
        alternativeAdvertisingNameEnabled: Bool?,
        result: @escaping FlutterResult) {
        guard let uuid = UUID(uuidString: address) else {
            result(FlutterError(code: "DEVICE_ADDRESS_ERROR", message: "Device address conver to uuid failed", details: "Device uuid \(address) convert to uuid failed"))
            return
        }
        
        guard let firmware = DFUFirmware(urlToZipFile: URL(fileURLWithPath: filePath)) else {
            result(FlutterError(code: "DFU_FIRMWARE_NOT_FOUND", message: "Could not dfu zip file", details: nil))
            return
        }
        
        let dfuInitiator = DFUServiceInitiator(queue: nil)
            .with(firmware: firmware);
        dfuInitiator.delegate = self
        dfuInitiator.progressDelegate = self
        dfuInitiator.logger = self
        
        if let enableUnsafeExperimentalButtonlessServiceInSecureDfu = enableUnsafeExperimentalButtonlessServiceInSecureDfu {
            dfuInitiator.enableUnsafeExperimentalButtonlessServiceInSecureDfu = enableUnsafeExperimentalButtonlessServiceInSecureDfu
        }
        
        if let forceDfu = forceDfu {
            dfuInitiator.forceDfu = forceDfu
        }
        
        if let alternativeAdvertisingNameEnabled = alternativeAdvertisingNameEnabled {
            dfuInitiator.alternativeAdvertisingNameEnabled = alternativeAdvertisingNameEnabled
        }
        
        pendingResult = result
        deviceAddress = address
        
        dfuController = dfuInitiator.start(targetWithIdentifier: uuid)
        print("dfuInitiator have start")
    }
    
    //MARK: DFUServiceDelegate
    public func dfuStateDidChange(to state: DFUState) {
        switch state {
        case .completed:
            pendingResult?(deviceAddress)
            pendingResult = nil
            print("\(deviceAddress!) onDfuCompleted")
            dfuController = nil
            channel.invokeMethod("onDfuCompleted", arguments: deviceAddress)
        case .disconnecting:
            print("\(deviceAddress!) onDeviceDisconnecting")
            channel.invokeMethod("onDeviceDisconnecting", arguments: deviceAddress)
        case .aborted:
            pendingResult?(FlutterError(code: "DFU_ABORRED", message: "Device address: \(deviceAddress!)", details: nil))
            pendingResult = nil
            print("\(deviceAddress!) onDfuAborted")
            channel.invokeMethod("onDfuAborted", arguments: deviceAddress)
        case .connecting:
            print("\(deviceAddress!) onDeviceConnecting")
            channel.invokeMethod("onDeviceConnecting", arguments: deviceAddress)
        case .starting:
            print("\(deviceAddress!) onDfuProcessStarting")
            channel.invokeMethod("onDfuProcessStarting", arguments: deviceAddress)
        case .enablingDfuMode:
            print("\(deviceAddress!) onEnablingDfuMode")
            channel.invokeMethod("onEnablingDfuMode", arguments: deviceAddress)
        case .validating:
            print("\(deviceAddress!) onFirmwareValidating")
            channel.invokeMethod("onFirmwareValidating", arguments: deviceAddress)
        case .uploading:
            print("\(deviceAddress!) onFirmwareUploading")
            channel.invokeMethod("onFirmwareUploading", arguments: deviceAddress)
        }
    }
    
    public func dfuError(_ error: DFUError, didOccurWithMessage message: String) {
        print("\(deviceAddress!) onError, message : \(message)")
        channel.invokeMethod("onError", arguments: deviceAddress)
        
        pendingResult?(FlutterError(code: "DFU_FAILED", message: "Device address: \(deviceAddress!)", details: nil))
        pendingResult = nil
    }
    
    //MARK: DFUProgressDelegate
    public func dfuProgressDidChange(for part: Int, outOf totalParts: Int, to progress: Int, currentSpeedBytesPerSecond: Double, avgSpeedBytesPerSecond: Double) {
        print("onProgressChanged: \(progress)")
        channel.invokeMethod("onProgressChanged", arguments: [
            "percent": progress,
            "speed": currentSpeedBytesPerSecond,
            "avgSpeed": avgSpeedBytesPerSecond,
            "currentPart": part,
            "partsTotal": totalParts,
            "deviceAddress": deviceAddress!
            ])
    }
    
    //MARK: - LoggerDelegate
    public func logWith(_ level: LogLevel, message: String) {
        //print("\(level.name()): \(message)")
    }
}
