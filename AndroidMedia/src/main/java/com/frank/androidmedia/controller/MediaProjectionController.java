package com.frank.androidmedia.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import com.frank.androidmedia.listener.VideoEncodeCallback;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaProjectionController {

    public static final int TYPE_SCREEN_SHOT = 0;
    public static final int TYPE_SCREEN_RECORD = 1;
    public static final int TYPE_SCREEN_LIVING = 2;

    private int type = TYPE_SCREEN_SHOT;
    private final int requestCode = 123456;
    private VirtualDisplay virtualDisplay;
    private DisplayMetrics displayMetrics;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;

    private Thread encodeThread;
    private MediaCodec videoEncoder;
    private boolean isVideoEncoding = false;
    private byte[] videoEncodeData;
    private VideoEncodeCallback videoEncodeCallback;

    public MediaProjectionController(int type) {
        this.type = type;
    }

    public void setVideoEncodeListener(VideoEncodeCallback encodeCallback) {
        this.videoEncodeCallback = encodeCallback;
    }

    public void startScreenRecord(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity) context).startActivityForResult(intent, requestCode);
    }

    public void createVirtualDisplay(Surface surface) {
        virtualDisplay = mediaProjection.createVirtualDisplay("hello", displayMetrics.widthPixels,
                displayMetrics.heightPixels, displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null);
    }

    private void saveBitmap(Bitmap bitmap, String path) {
        if (path.isEmpty() || bitmap == null)
            return;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getBitmap() {
        ImageReader imageReader = ImageReader.newInstance(displayMetrics.widthPixels,
                displayMetrics.heightPixels, PixelFormat.RGBA_8888, 3);
        createVirtualDisplay(imageReader.getSurface());
        imageReader.setOnImageAvailableListener(reader -> {
            android.media.Image image = reader.acquireNextImage();
            android.media.Image.Plane[] planes = image.getPlanes();
            java.nio.ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * image.getWidth();
            Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride,
                    image.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/hello.jpg";
            saveBitmap(bitmap, filePath);
            image.close();
            imageReader.close();
        }, null);
    }

    private void initMediaCodec(int width, int height) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        try {
            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            videoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            createVirtualDisplay(videoEncoder.createInputSurface());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startVideoEncoder() {
        if (videoEncoder == null || isVideoEncoding)
            return;
        encodeThread = new Thread(() -> {
            try {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                videoEncoder.start();

                while (isVideoEncoding && !Thread.currentThread().isInterrupted()) {
                    int outputIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 30 * 1000);
                    if (outputIndex >= 0) {
                        java.nio.ByteBuffer byteBuffer = videoEncoder.getOutputBuffer(outputIndex);
                        if (videoEncodeData == null || videoEncodeData.length < bufferInfo.size) {
                            videoEncodeData = new byte[bufferInfo.size];
                        }
                        if (videoEncodeCallback != null && byteBuffer != null) {
                            byteBuffer.get(videoEncodeData, bufferInfo.offset, bufferInfo.size);
                            videoEncodeCallback.onVideoEncodeData(videoEncodeData, bufferInfo.size,
                                    bufferInfo.flags, bufferInfo.presentationTimeUs);
                        }
                        videoEncoder.releaseOutputBuffer(outputIndex, false);
                    } else {
                        Log.e("EncodeThread", "invalid index=" + outputIndex);
                    }
                }
            } catch (Exception e) {
                isVideoEncoding = false;
                Log.e("EncodeThread", "encode error=" + e);
            }
        });
        isVideoEncoding = true;
        encodeThread.start();
    }

    public void onActivityResult(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (type == TYPE_SCREEN_SHOT) {
            getBitmap();
        } else if (type == TYPE_SCREEN_LIVING) {
            initMediaCodec(displayMetrics.widthPixels, displayMetrics.heightPixels);
            startVideoEncoder();
        }
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void stopScreenRecord() {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        isVideoEncoding = false;
        if (encodeThread != null) {
            encodeThread.interrupt();
        }
        if (videoEncoder != null) {
            videoEncoder.release();
        }
    }
}