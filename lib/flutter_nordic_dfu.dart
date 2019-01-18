import 'dart:async';

import 'package:flutter/services.dart';

class FlutterNordicDfu {

  static const String NAMESPACE = 'com.timeyaa.flutter_nordic_dfu';

  static const MethodChannel _channel =
      const MethodChannel('$NAMESPACE/method');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
