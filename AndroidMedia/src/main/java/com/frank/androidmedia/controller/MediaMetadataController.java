package com.frank.androidmedia.controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

public class MediaMetadataController {
    private static final String TAG = MediaMetadataController.class.getSimpleName();

    private String title;
    private long duration;
    private int bitrate;
    private int width;
    private int height;
    private float frameRate;
    private Bitmap thumbnail;
    private MediaMetadataRetriever mRetriever;

    public void retrieveMetadata(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null) {
                Log.i(TAG, "title=" + title);
            }
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                duration = Long.parseLong(durationStr);
                Log.i(TAG, "duration=" + duration);
            }
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrateStr != null) {
                bitrate = Integer.parseInt(bitrateStr);
            }
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (widthStr != null) {
                width = Integer.parseInt(widthStr);
            }
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (heightStr != null) {
                height = Integer.parseInt(heightStr);
                Log.i(TAG, "video width=" + width + ",height=" + height);
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
                    if (frameRateStr != null) {
                        frameRate = Float.parseFloat(frameRateStr);
                    }
                    Log.i(TAG, "frameRate=" + frameRate);
                }
            } catch (Exception e) {
                Log.e(TAG, "retrieve frameRate error=" + e);
            }
            String hasVideoStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            String hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            if (hasVideoStr != null && "yes".equals(hasVideoStr)) {
                thumbnail = retriever.getFrameAtTime(0);
            } else if (hasAudioStr != null && "yes".equals(hasAudioStr)) {
                byte[] byteArray = retriever.getEmbeddedPicture();
                if (byteArray != null) {
                    thumbnail = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, null);
                }
            }
            if (thumbnail != null) {
                Log.i(TAG, "thumbnail width=" + thumbnail.getWidth() + ", height=" + thumbnail.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "retrieve error=" + e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void initRetriever(String path) {
        mRetriever = new MediaMetadataRetriever();
        try {
            mRetriever.setDataSource(path);
        } catch (Exception e) {
            Log.e(TAG, "initRetriever error=" + e);
        }
    }

    public Bitmap getFrameAtTime(long timeUs) {
        if (mRetriever == null)
            return null;
        return mRetriever.getFrameAtTime(timeUs);
    }

    public void releaseRetriever() {
        if (mRetriever != null) {
            try {
                mRetriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
