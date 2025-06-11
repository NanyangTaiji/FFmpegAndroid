package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.handler.FFmpegHandler;
import com.frank.ffmpeg.listener.OnLrcListener;
import com.frank.ffmpeg.model.AudioBean;
import com.frank.ffmpeg.model.LrcInfo;
import com.frank.ffmpeg.model.MediaBean;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.TimeUtil;
import com.frank.ffmpeg.model.LrcLine;
import com.frank.ffmpeg.tool.LrcLineTool;
import com.frank.ffmpeg.tool.LrcParser;
import com.frank.ffmpeg.util.ThreadPoolUtil;
import com.frank.ffmpeg.view.LrcView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_FINISH;

public class AudioPlayActivity extends AppCompatActivity {

    private static final String TAG = AudioPlayActivity.class.getSimpleName();

    private static final int MSG_TIME = 123;
    private static final int MSG_DURATION = 234;

    private String path;

    private TextView txtTitle;
    private TextView txtArtist;
    private TextView txtTime;
    private TextView txtDuration;
    private SeekBar audioBar;
    private LrcView lrcView;

    private MediaPlayer audioPlayer;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TIME:
                    audioBar.setProgress(audioPlayer.getCurrentPosition());
                    txtTime.setText(TimeUtil.getVideoTime((long) audioPlayer.getCurrentPosition()));
                    sendEmptyMessageDelayed(MSG_TIME, 1000);
                    lrcView.seekToTime((long) audioPlayer.getCurrentPosition());
                    break;
                case MSG_DURATION:
                    int duration = (Integer) msg.obj;
                    txtDuration.setText(TimeUtil.getVideoTime((long) duration));
                    audioBar.setMax(duration);
                    break;
                case MSG_FINISH:
                    if (msg.obj == null) return;
                    MediaBean result = (MediaBean) msg.obj;
                    AudioBean audioBean = result.getAudioBean();
                    txtTitle.setText(audioBean.getTitle());
                    txtArtist.setText(audioBean.getArtist());
                    List<String> lyrics = audioBean.getLyrics();
                    if (lyrics != null) {
                        ArrayList<LrcLine> lrcList = new ArrayList<>();
                        for (int i = 0; i < lyrics.size(); i++) {
                            Log.e(TAG, "lyrics=_=" + lyrics.get(i));
                            List<LrcLine> line = LrcLineTool.getLrcLine(lyrics.get(i));
                            if (line != null) lrcList.addAll(line);
                        }
                        LrcLineTool.sortLyrics(lrcList);
                        lrcView.setLrc(lrcList);
                    } else if (audioBean.getLrcLineList() != null) {
                        lrcView.setLrc(audioBean.getLrcLineList());
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_play);

        initView();
        initAudioPlayer();
        initLrc();
    }

    private void initView() {
        txtTitle = findViewById(R.id.txt_title);
        txtArtist = findViewById(R.id.txt_artist);
        txtTime = findViewById(R.id.txt_time);
        txtDuration = findViewById(R.id.txt_duration);
        lrcView = findViewById(R.id.list_lyrics);
        ImageView btnPlay = findViewById(R.id.img_play);
        btnPlay.setOnClickListener(v -> {
            if (isPlaying()) {
                audioPlayer.pause();
                btnPlay.setImageResource(R.drawable.ic_play);
            } else {
                audioPlayer.start();
                btnPlay.setImageResource(R.drawable.ic_pause);
            }
        });
        audioBar = findViewById(R.id.audio_bar);
        audioBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                audioBar.setProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = audioBar.getProgress();
                audioPlayer.seekTo(progress);
                lrcView.seekToTime((long) progress);
            }
        });
        lrcView.setListener(new OnLrcListener() {
            @Override
            public void onLrcSeek(int position, LrcLine lrcLine) {
                audioPlayer.seekTo((int) lrcLine.getStartTime());
                Log.e(TAG, "lrc position=" + position + "--time=" + lrcLine.getStartTime());
            }
        });
    }

    private void initAudioPlayer() {
        if (getIntent().getData() != null) {
            path = getIntent().getData().getPath();
        }
        Log.e(TAG, "path=" + path);
        if (TextUtils.isEmpty(path)) return;
        audioPlayer = new MediaPlayer();
        try {
            audioPlayer.setDataSource(path);
            audioPlayer.prepareAsync();
            audioPlayer.setOnPreparedListener(mp -> {
                Log.e(TAG, "onPrepared...");
                audioPlayer.start();
                int duration = audioPlayer.getDuration();
                mHandler.obtainMessage(MSG_TIME).sendToTarget();
                mHandler.obtainMessage(MSG_DURATION, duration).sendToTarget();
            });
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer error: " + e.getMessage());
        }
    }

    private void initLrc() {
        if (TextUtils.isEmpty(path)) return;
        String lrcPath = null;
        if (path.contains(".")) {
            lrcPath = path.substring(0, path.lastIndexOf(".")) + ".lrc";
            Log.e(TAG, "lrcPath=" + lrcPath);
        }
        if (!TextUtils.isEmpty(lrcPath) && new File(lrcPath).exists()) {
            // should parsing in work thread
            final String finalLrcPath = lrcPath;
            ThreadPoolUtil.executeSingleThreadPool(new Runnable() {
                @Override
                public void run() {
                    LrcParser lrcParser = new LrcParser();
                    LrcInfo lrcInfo = lrcParser.readLrc(finalLrcPath);
                    Log.e(TAG, "title=" + (lrcInfo != null ? lrcInfo.getTitle() : null) +
                            ",album=" + (lrcInfo != null ? lrcInfo.getAlbum() : null) +
                            ",artist=" + (lrcInfo != null ? lrcInfo.getArtist() : null));
                    MediaBean mediaBean = new MediaBean();
                    AudioBean audioBean = new AudioBean();
                    if (lrcInfo != null) {
                        audioBean.setTitle(lrcInfo.getTitle());
                        audioBean.setAlbum(lrcInfo.getAlbum());
                        audioBean.setArtist(lrcInfo.getArtist());
                        audioBean.setLrcLineList(lrcInfo.getLrcLineList());
                    }
                    mediaBean.setAudioBean(audioBean);
                    mHandler.obtainMessage(MSG_FINISH, mediaBean).sendToTarget();
                }
            });
        } else {
            FFmpegHandler ffmpegHandler = new FFmpegHandler(mHandler);
            String[] commandLine = FFmpegUtil.probeFormat(path);
            ffmpegHandler.executeFFprobeCmd(commandLine);
        }
    }

    private boolean isPlaying() {
        return audioPlayer != null && audioPlayer.isPlaying();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mHandler.removeCallbacksAndMessages(null);
            audioPlayer.stop();
            audioPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "release player err=" + e);
        }
    }
}