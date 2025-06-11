package com.frank.androidmedia.controller;

import android.media.*;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioTrackController {
    private static final String TAG = AudioTrackController.class.getSimpleName();
    private static final long DEQUEUE_TIME = 10 * 1000;
    private static final long SLEEP_TIME = 20;

    private AtomicBoolean running;
    private AudioTrack audioTrack;
    private MediaCodec mediaCodec;
    private MediaExtractor mediaExtractor;

    private MediaFormat parseAudioFormat(String path) {
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(path);
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType != null && mimeType.startsWith("audio")) {
                    mediaExtractor.selectTrack(i);
                    return mediaFormat;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseAudioFormat err=" + e);
        }
        return null;
    }

    private boolean initMediaCodec(MediaFormat mediaFormat) {
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        try {
            mediaCodec = MediaCodec.createDecoderByType(mimeType);
            mediaCodec.configure(mediaFormat, null, null, 0);
            mediaCodec.start();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "initMediaCodec err=" + e);
            return false;
        }
    }

    private boolean initAudioTrack(MediaFormat mediaFormat) {
        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int channelConfig = channelCount == 1 ?
                AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        Log.e(TAG, "sampleRate=" + sampleRate + ", channelCount=" + channelCount + ", bufferSize=" + bufferSize);

        try {
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build();
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build();
            audioTrack = new AudioTrack(audioAttributes, audioFormat,
                    bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            audioTrack.play();
        } catch (Exception e) {
            Log.e(TAG, "initAudioTrack err=" + e);
            return false;
        }
        return true;
    }

    private void release() {
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }
        if (mediaCodec != null) {
            mediaCodec.release();
            mediaCodec = null;
        }
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
        Log.e(TAG, "release done...");
    }

    public void playAudio(String path) {
        boolean finished = false;
        byte[] data = new byte[10 * 1024];
        running = new AtomicBoolean(true);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        MediaFormat mediaFormat = parseAudioFormat(path);
        if (mediaFormat == null) {
            release();
            return;
        }
        boolean result = initMediaCodec(mediaFormat);
        if (!result) {
            release();
            return;
        }
        result = initAudioTrack(mediaFormat);
        if (!result) {
            release();
            return;
        }

        while (!finished) {
            if (!running.get()) {
                break;
            }
            int inputIndex = mediaCodec.dequeueInputBuffer(DEQUEUE_TIME);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    mediaCodec.queueInputBuffer(inputIndex, 0, 0,
                            0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    finished = true;
                } else {
                    mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize,
                            mediaExtractor.getSampleTime(), mediaExtractor.getSampleFlags());
                    mediaExtractor.advance();
                }
            }

            int outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME);
            if (outputIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputIndex);
                int size = outputBuffer.limit();
                outputBuffer.get(data, outputBuffer.position(), size - outputBuffer.position());
                audioTrack.write(data, 0, size);
                mediaCodec.releaseOutputBuffer(outputIndex, false);
                SystemClock.sleep(SLEEP_TIME);
            }
        }

        release();
    }

    public void stop() {
        if (running != null) {
            running.set(false);
        }
    }

    public int getAudioSessionId() {
        if (audioTrack == null)
            return 0;
        return audioTrack.getAudioSessionId();
    }

    public void attachAudioEffect(int effectId) {
        if (audioTrack == null)
            return;
        audioTrack.attachAuxEffect(effectId);
    }
}