package com.frank.ffmpeg.activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.frank.ffmpeg.FFmpegCmd;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.adapter.WaterfallAdapter;
import com.frank.ffmpeg.listener.OnItemClickListener;
import com.frank.ffmpeg.model.MediaBean;
import com.frank.ffmpeg.model.VideoLayout;
import com.frank.ffmpeg.gif.HighQualityGif;
import com.frank.ffmpeg.handler.FFmpegHandler;
import com.frank.ffmpeg.tool.JsonParseTool;
import com.frank.ffmpeg.util.BitmapUtil;
import com.frank.ffmpeg.util.FFmpegUtil;
import com.frank.ffmpeg.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_BEGIN;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_FINISH;
import static com.frank.ffmpeg.handler.FFmpegHandler.MSG_PROGRESS;

import org.jetbrains.annotations.NotNull;

public class VideoHandleActivity extends BaseActivity {

    private RecyclerView layoutVideoHandle;
    private LinearLayout layoutProgress;
    private TextView txtProgress;
    private int currentPosition = 0;
    private FFmpegHandler ffmpegHandler;

    private final String appendPath = PATH + File.separator + "snow.mp4";
    private final String outputPath1 = PATH + File.separator + "output1.ts";
    private final String outputPath2 = PATH + File.separator + "output2.ts";
    private final String listPath = PATH + File.separator + "listFile.txt";

    private List<String> list;
    private boolean isJointing = false;

    private String firstSrcFile = "";
    private boolean selectingSecondFile = false;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BEGIN:
                    layoutProgress.setVisibility(View.VISIBLE);
                    layoutVideoHandle.setVisibility(View.GONE);
                    break;
                case MSG_FINISH:
                    layoutProgress.setVisibility(View.GONE);
                    layoutVideoHandle.setVisibility(View.VISIBLE);
                    if (isJointing) {
                        isJointing = false;
                        FileUtil.deleteFile(outputPath1);
                        FileUtil.deleteFile(outputPath2);
                        FileUtil.deleteFile(listPath);
                    }
                    if (outputPath != null && !outputPath.isEmpty() && !VideoHandleActivity.this.isDestroyed()) {
                        showToast("Save to:" + outputPath);
                        outputPath = "";
                    }
                    // reset progress
                    txtProgress.setText(String.format(Locale.getDefault(), "%d%%", 0));
                    break;
                case MSG_PROGRESS:
                    int progress = msg.arg1;
                    int duration = msg.arg2;
                    if (progress > 0) {
                        txtProgress.setVisibility(View.VISIBLE);
                        String percent = duration > 0 ? "%" : "";
                        String strProgress = progress + percent;
                        txtProgress.setText(strProgress);
                    } else {
                        txtProgress.setVisibility(View.INVISIBLE);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.activity_video_handle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        ffmpegHandler = new FFmpegHandler(mHandler);
       // if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
           // PATH = getCacheDir().getAbsolutePath();
       // }
    }

