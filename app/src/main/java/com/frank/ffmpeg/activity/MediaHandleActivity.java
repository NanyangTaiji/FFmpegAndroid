package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frank.ffmpeg.FFmpegCmd;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.handler.FFmpegHandler;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;
import com.frank.ffmpeg.util.ThreadPoolUtil;

import java.io.File;
import java.util.Locale;

import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_BEGIN;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_FINISH;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_PROGRESS;

/**
 * using ffmpeg to handle media
 * Created by frank on 2018/1/23.
 */
public class MediaHandleActivity extends BaseActivity {

    private static final String TAG = MediaHandleActivity.class.getSimpleName();
    private static String PATH = Environment.getExternalStorageDirectory().getPath();
    private static String outputPath = null;

    private final String audioFile = PATH + File.separator + "extractAudio.aac";

    private LinearLayout layoutProgress;
    private TextView txtProgress;
    private int viewId;
    private LinearLayout layoutMediaHandle;
    private FFmpegHandler ffmpegHandler;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BEGIN:
                    layoutProgress.setVisibility(View.VISIBLE);
                    layoutMediaHandle.setVisibility(View.GONE);
                    break;
                case MSG_FINISH:
                    layoutProgress.setVisibility(View.GONE);
                    layoutMediaHandle.setVisibility(View.VISIBLE);
                    if (outputPath != null && !outputPath.isEmpty() && !MediaHandleActivity.this.isDestroyed()) {
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
                        txtProgress.setVisibility(View.INVISIBLE);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_media_handle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        initView();
        ffmpegHandler = new FFmpegHandler(mHandler);
        // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        //     PATH = getCacheDir().getAbsolutePath();
        // }
    }

    private void initView() {
        layoutProgress = getView(R.id.layout_progress);
        txtProgress = getView(R.id.txt_progress);
        layoutMediaHandle = getView(R.id.layout_media_handle);
        initViewsWithClick(
                R.id.btn_mux,
                R.id.btn_extract_audio,
                R.id.btn_extract_video,
                R.id.btn_dubbing
        );
    }

    @Override
    protected void onViewClick(View view) {
        viewId = view.getId();
        selectFile();
    }

    @Override
    protected void onSelectedFile(String filePath) {
        doHandleMedia(filePath);
    }

    /**
     * Using ffmpeg cmd to handle media
     *
     * @param srcFile srcFile
     */
    private void doHandleMedia(String srcFile) {
        String[] commandLine = null;
        if (!FileUtil.checkFileExist(srcFile)) {
            return;
        }
        if (!FileUtil.isVideo(srcFile)) {
            showToast(getString(R.string.wrong_video_format));
            return;
        }

        if (viewId == R.id.btn_mux) { // mux: pure video and pure audio
            ThreadPoolUtil.executeSingleThreadPool(new Runnable() {
                @Override
                public void run() {
                    mediaMux(srcFile);
                }
            });
            return;
        } else if (viewId == R.id.btn_extract_audio) { // extract audio
            outputPath = PATH + File.separator + "extractAudio.aac";
            commandLine = FFmpegUtil.extractAudio(srcFile, outputPath);
        } else if (viewId == R.id.btn_extract_video) { // extract video
            outputPath = PATH + File.separator + "extractVideo.mp4";
            commandLine = FFmpegUtil.extractVideo(srcFile, outputPath);
        } else if (viewId == R.id.btn_dubbing) { // dubbing
            ThreadPoolUtil.executeSingleThreadPool(new Runnable() {
                @Override
                public void run() {
                    mediaDubbing(srcFile);
                }
            });
            return;
        }

        if (ffmpegHandler != null) {
            ffmpegHandler.executeFFmpegCmd(commandLine);
        }
    }

    private void muxVideoAndAudio(String videoPath, String outputPath) {
        String[] commandLine = FFmpegUtil.mediaMux(videoPath, audioFile, true, outputPath);
        int result = FFmpegCmd.executeSync(commandLine);
        if (result != 0) {
            commandLine = FFmpegUtil.mediaMux(videoPath, audioFile, false, outputPath);
            result = FFmpegCmd.executeSync(commandLine);
            Log.e(TAG, "mux audio and video result=" + result);
        }
    }

    private void mediaMux(String srcFile) {
        mHandler.sendEmptyMessage(MSG_BEGIN);
        String suffix = FileUtil.getFileSuffix(srcFile);
        String muxPath = PATH + File.separator + "mux" + suffix;
        Log.e(TAG, "muxPath=" + muxPath);
        muxVideoAndAudio(srcFile, muxPath);
        mHandler.sendEmptyMessage(MSG_FINISH);
    }

    private void mediaDubbing(String srcFile) {
        mHandler.sendEmptyMessage(MSG_BEGIN);
        String dubbingSuffix = FileUtil.getFileSuffix(srcFile);
        String dubbingPath = PATH + File.separator + "dubbing" + dubbingSuffix;
        String temp = PATH + File.separator + "temp" + dubbingSuffix;
        String[] commandLine1 = FFmpegUtil.extractVideo(srcFile, temp);
        int dubbingResult = FFmpegCmd.executeSync(commandLine1);
        if (dubbingResult != 0) {
            Log.e(TAG, "extract video fail, result=" + dubbingResult);
            return;
        }
        muxVideoAndAudio(temp, dubbingPath);
        FileUtil.deleteFile(temp);
        mHandler.sendEmptyMessage(MSG_FINISH);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
