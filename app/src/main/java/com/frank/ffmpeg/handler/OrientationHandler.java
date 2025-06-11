package com.frank.ffmpeg.handler;

import android.content.Context;
import android.util.Log;
import android.view.OrientationEventListener;

public class OrientationHandler {
    private static final String TAG = "OrientationHandler";
    private static final int OFFSET_ANGLE = 5;

    private int lastOrientationDegree = 0;
    private OnOrientationListener onOrientationListener;
    private OrientationEventListener orientationEventListener;

    public interface OnOrientationListener {
        void onOrientation(int orientation);
    }

    public OrientationHandler(Context context) {
        initOrientation(context);
    }

    public void setOnOrientationListener(OnOrientationListener listener) {
        this.onOrientationListener = listener;
    }

    private void initOrientation(Context context) {
        orientationEventListener = new OrientationEventListener(context.getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                if (orientation >= 0 - OFFSET_ANGLE && orientation <= OFFSET_ANGLE) {
                    if (lastOrientationDegree != 0) {
                        Log.i(TAG, "0, portrait down");
                        lastOrientationDegree = 0;
                        if (onOrientationListener != null) {
                            onOrientationListener.onOrientation(lastOrientationDegree);
                        }
                    }
                } else if (orientation >= 90 - OFFSET_ANGLE && orientation <= 90 + OFFSET_ANGLE) {
                    if (lastOrientationDegree != 90) {
                        Log.i(TAG, "90, landscape right");
                        lastOrientationDegree = 90;
                        if (onOrientationListener != null) {
                            onOrientationListener.onOrientation(lastOrientationDegree);
                        }
                    }
                } else if (orientation >= 180 - OFFSET_ANGLE && orientation <= 180 + OFFSET_ANGLE) {
                    if (lastOrientationDegree != 180) {
                        Log.i(TAG, "180, portrait up");
                        lastOrientationDegree = 180;
                        if (onOrientationListener != null) {
                            onOrientationListener.onOrientation(lastOrientationDegree);
                        }
                    }
                } else if (orientation >= 270 - OFFSET_ANGLE && orientation <= 270 + OFFSET_ANGLE) {
                    if (lastOrientationDegree != 270) {
                        Log.i(TAG, "270, landscape left");
                        lastOrientationDegree = 270;
                        if (onOrientationListener != null) {
                            onOrientationListener.onOrientation(lastOrientationDegree);
                        }
                    }
                }
            }
        };
    }

    public void enable() {
        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
    }

    public void disable() {
        if (orientationEventListener != null && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.disable();
        }
    }
}
