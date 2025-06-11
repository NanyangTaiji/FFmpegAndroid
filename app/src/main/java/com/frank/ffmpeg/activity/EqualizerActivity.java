package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.frank.ffmpeg.AudioPlayer;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.adapter.EqualizerAdapter;
import com.frank.ffmpeg.listener.OnSeekBarListener;
import com.frank.ffmpeg.util.TimeUtil;
import java.util.ArrayList;

public class EqualizerActivity extends BaseActivity implements OnSeekBarListener {

    // unit: Hz  gain:0-20
    /*- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     |   1b   |   2b   |   3b   |   4b   |   5b   |   6b   |   7b   |   8b   |   9b   |
     |   65   |   92   |   131  |   185  |   262  |   370  |   523  |   740  |  1047  |
     |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     |   10b  |   11b  |   12b  |   13b  |   14b  |   15b  |   16b  |   17b  |   18b  |
     |   1480 |   2093 |   2960 |   4186 |   5920 |   8372 |  11840 |  16744 |  20000 |
     |- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/
    private final int[] bandsList = {
            65, 92, 131, 185, 262, 370,
            523, 740, 1047, 1480, 2093, 2960,
            4180, 5920, 8372, 11840, 16744, 20000
    };

    private SeekBar audioBar;
    private TextView txtTime;
    private TextView txtDuration;

    private final int[] selectBandList = new int[bandsList.length];
    private final int minEQLevel = 0;
    private Thread filterThread;
    private AudioPlayer mAudioPlayer;
    private EqualizerAdapter equalizerAdapter;
    private String audioPath = Environment.getExternalStorageDirectory().getPath() + "/tiger.mp3";

    private static final int MSG_POSITION = 0x01;
    private static final int MSG_DURATION = 0x02;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_POSITION:
                    if (audioBar != null && mAudioPlayer != null) {
                        audioBar.setProgress((int) mAudioPlayer.getCurrentPosition());
                        txtTime.setText(TimeUtil.getVideoTime(mAudioPlayer.getCurrentPosition()));
                        sendEmptyMessageDelayed(MSG_POSITION, 1000);
                    }
                    break;
                case MSG_DURATION:
                    long duration = (Long) msg.obj;
                    if (txtDuration != null && audioBar != null) {
                        txtDuration.setText(TimeUtil.getVideoTime(duration));
                        audioBar.setMax((int) duration);
                    }
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_equalizer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        setupEqualizer();
        doEqualize();
    }

    private void initView() {
        audioBar = findViewById(R.id.eq_bar);
        txtTime = findViewById(R.id.txt_eq_time);
        txtDuration = findViewById(R.id.txt_eq_duration);

        RecyclerView equalizerView = findViewById(R.id.list_equalizer);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        equalizerView.setLayoutManager(layoutManager);
        equalizerAdapter = new EqualizerAdapter(this, this);
        equalizerView.setAdapter(equalizerAdapter);

        RadioButton effectEcho = findViewById(R.id.btn_effect_echo);
        RadioButton effectFunny = findViewById(R.id.btn_effect_funny);
        RadioButton effectTremolo = findViewById(R.id.btn_effect_tremolo);
        RadioButton effectLolita = findViewById(R.id.btn_effect_lolita);
        RadioButton effectUncle = findViewById(R.id.btn_effect_uncle);
        RadioGroup effectGroup = findViewById(R.id.group_audio_effect);

        effectGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == effectEcho.getId()) {
                    doAudioEffect(0);
                } else if (checkedId == effectFunny.getId()) {
                    doAudioEffect(1);
                } else if (checkedId == effectTremolo.getId()) {
                    doAudioEffect(2);
                } else if (checkedId == effectLolita.getId()) {
                    doAudioEffect(3);
                } else if (checkedId == effectUncle.getId()) {
                    doAudioEffect(4);
                }
            }
        });
    }

    private void setupEqualizer() {
        ArrayList<Pair<String, Integer>> equalizerList = new ArrayList<>();
        int maxEQLevel = 20;

        for (int band : bandsList) {
            String centerFreq = band + " Hz";
            Pair<String, Integer> pair = Pair.create(centerFreq, 0);
            equalizerList.add(pair);
        }

        if (equalizerAdapter != null) {
            equalizerAdapter.setMaxProgress(maxEQLevel - minEQLevel);
            equalizerAdapter.setEqualizerList(equalizerList);
        }

        mAudioPlayer = new AudioPlayer();
        mAudioPlayer.setOnPlayInfoListener(new AudioPlayer.OnPlayInfoListener() {
            @Override
            public void onPrepared() {
                long duration = mAudioPlayer.getDuration();
                mHandler.obtainMessage(MSG_POSITION).sendToTarget();
                mHandler.obtainMessage(MSG_DURATION, duration).sendToTarget();
            }

            @Override
            public void onComplete() {
                Log.e("EQ", "onComplete");
                mHandler.removeCallbacksAndMessages(null);
            }
        });
    }

    private void doEqualize() {
        doEqualize(0, 0);
    }

    private void doEqualize(int index, int progress) {
        if (filterThread == null) {
            String filter = "superequalizer=6b=4:8b=5:10b=5";
            filterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mAudioPlayer.play(audioPath, filter);
                }
            });
            filterThread.start();
        } else {
            if (index < 0 || index >= selectBandList.length) return;
            selectBandList[index] = progress;
            StringBuilder builder = new StringBuilder();
            builder.append("superequalizer=");
            for (int i = 0; i < selectBandList.length; i++) {
                if (selectBandList[i] > 0) {
                    builder.append(i + 1).append("b=").append(selectBandList[i]).append(":");
                }
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            Log.e("Equalizer", "update filter=" + builder.toString());
            mAudioPlayer.again(builder.toString());
        }
    }

    private String getAudioEffect(int index) {
        switch (index) {
            case 0:
                return "aecho=0.8:0.8:1000:0.5";
            case 1:
                return "atempo=2";
            case 2:
                return "tremolo=5:0.9";
            case 3:
                return "asetrate=44100*1.4,aresample=44100,atempo=1/1.4";
            case 4:
                return "asetrate=44100*0.6,aresample=44100,atempo=1/0.6";
            default:
                return "";
        }
    }

    private void doAudioEffect(int index) {
        String effect = getAudioEffect(index);
        if (effect.isEmpty()) return;
        String filter = ",superequalizer=8b=5";
        effect += filter;

        if (filterThread == null) {
            final String finalEffect = effect;
            filterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mAudioPlayer.play(audioPath, finalEffect);
                }
            });
            filterThread.start();
        } else {
            mAudioPlayer.again(effect);
        }
    }

    @Override
    public void onProgress(int index, int progress) {
        doEqualize(index, progress);
    }

    @Override
    public void onViewClick(View view) {
        // Empty implementation
    }

    @Override
    public void onSelectedFile(String filePath) {
        audioPath = filePath;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (filterThread != null) {
            if (mAudioPlayer != null) {
                mAudioPlayer.release();
            }
            filterThread.interrupt();
            filterThread = null;
        }
    }
}
