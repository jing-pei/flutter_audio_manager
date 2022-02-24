import Flutter
import UIKit
import AVFoundation

public class SwiftFlutterAudioManagerPlugin: NSObject, FlutterPlugin {
  var channel : FlutterMethodChannel?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_audio_manager", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterAudioManagerPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    instance.channel = channel;
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
      DispatchQueue.global().async {
          if (call.method == "getCurrentOutput"){
                  result(self.getCurrentOutput())
          }
          else if(call.method == "getAvailableInputs"){
              result(self.getAvailableInputs())
          }
          else if(call.method == "changeToSpeaker"){
              result(self.changeToSpeaker())
          }
          else if(call.method == "changeToReceiver"){
              result(self.changeToReceiver())
          }
          else if(call.method == "changeToHeadphones"){
              result(self.changeToBluetooth())
          }
          else if(call.method == "changeToBluetooth"){
              result(self.changeToBluetooth())
          } else {
              result("iOS " + UIDevice.current.systemVersion)
          }
      }
  }
  func getCurrentOutput() -> [String]  {
        let currentRoute = AVAudioSession.sharedInstance().currentRoute
        for output in currentRoute.outputs {
            return getInfo(output);
        }
        return ["unknow","0"];
    }
    
    func getAvailableInputs() -> [[String]]  {
        var arr = [[String]]()
        if let inputs = AVAudioSession.sharedInstance().availableInputs {
            for input in inputs {
                arr.append(getInfo(input));
             }
        }
        return arr;
    }
    
    func getInfo(_ input:AVAudioSessionPortDescription) -> [String] {
        var type="0";
        let port = AVAudioSession.Port.self;
        switch input.portType {
        case port.builtInReceiver,port.builtInMic:
            type="1";
            break;
        case port.builtInSpeaker:
            type="2";
            break;
        case port.headsetMic,port.headphones:
            type="3";
            break;
        case port.bluetoothA2DP,port.bluetoothLE,port.bluetoothHFP:
            type="4";
            break;
        default:
            type="0";
        }
        return [input.portName,type];
    }
    
    
    func changeToSpeaker() -> Bool{
        do {
//            let session = AVAudioSession.sharedInstance()
//            try session.setCategory(AVAudioSession.Category.playAndRecord, options: [AVAudioSession.CategoryOptions.defaultToSpeaker, AVAudioSession.CategoryOptions.allowBluetooth, AVAudioSession.CategoryOptions.duckOthers, AVAudioSession.CategoryOptions.mixWithOthers]);
//            try session.setActive(true, options: AVAudioSession.SetActiveOptions.notifyOthersOnDeactivation)
            let session = AVAudioSession.sharedInstance()
            try session.overrideOutputAudioPort(.speaker)
            return true;
        } catch {
            return false;
        }
    }
        
    
    func changeToReceiver() -> Bool{
        do {
//            let session = AVAudioSession.sharedInstance()
//            try session.setCategory(AVAudioSession.Category.playAndRecord, options: [AVAudioSession.CategoryOptions.allowBluetooth, AVAudioSession.CategoryOptions.duckOthers, AVAudioSession.CategoryOptions.mixWithOthers])
//            try session.setActive(true, options: AVAudioSession.SetActiveOptions.notifyOthersOnDeactivation)
            
            let session = AVAudioSession.sharedInstance()
            try session.overrideOutputAudioPort(.none)
            
            return true;
        } catch {
            return false;
        }
    }
    
    
    func changeToHeadphones() -> Bool{
        return changeByPortType([AVAudioSession.Port.headsetMic])
    }
    
    func changeToBluetooth() -> Bool{
        let arr = [AVAudioSession.Port.bluetoothLE,AVAudioSession.Port.bluetoothHFP,AVAudioSession.Port.bluetoothA2DP];
        return changeByPortType(arr)
    }
    
    func changeByPortType(_ ports:[AVAudioSession.Port]) -> Bool{
        let currentRoute = AVAudioSession.sharedInstance().currentRoute
        for output in currentRoute.outputs {
            if(ports.firstIndex(of: output.portType) != nil){
                return true;
            }
        }
        if let inputs = AVAudioSession.sharedInstance().availableInputs {
            for input in inputs {
                if(ports.firstIndex(of: input.portType) != nil){
                    try?AVAudioSession.sharedInstance().setPreferredInput(input);
                    return true;
                }
             }
        }
        return false;
    }
    
    public override init() {
        super.init()
        registerAudioRouteChangeBlock()
    }
    
    func registerAudioRouteChangeBlock(){
        NotificationCenter.default.addObserver( forName:AVAudioSession.routeChangeNotification, object: AVAudioSession.sharedInstance(), queue: nil) { notification in
            guard let userInfo = notification.userInfo,
                let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
                let reason = AVAudioSession.RouteChangeReason(rawValue:reasonValue) else {
                    return
            }
            self.channel!.invokeMethod("inputChanged",arguments: 1)
        }
    }
}