    private void initView() {
        layoutProgress = getView(R.id.layout_progress);
        txtProgress = getView(R.id.txt_progress);
        list = new ArrayList<String>() {{
            add(getString(R.string.video_transform));
            add(getString(R.string.video_cut));
            add(getString(R.string.video_concat));
            add(getString(R.string.video_screen_shot));
            add(getString(R.string.video_water_mark));
            add(getString(R.string.video_remove_logo));
            add(getString(R.string.video_stereo3d));
            add(getString(R.string.video_to_gif));
            add(getString(R.string.video_multi));
            add(getString(R.string.video_reverse));
            add(getString(R.string.video_denoise));
            add(getString(R.string.video_image));
            add(getString(R.string.video_pip));
            add(getString(R.string.video_speed));
            add(getString(R.string.video_thumbnail));
            add(getString(R.string.video_subtitle));
            add(getString(R.string.video_rotate));
            add(getString(R.string.video_gray));
            add(getString(R.string.video_zoom));
        }};

        layoutVideoHandle = findViewById(R.id.list_video_item);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutVideoHandle.setLayoutManager(layoutManager);

        WaterfallAdapter adapter = new WaterfallAdapter(list);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                currentPosition = position;
                if (getString(R.string.video_from_photo).equals(list.get(position))) {
                    handlePhoto();
                } else {
                    selectFile();
                }
            }
        });
        layoutVideoHandle.setAdapter(adapter);
    }

    @Override
    protected void onViewClick(View view) {
    }

    @Override
    protected void onSelectedFile(String filePath) {
        doHandleVideo(filePath);
    }

    private void doHandleVideo(String srcFile) {
        String[] commandLine = null;
        if (!FileUtil.checkFileExist(srcFile)) {
            return;
        }
        if (!FileUtil.isVideo(srcFile)
                && !list.get(currentPosition).equals(getString(R.string.video_zoom))) {
            showToast(getString(R.string.wrong_video_format));
            return;
        }
        String suffix = FileUtil.getFileSuffix(srcFile);
        if (suffix == null || suffix.isEmpty()) {
            return;
        }
        switch (currentPosition) {
            case 0: //transform format
                outputPath = PATH + File.separator + "transformVideo.mp4";
                int width = 1280;
                int height = 720;
                commandLine = FFmpegUtil.transformVideoWithEncode(srcFile, width, height, outputPath);
                break;
            case 1: //cut video
                outputPath = PATH + File.separator + "cutVideo" + suffix;
                float startTime = 5.5f;
                float duration = 20.0f;
                commandLine = FFmpegUtil.cutVideo(srcFile, startTime, duration, outputPath);
                break;
            case 2: //concat video together
                concatVideo(srcFile);
                break;
            case 3: //video snapshot
                outputPath = PATH + File.separator + "screenShot.jpg";
                float time = 10.5f;
                commandLine = FFmpegUtil.screenShot(srcFile, time, outputPath);
                break;
            case 4: //add watermark to video
                // the unit of bitRate is kb
                int bitRate = 500;
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(srcFile);
                String mBitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                if (mBitRate != null && !mBitRate.isEmpty()) {
                    int probeBitrate = Integer.valueOf(mBitRate);
                    bitRate = probeBitrate / 1000 / 100 * 100;
                }
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //1:top left 2:top right 3:bottom left 4:bottom right
                int location = 1;
                int offsetXY = 10;
                switch (waterMarkType) {
                    case TYPE_IMAGE: // image
                        String photo = PATH + File.separator + "hello.png";
                        outputPath = PATH + File.separator + "photoMark.mp4";
                        commandLine = FFmpegUtil.addWaterMarkImg(srcFile, photo, location, bitRate, offsetXY, outputPath);
                        break;
                    case TYPE_GIF: // gif
                        String gifPath = PATH + File.separator + "ok.gif";
                        outputPath = PATH + File.separator + "gifWaterMark.mp4";
                        commandLine = FFmpegUtil.addWaterMarkGif(srcFile, gifPath, location, bitRate, offsetXY, outputPath);
                        break;
                    case TYPE_TEXT: // text
                        String text = "Hello,FFmpeg";
                        String textPath = PATH + File.separator + "text.png";
                        boolean result = BitmapUtil.textToPicture(textPath, text, Color.BLUE, 20);
                        outputPath = PATH + File.separator + "textMark.mp4";
                        commandLine = FFmpegUtil.addWaterMarkImg(srcFile, textPath, location, bitRate, offsetXY, outputPath);
                        break;
                    default:
                        break;
                }
                break;
            case 5: //Remove logo from video, or use to mosaic video
                outputPath = PATH + File.separator + "removeLogo" + suffix;
                int widthL = 64;
                int heightL = 40;
                commandLine = FFmpegUtil.removeLogo(srcFile, 10, 10, widthL, heightL, outputPath);
                break;
            case 6: // VR video: convert to stereo3d video
                outputPath = PATH + File.separator + "stereo3d" + suffix;
                commandLine = FFmpegUtil.videoStereo3D(srcFile, outputPath);
                break;
            case 7: //convert video into gif
                outputPath = PATH + File.separator + "video2Gif.gif";
                int gifStart = 10;
                int gifDuration = 3;
                int gifWidth = 320;
                int frameRate = 10;

                if (convertGifWithFFmpeg) {
                    String palettePath = PATH + "/palette.png";
                    FileUtil.deleteFile(palettePath);
                    String[] paletteCmd = FFmpegUtil.generatePalette(srcFile, gifStart, gifDuration,
                            frameRate, gifWidth, palettePath);
                    String[] gifCmd = FFmpegUtil.generateGifByPalette(srcFile, palettePath, gifStart, gifDuration,
                            frameRate, gifWidth, outputPath);
                    List<String[]> cmdList = new ArrayList<>();
                    cmdList.add(paletteCmd);
                    cmdList.add(gifCmd);
                    ffmpegHandler.executeFFmpegCmds(cmdList);
                } else {
                    convertGifInHighQuality(outputPath, srcFile, gifStart, gifDuration, frameRate);
                }
                break;
            case 8: //combine video which layout could be horizontal of vertical
                String input1 = PATH + File.separator + "input1.mp4";
                String input2 = PATH + File.separator + "input2.mp4";
                outputPath = PATH + File.separator + "multi.mp4";
                if (!FileUtil.checkFileExist(input1) || !FileUtil.checkFileExist(input2)) {
                    return;
                }
                commandLine = FFmpegUtil.multiVideo(input1, input2, outputPath, VideoLayout.LAYOUT_HORIZONTAL);
                break;
            case 9: //video reverse
                outputPath = PATH + File.separator + "reverse.mp4";
                commandLine = FFmpegUtil.reverseVideo(srcFile, outputPath);
                break;
            case 10: //noise reduction of video
                outputPath = PATH + File.separator + "denoise.mp4";
                commandLine = FFmpegUtil.denoiseVideo(srcFile, outputPath);
                break;
            case 11: //convert video to picture
                outputPath = PATH + File.separator + "Video2Image/";
                File imageFile = new File(outputPath);
                if (!imageFile.exists()) {
                    if (!imageFile.mkdir()) {
                        return;
                    }
                }
                int mStartTime = 10;//start time
                int mDuration = 5;//duration
                int mFrameRate = 10;//frameRate
                commandLine = FFmpegUtil.videoToImage(srcFile, mStartTime, mDuration, mFrameRate, outputPath);
                break;
            case 12: //combine into picture-in-picture video
                // NOTE: The first video should be bigger than the second one.
                if (selectingSecondFile) { // User has selected 2 files
                    selectingSecondFile = false;
                    if (!FileUtil.checkFileExist(firstSrcFile) && !FileUtil.checkFileExist(srcFile)) {
                        showToast("请选择两个源文件");
                        return;
                    }
                    outputPath = PATH + File.separator + "PicInPic.mp4";
                    commandLine = FFmpegUtil.picInPicVideoInCorner(firstSrcFile, srcFile, 2, outputPath);
                } else {
                    firstSrcFile = srcFile;
                    selectingSecondFile = true;
                    showToast("画中画：请再选择一个文件");
                    selectFile(); // pop up to select another source file
                }
                break;
            case 13: //playing speed of video
                outputPath = PATH + File.separator + "speed.mp4";
                commandLine = FFmpegUtil.changeSpeed(srcFile, outputPath, 2f, false);
                break;
            case 14: // insert thumbnail into video
                String thumbnailPath = PATH + File.separator + "thumb.jpg";
                outputPath = PATH + File.separator + "thumbnailVideo" + suffix;
                commandLine = FFmpegUtil.insertPicIntoVideo(srcFile, thumbnailPath, outputPath);
                break;
            case 15: //add subtitle into video
                String subtitlePath = PATH + File.separator + "test.ass";
                outputPath = PATH + File.separator + "subtitle.mkv";
                commandLine = FFmpegUtil.addSubtitleIntoVideo(srcFile, subtitlePath, outputPath);
                break;
            case 16: // set the rotate degree of video
                int rotateDegree = 90;
                outputPath = PATH + File.separator + "rotate" + rotateDegree + suffix;
                commandLine = FFmpegUtil.rotateVideo(srcFile, rotateDegree, outputPath);
                break;
            case 17: // change video from RGB to gray
                outputPath = PATH + File.separator + "gray" + suffix;
                commandLine = FFmpegUtil.toGrayVideo(srcFile, outputPath);
                break;
            case 18: // zoom photo to video
                outputPath = PATH + File.separator + "zoom.mp4";
                int position = 0;
                commandLine = FFmpegUtil.photoZoomToVideo(srcFile, position, outputPath);
                break;
            default:
                break;
        }
        if (ffmpegHandler != null && commandLine != null) {
            ffmpegHandler.executeFFmpegCmd(commandLine);
        }
    }

    private void concatVideo(String selectedPath) {
        if (ffmpegHandler == null || selectedPath.isEmpty()) {
            return;
        }
        isJointing = true;
        String targetPath = PATH + File.separator + "jointVideo.mp4";
        String[] transformCmd1 = FFmpegUtil.transformVideoWithEncode(selectedPath, outputPath1);
        int width = 0;
        int height = 0;
        //probe width and height of the selected video
        String probeResult = FFmpegCmd.executeProbeSynchronize(FFmpegUtil.probeFormat(selectedPath));
        MediaBean mediaBean = JsonParseTool.parseMediaFormat(probeResult);
        if (mediaBean != null && mediaBean.getVideoBean() != null) {
            width = mediaBean.getVideoBean().getWidth();
            height = mediaBean.getVideoBean().getHeight();
            Log.e(TAG, "width=" + width + "--height=" + height);
        }
        String[] transformCmd2 = FFmpegUtil.transformVideoWithEncode(appendPath, width, height, outputPath2);
        List<String> fileList = new ArrayList<>();
        fileList.add(outputPath1);
        fileList.add(outputPath2);
        FileUtil.createListFile(listPath, fileList);
        String[] jointVideoCmd = FFmpegUtil.jointVideo(listPath, targetPath);
        List<String[]> commandList = new ArrayList<>();
        commandList.add(transformCmd1);
        commandList.add(transformCmd2);
        commandList.add(jointVideoCmd);
        ffmpegHandler.executeFFmpegCmds(commandList);
    }

    private void handlePhoto() {
        // The path of pictures, naming format: img+number.jpg
        String picturePath = PATH + "/img/";
        if (!FileUtil.checkFileExist(picturePath)) {
            return;
        }
        String tempPath = PATH + "/temp/";
        FileUtil.deleteFolder(tempPath);
        File photoFile = new File(picturePath);
        File[] files = photoFile.listFiles();
        List<String[]> cmdList = new ArrayList<>();
        //the resolution of photo which you want to convert
        String resolution = "640x480";
        for (File file : files) {
            String inputPath = file.getAbsolutePath();
            String outputPath = tempPath + file.getName();
            String[] convertCmd = FFmpegUtil.convertResolution(inputPath, resolution, outputPath);
            cmdList.add(convertCmd);
        }
        String combineVideo = PATH + File.separator + "combineVideo.mp4";
        int frameRate = 2;// suggested synthetic frameRate:1-10
        String[] commandLine = FFmpegUtil.pictureToVideo(tempPath, frameRate, combineVideo);
        cmdList.add(commandLine);
        if (ffmpegHandler != null) {
            ffmpegHandler.executeFFmpegCmds(cmdList);
        }
    }

    private void convertGifInHighQuality(String gifPath, String videoPath, int startTime, int duration, int frameRate) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_BEGIN);
                int width = 0;
                int height = 0;
                int rotateDegree = 0;
                MediaMetadataRetriever retriever = null;
                try {
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(videoPath);
                    String mWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String mHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    width = Integer.valueOf(mWidth);
                    height = Integer.valueOf(mHeight);
                    String rotate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    if (rotate != null && !rotate.isEmpty()) {
                        rotateDegree = Integer.valueOf(rotate);
                    }
                    Log.e(TAG, "retrieve width=" + width + "--height=" + height + "--rotate=" + rotate);
                } catch (Exception e) {
                    Log.e(TAG, "retrieve error=" + e);
                } finally {
                    if (retriever != null) {
                        try {
                            retriever.release();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                long start = System.currentTimeMillis();
                HighQualityGif highQualityGif = new HighQualityGif(width, height, rotateDegree);
                boolean result = highQualityGif.convertGIF(gifPath, videoPath, startTime, duration, frameRate);
                Log.e(TAG, "convert gif result=" + result + "--time=" + (System.currentTimeMillis() - start));
                mHandler.sendEmptyMessage(MSG_FINISH);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private static final String TAG = VideoHandleActivity.class.getSimpleName();
    private static String PATH = Environment.getExternalStorageDirectory().getPath();
    private static String outputPath = null;

    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_GIF = 2;
    private static final int TYPE_TEXT = 3;
    private static final int waterMarkType = TYPE_IMAGE;
    private static final boolean convertGifWithFFmpeg = false;

}
