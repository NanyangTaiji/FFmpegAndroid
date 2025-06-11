// VisualizerView.java
package com.frank.ffmpeg.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class VisualizerView extends View {
    private Path wavePath = new Path();
    private Paint lumpPaint;
    private byte[] waveData;
    private List<Point> pointList;
    private ShowStyle mShowStyle = ShowStyle.STYLE_HOLLOW_LUMP;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lumpPaint = new Paint();
        lumpPaint.setAntiAlias(true);
        lumpPaint.setColor(LUMP_COLOR);
        lumpPaint.setStrokeWidth(2f);
        lumpPaint.setStyle(Paint.Style.STROKE);
    }

    public void setWaveData(byte[] data) {
        this.waveData = readyData(data);
        genSamplingPoint(data);
        invalidate();
    }

    public void setStyle(ShowStyle showStyle) {
        this.mShowStyle = showStyle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        wavePath.reset();

        for (int i = 0; i < LUMP_COUNT; i++) {
            if (waveData == null) {
                canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i,
                        LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT,
                        (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH,
                        LUMP_MAX_HEIGHT,
                        lumpPaint);
                continue;
            }

            switch (mShowStyle) {
                case STYLE_HOLLOW_LUMP:
                    drawLump(canvas, i, false);
                    break;
                case STYLE_WAVE:
                    drawWave(canvas, i, false);
                    break;
            }
        }
    }

    private void drawWave(Canvas canvas, int i, boolean reverse) {
        if (pointList == null || pointList.size() < 2) return;

        float ratio = SCALE * (reverse ? -1 : 1);
        if (i < pointList.size() - 2) {
            Point point = pointList.get(i);
            Point nextPoint = pointList.get(i + 1);
            int midX = (point.x + nextPoint.x) >> 1;

            if (i == 0) {
                wavePath.moveTo(point.x, LUMP_MAX_HEIGHT - point.y * ratio);
            }

            wavePath.cubicTo(midX, LUMP_MAX_HEIGHT - point.y * ratio,
                    midX, LUMP_MAX_HEIGHT - nextPoint.y * ratio,
                    nextPoint.x, LUMP_MAX_HEIGHT - nextPoint.y * ratio);

            canvas.drawPath(wavePath, lumpPaint);
        }
    }

    private void drawLump(Canvas canvas, int i, boolean reverse) {
        int minus = reverse ? -1 : 1;
        float top = LUMP_MAX_HEIGHT - (LUMP_MIN_HEIGHT + waveData[i] * SCALE) * minus;

        canvas.drawRect(LUMP_SIZE * i,
                top,
                LUMP_SIZE * i + LUMP_WIDTH,
                LUMP_MAX_HEIGHT,
                lumpPaint);
    }

    private void genSamplingPoint(byte[] data) {
        if (mShowStyle != ShowStyle.STYLE_WAVE) return;

        if (pointList == null) {
            pointList = new ArrayList<>();
        } else {
            pointList.clear();
        }

        pointList.add(new Point(0, 0));
        for (int i = WAVE_SAMPLING_INTERVAL; i < LUMP_COUNT; i += WAVE_SAMPLING_INTERVAL) {
            pointList.add(new Point(LUMP_SIZE * i, data[i]));
        }
        pointList.add(new Point(LUMP_SIZE * LUMP_COUNT, 0));
    }

    public enum ShowStyle {
        STYLE_HOLLOW_LUMP,
        STYLE_WAVE
    }

    private static final int LUMP_COUNT = 128;
    private static final int LUMP_WIDTH = 6;
    private static final int LUMP_SPACE = 2;
    private static final int LUMP_MIN_HEIGHT = LUMP_WIDTH;
    private static final int LUMP_MAX_HEIGHT = 200;
    private static final int LUMP_SIZE = LUMP_WIDTH + LUMP_SPACE;
    private static final int LUMP_COLOR = Color.parseColor("#00eeee");
    private static final int WAVE_SAMPLING_INTERVAL = 3;
    private static final float SCALE = (float) LUMP_MAX_HEIGHT / LUMP_COUNT;

    private static byte[] readyData(byte[] fft) {
        byte[] newData = new byte[LUMP_COUNT];
        byte abs;
        for (int i = 0; i < LUMP_COUNT; i++) {
            abs = (byte) Math.abs(fft[i]);
            newData[i] = abs < 0 ? 127 : abs;
        }
        return newData;
    }
}