package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.frank.ffmpeg.R;
import com.frank.ffmpeg.adapter.WaterfallAdapter;
import com.frank.ffmpeg.handler.FFmpegHandler;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;
import com.frank.ffmpeg.listener.OnItemClickListener;

import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_INFO;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_BEGIN;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_FINISH;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_PROGRESS;

/**
 * Using ffmpeg command to handle audio
 * Created by frank on 2018/1/23.
 */

public class AudioHandleActivity extends BaseActivity {

    private static String PATH = Environment.getExternalStorageDirectory().getPath();
    private static final boolean useFFmpeg = true;
    private static final boolean mixAudio = true;
    private static String outputPath = null;

    private String appendFile = PATH + File.separator + "heart.m4a";

    private RecyclerView layoutAudioHandle;
    private LinearLayout layoutProgress;
    private TextView txtProgress;
    private int currentPosition = 0;
    private FFmpegHandler ffmpegHandler;

    private String outputPath1 = PATH + File.separator + "output1.mp3";
    private String outputPath2 = PATH + File.separator + "output2.mp3";
    private boolean isJointing = false;
    private StringBuilder infoBuilder;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BEGIN:
                    layoutProgress.setVisibility(View.VISIBLE);
                    layoutAudioHandle.setVisibility(View.GONE);
                    break;
                case MSG_FINISH:
                    layoutProgress.setVisibility(View.GONE);
                    layoutAudioHandle.setVisibility(View.VISIBLE);
                    if (isJointing) {
                        isJointing = false;
                        FileUtil.deleteFile(outputPath1);
                        FileUtil.deleteFile(outputPath2);
                    }
                    if (infoBuilder != null) {
                        Toast.makeText(AudioHandleActivity.this,
                                infoBuilder.toString(), Toast.LENGTH_LONG).show();
                        infoBuilder = null;
                    }
                    if (outputPath != null && !outputPath.isEmpty() && !AudioHandleActivity.this.isDestroyed()) {
                        showToast("Save to:" + outputPath);
                        outputPath = "";
                    }
                    // reset progress
                    txtProgress.setText(String.format(Locale.getDefault(), "%d%%", 0));
                    break;
                case MSG_PROGRESS:
                    int progress = msg.arg1;
                    if (progress > 0) {
                        txtProgress.setVisibility(View.VISIBLE);
                        txtProgress.setText(String.format(Locale.getDefault(), "%d%%", progress));
                    } else {
                        txtProgress.setVisibility(View.GONE);
                    }
                    break;
                case MSG_INFO:
                    if (infoBuilder == null) infoBuilder = new StringBuilder();
                    infoBuilder.append(msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_audio_handle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        ffmpegHandler = new FFmpegHandler(mHandler);
        // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        //     PATH = getCacheDir().getAbsolutePath();
        // }
    }

