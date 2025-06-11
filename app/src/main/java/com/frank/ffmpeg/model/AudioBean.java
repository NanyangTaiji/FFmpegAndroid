package com.frank.ffmpeg.model;

import java.util.List;

/**
 * the model of audio data
 * Created by frank on 2020/1/7.
 */
public class AudioBean {

    //"codec_tag_string": "mp4a"
    private String audioCodec;

    //"sample_rate": "44100"
    private int sampleRate;

    //"channels": 2
    private int channels;

    //"channel_layout": "stereo"
    private String channelLayout;

    private String title;

    private String artist;

    private String album;

    private String albumArtist;

    private String composer;

    private String genre;

    private List<String> lyrics;

    private List<LrcLine> lrcLineList;

    // Getters and Setters
    public String getAudioCodec() {
        return "[0][0][0][0]".equals(audioCodec) ? null : audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public String getChannelLayout() {
        return channelLayout;
    }

    public void setChannelLayout(String channelLayout) {
        this.channelLayout = channelLayout;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public List<String> getLyrics() {
        return lyrics;
    }

    public void setLyrics(List<String> lyrics) {
        this.lyrics = lyrics;
    }

    public List<LrcLine> getLrcLineList() {
        return lrcLineList;
    }

    public void setLrcLineList(List<LrcLine> lrcLineList) {
        this.lrcLineList = lrcLineList;
    }
}

