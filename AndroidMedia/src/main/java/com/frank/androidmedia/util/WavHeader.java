package com.frank.androidmedia.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The header of wave format
 * @author frank
 * @date 2022/3/22
 */
public class WavHeader {
    public char[] riffID = {'R', 'I', 'F', 'F'};
    public int riffSize = 0;
    public char[] riffType = {'W', 'A', 'V', 'E'};
    public char[] formatID = {'f', 'm', 't', ' '};
    public int formatSize = 0;
    public short formatTag = 0;
    public short numChannels = 0;
    public int sampleRate = 0;
    public int avgBytesPerSec = 0;
    public short blockAlign = 0;
    public short bitsPerSample = 0;
    public char[] dataID = {'d', 'a', 't', 'a'};
    public int dataSize = 0;

    public byte[] getHeader() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeChar(bos, riffID);
        writeInt(bos, riffSize);
        writeChar(bos, riffType);
        writeChar(bos, formatID);
        writeInt(bos, formatSize);
        writeShort(bos, formatTag);
        writeShort(bos, numChannels);
        writeInt(bos, sampleRate);
        writeInt(bos, avgBytesPerSec);
        writeShort(bos, blockAlign);
        writeShort(bos, bitsPerSample);
        writeChar(bos, dataID);
        writeInt(bos, dataSize);
        bos.flush();
        byte[] r = bos.toByteArray();
        bos.close();
        return r;
    }

    private void writeShort(ByteArrayOutputStream bos, short s) throws IOException {
        byte[] data = new byte[2];
        data[1] = (byte) ((s << 16) >> 24);
        data[0] = (byte) ((s << 24) >> 24);
        bos.write(data);
    }

    private void writeInt(ByteArrayOutputStream bos, int n) throws IOException {
        byte[] buf = new byte[4];
        buf[3] = (byte) (n >> 24);
        buf[2] = (byte) ((n << 8) >> 24);
        buf[1] = (byte) ((n << 16) >> 24);
        buf[0] = (byte) ((n << 24) >> 24);
        bos.write(buf);
    }

    private void writeChar(ByteArrayOutputStream bos, char[] id) throws IOException {
        for (char c : id) {
            bos.write(c);
        }
    }
}
