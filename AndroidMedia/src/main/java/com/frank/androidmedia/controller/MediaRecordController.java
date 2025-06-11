package com.frank.androidmedia.controller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

public class MediaRecordController {
    private final boolean usingProfile = true;
    private Camera mCamera;
    private String mOutputPath;
    private MediaRecorder mMediaRecorder;
    private DisplayMetrics mDisplayMetrics;
    private MediaProjectionController mMediaProjectionController;

    private void initMediaRecord(int videoSource, Surface surface, String outputPath) {
        if (videoSource == MediaRecorder.VideoSource.CAMERA
                || videoSource == MediaRecorder.VideoSource.DEFAULT) {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
        }

        mMediaRecorder.setVideoSource(videoSource);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        if (usingProfile && (videoSource == MediaRecorder.VideoSource.CAMERA
                || videoSource == MediaRecorder.VideoSource.DEFAULT)) {
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            mMediaRecorder.setProfile(profile);
        } else {
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
            mMediaRecorder.setVideoEncodingBitRate(5000 * 1000);
            mMediaRecorder.setVideoFrameRate(25);
            mMediaRecorder.setAudioChannels(2);
            mMediaRecorder.setAudioSamplingRate(48000);
        }

        mMediaRecorder.setOutputFile(outputPath);
        if (surface != null && (videoSource == MediaRecorder.VideoSource.CAMERA
                || videoSource == MediaRecorder.VideoSource.DEFAULT)) {
            mMediaRecorder.setPreviewDisplay(surface);
        }

        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            Log.e("MediaRecorder", "prepare recorder error=" + e);
        }
    }

    private void startRecordInternal(int videoSource, Surface surface, String outputPath) {
        initMediaRecord(videoSource, surface, outputPath);
        try {
            if (videoSource == MediaRecorder.VideoSource.SURFACE) {
                mMediaProjectionController.createVirtualDisplay(mMediaRecorder.getSurface());
            }
            mMediaRecorder.start();
        } catch (Exception e) {
            Log.e("MediaRecorder", "start recorder error=" + e);
        }
    }

    public void startRecord(int videoSource, Surface surface, Context context, String outputPath) {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            mDisplayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
        }

        if (videoSource == MediaRecorder.VideoSource.SURFACE) {
            mOutputPath = outputPath;
            mMediaProjectionController = new MediaProjectionController(MediaProjectionController.TYPE_SCREEN_RECORD);
            mMediaProjectionController.startScreenRecord(context);
            return;
        }

        startRecordInternal(videoSource, surface, outputPath);
        Log.i("MediaRecorder", "startRecord...");
    }

    public void stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        if (mMediaProjectionController != null) {
            mMediaProjectionController.stopScreenRecord();
        }
        Log.i("MediaRecorder", "stopRecord...");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mMediaProjectionController != null && requestCode == mMediaProjectionController.getRequestCode()) {
            mMediaProjectionController.onActivityResult(resultCode, data);
            startRecordInternal(MediaRecorder.VideoSource.SURFACE, null, mOutputPath);
        }
    }

    public void release() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera.lock();
            mCamera = null;
        }
    }
}
