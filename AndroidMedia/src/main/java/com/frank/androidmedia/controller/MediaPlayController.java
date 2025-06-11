package com.frank.androidmedia.controller;

import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.TimedText;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import com.frank.androidmedia.listener.PlayerCallback;
import java.io.IOException;

public class MediaPlayController {

    private MediaPlayer mediaPlayer;
    private boolean renderFirstFrame;
    private PlayerCallback playerCallback;

    public MediaPlayController(PlayerCallback playerCallback) {
        this.playerCallback = playerCallback;
    }

    public void initPlayer(String filePath, Surface surface) {
        if (mediaPlayer != null) {
            releasePlayer();
        }
        try {
            renderFirstFrame = false;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setSurface(surface);
            setListener();
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setListener() {
        mediaPlayer.setOnPreparedListener(mp -> {
            mediaPlayer.start();
            if (playerCallback != null) {
                playerCallback.onPrepare();
            }
        });

        mediaPlayer.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                if (!renderFirstFrame) {
                    renderFirstFrame = true;
                    if (playerCallback != null) {
                        playerCallback.onRenderFirstFrame();
                    }
                }
            }
            return true;
        });

        mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
            Log.i("MediaPlayer", "buffer percent=" + percent);
        });

        mediaPlayer.setOnTimedTextListener((mp, text) -> {
            Log.i("MediaPlayer", "subtitle=" + (text != null ? text.getText() : null));
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            return playerCallback != null && playerCallback.onError(what, extra);
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            if (playerCallback != null) {
                playerCallback.onCompleteListener();
            }
        });
    }

    public int currentPosition() {
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getCurrentPosition();
    }

    public int duration() {
        if (mediaPlayer == null)
            return 0;
        return mediaPlayer.getDuration();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public void togglePlay() {
        if (mediaPlayer == null)
            return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    public int getVideoWidth() {
        return mediaPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return mediaPlayer.getVideoHeight();
    }

    public void mute() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0.0f, 0.0f);
        }
    }

    public void setVolume(float volume) {
        if (volume < 0 || volume > 1)
            return;
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public void setSpeed(float speed) {
        if (speed <= 0 || speed > 8)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);
            mediaPlayer.setPlaybackParams(params);
        }
    }

    public void selectTrack(int trackId) {
        if (mediaPlayer != null) {
            mediaPlayer.selectTrack(trackId);
        }
    }

    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
