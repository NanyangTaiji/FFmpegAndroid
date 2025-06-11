package com.frank.androidmedia.controller;

import android.Manifest;
import android.media.*;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.frank.androidmedia.util.WavUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecordController {
    private static final String TAG = AudioTrackController.class.getSimpleName();

    private int minBufferSize = 0;
    private AudioRecord mAudioRecord;
    private RecordThread mRecordThread;
    private final boolean enableAudioProcessor = false;
    private AudioProcessController mAudioProcessController;

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void initAudioRecord() {
        int sampleRate = 44100;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize);

        if (enableAudioProcessor) {
            mAudioProcessController = new AudioProcessController();
            Boolean result = mAudioProcessController.initAEC(mAudioRecord.getAudioSessionId());
            Log.e(TAG, "init AEC result=" + result);
            result = mAudioProcessController.initAGC(mAudioRecord.getAudioSessionId());
            Log.e(TAG, "init AGC result=" + result);
            result = mAudioProcessController.initNS(mAudioRecord.getAudioSessionId());
            Log.e(TAG, "init NS result=" + result);
        }
    }

    private class RecordThread extends Thread {
        private boolean isRecording = false;
        private final Object lock = new Object();
        private String mPath;
        private byte[] mData;
        private int mBufferSize;
        private AudioRecord mAudioRecord;
        private FileOutputStream mOutputStream;

        RecordThread(String recordPath, AudioRecord audioRecord, int bufferSize) {
            mPath = recordPath;
            isRecording = true;
            mBufferSize = bufferSize;
            mAudioRecord = audioRecord;
        }

        @Override
        public void run() {
            super.run();

            try {
                mData = new byte[mBufferSize];
                mOutputStream = new FileOutputStream(mPath);
            } catch (Exception e) {
                Log.e(TAG, "open file error=" + e);
                isRecording = false;
            }

            while (isRecording) {
                synchronized (lock) {
                    if (isRecording) {
                        int size = mAudioRecord.read(mData, 0, mBufferSize);
                        if (size > 0) {
                            try {
                                mOutputStream.write(mData, 0, size);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (size < 0) {
                            Log.e(TAG, "read data error, size=" + size);
                        }
                    }
                }
            }

            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // convert pcm to wav
            String wavPath = new File(mPath).getParent() + "/test.wav";
            WavUtil.makePCMToWAVFile(mPath, wavPath, true);
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public void startRecord(String recordPath) {
        if (mAudioRecord == null) {
            try {
                initAudioRecord();
            } catch (Exception e) {
                Log.e(TAG, "init AudioRecord error=" + e);
                return;
            }
        }

        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG, "is recording audio...");
            return;
        }

        Log.i(TAG, "start record...");
        mAudioRecord.startRecording();
        mRecordThread = new RecordThread(recordPath, mAudioRecord, minBufferSize);
        mRecordThread.start();
    }

    public void stopRecord() {
        Log.i(TAG, "stop record...");
        if (mRecordThread != null) {
            mRecordThread.isRecording = false;
            mRecordThread.interrupt();
            mRecordThread = null;
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
    }

    public void release() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        if (mAudioProcessController != null) {
            mAudioProcessController.release();
            mAudioProcessController = null;
        }
    }
}
