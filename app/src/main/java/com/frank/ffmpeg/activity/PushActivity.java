package com.frank.ffmpeg.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.frank.ffmpeg.FFmpegPusher;
import com.frank.ffmpeg.R;

/**
 * Using FFmpeg to push rtmp stream,
 * with SRS media server convert to http-flv stream
 * Created by frank on 2018/2/2.
 */
public class PushActivity extends BaseActivity {

    // storage/emulated/0/beyond.mp4
    private static final String INPUT_PATH = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private static final String LIVE_URL = "rtmp://192.168.17.168/live/stream";

    private EditText editInputPath;
    private EditText editLiveURL;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_push;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();
        initView();
    }

    private void initView() {
        editInputPath = getView(R.id.edit_file_path);
        editLiveURL = getView(R.id.edit_live_url);
        editInputPath.setText(INPUT_PATH);
        editLiveURL.setText(LIVE_URL);

        initViewsWithClick(R.id.btn_push_stream);
    }

    private void startPushStreaming() {
        String filePath = editInputPath.getText().toString();
        String liveUrl = editLiveURL.getText().toString();

        if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(liveUrl)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    new FFmpegPusher().pushStream(filePath, liveUrl);
                }
            }).start();
        }
    }

    @Override
    public void onViewClick(View view) {
        if (view.getId() == R.id.btn_push_stream) {
            startPushStreaming();
        }
    }

    @Override
    public void onSelectedFile(String filePath) {
        // Empty implementation
    }
}
