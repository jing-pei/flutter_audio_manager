import 'dart:async';
import 'package:flutter/services.dart';

enum AudioPort {
  /// unknow 0
  unknow,

  /// input 1
  receiver, //听筒

  /// out speaker 2
  speaker, //扬声器

  /// headset 3
  headphones, //耳机

  /// bluetooth 4
  bluetooth, //蓝牙
}

class AudioInput {
  final String? name;
  final int _port;
  bool isSelected;
  AudioPort get port {
    return AudioPort.values[_port];
  }

  AudioInput(this.name, this._port, {this.isSelected = false});

  @override
  String toString() {
    return "name:$name,port:$port";
  }
}

class FlutterAudioManager {
  static const MethodChannel _channel =
      const MethodChannel('flutter_audio_manager');
  static void Function()? _onInputChanged;

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<AudioInput> getCurrentOutput() async {
    final List<dynamic> data =
        await (_channel.invokeMethod('getCurrentOutput'));
    return AudioInput(data[0], int.parse(data[1]));
  }

  static Future<List<AudioInput>> getAllOutputDevices() async {
    final List<dynamic> list =
        await (_channel.invokeMethod('getAllOutputDevices'));
    List<AudioInput> arr = [];
    int port = 0;
    list.forEach((device) {
      if (device == "Bluetooth") {
        port = 4;
      } else if (device == "Headset") {
        port = 3;
      } else if (device == "Speaker") {
        port = 2;
      } else if (device == "Receiver") {
        port = 1;
      }
      arr.add(AudioInput(device, port));
    });
    return arr;
  }

  static Future<List<AudioInput>> getAvailableInputs() async {
    final List<dynamic> list =
        await (_channel.invokeMethod('getAvailableInputs'));

    List<AudioInput> arr = [];
    list.forEach((data) {
      arr.add(AudioInput(data[0], int.parse(data[1])));
    });
    return arr;
  }

  static Future<bool?> changeToSpeaker() async {
    return await _channel.invokeMethod('changeToSpeaker');
  }

  static Future<bool?> changeToReceiver() async {
    return await _channel.invokeMethod('changeToReceiver');
  }

  static Future<bool?> changeToHeadphones() async {
    return await _channel.invokeMethod('changeToHeadphones');
  }

  static Future<bool?> changeToBluetooth() async {
    return await _channel.invokeMethod('changeToBluetooth');
  }

  static void setListener(void Function() onInputChanged) {
    FlutterAudioManager._onInputChanged = onInputChanged;
    _channel.setMethodCallHandler(_methodHandle);
  }

  static Future<void> _methodHandle(MethodCall call) async {
    if (_onInputChanged == null) return;
    switch (call.method) {
      case "inputChanged":
        return _onInputChanged!();
      default:
        break;
    }
  }
}
