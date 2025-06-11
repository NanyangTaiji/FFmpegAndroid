package com.frank.ffmpeg.util;

import com.frank.camerafilter.factory.BeautyFilterType;
import com.frank.ffmpeg.R;

public final class FilterTypeUtil {

    private FilterTypeUtil() {}

    public static int filterTypeToNameId(BeautyFilterType type) {
        if (type == null) {
            return R.string.camera_filter_none;
        }
        switch (type) {
            case NONE:
                return R.string.camera_filter_none;
            case BRIGHTNESS:
                return R.string.camera_filter_brightness;
            case SATURATION:
                return R.string.camera_filter_saturation;
            case CONTRAST:
                return R.string.camera_filter_contrast;
            case SHARPEN:
                return R.string.camera_filter_sharpen;
            case BLUR:
                return R.string.camera_filter_blur;
            case HUE:
                return R.string.camera_filter_hue;
            case WHITE_BALANCE:
                return R.string.camera_filter_balance;
            case SKETCH:
                return R.string.camera_filter_sketch;
            case OVERLAY:
                return R.string.camera_filter_overlay;
            case BREATH_CIRCLE:
                return R.string.camera_filter_circle;
            default:
                return R.string.camera_filter_none;
        }
    }
}
