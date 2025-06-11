package com.frank.androidmedia.listener;

/**
 * @author frank
 * @date 2022/3/18
 */
public interface PlayerCallback {
    void onPrepare();

    boolean onError(int what, int extra);

    void onRenderFirstFrame();

    void onCompleteListener();
}
