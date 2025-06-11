package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.frank.ffmpeg.R;
import com.frank.ffmpeg.handler.ConnectionReceiver;
import com.frank.ffmpeg.handler.OrientationHandler;
import com.frank.ffmpeg.listener.OnNetworkChangeListener;
import com.frank.live.camera.Camera2Helper;
import com.frank.live.listener.LiveStateChangeListener;
import com.frank.live.param.AudioParam;
import com.frank.live.param.VideoParam;
import com.frank.live.LivePusherNew;
import com.frank.live.camera.CameraType;

/**
 * Realtime living with rtmp stream
 * Created by frank on 2018/1/28.
 */
public class LiveActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        LiveStateChangeListener, OnNetworkChangeListener {

    private static final String TAG = LiveActivity.class.getSimpleName();
    private static final String LIVE_URL = "rtmp://192.168.17.168/live/stream";
    private static final int MSG_ERROR = 100;

    private View liveView;
    private LivePusherNew livePusher;
    private boolean isPushing = false;
    private ConnectionReceiver connectionReceiver;
    private OrientationHandler orientationHandler;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_ERROR) {
                String errMsg = (String) msg.obj;
                if (!TextUtils.isEmpty(errMsg)) {
                    Toast.makeText(LiveActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_live;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        initView();
        initPusher();
        registerBroadcast(this);
        orientationHandler = new OrientationHandler(this);
        orientationHandler.enable();
        orientationHandler.setOnOrientationListener(new OrientationHandler.OnOrientationListener() {
            @Override
            public void onOrientation(int orientation) {
                int previewDegree = (orientation + 90) % 360;
                if (livePusher != null) {
                    livePusher.setPreviewDegree(previewDegree);
                }
            }
        });
    }

    private void initView() {
        initViewsWithClick(R.id.btn_swap);
        ((ToggleButton) findViewById(R.id.btn_live)).setOnCheckedChangeListener(this);
        ((ToggleButton) findViewById(R.id.btn_mute)).setOnCheckedChangeListener(this);
        liveView = getView(R.id.surface_camera);
    }

    private void initPusher() {
        int width = 640;
        int height = 480;
        int videoBitRate = 800000; // kb/s
        int videoFrameRate = 10; // fps
        VideoParam videoParam = new VideoParam(width, height,
                Integer.valueOf(Camera2Helper.CAMERA_ID_BACK), videoBitRate, videoFrameRate);

        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int numChannels = 2;
        AudioParam audioParam = new AudioParam(sampleRate, channelConfig, audioFormat, numChannels);

        // Camera1: SurfaceView  Camera2: TextureView
        livePusher = new LivePusherNew(this, videoParam, audioParam, liveView, CameraType.CAMERA2);
        if (liveView instanceof SurfaceView) {
            SurfaceHolder holder = ((SurfaceView) liveView).getHolder();
            livePusher.setPreviewDisplay(holder);
        }
    }

    private void registerBroadcast(OnNetworkChangeListener networkChangeListener) {
        connectionReceiver = new ConnectionReceiver(networkChangeListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectionReceiver, intentFilter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();

        if (id == R.id.btn_live) { // start or stop living
            if (isChecked) {
                livePusher.startPush(LIVE_URL, this);
                isPushing = true;
            } else {
                livePusher.stopPush();
                isPushing = false;
            }
        } else if (id == R.id.btn_mute) { // mute or not
            Log.i(TAG, "isChecked=" + isChecked);
            livePusher.setMute(isChecked);
        }

    }

    @Override
    public void onError(String msg) {
        Log.e(TAG, "errMsg=" + msg);
        mHandler.obtainMessage(MSG_ERROR, msg).sendToTarget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orientationHandler != null) {
            orientationHandler.disable();
        }
        if (livePusher != null) {
            if (isPushing) {
                isPushing = false;
                livePusher.stopPush();
            }
            livePusher.release();
        }
        if (connectionReceiver != null) {
            unregisterReceiver(connectionReceiver);
        }
    }

    @Override
    public void onViewClick(View view) {
        if (view.getId() == R.id.btn_swap) { // switch camera
            livePusher.switchCamera();
        }
    }

    @Override
    public void onSelectedFile(String filePath) {
        // Empty implementation
    }

    @Override
    public void onNetworkChange() {
        Toast.makeText(this, "network is not available", Toast.LENGTH_SHORT).show();
        if (livePusher != null && isPushing) {
            livePusher.stopPush();
            isPushing = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged, orientation=" + newConfig.orientation);
    }
}
