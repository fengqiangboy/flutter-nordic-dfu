import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_nordic_dfu/flutter_nordic_dfu.dart';
import 'package:flutter_blue/flutter_blue.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final FlutterBlue flutterBlue = FlutterBlue.instance;
  StreamSubscription<ScanResult> scanSubscription;

  @override
  void initState() {
    super.initState();
  }

  void test() async {
    var s = await FlutterNordicDfu.startDfu(
      'C0:D0:59:F1:A8:3F',
      'assets/file.zip',
      fileInAsset: true,
      progressListener:
          DefaultDfuProgressListenerAdapter(onProgressChangedHandle: (
        deviceAddress,
        percent,
        speed,
        avgSpeed,
        currentPart,
        partsTotal,
      ) {
        print('deviceAddress: $deviceAddress, percent: $percent');
      }),
    );
    print(s);
  }

  void startScan() {
    scanSubscription?.cancel();
    scanSubscription = null;
    scanSubscription = flutterBlue.scan().listen((scanResult) {
      print(scanResult.device.id);
    });
    setState(() {});
  }

  void stopScan() {
    scanSubscription?.cancel();
    scanSubscription = null;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final isScanning = scanSubscription != null;
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: <Widget>[
            isScanning
                ? IconButton(
                    icon: Icon(Icons.pause_circle_filled),
                    onPressed: stopScan,
                  )
                : IconButton(
                    icon: Icon(Icons.play_arrow),
                    onPressed: startScan,
                  )
          ],
        ),
        body: Center(
          child: RaisedButton(
            child: Text('Running on'),
            onPressed: () {
              test();
            },
          ),
        ),
      ),
    );
  }
}

class ProgressListenerListener extends DfuProgressListenerAdapter {
  @override
  void onProgressChanged(String deviceAddress, int percent, double speed,
      double avgSpeed, int currentPart, int partsTotal) {
    super.onProgressChanged(
        deviceAddress, percent, speed, avgSpeed, currentPart, partsTotal);
    print('deviceAddress: $deviceAddress, percent: $percent');
  }
}
