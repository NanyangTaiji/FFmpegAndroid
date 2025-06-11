package com.frank.androidmedia.listener;

import android.util.Pair;
import android.widget.SeekBar;
import java.util.ArrayList;
import java.util.List;

/**
 * The callback of AudioEffect
 *
 * @author frank
 * @date 2022/3/23
 */
public interface AudioEffectCallback {
    List<SeekBar> getSeekBarList();

    void setEqualizerList(int maxProgress, ArrayList<Pair<String, Integer>> equalizerList);

    void onFFTDataCallback(byte[] fft);
}