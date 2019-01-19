import 'package:flutter/material.dart';
import 'package:flutter_nordic_dfu/flutter_nordic_dfu.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  void test() async {
    var s = await FlutterNordicDfu.startDfu(
        'EB:75:AD:E3:CA:CF', 'assets/318_nrf52810_190116_3L.zip',
        fileInAsset: true);
    print(s);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
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
