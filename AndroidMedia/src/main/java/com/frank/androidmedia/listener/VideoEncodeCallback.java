package com.frank.androidmedia.listener;

/**
 * @author xufulong
 * @date 4/1/22 1:44 PM
 * @desc
 */
public interface VideoEncodeCallback {
    void onVideoEncodeData(byte[] data, int size, int flag, long timestamp);
}
