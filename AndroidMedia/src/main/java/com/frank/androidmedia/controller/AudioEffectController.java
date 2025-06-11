package com.frank.androidmedia.controller;

import android.content.Context;
import android.media.audiofx.*;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import androidx.annotation.RequiresApi;
import com.frank.androidmedia.listener.AudioEffectCallback;
import com.frank.androidmedia.wrap.AudioVisualizer;
import java.util.ArrayList;
import java.util.List;

public class AudioEffectController {
    private static final String TAG = AudioEffectController.class.getSimpleName();

    private short mBands = 0;
    private short minEQLevel = 0;
    private Equalizer mEqualizer;
    private BassBoost mBass;
    private PresetReverb mPresetReverb;
    private AudioVisualizer mVisualizer;
    private LoudnessEnhancer mLoudnessEnhancer;
    private AudioEffectCallback mAudioEffectCallback;
    private final String[] presetReverb = {"None", "SmallRoom", "MediumRoom",
            "LargeRoom", "MediumHall", "LargeHall", "Plate"};

    public AudioEffectController(AudioEffectCallback audioEffectCallback) {
        mAudioEffectCallback = audioEffectCallback;
    }

    public void setupEqualizer(int audioSessionId) {
        ArrayList<Pair<String, Integer>> equalizerList = new ArrayList<>();
        mEqualizer = new Equalizer(0, audioSessionId);
        mEqualizer.setEnabled(true);

        minEQLevel = mEqualizer.getBandLevelRange()[0];
        short maxEQLevel = mEqualizer.getBandLevelRange()[1];
        mBands = mEqualizer.getNumberOfBands();

        for (short i = 0; i < mBands; i++) {
            String centerFreq = (mEqualizer.getCenterFreq(i) / 1000) + " Hz";
            Pair<String, Integer> pair = new Pair<>(centerFreq, mEqualizer.getBandLevel(i) - minEQLevel);
            equalizerList.add(pair);
        }

        if (mAudioEffectCallback != null) {
            mAudioEffectCallback.setEqualizerList(maxEQLevel - minEQLevel, equalizerList);
        }
    }

    public void setupPresetStyle(Context context, Spinner spinnerStyle) {
        ArrayList<String> mReverbValues = new ArrayList<>();
        for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
            mReverbValues.add(mEqualizer.getPresetName(i));
        }

        spinnerStyle.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mReverbValues));
        spinnerStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                try {
                    mEqualizer.usePreset((short) arg2);
                    List<SeekBar> seekBarList = mAudioEffectCallback != null ? mAudioEffectCallback.getSeekBarList() : null;
                    if (mBands > 0 && seekBarList != null && mEqualizer != null) {
                        for (short band = 0; band < mBands; band++) {
                            seekBarList.get(band).setProgress(mEqualizer.getBandLevel(band) - minEQLevel);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "preset style error=" + e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });
    }

    public void setupBassBoost(int audioSessionId, SeekBar barBassBoost) {
        mBass = new BassBoost(0, audioSessionId);
        mBass.setEnabled(true);
        barBassBoost.setMax(1000);
        barBassBoost.setProgress(0);
        barBassBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBass.setStrength((short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public void setLoudnessEnhancer(int audioSessionId, SeekBar barEnhancer) {
        mLoudnessEnhancer = new LoudnessEnhancer(audioSessionId);
        mLoudnessEnhancer.setEnabled(true);
        mLoudnessEnhancer.setTargetGain(500);
        barEnhancer.setMax(1000);
        barEnhancer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mLoudnessEnhancer.setTargetGain(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void setupVisualizer(int audioSessionId) {
        mVisualizer = new AudioVisualizer();
        mVisualizer.initVisualizer(audioSessionId, false, true, new Visualizer.OnDataCaptureListener() {
            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (fft != null && mAudioEffectCallback != null) {
                    mAudioEffectCallback.onFFTDataCallback(fft);
                }
            }

            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}
        });
    }

    public void onEqualizerProgress(int index, int progress) {
        mEqualizer.setBandLevel((short) index, (short) (progress + minEQLevel));
    }

    public void release() {
        if (mBass != null) mBass.release();
        if (mEqualizer != null) mEqualizer.release();
        if (mPresetReverb != null) mPresetReverb.release();
        if (mLoudnessEnhancer != null) mLoudnessEnhancer.release();
        if (mVisualizer != null) mVisualizer.releaseVisualizer();
    }
}
