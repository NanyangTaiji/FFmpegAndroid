package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import com.frank.androidmedia.controller.MediaPlayController;
import com.frank.androidmedia.listener.PlayerCallback;

import com.frank.ffmpeg.R;
import com.frank.ffmpeg.view.VideoPreviewBar;

import com.frank.ffmpeg.handler.FFmpegHandler;

/**
 * Preview the thumbnail of video when seeking
 * Created by frank on 2019/11/16.
 */
public class VideoPreviewActivity extends BaseActivity implements VideoPreviewBar.PreviewBarCallback, PlayerCallback {

    private MediaPlayController playController;
    private SurfaceView surfaceVideo;
    private VideoPreviewBar videoPreviewBar;

    private static final String TAG = VideoPreviewActivity.class.getSimpleName();
    private static final int TIME_UPDATE = 1000;
    private static final int MSG_UPDATE = 1234;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_UPDATE) {
                if (videoPreviewBar != null && playController != null) {
                    videoPreviewBar.updateProgress(playController.currentPosition());
                }
                this.sendEmptyMessageDelayed(MSG_UPDATE, TIME_UPDATE);
            } else if (msg.what == FFmpegHandler.MSG_TOAST) {
                showToast(getString(R.string.please_click_select));
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_preview;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        mHandler.sendEmptyMessageDelayed(FFmpegHandler.MSG_TOAST, 500);
        playController = new MediaPlayController(this);
    }

    private void initView() {
        surfaceVideo = getView(R.id.surface_view);
        videoPreviewBar = getView(R.id.preview_video);
    }

    private void setPlayCallback(String filePath, SurfaceView surfaceView) {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                doPlay(filePath, holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void doPlay(String filePath, Surface surface) {
        if (surface == null || TextUtils.isEmpty(filePath)) {
            return;
        }
        if (playController != null) {
            playController.initPlayer(filePath, surface);
        }
    }

    @Override
    public void onViewClick(View view) {

    }

    @Override
    public void onSelectedFile(String filePath) {
        setPlayCallback(filePath, surfaceVideo);
        videoPreviewBar.init(filePath, this);
    }

    @Override
    public void onStopTracking(long progress) {
        if (playController != null) {
            playController.seekTo((int) progress);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playController != null) {
            playController.releasePlayer();
        }
        if (videoPreviewBar != null) {
            videoPreviewBar.release();
        }
    }

    // player callback

    @Override
    public void onPrepare() {
        Log.i(TAG, "onPrepare...");
        mHandler.sendEmptyMessage(MSG_UPDATE);
    }

    @Override
    public void onRenderFirstFrame() {
        Log.i(TAG, "onRenderFirstFrame...");
    }

    @Override
    public boolean onError(int what, int extra) {
        Log.e(TAG, "onError...");
        return true;
    }

    @Override
    public void onCompleteListener() {
        Log.i(TAG, "onCompleteListener...");
    }
}
