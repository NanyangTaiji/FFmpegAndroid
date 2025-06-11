package com.frank.ffmpeg.activity;

import android.Manifest;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.frank.androidmedia.controller.AudioEffectController;
import com.frank.androidmedia.listener.AudioEffectCallback;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.adapter.EqualizerAdapter;
import com.frank.ffmpeg.listener.OnSeekBarListener;
import com.frank.ffmpeg.util.FileUtil;
import com.frank.ffmpeg.view.VisualizerView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Audio effect: equalizer, enhancer, visualizer, bassBoost
 * Created by frank on 2020/10/20.
 */
public class AudioEffectActivity extends BaseActivity implements OnSeekBarListener, AudioEffectCallback {

    private static String audioPath = Environment.getExternalStorageDirectory().getPath() + "/tiger.mp3";

    private MediaPlayer mPlayer;
    private Spinner spinnerStyle;
    private Spinner spinnerReverb;
    private SeekBar barBassBoost;
    private EqualizerAdapter equalizerAdapter;
    private SeekBar barEnhancer;
    private VisualizerView visualizerView;

    private AudioEffectController mAudioEffectController;

    private String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mAudioEffectController = new AudioEffectController(AudioEffectActivity.this);
            mAudioEffectController.setupEqualizer(mPlayer.getAudioSessionId());
            mAudioEffectController.setupPresetStyle(AudioEffectActivity.this, spinnerStyle);
            mAudioEffectController.setupBassBoost(mPlayer.getAudioSessionId(), barBassBoost);
            mAudioEffectController.setLoudnessEnhancer(mPlayer.getAudioSessionId(), barEnhancer);
            mAudioEffectController.setupVisualizer(mPlayer.getAudioSessionId());

            mPlayer.start();
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_audio_effect;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermission();
        initView();
        initPlayer();
    }

    private void initView() {
        spinnerStyle = findViewById(R.id.spinner_style);
        spinnerReverb = findViewById(R.id.spinner_reverb);
        barBassBoost = findViewById(R.id.bar_bassboost);
        barEnhancer = findViewById(R.id.bar_enhancer);
        visualizerView = findViewById(R.id.visualizer_view);
        RecyclerView equalizerView = findViewById(R.id.equalizer_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        equalizerView.setLayoutManager(layoutManager);
        equalizerAdapter = new EqualizerAdapter(this, this);
        equalizerView.setAdapter(equalizerAdapter);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 456);
        }
    }

    private void initPlayer() {
        if (!FileUtil.checkFileExist(audioPath)) {
            if (visualizerView != null) {
                visualizerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSelectFile();
                    }
                }, 500);
            }
            return;
        }
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(audioPath);
            mPlayer.setOnPreparedListener(onPreparedListener);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("AudioEffect", "play error=" + e);
        }
    }

    @Override
    public void onProgress(int index, int progress) {
        if (mAudioEffectController != null) {
            mAudioEffectController.onEqualizerProgress(index, progress);
        }
    }

    @Override
    public void onViewClick(View view) {

    }

    @Override
    public void onSelectedFile(String filePath) {
        audioPath = filePath;
        initPlayer();
    }

    @Override
    public void setEqualizerList(int maxProgress, ArrayList<Pair<String, Integer>> equalizerList) {
        if (equalizerAdapter != null) {
            equalizerAdapter.setMaxProgress(maxProgress);
            equalizerAdapter.setEqualizerList(equalizerList);
        }
    }

    @Override
    public List<SeekBar> getSeekBarList() {
        return equalizerAdapter != null ? equalizerAdapter.getSeekBarList() : null;
    }

    @Override
    public void onFFTDataCallback(byte[] fft) {
        if (fft != null && visualizerView != null) {
            visualizerView.post(new Runnable() {
                @Override
                public void run() {
                    visualizerView.setWaveData(fft);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAudioEffectController != null) {
            mAudioEffectController.release();
        }
        if (mPlayer != null) {
            mPlayer.release();
        }
    }
}
