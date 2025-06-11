package com.frank.androidmedia.util;

import android.util.Log;
import java.io.*;

/**
 * Convert pcm to wav
 *
 * @author frank
 * @date 2022/3/22
 */
public class WavUtil {
    private static final String TAG = "WavUtil";

    public static boolean makePCMToWAVFile(String pcmPath, String wavPath, boolean deletePcmFile) {
        byte[] buffer;
        File file = new File(pcmPath);
        if (!file.exists()) {
            return false;
        }

        int len = (int) file.length();
        WavHeader header = new WavHeader();
        header.riffSize = len + (44 - 8);
        header.formatSize = 16;
        header.bitsPerSample = 16;
        header.numChannels = 2;
        header.formatTag = 0x0001;
        header.sampleRate = 44100;
        header.blockAlign = (short) (header.numChannels * header.bitsPerSample / 8);
        header.avgBytesPerSec = header.blockAlign * header.sampleRate;
        header.dataSize = len;

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.e(TAG, e1.getMessage());
            return false;
        }

        if (h.length != 44) return false;

        File dstFile = new File(wavPath);
        if (dstFile.exists()) dstFile.delete();

        try {
            buffer = new byte[1024 * 4];
            InputStream inStream;
            OutputStream ouStream;
            ouStream = new BufferedOutputStream(new FileOutputStream(wavPath));
            ouStream.write(h, 0, h.length);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            while (size != -1) {
                ouStream.write(buffer, 0, size);
                size = inStream.read(buffer);
            }
            inStream.close();
            ouStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }

        if (deletePcmFile) {
            file.delete();
        }

        Log.i(TAG, "makePCMToWAVFile success...");
        return true;
    }
}
