// OnLrcListener.java
package com.frank.ffmpeg.listener;

import com.frank.ffmpeg.model.LrcLine;

public interface OnLrcListener {
    void onLrcSeek(int position, LrcLine lrcLine);
}
