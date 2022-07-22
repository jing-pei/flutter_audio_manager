package com.yung.flutter_audio_manager;

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

interface AudioEventListener {
    public static final int CHANGE_TO_RECEIVER = 1;
    public static final int CHANGE_TO_SPEAKER = 2;
    public static final int CHANGE_TO_HEADSET = 3;
    public static final int CHANGE_TO_BLUETOOTH = 4;
    void onChanged(int action);
    void onBlueChange(boolean isOpen);
}

public class AudioChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioChangeReceiver";
    AudioEventListener audioEventListener;
    private static boolean isWiredHeadset=false;  //有线耳机是否连接
    private boolean blueScoConnected = false;  //蓝牙是否连接
    private boolean isSpeaker=false;  //扬声器是否已启

    public AudioChangeReceiver(final AudioEventListener listener) {
        this.audioEventListener = listener;
    }


    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
//            final int state = intent.getIntExtra("state", -1);
            int state = intent.getIntExtra("state", 0);
            Log.e(TAG, "AudioChangeReceiver onReceive  ACTION_HEADSET_PLUG state-----"+state);
            if (state == 0) { // 耳机拔出
                isWiredHeadset=false;
                if(isSpeaker){
                    audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
                }else{
                    audioEventListener.onChanged(AudioEventListener.CHANGE_TO_RECEIVER);
                }
            } else if (state == 1) { // 耳机插入
                isWiredHeadset=true;
                isSpeaker=false;
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_HEADSET);
            }

        } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            //监听蓝牙连接相关
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
            Log.e(TAG, "AudioChangeReceiver onReceive  ACTION_CONNECTION_STATE_CHANGED state-----"+state);
            updateBluetoothIndication(state);
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            //声音将要从speaker播放
            isSpeaker=true;
            Log.e(TAG, "AudioChangeReceiver onReceive  ACTION_AUDIO_BECOMING_NOISY -----");
            audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
        }else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
            final int state = intent.getIntExtra(
                    BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
            if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                //audioEventListener.onChanged(AudioEventListener.CHANGE_TO_BLUETOOTH);
                //Log.e(TAG, "AudioChangeReceiver onReceive+++ Bluetooth audio SCO is now connected");
            } else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
                // Log.e(TAG, "AudioChangeReceiver onReceive+++ Bluetooth audio SCO is now connecting...");
            } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                // Log.e(TAG, "AudioChangeReceiver onReceive+++ Bluetooth audio SCO is now disconnected");
                if (isInitialStickyBroadcast()) {
                    // Log.d(TAG, "Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.");
                    return;
                }
                //audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
            }
        }

    }

    public void updateBluetoothIndication(int bluetoothHeadsetState) {
        if (bluetoothHeadsetState == BluetoothProfile.STATE_CONNECTED) {
            isSpeaker=false;
            blueScoConnected=true;
            audioEventListener.onBlueChange(true);
            audioEventListener.onChanged(AudioEventListener.CHANGE_TO_BLUETOOTH);
        } else if (bluetoothHeadsetState == BluetoothProfile.STATE_DISCONNECTED) {
//            audioManager.stopBluetoothSco();
//            audioManager.setBluetoothScoOn(false);
            blueScoConnected=false;
            audioEventListener.onBlueChange(false);
            if(isWiredHeadset){
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_HEADSET);
            }else if(isSpeaker){
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
            }else{
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_RECEIVER);
            }
        } else {
            Log.i(TAG, "BluetoothProfile.OTHER");
        }
    }
}