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
}

public class AudioChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "AudioChangeReceiver";
    AudioEventListener audioEventListener;

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
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
            } else if (state == 1) { // 耳机插入
                audioEventListener.onChanged(AudioEventListener.CHANGE_TO_HEADSET);
            }

        } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
            Log.e(TAG, "AudioChangeReceiver onReceive  ACTION_CONNECTION_STATE_CHANGED state-----"+state);
            updateBluetoothIndication(state);
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            Log.e(TAG, "AudioChangeReceiver onReceive  ACTION_AUDIO_BECOMING_NOISY -----");
            audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
        }
//        else if(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())){
//            audioEventListener.onChanged();
//        }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
//            audioEventListener.onChanged();
//        }
    }

    public void updateBluetoothIndication(int bluetoothHeadsetState) {
        if (bluetoothHeadsetState == BluetoothProfile.STATE_CONNECTED) {
            audioEventListener.onChanged(AudioEventListener.CHANGE_TO_BLUETOOTH);
        } else if (bluetoothHeadsetState == BluetoothProfile.STATE_DISCONNECTED) {
            audioEventListener.onChanged(AudioEventListener.CHANGE_TO_SPEAKER);
        } else {
            Log.i(TAG, "BluetoothProfile.OTHER");
        }
    }
}