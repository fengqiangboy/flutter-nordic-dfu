import Flutter
import UIKit
import iOSDFULibrary
import CoreBluetooth

public class SwiftFlutterNordicDfuPlugin: NSObject, FlutterPlugin, DFUServiceDelegate, DFUProgressDelegate, LoggerDelegate, CBCentralManagerDelegate {
    
    let registrar: FlutterPluginRegistrar
    let channel: FlutterMethodChannel
    var pendingResult: FlutterResult?
    var deviceAddress: String?
    var centralManager: CBCentralManager?
    
    init(_ registrar: FlutterPluginRegistrar, _ channel: FlutterMethodChannel) {
        self.registrar = registrar
        self.channel = channel
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.timeyaa.flutter_nordic_dfu/method", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterNordicDfuPlugin(registrar, channel)
        instance.centralManager = CBCentralManager(delegate: instance, queue: nil)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if (call.method == "startDfu") {
            guard let arguements = call.arguments as? Dictionary<String, AnyObject> else {
                result(FlutterError(code: "ABNORMAL_PARAMETER", message: "no parameters", details: nil))
                return
            }
            let name = arguements["name"] as? String
            guard let address = arguements["address"] as? String,
                let filePath = arguements["filePath"] as? String else {
                    result(FlutterError(code: "ABNORMAL_PARAMETER", message: "address and filePath are required", details: nil))
                    return
            }
            
            startDfu(address, name: name, filePath: filePath, result: result)
        }
    }
    
    private func startDfu(_ address: String, name: String?, filePath: String, result: @escaping FlutterResult) {
        guard let uuid = UUID(uuidString: address) else {
            result(FlutterError(code: "DEVICE_ADDRESS_ERROR", message: "Device address conver to uuid failed", details: "Device uuid \(address) convert to uuid failed"))
            return
        }
        let peripherals = centralManager!.retrievePeripherals(withIdentifiers: [uuid])
        
        if peripherals.count != 1 {
            result(FlutterError(code: "UNABLE_TO_FIND_DEVICE", message: "Could not find device with deviceAddress", details: nil))
            return
        }
        
        guard let firmware = DFUFirmware(urlToZipFile: URL(fileURLWithPath: filePath)) else {
            result(FlutterError(code: "DFU_FIRMWARE_NOT_FOUND", message: "Could not dfu zip file", details: nil))
            return
        }
        
        let dfuInitiator = DFUServiceInitiator(target:peripherals.first!)
            .with(firmware: firmware);
        dfuInitiator.delegate = self
        dfuInitiator.progressDelegate = self
        dfuInitiator.logger = self
        dfuInitiator.enableUnsafeExperimentalButtonlessServiceInSecureDfu = true
        pendingResult = result
        deviceAddress = address
        
        _ = dfuInitiator.start()
    }
    
    //MARK: DFUServiceDelegate
    public func dfuStateDidChange(to state: DFUState) {
        switch state {
        case .completed:
            pendingResult?(deviceAddress)
            pendingResult = nil
            channel.invokeMethod("onDfuCompleted", arguments: deviceAddress)
        case .disconnecting:
            channel.invokeMethod("onDeviceDisconnecting", arguments: deviceAddress)
        case .aborted:
            pendingResult?(FlutterError(code: "DFU_ABORRED", message: "Device address: \(deviceAddress!)", details: nil))
            pendingResult = nil
            channel.invokeMethod("onDfuAborted", arguments: deviceAddress)
        case .connecting:
            channel.invokeMethod("onDeviceConnecting", arguments: deviceAddress)
        case .starting:
            channel.invokeMethod("onDfuProcessStarting", arguments: deviceAddress)
        case .enablingDfuMode:
            channel.invokeMethod("onEnablingDfuMode", arguments: deviceAddress)
        case .validating:
            channel.invokeMethod("onFirmwareValidating", arguments: deviceAddress)
        case .uploading:
            channel.invokeMethod("onFirmwareUploading", arguments: deviceAddress)
        }
    }
    
    public func dfuError(_ error: DFUError, didOccurWithMessage message: String) {
        channel.invokeMethod("onError", arguments: deviceAddress)
        
        pendingResult?(FlutterError(code: "DFU_FAILED", message: "Device address: \(deviceAddress!)", details: nil))
        pendingResult = nil
    }
    
    //MARK: DFUProgressDelegate
    public func dfuProgressDidChange(for part: Int, outOf totalParts: Int, to progress: Int, currentSpeedBytesPerSecond: Double, avgSpeedBytesPerSecond: Double) {
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
    
    // MARK: - CBCentralManagerDelegate
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        
    }
}
