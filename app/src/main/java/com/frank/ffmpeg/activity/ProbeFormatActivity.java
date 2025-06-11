package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.frank.ffmpeg.R;
import com.frank.ffmpeg.handler.FFmpegHandler;
import com.frank.ffmpeg.model.MediaBean;
import com.frank.ffmpeg.tool.JsonParseTool;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;
import com.frank.ffmpeg.metadata.FFmpegMediaRetriever;
import com.frank.ffmpeg.util.BitmapUtil;

/**
 * Using ffprobe to parse media format data
 * Created by frank on 2020/1/7.
 */
public class ProbeFormatActivity extends BaseActivity {

    private TextView txtProbeFormat;
    private ProgressBar progressProbe;
    private RelativeLayout layoutProbe;
    private FFmpegHandler ffmpegHandler;

    private View view;

    private static final int MSG_FRAME = 9099;

    private boolean savePhoto = false;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FFmpegHandler.MSG_BEGIN:
                    progressProbe.setVisibility(View.VISIBLE);
                    layoutProbe.setVisibility(View.GONE);
                    break;
                case FFmpegHandler.MSG_FINISH:
                    progressProbe.setVisibility(View.GONE);
                    layoutProbe.setVisibility(View.VISIBLE);
                    if (msg.obj != null) {
                        String mediaInfo = JsonParseTool.stringFormat((MediaBean) msg.obj);
                        if (mediaInfo != null && !mediaInfo.isEmpty() && txtProbeFormat != null) {
                            txtProbeFormat.setText(mediaInfo);
                        }
                    }
                    break;
                case FFmpegHandler.MSG_INFO:
                    String mediaInfo = (String) msg.obj;
                    if (mediaInfo != null && !mediaInfo.isEmpty()) {
                        txtProbeFormat.setText(mediaInfo);
                    }
                    break;
                case MSG_FRAME:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    ((ImageView) findViewById(R.id.img_frame)).setImageBitmap(bitmap);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_probe;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        ffmpegHandler = new FFmpegHandler(mHandler);
    }

    private void initView() {
        progressProbe = getView(R.id.progress_probe);
        layoutProbe = getView(R.id.layout_probe);
        initViewsWithClick(R.id.btn_probe_format);
        initViewsWithClick(R.id.btn_retrieve_format);
        txtProbeFormat = getView(R.id.txt_probe_format);
    }

    @Override
    public void onViewClick(View view) {
        this.view = view;
        selectFile();
    }

    @Override
    public void onSelectedFile(String filePath) {
        int id = view.getId();
        if (id == R.id.btn_probe_format) {
            ((ImageView) findViewById(R.id.img_frame)).setImageBitmap(null);
            doHandleProbe(filePath);
        } else if (id == R.id.btn_retrieve_format) {
            new Thread(() -> retrieveMediaMetadata(filePath)).start();
        }
    }

    /**
     * use ffprobe to parse video/audio format metadata
     *
     * @param srcFile srcFile
     */
    private void doHandleProbe(String srcFile) {
        if (!FileUtil.checkFileExist(srcFile)) {
            return;
        }
        String[] commandLine = FFmpegUtil.probeFormat(srcFile);
        if (ffmpegHandler != null) {
            ffmpegHandler.executeFFprobeCmd(commandLine);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void retrieveMediaMetadata(String path) {
        if (path.isEmpty()) {
            return;
        }

        try {
            StringBuilder resultBuilder = new StringBuilder();
            FFmpegMediaRetriever retriever = new FFmpegMediaRetriever();
            retriever.setDataSource(path);

            String duration = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_DURATION);
            if (duration != null)
                resultBuilder.append("duration:").append(duration).append("\n");

            String audioCodec = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_AUDIO_CODEC);
            if (audioCodec != null)
                resultBuilder.append("audioCodec:").append(audioCodec).append("\n");

            String videoCodec = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_VIDEO_CODEC);
            if (videoCodec != null)
                resultBuilder.append("videoCodec:").append(videoCodec).append("\n");

            String width = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (width != null && height != null)
                resultBuilder.append("resolution:").append(width).append(" x ").append(height).append("\n");

            String frameRate = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_FRAME_RATE);
            if (frameRate != null)
                resultBuilder.append("frameRate:").append(frameRate).append("\n");

            String sampleRate = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_SAMPLE_RATE);
            if (sampleRate != null)
                resultBuilder.append("sampleRate:").append(sampleRate).append("\n");

            String channelCount = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_CHANNEL_COUNT);
            if (channelCount != null)
                resultBuilder.append("channelCount:").append(channelCount).append("\n");

            String channelLayout = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_CHANNEL_LAYOUT);
            if (channelLayout != null)
                resultBuilder.append("channelLayout:").append(channelLayout).append("\n");

            String pixelFormat = retriever.extractMetadata(FFmpegMediaRetriever.METADATA_KEY_PIXEL_FORMAT);
            if (pixelFormat != null)
                resultBuilder.append("pixelFormat:").append(pixelFormat).append("\n");

            mHandler.obtainMessage(FFmpegHandler.MSG_INFO, resultBuilder.toString()).sendToTarget();

            // Retrieve frame with timeUs
            long frameTime;
            if (duration != null) {
                frameTime = Long.parseLong(duration) / 3 * 1000;
            } else {
                frameTime = 5 * 1000000;
            }
            Bitmap bitmap = retriever.getFrameAtTime(frameTime);
            // Retrieve audio thumbnail, if it has embedded
            // Bitmap bitmap = retriever.audioThumbnail();
            if (bitmap != null) {
                Log.e("FFmpegRetriever", "bitmap width=" + bitmap.getWidth() + "--height=" + bitmap.getHeight());
                mHandler.obtainMessage(MSG_FRAME, bitmap).sendToTarget();
                if (savePhoto) {
                    String thumbPath = Environment.getExternalStorageDirectory().getPath() + "/thumb_" + System.currentTimeMillis() + ".png";
                    BitmapUtil.savePhoto(bitmap, thumbPath, this);
                }
            }

            retriever.release();
        } catch (Exception e) {
            Log.e("FFmpegRetriever", "ffmpeg error=" + e);
        }
    }
}
