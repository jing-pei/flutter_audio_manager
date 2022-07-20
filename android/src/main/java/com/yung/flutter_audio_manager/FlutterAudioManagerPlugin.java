package com.yung.flutter_audio_manager;


import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterAudioManagerPlugin
 */
public class FlutterAudioManagerPlugin implements FlutterPlugin, MethodCallHandler {
    private static final String TAG = "FlutterAudioManager";
    private static MethodChannel channel;
    private static AudioManager audioManager;
    private static Context activeContext;

    public static final String AUDIO_TYPE_NONE = "0";
    public static final String AUDIO_TYPE_NONE_NAME = "None";

    public static final String AUDIO_TYPE_RECEIVER = "1";
    public static final String AUDIO_TYPE_RECEIVER_NAME = "Receiver";

    public static final String AUDIO_TYPE_SPEAKER = "2";
    public static final String AUDIO_TYPE_SPEAKER_NAME = "Speaker";

    public static final String AUDIO_TYPE_HEADSET= "3";
    public static final String AUDIO_TYPE_HEADSET_NAME= "Headset";

    public static final String AUDIO_TYPE_BLUETOOTH= "4";
    public static final String AUDIO_TYPE_BLUETOOTH_NAME= "Bluetooth";


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_audio_manager");
        channel.setMethodCallHandler(new FlutterAudioManagerPlugin());
        AudioChangeReceiver receiver = new AudioChangeReceiver(listener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        activeContext = flutterPluginBinding.getApplicationContext();
        activeContext.registerReceiver(receiver, filter);
        audioManager = (AudioManager) activeContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public static void registerWith(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), "flutter_audio_manager");
        channel.setMethodCallHandler(new FlutterAudioManagerPlugin());
        AudioChangeReceiver receiver = new AudioChangeReceiver(listener);
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        activeContext = registrar.activeContext();
        activeContext.registerReceiver(receiver, filter);
        audioManager = (AudioManager) activeContext.getSystemService(Context.AUDIO_SERVICE);
    }

    static AudioEventListener listener = new AudioEventListener() {
        int type;
        @Override
        public void onChanged(int action) {
            try {
//                switch (action) {
//                    case CHANGE_TO_RECEIVER:
//                        type=1;
//                        changeToReceiver();
//                        break;
//                    case CHANGE_TO_SPEAKER:
//                        type=2;
//                        changeToSpeaker();
//                        break;
//                    case CHANGE_TO_HEADSET:
//                        type=3;
//                        changeToReceiver();
//                        break;
//                    case CHANGE_TO_BLUETOOTH:
//                        type=4;
//                        changeToBluetooth();
//                        break;
//                }
                if (channel != null) channel.invokeMethod("inputChanged", action);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getAllOutputDevices")) {
            result.success(getAllOutputDevices());
        }else if (call.method.equals("getCurrentOutput")) {
            result.success(getCurrentOutput());
        } else if (call.method.equals("getAvailableInputs")) {
            result.success(getAvailableInputs());
        } else if (call.method.equals("changeToReceiver")) {
            result.success(changeToReceiver());
        } else if (call.method.equals("changeToSpeaker")) {
            result.success(changeToSpeaker());
        } else if (call.method.equals("changeToHeadphones")) {
            result.success(changeToHeadphones());
        } else if (call.method.equals("changeToBluetooth")) {
            result.success(changeToBluetooth());
        } else {
            result.notImplemented();
        }
    }

    /**
     * 切换至听筒
     *
     * @return
     */
    private static Boolean changeToReceiver() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(false);

//        audioManager.setSpeakerphoneOn(false);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//        } else {
//            audioManager.setMode(AudioManager.MODE_IN_CALL);
//        }
        listener.onChanged(AudioEventListener.CHANGE_TO_RECEIVER);
        return true;
    }


    /**
     * 切换至扬声器
     *
     * @return
     */
    private static Boolean changeToSpeaker() {
        //注意此处，蓝牙未断开时使用MODE_IN_COMMUNICATION而不是MODE_NORMAL
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothScoOn(false);
        audioManager.setSpeakerphoneOn(true);
        listener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
        return true;
    }



    private static Boolean changeToHeadphones() {
        return changeToReceiver();
    }

    /**
     * 切换至蓝牙耳机
     *
     * @return
     */
    private static Boolean changeToBluetooth() {
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.startBluetoothSco();
        audioManager.setBluetoothScoOn(true);
        audioManager.setSpeakerphoneOn(false);
        listener.onChanged(AudioEventListener.CHANGE_TO_BLUETOOTH);
        return true;
    }

    private  List<String> newAudioDevices;
    private List<String> getAllOutputDevices() {
        newAudioDevices = new ArrayList<>();
        if (audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn()){
            newAudioDevices.add(AUDIO_TYPE_BLUETOOTH_NAME);
        }
        if (hasWiredHeadset()) {
            // If a wired headset is connected, then it is the only possible option.
            newAudioDevices.add(AUDIO_TYPE_HEADSET_NAME);
        } else {
            // No wired headset, hence the audio-device list can contain speaker
            // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
            if (hasEarpiece()) {
                newAudioDevices.add(AUDIO_TYPE_RECEIVER_NAME);
            }
        }
        newAudioDevices.add(AUDIO_TYPE_SPEAKER_NAME);
        return newAudioDevices;
    }

    private boolean hasWiredHeadset() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return audioManager.isWiredHeadsetOn();
        } else {
            final AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
            for (AudioDeviceInfo device : devices) {
                final int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    Log.d(TAG, "hasWiredHeadset: found wired headset");
                    return true;
                } else if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    Log.d(TAG, "hasWiredHeadset: found USB audio device");
                    return true;
                }
            }
            return false;
        }
    }


    /** Gets the current earpiece state. */
    private boolean hasEarpiece() {
        return activeContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }


    private List<String> getCurrentOutput() {
        List<String> info = new ArrayList();
        if (audioManager.isBluetoothScoOn()) {
            info.add(AUDIO_TYPE_BLUETOOTH_NAME);
            info.add(AUDIO_TYPE_BLUETOOTH);
        } else if (audioManager.isSpeakerphoneOn()) {
            info.add(AUDIO_TYPE_SPEAKER_NAME);
            info.add(AUDIO_TYPE_SPEAKER);
        } else if (audioManager.isWiredHeadsetOn()) {
            info.add(AUDIO_TYPE_HEADSET_NAME);
            info.add(AUDIO_TYPE_HEADSET);
        } else {
            info.add(AUDIO_TYPE_RECEIVER_NAME);
            info.add(AUDIO_TYPE_RECEIVER);
        }
        return info;
        // if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // MediaRouter mr = (MediaRouter)
        // activeContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        // MediaRouter.RouteInfo routeInfo =
        // mr.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
        // Log.d("aaa", "getCurrentOutput:
        // "+audioManager.isSpeakerphoneOn()+audioManager.isWiredHeadsetOn()+audioManager.isSpeakerphoneOn());
        // info.add(routeInfo.getName().toString());
        // info.add(_getDeviceType(routeInfo.getDeviceType()));
        // } else {
        // info.add("unknow");
        // info.add("0");
        // }
        // return info;
    }

    private List<List<String>> getAvailableInputs() {
        List<List<String>> list = new ArrayList();
        list.add(Arrays.asList(AUDIO_TYPE_RECEIVER_NAME, AUDIO_TYPE_RECEIVER));
        if (audioManager.isWiredHeadsetOn()) {
            list.add(Arrays.asList(AUDIO_TYPE_HEADSET_NAME, AUDIO_TYPE_HEADSET));
        }
        if (audioManager.isBluetoothScoOn()) {
            list.add(Arrays.asList(AUDIO_TYPE_BLUETOOTH_NAME, AUDIO_TYPE_BLUETOOTH));
        }
        return list;
    }

    private String _getDeviceType(int type) {
        Log.d("type", "type: " + type);
        switch (type) {
            case 3:
                return AUDIO_TYPE_HEADSET;
            case 2:
                return AUDIO_TYPE_SPEAKER;
            case 1:
                return AUDIO_TYPE_BLUETOOTH;
            default:
                return AUDIO_TYPE_NONE;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        // 切换到正常的模式
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
    }
}
