[![pub package](https://img.shields.io/pub/v/flutter-nordic-dfu.svg)](https://pub.dartlang.org/packages/flutter-nordic-dfu)


This library allows you to do a Device Firmware Update (DFU) of your nrf51 or
nrf52 chip from Nordic Semiconductor. Current ,it only works for Android.

For more info about the DFU process, see: [Resources](#resources)


## Usage

### startDFU

#### Examples

```dart
await FlutterNordicDfu.startDfu(
        'EB:75:AD:E3:CA:CF', 'assets/318_nrf52810_190116_3L.zip',
        fileInAsset: true, progressListener: ProgressListenerListener());

class ProgressListenerListener extends DfuProgressListenerAdapter {
  @override
  void onProgressChanged(String deviceAddress, int percent, double speed,
      double avgSpeed, int currentPart, int partsTotal) {
    super.onProgressChanged(
        deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
    print('deviceAddress: $deviceAddress, percent: $percent');
  }
}
```

## Resources

-   [DFU Introduction](http://infocenter.nordicsemi.com/topic/com.nordic.infocenter.sdk5.v11.0.0/examples_ble_dfu.html?cp=6_0_0_4_3_1 "BLE Bootloader/DFU")
-   [Secure DFU Introduction](http://infocenter.nordicsemi.com/topic/com.nordic.infocenter.sdk5.v12.0.0/ble_sdk_app_dfu_bootloader.html?cp=4_0_0_4_3_1 "BLE Secure DFU Bootloader")
-   [How to create init packet](https://github.com/NordicSemiconductor/Android-nRF-Connect/tree/master/init%20packet%20handling "Init packet handling")
-   [nRF51 Development Kit (DK)](http://www.nordicsemi.com/eng/Products/nRF51-DK "nRF51 DK") (compatible with Arduino Uno Revision 3)
-   [nRF52 Development Kit (DK)](http://www.nordicsemi.com/eng/Products/Bluetooth-Smart-Bluetooth-low-energy/nRF52-DK "nRF52 DK") (compatible with Arduino Uno Revision 3)

