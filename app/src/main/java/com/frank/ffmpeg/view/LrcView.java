// LrcView.java
package com.frank.ffmpeg.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.IntDef;

import com.frank.ffmpeg.listener.OnLrcListener;
import com.frank.ffmpeg.model.LrcLine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class LrcView extends View {
    private final int mPadding = 10;
    private final int mLrcFontSize = 45;
    private int mHighLightRow = 0;
    private float mLastMotionY = 0f;
    private long currentMillis = 0;
    private List<LrcLine> mLrcLines;
    private OnLrcListener mLrcViewListener;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mNormalRowColor = Color.BLACK;
    private final int mHighLightRowColor = Color.BLUE;
    private int mDisplayMode = DISPLAY_MODE_NORMAL;

    @HighLightMode
    private int mode = MODE_HIGH_LIGHT_NORMAL;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_HIGH_LIGHT_NORMAL, MODE_HIGH_LIGHT_KARAOKE})
    public @interface HighLightMode {}

    public LrcView(Context context, AttributeSet attr) {
        super(context, attr);
        mPaint.setTextSize(mLrcFontSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getHeight();
        int width = getWidth();
        if (mLrcLines == null || mLrcLines.isEmpty()) {
            mPaint.setColor(mHighLightRowColor);
            mPaint.setTextSize(mLrcFontSize);
            mPaint.setTextAlign(Align.CENTER);
            canvas.drawText(noLrcTip, width / 2f, height / 2f - mLrcFontSize, mPaint);
            return;
        }

        int rowY;
        int rowX = width / 2;
        int rowNum = mHighLightRow - 1;
        int highlightRowY = height / 2 - mLrcFontSize;

        if (mode == MODE_HIGH_LIGHT_KARAOKE) {
            drawKaraokeHighLightLrcRow(canvas, width, rowX, highlightRowY);
        } else {
            drawHighLrcRow(canvas, height, rowX, highlightRowY);
        }

        if (mDisplayMode == DISPLAY_MODE_SEEK) {
            mPaint.setColor(mSeekLineColor);
            int mSeekLinePaddingX = 0;
            canvas.drawLine(mSeekLinePaddingX, highlightRowY + mPadding,
                    width - mSeekLinePaddingX, highlightRowY + mPadding, mPaint);
            mPaint.setColor(mSeekLineTextColor);
            mPaint.setTextSize(mSeekLineTextSize);
            mPaint.setTextAlign(Align.LEFT);
            canvas.drawText(mLrcLines.get(mHighLightRow).getTimeString(), 0, highlightRowY, mPaint);
        }

        mPaint.setColor(mNormalRowColor);
        mPaint.setTextSize(mLrcFontSize);
        mPaint.setTextAlign(Align.CENTER);
        rowY = highlightRowY - mPadding - mLrcFontSize;
        while (rowY > -mLrcFontSize && rowNum >= 0) {
            String text = mLrcLines.get(rowNum).getContent();
            canvas.drawText(text, rowX, rowY, mPaint);
            rowY -= mPadding + mLrcFontSize;
            rowNum--;
        }

        rowNum = mHighLightRow + 1;
        rowY = highlightRowY + mPadding + mLrcFontSize;
        while (rowY < height && rowNum < mLrcLines.size()) {
            String text = mLrcLines.get(rowNum).getContent();
            canvas.drawText(text, rowX, rowY, mPaint);
            rowY += mPadding + mLrcFontSize;
            rowNum++;
        }
    }

    private void drawKaraokeHighLightLrcRow(Canvas canvas, int width, int rowX, int highlightRowY) {
        if (width <= 0 || rowX <= 0 || highlightRowY <= 0) return;

        LrcLine highLrcLine = mLrcLines.get(mHighLightRow);
        String highlightText = highLrcLine.getContent();
        if (highlightText == null || highlightText.isEmpty()) return;

        mPaint.setColor(mNormalRowColor);
        mPaint.setTextSize(mLrcFontSize);
        mPaint.setTextAlign(Align.CENTER);
        canvas.drawText(highlightText, rowX, highlightRowY, mPaint);

        int highLineWidth = (int) mPaint.measureText(highlightText);
        int leftOffset = (width - highLineWidth) / 2;
        long start = highLrcLine.getStartTime();
        long end = highLrcLine.getEndTime();
        int highWidth = (int) ((currentMillis - start) * 1.0f / (end - start) * highLineWidth);
        if (highWidth > 0 && highWidth < Integer.MAX_VALUE) {
            mPaint.setColor(mHighLightRowColor);
            Bitmap textBitmap = Bitmap.createBitmap(highWidth, highlightRowY + mPadding, Bitmap.Config.ARGB_8888);
            Canvas textCanvas = new Canvas(textBitmap);
            textCanvas.drawText(highlightText, highLineWidth / 2f, highlightRowY, mPaint);
            canvas.drawBitmap(textBitmap, leftOffset, 0, mPaint);
        }
    }

    private void drawHighLrcRow(Canvas canvas, int height, int rowX, int highlightRowY) {
        String highlightText = mLrcLines.get(mHighLightRow).getContent();
        mPaint.setColor(mHighLightRowColor);
        mPaint.setTextSize(mLrcFontSize);
        mPaint.setTextAlign(Align.CENTER);
        canvas.drawText(highlightText, rowX, highlightRowY, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mLrcLines == null || mLrcLines.isEmpty()) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                doSeek(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mDisplayMode == DISPLAY_MODE_SEEK) {
                    seekLrc(mHighLightRow, true);
                }
                mDisplayMode = DISPLAY_MODE_NORMAL;
                invalidate();
                break;
        }
        return true;
    }

    private void doSeek(MotionEvent event) {
        float y = event.getY();
        float offsetY = y - mLastMotionY;
        if (Math.abs(offsetY) < mMinSeekFiredOffset) {
            return;
        }
        mDisplayMode = DISPLAY_MODE_SEEK;
        int rowOffset = (int) (Math.abs(offsetY) / mLrcFontSize);

        if (offsetY < 0) {
            mHighLightRow += rowOffset;
        } else if (offsetY > 0) {
            mHighLightRow -= rowOffset;
        }
        mHighLightRow = Math.max(0, mHighLightRow);
        mHighLightRow = Math.min(mHighLightRow, mLrcLines.size() - 1);
        if (rowOffset > 0) {
            mLastMotionY = y;
            invalidate();
        }
    }

    public void setListener(OnLrcListener listener) {
        mLrcViewListener = listener;
    }

    public void setLrc(List<LrcLine> lrcLines) {
        mLrcLines = lrcLines;
        invalidate();
    }

    public void setHighLightMode(@HighLightMode int mode) {
        this.mode = mode;
    }

    private void seekLrc(int position, boolean cb) {
        if (mLrcLines == null || position < 0 || position >= mLrcLines.size()) {
            return;
        }
        LrcLine lrcLine = mLrcLines.get(position);
        mHighLightRow = position;
        invalidate();
        if (mLrcViewListener != null && cb) {
            mLrcViewListener.onLrcSeek(position, lrcLine);
        }
    }

    public void seekToTime(long time) {
        if (mLrcLines == null || mLrcLines.isEmpty()) {
            return;
        }
        if (mDisplayMode != DISPLAY_MODE_NORMAL) {
            return;
        }
        currentMillis = time;

        for (int i = 0; i < mLrcLines.size(); i++) {
            LrcLine current = mLrcLines.get(i);
            LrcLine next = (i + 1 == mLrcLines.size()) ? null : mLrcLines.get(i + 1);
            if ((time >= current.getStartTime() && next != null && time < next.getStartTime()) ||
                    (time > current.getStartTime() && next == null)) {
                seekLrc(i, false);
                return;
            }
        }
    }

    private static final int mSeekLineTextSize = 25;
    private static final int mSeekLineColor = Color.RED;
    private static final int mSeekLineTextColor = Color.BLUE;
    public static final int DISPLAY_MODE_NORMAL = 0;
    public static final int DISPLAY_MODE_SEEK = 1;
    private static final int mMinSeekFiredOffset = 10;
    private static final String noLrcTip = "No lyrics...";
    public static final int MODE_HIGH_LIGHT_NORMAL = 0;
    public static final int MODE_HIGH_LIGHT_KARAOKE = 1;
}
