package com.frank.ffmpeg.model;

import java.util.List;

public class LrcInfo {
    private String title;
    private String album;
    private String artist;
    private String author;
    private String creator;
    private String encoder;
    private String version;
    private int offset;
    private List<LrcLine> lrcLineList;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getEncoder() {
        return encoder;
    }

    public void setEncoder(String encoder) {
        this.encoder = encoder;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<LrcLine> getLrcLineList() {
        return lrcLineList;
    }

    public void setLrcLineList(List<LrcLine> lrcLineList) {
        this.lrcLineList = lrcLineList;
    }
}
