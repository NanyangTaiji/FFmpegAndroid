package com.frank.ffmpeg.gif;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.frank.ffmpeg.FFmpegCmd;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HighQualityGif {
    private static final String TAG = HighQualityGif.class.getSimpleName();
    private static final int TARGET_WIDTH = 320;
    private static final int TARGET_HEIGHT = 180;

    private int mWidth;
    private int mHeight;
    private int mRotateDegree;

    public HighQualityGif(int width, int height, int rotateDegree) {
        this.mWidth = width;
        this.mHeight = height;
        this.mRotateDegree = rotateDegree;
    }

    private int chooseWidth(int width, int height) {
        if (width <= 0 || height <= 0) {
            return TARGET_WIDTH;
        }
        if (mRotateDegree == 0 || mRotateDegree == 180) { // landscape
            return width > TARGET_WIDTH ? TARGET_WIDTH : width;
        } else { // portrait
            return height > TARGET_HEIGHT ? TARGET_HEIGHT : height;
        }
    }

    private byte[] generateGif(String filePath, int startTime, int duration, int frameRate) throws IllegalArgumentException {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        String folderPath = Environment.getExternalStorageDirectory().toString() + "/gif_frames/";
        FileUtil.deleteFolder(folderPath);
        int targetWidth = chooseWidth(mWidth, mHeight);
        String[] commandLine = FFmpegUtil.videoToImageWithScale(filePath, startTime, duration, frameRate, targetWidth, folderPath);
        FFmpegCmd.executeSync(commandLine);
        File fileFolder = new File(folderPath);
        if (!fileFolder.exists() || fileFolder.listFiles() == null) {
            return null;
        }
        File[] files = fileFolder.listFiles();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BeautyGifEncoder gifEncoder = new BeautyGifEncoder();
        gifEncoder.setRepeat(0);
        gifEncoder.setFrameRate(10f);
        gifEncoder.start(outputStream);

        for (File file : files) {
            android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                gifEncoder.addFrame(bitmap);
            }
        }
        gifEncoder.finish();
        return outputStream.toByteArray();
    }

    private boolean saveGif(byte[] data, String gifPath) {
        if (data == null || data.length == 0 || TextUtils.isEmpty(gifPath)) {
            return false;
        }
        boolean result = true;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(gifPath);
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            result = false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public boolean convertGIF(String gifPath, String filePath, int startTime, int duration, int frameRate) {
        byte[] data;
        try {
            data = generateGif(filePath, startTime, duration, frameRate);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "generateGif error=" + e);
            return false;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "generateGif error=" + e);
            return false;
        }

        return saveGif(data, gifPath);
    }
}
