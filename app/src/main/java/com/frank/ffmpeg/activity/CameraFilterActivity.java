package com.frank.ffmpeg.activity;

import android.os.Bundle;
import android.view.View;
import com.frank.camerafilter.factory.BeautyFilterType;
import com.frank.camerafilter.widget.BeautyCameraView;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.util.FilterTypeUtil;

public class CameraFilterActivity extends BaseActivity {

    private BeautyCameraView cameraView;
    private int index = 0;

    private final BeautyFilterType[] filterType = {
            BeautyFilterType.NONE,
            BeautyFilterType.SATURATION,
            BeautyFilterType.CONTRAST,
            BeautyFilterType.BRIGHTNESS,
            BeautyFilterType.SHARPEN,
            BeautyFilterType.BLUR,
            BeautyFilterType.HUE,
            BeautyFilterType.WHITE_BALANCE,
            BeautyFilterType.SKETCH,
            BeautyFilterType.OVERLAY,
            BeautyFilterType.BREATH_CIRCLE
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera_filter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        cameraView = getView(R.id.surface_camera_filter);
        initViewsWithClick(R.id.btn_video_recorder);
        initViewsWithClick(R.id.btn_camera_filter);
    }

    @Override
    public void onViewClick(View view) {
        if (view.getId() == R.id.btn_video_recorder) {
            boolean isRecording = cameraView.isRecording();
            cameraView.setRecording(!isRecording);
            if (!isRecording) {
                showToast("start recording...");
            } else {
                showToast("stop recording...");
            }
        } else if (view.getId() == R.id.btn_camera_filter) {
            index++;
            if (index >= filterType.length) {
                index = 0;
            }
            cameraView.setFilter(filterType[index]);
            showToast(getString(FilterTypeUtil.filterTypeToNameId(filterType[index])));
        }
    }

    @Override
    public void onSelectedFile(String filePath) {
        // Empty implementation
    }
}
