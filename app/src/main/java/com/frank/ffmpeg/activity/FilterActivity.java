package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frank.ffmpeg.FFmpegApplication;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.VideoPlayer;
import com.frank.ffmpeg.adapter.HorizontalAdapter;
import com.frank.ffmpeg.listener.OnItemClickListener;
import com.frank.ffmpeg.util.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Using ffmpeg to filter
 * Created by frank on 2018/6/5.
 */
public class FilterActivity extends BaseActivity implements SurfaceHolder.Callback {

    private static final int MSG_HIDE = 222;
    private static final int DELAY_TIME = 5000;

    private String videoPath = Environment.getExternalStorageDirectory().getPath() + "/what.mp4";

    private VideoPlayer videoPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean surfaceCreated = false;

    // vflip is up and down, hflip is left and right
    private String[] txtArray = {
            FFmpegApplication.getInstance().getString(R.string.filter_sketch),
            FFmpegApplication.getInstance().getString(R.string.filter_luminance),
            FFmpegApplication.getInstance().getString(R.string.filter_saturation),
            FFmpegApplication.getInstance().getString(R.string.filter_contrast),
            FFmpegApplication.getInstance().getString(R.string.filter_sharpening),
            FFmpegApplication.getInstance().getString(R.string.filter_edge),
            FFmpegApplication.getInstance().getString(R.string.filter_vr),
            FFmpegApplication.getInstance().getString(R.string.filter_division),
            FFmpegApplication.getInstance().getString(R.string.filter_flip),
            FFmpegApplication.getInstance().getString(R.string.filter_equalize),
            FFmpegApplication.getInstance().getString(R.string.filter_blur),
            FFmpegApplication.getInstance().getString(R.string.filter_rotate)
    };

    private HorizontalAdapter horizontalAdapter;
    private RecyclerView recyclerView;
    private boolean playAudio = true;
    private ToggleButton btnSound;
    private Button btnSelect;
    private Thread filterThread;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_HIDE) { // after idle 5s, hide the controller view
                recyclerView.setVisibility(View.GONE);
                btnSound.setVisibility(View.GONE);
                btnSelect.setVisibility(View.GONE);
            }
        }
    };

    private HideRunnable hideRunnable;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_filter;
    }

    private class HideRunnable implements Runnable {
        @Override
        public void run() {
            mHandler.obtainMessage(MSG_HIDE).sendToTarget();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        initView();
        registerListener();

        hideRunnable = new HideRunnable();
        mHandler.postDelayed(hideRunnable, DELAY_TIME);
    }

    private void initView() {
        surfaceView = getView(R.id.surface_filter);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        videoPlayer = new VideoPlayer();
        btnSound = getView(R.id.btn_sound);

        recyclerView = getView(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        ArrayList<String> itemList = new ArrayList<>(Arrays.asList(txtArray));
        horizontalAdapter = new HorizontalAdapter(itemList);
        recyclerView.setAdapter(horizontalAdapter);

        btnSelect = getView(R.id.btn_select_file);
        initViewsWithClick(R.id.btn_select_file);
    }

    private void registerListener() {
        horizontalAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (!surfaceCreated) {
                    return;
                }
                if (!FileUtil.checkFileExist(videoPath)) {
                    showSelectFile();
                    return;
                }
                doFilterPlay(position);
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSelect.setVisibility(View.VISIBLE);
                btnSound.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
                if (hideRunnable != null) {
                    mHandler.postDelayed(hideRunnable, DELAY_TIME);
                }
            }
        });

        btnSound.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                setPlayAudio();
            }
        });
    }

    /**
     * switch filter
     * @param position position in the array of filters
     */
    private void doFilterPlay(int position) {
        if (filterThread == null) {
            filterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    videoPlayer.filter(videoPath, surfaceHolder.getSurface(), position);
                }
            });
            filterThread.start();
        } else {
            videoPlayer.again(position);
        }
    }

    private void setPlayAudio() {
        playAudio = !playAudio;
        videoPlayer.playAudio(playAudio);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        if (FileUtil.checkFileExist(videoPath)) {
            doFilterPlay(4);
            btnSound.setChecked(true);
        } else {
            showSelectFile();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Empty implementation
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
        if (filterThread != null) {
            if (videoPlayer != null) {
                videoPlayer.release();
            }
            filterThread.interrupt();
            filterThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer = null;
        horizontalAdapter = null;
    }

    @Override
    public void onViewClick(View view) {
        if (view.getId() == R.id.btn_select_file) {
            selectFile();
        }
    }

    @Override
    public void onSelectedFile(String filePath) {
        videoPath = filePath;
        doFilterPlay(8);
        // sound off by default
        btnSound.setChecked(true);
    }
}
