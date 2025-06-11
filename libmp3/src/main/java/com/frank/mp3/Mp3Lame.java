package com.frank.mp3;

public class Mp3Lame {

    Mp3Lame(Mp3LameBuilder builder) {
        initialize(builder);
    }

    private void initialize(Mp3LameBuilder builder) {
        Mp3Lite.lameInit(builder.inSampleRate, builder.outChannel, builder.outSampleRate,
                builder.outBitrate, builder.scaleInput, getIntForMode(builder.mode),
                getIntForVbrMode(builder.vbrMode), builder.quality, builder.vbrQuality,
                builder.abrMeanBitrate, builder.lowPassFreq, builder.highPassFreq,
                builder.id3tagTitle, builder.id3tagArtist, builder.id3tagAlbum,
                builder.id3tagYear, builder.id3tagComment);
    }

    public int encode(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf) {
        return Mp3Lite.lameEncode(buffer_l, buffer_r, samples, mp3buf);
    }

    int encodeBufferInterLeaved(short[] pcm, int samples, byte[] mp3buf) {
        return Mp3Lite.encodeBufferInterleaved(pcm, samples, mp3buf);
    }

    public int flush(byte[] mp3buf) {
        return Mp3Lite.lameFlush(mp3buf);
    }

    public void close() {
        Mp3Lite.lameClose();
    }

    private int getIntForMode(Mp3LameBuilder.Mode mode) {
        switch (mode) {
            case STEREO:
                return 0;
            case JSTEREO:
                return 1;
            case MONO:
                return 3;
            case DEFAULT:
            default:
                return 4;
        }
    }

    private int getIntForVbrMode(Mp3LameBuilder.VbrMode mode) {
        switch (mode) {
            case VBR_OFF:
                return 0;
            case VBR_RH:
                return 2;
            case VBR_ABR:
                return 3;
            case VBR_MTRH:
                return 4;
            case VBR_DEFAUT:
            default:
                return 6;
        }
    }
}
