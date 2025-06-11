package com.frank.ffmpeg.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.frank.ffmpeg.R;
import com.frank.ffmpeg.adapter.WaterfallAdapter;
import com.frank.ffmpeg.listener.OnItemClickListener;

import java.util.Arrays;
import java.util.List;

/**
 * The main entrance of all Activity
 * Created by frank on 2018/1/23.
 */
public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        managerPermission();
    }

    private void managerPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }

        if (Environment.isExternalStorageManager()) {
            return;
        }

        String packageName = this.getPackageName();
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }

    private void initView() {
        List<String> list = Arrays.asList(
                getString(R.string.audio_handle),
                getString(R.string.video_handle),
                getString(R.string.media_handle),
                getString(R.string.video_push),
                getString(R.string.video_live),
                getString(R.string.video_filter),
                getString(R.string.video_preview),
                getString(R.string.media_probe),
                getString(R.string.audio_effect),
                getString(R.string.camera_filter)
        );

        RecyclerView viewWaterfall = findViewById(R.id.list_main_item);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        viewWaterfall.setLayoutManager(layoutManager);

        WaterfallAdapter adapter = new WaterfallAdapter(list);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                doClick(position);
            }
        });
        viewWaterfall.setAdapter(adapter);
    }

    private void doClick(int pos) {
        Intent intent = new Intent();
        switch (pos) {
            case 0: // handle audio
                intent.setClass(MainActivity.this, AudioHandleActivity.class);
                break;
            case 1: // handle video
                intent.setClass(MainActivity.this, VideoHandleActivity.class);
                break;
            case 2: // handle media
                intent.setClass(MainActivity.this, MediaHandleActivity.class);
                break;
            case 3: // pushing
                intent.setClass(MainActivity.this, PushActivity.class);
                break;
            case 4: // realtime living with rtmp stream
                intent.setClass(MainActivity.this, LiveActivity.class);
                break;
            case 5: // filter effect
                intent.setClass(MainActivity.this, FilterActivity.class);
                break;
            case 6: // preview thumbnail
                intent.setClass(MainActivity.this, VideoPreviewActivity.class);
                break;
            case 7: // probe media format
                intent.setClass(MainActivity.this, ProbeFormatActivity.class);
                break;
            case 8: // audio effect
                intent.setClass(MainActivity.this, AudioEffectActivity.class);
                break;
            case 9: // camera filter
                intent.setClass(MainActivity.this, CameraFilterActivity.class);
                break;
            default:
                // Do nothing
                break;
        }
        startActivity(intent);
    }

    @Override
    public void onViewClick(View view) {
        // Empty implementation
    }

    @Override
    public void onSelectedFile(String filePath) {
        // Empty implementation
    }
}
