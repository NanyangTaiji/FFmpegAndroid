package com.frank.androidmedia.controller;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaMuxController {

    @SuppressLint("WrongConstant")
    public boolean muxMediaFile(String inputPath, String outputPath) {
        if (inputPath.isEmpty() || outputPath.isEmpty()) {
            return false;
        }
        boolean happenError = false;
        MediaMuxer mediaMuxer = null;
        try {
            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            int videoIndex = 0;
            int audioIndex = 0;
            MediaFormat audioFormat = null;
            MediaFormat videoFormat = null;
            boolean finished = false;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer inputBuffer = ByteBuffer.allocate(2 * 1024 * 1024);
            mediaExtractor.setDataSource(inputPath);

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType != null && mimeType.startsWith("video")) {
                    videoIndex = i;
                    videoFormat = mediaFormat;
                    mediaExtractor.selectTrack(i);
                } else if (mimeType != null && mimeType.startsWith("audio") && audioFormat == null) {
                    audioIndex = i;
                    audioFormat = mediaFormat;
                    mediaExtractor.selectTrack(i);
                }
            }

            if (videoFormat != null) {
                mediaMuxer.addTrack(videoFormat);
            }
            if (audioFormat != null) {
                mediaMuxer.addTrack(audioFormat);
            }

            mediaMuxer.start();

            while (!finished) {
                int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize > 0) {
                    bufferInfo.size = sampleSize;
                    bufferInfo.flags = mediaExtractor.getSampleFlags();
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                    if (mediaExtractor.getSampleTrackIndex() == videoIndex) {
                        mediaMuxer.writeSampleData(videoIndex, inputBuffer, bufferInfo);
                    } else if (mediaExtractor.getSampleTrackIndex() == audioIndex) {
                        mediaMuxer.writeSampleData(audioIndex, inputBuffer, bufferInfo);
                    }
                    inputBuffer.flip();
                    mediaExtractor.advance();
                } else if (sampleSize < 0) {
                    finished = true;
                }
            }
        } catch (Exception e) {
            Log.e("MediaMuxController", "mux error=" + e);
            happenError = true;
        } finally {
            mediaMuxer.release();
            mediaExtractor.release();
            return !happenError;
        }
    }
}
