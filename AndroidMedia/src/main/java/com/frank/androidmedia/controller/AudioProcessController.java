package com.frank.androidmedia.controller;

import android.media.audiofx.*;
import android.util.Log;

public class AudioProcessController {
    private static final String TAG = AudioProcessController.class.getSimpleName();

    private NoiseSuppressor noiseSuppressor;
    private AutomaticGainControl automaticGainControl;
    private AcousticEchoCanceler acousticEchoCanceler;

    public boolean initAEC(int audioSessionId) {
        if (!AcousticEchoCanceler.isAvailable()) {
            Log.e(TAG, "AEC not available...");
            return false;
        }
        try {
            acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "init AcousticEchoCanceler error=" + e);
            return false;
        }
        int result = acousticEchoCanceler.setEnabled(true);
        if (result != AudioEffect.SUCCESS) {
            acousticEchoCanceler.release();
            acousticEchoCanceler = null;
            return false;
        }
        return true;
    }

    public boolean initAGC(int audioSessionId) {
        if (!AutomaticGainControl.isAvailable()) {
            Log.e(TAG, "AGC not available...");
            return false;
        }
        try {
            automaticGainControl = AutomaticGainControl.create(audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "init AutomaticGainControl error=" + e);
            return false;
        }
        int result = automaticGainControl.setEnabled(true);
        if (result != AudioEffect.SUCCESS) {
            automaticGainControl.release();
            automaticGainControl = null;
            return false;
        }
        return true;
    }

    public boolean initNS(int audioSessionId) {
        if (!NoiseSuppressor.isAvailable()) {
            Log.e(TAG, "NS not available...");
            return false;
        }
        try {
            noiseSuppressor = NoiseSuppressor.create(audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "init NoiseSuppressor error=" + e);
            return false;
        }
        int result = noiseSuppressor.setEnabled(true);
        if (result != AudioEffect.SUCCESS) {
            noiseSuppressor.release();
            noiseSuppressor = null;
            return false;
        }
        return true;
    }

    public void release() {
        if (noiseSuppressor != null) noiseSuppressor.release();
        if (acousticEchoCanceler != null) acousticEchoCanceler.release();
        if (automaticGainControl != null) automaticGainControl.release();
    }
}