    private void initView() {
        layoutProgress = getView(R.id.layout_progress);
        txtProgress = getView(R.id.txt_progress);

        List<String> list = Arrays.asList(
                getString(R.string.audio_transform),
                getString(R.string.audio_cut),
                getString(R.string.audio_concat),
                getString(R.string.audio_mix),
                getString(R.string.audio_play),
                getString(R.string.audio_speed),
                getString(R.string.audio_echo),
                getString(R.string.audio_tremolo),
                getString(R.string.audio_denoise),
                getString(R.string.audio_add_equalizer),
                getString(R.string.audio_silence),
                getString(R.string.audio_volume),
                getString(R.string.audio_waveform),
                getString(R.string.audio_encode),
                getString(R.string.audio_surround),
                getString(R.string.audio_reverb),
                getString(R.string.audio_fade)
        );

        layoutAudioHandle = findViewById(R.id.list_audio_item);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutAudioHandle.setLayoutManager(layoutManager);

        WaterfallAdapter adapter = new WaterfallAdapter(list);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                currentPosition = position;
                selectFile();
            }
        });
        layoutAudioHandle.setAdapter(adapter);
    }

    @Override
    public void onViewClick(View view) {

    }

    @Override
    public void onSelectedFile(String filePath) {
        doHandleAudio(filePath);
    }

    /**
     * Using ffmpeg cmd to handle audio
     *
     * @param srcFile srcFile
     */
    private void doHandleAudio(String srcFile) {
        String[] commandLine = null;
        if (!FileUtil.checkFileExist(srcFile)) {
            return;
        }
        if (!FileUtil.isAudio(srcFile)) {
            showToast(getString(R.string.wrong_audio_format));
            return;
        }
        switch (currentPosition) {
            case 0:
                if (useFFmpeg) { //use FFmpeg to transform
                    outputPath = PATH + File.separator + "transformAudio.mp3";
                    commandLine = FFmpegUtil.transformAudio(srcFile, outputPath);
                } else { //use MediaCodec and libmp3lame to transform
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            outputPath = PATH + File.separator + "transformAudio.mp3";
                            try {
                                mHandler.obtainMessage(MSG_BEGIN).sendToTarget();
                                Class<?> clazz = Class.forName("com.frank.mp3.Mp3Converter");
                                Object instance = clazz.newInstance();
                                Method method = clazz.getDeclaredMethod("convertToMp3", String.class, String.class);
                                method.invoke(instance, srcFile, outputPath);
                                mHandler.obtainMessage(MSG_FINISH).sendToTarget();
                            } catch (Exception e) {
                                Log.e("AudioHandleActivity", "convert mp3 error=" + e.getMessage());
                            }
                        }
                    }).start();
                }
                break;
            case 1: { //cut audio, it's best not include special characters
                String suffix = FileUtil.getFileSuffix(srcFile);
                if (suffix == null || suffix.isEmpty()) {
                    return;
                }
                outputPath = PATH + File.separator + "cutAudio" + suffix;
                commandLine = FFmpegUtil.cutAudio(srcFile, 10.5f, 15.0f, outputPath);
                break;
            }
            case 2: { //concat audio
                if (!FileUtil.checkFileExist(appendFile)) {
                    return;
                }
                concatAudio(srcFile);
                return;
            }
            case 3: { //mix audio
                if (!FileUtil.checkFileExist(appendFile)) {
                    return;
                }
                String mixSuffix = FileUtil.getFileSuffix(srcFile);
                if (mixSuffix == null || mixSuffix.isEmpty()) {
                    return;
                }
                if (mixAudio) {
                    outputPath = PATH + File.separator + "mix" + mixSuffix;
                    commandLine = FFmpegUtil.mixAudio(srcFile, appendFile, outputPath);
                } else {
                    outputPath = PATH + File.separator + "merge" + mixSuffix;
                    commandLine = FFmpegUtil.mergeAudio(srcFile, appendFile, outputPath);
                }
                break;
            }
            case 4: { //use AudioTrack to play audio
                Intent audioIntent = new Intent(AudioHandleActivity.this, AudioPlayActivity.class);
                audioIntent.setData(Uri.parse(srcFile));
                startActivity(audioIntent);
                return;
            }
            case 5: { //change audio speed
                float speed = 2.0f; // funny effect, range from 0.5 to 100.0
                outputPath = PATH + File.separator + "speed.mp3";
                commandLine = FFmpegUtil.changeAudioSpeed(srcFile, outputPath, speed);
                break;
            }
            case 6: { //echo effect
                int echo = 1000; // echo effect, range from 0 to 90000
                outputPath = PATH + File.separator + "echo.mp3";
                commandLine = FFmpegUtil.audioEcho(srcFile, echo, outputPath);
                break;
            }
            case 7: { //tremolo effect
                int frequency = 5; // range from 0.1 to 20000.0
                float depth = 0.9f; // range from 0 to 1
                outputPath = PATH + File.separator + "tremolo.mp3";
                commandLine = FFmpegUtil.audioTremolo(srcFile, frequency, depth, outputPath);
                break;
            }
            case 8: { //audio denoise
                outputPath = PATH + File.separator + "denoise.mp3";
                commandLine = FFmpegUtil.audioDenoise(srcFile, outputPath);
                break;
            }
            case 9: { // equalizer plus
                // key:band  value:gain=[0-20]
                ArrayList<String> bandList = new ArrayList<>();
                bandList.add("6b=5");
                bandList.add("8b=4");
                bandList.add("10b=3");
                bandList.add("12b=2");
                bandList.add("14b=1");
                bandList.add("16b=0");
                outputPath = PATH + File.separator + "equalize.mp3";
                commandLine = FFmpegUtil.audioEqualizer(srcFile, bandList, outputPath);
                break;
            }
            case 10: { //silence detect
                commandLine = FFmpegUtil.audioSilenceDetect(srcFile);
                break;
            }
            case 11: { // modify volume
                float volume = 0.5f; // 0.0-1.0
                outputPath = PATH + File.separator + "volume.mp3";
                commandLine = FFmpegUtil.audioVolume(srcFile, volume, outputPath);
                break;
            }
            case 12: { // audio waveform
                outputPath = PATH + File.separator + "waveform.png";
                String resolution = "1280x720";
                commandLine = FFmpegUtil.showAudioWaveform(srcFile, resolution, 1, outputPath);
                break;
            }
            case 13: { //audio encode
                String pcmFile = PATH + File.separator + "raw.pcm";
                outputPath = PATH + File.separator + "convert.mp3";
                //sample rate, normal is 8000/16000/44100
                int sampleRate = 44100;
                //channel num of pcm
                int channel = 2;
                commandLine = FFmpegUtil.encodeAudio(pcmFile, outputPath, sampleRate, channel);
                break;
            }
            case 14: { // change to surround sound
                outputPath = PATH + File.separator + "surround.mp3";
                commandLine = FFmpegUtil.audioSurround(srcFile, outputPath);
                break;
            }
            case 15: {
                outputPath = PATH + File.separator + "reverb.mp3";
                commandLine = FFmpegUtil.audioReverb(srcFile, outputPath);
                break;
            }
            case 16: {
                outputPath = PATH + File.separator + "fade.mp3";
                commandLine = FFmpegUtil.audioFadeTransition(srcFile, outputPath);
                break;
            }
            default:
                break;
        }
        if (ffmpegHandler != null && commandLine != null) {
            ffmpegHandler.executeFFmpegCmd(commandLine);
        }
    }

    private void concatAudio(String selectedPath) {
        if (ffmpegHandler == null || selectedPath.isEmpty() || appendFile.isEmpty()) {
            return;
        }
        isJointing = true;
        String targetPath = PATH + File.separator + "concatAudio.mp3";
        String[] transformCmd1 = FFmpegUtil.transformAudio(selectedPath, "libmp3lame", outputPath1);
        String[] transformCmd2 = FFmpegUtil.transformAudio(appendFile, "libmp3lame", outputPath2);
        ArrayList<String> fileList = new ArrayList<>();
        fileList.add(outputPath1);
        fileList.add(outputPath2);
        String[] jointVideoCmd = FFmpegUtil.concatAudio(fileList, targetPath);
        ArrayList<String[]> commandList = new ArrayList<>();
        commandList.add(transformCmd1);
        commandList.add(transformCmd2);
        commandList.add(jointVideoCmd);
        ffmpegHandler.executeFFmpegCmds(commandList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
