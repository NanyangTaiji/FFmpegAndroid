package com.frank.mp3;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingDeque;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class Mp3Converter {

    private static final String TAG = Mp3Converter.class.getSimpleName();
    private static final int DEFAULT_QUEUE_SIZE = 512;

    private MediaCodec mMediaCodec;
    private MediaExtractor mediaExtractor;
    private BufferInfo bufferInfo;
    private ByteBuffer[] rawInputBuffers;
    private ByteBuffer[] encodedOutputBuffers;
    private int inSampleRate = 0;
    private int channels = 0;
    private final LinkedBlockingDeque<BufferEncoded> writeQueue = new LinkedBlockingDeque<>(DEFAULT_QUEUE_SIZE);
    private final LinkedBlockingDeque<BufferDecoded> encodeQueue = new LinkedBlockingDeque<>(DEFAULT_QUEUE_SIZE);
    private byte[] mp3buf;

    private boolean decodeFinished = false;
    private boolean encodeFinished = false;

    private long readSize = 0;
    private long decodeSize = 0;
    private long encodeSize = 0;

    private Mp3Lame mp3Lame;
    private WriteThread writeThread;
    private EncodeThread encodeThread;
    private long lastPts = 0;

    private class BufferDecoded {
        int channels = 0;
        short[] leftBuffer;
        short[] rightBuffer;
        short[] pcm;
        long pts = 0;
    }

    private class BufferEncoded {
        byte[] buffer;
        int length = 0;
    }

    private class WriteThread extends Thread {
        private final String mp3Path;
        private FileOutputStream fos;
        private BufferedOutputStream bos;

        WriteThread(String mp3Path) {
            this.mp3Path = mp3Path;
        }

        @Override
        public void run() {
            try {
                Log.i(TAG, "WriteThread start");

                fos = new FileOutputStream(new File(mp3Path));
                bos = new BufferedOutputStream(fos, 200 * 1024);

                while (true) {
                    if (encodeFinished && writeQueue.size() == 0) {
                        break;
                    }

                    BufferEncoded buffer = null;
                    try {
                        buffer = writeQueue.take();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "WriteThread InterruptedException=" + e);
                    }

                    if (buffer != null) {
                        bos.write(buffer.buffer, 0, buffer.length);
                    }
                }
                bos.flush();
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null) {
                        bos.flush();
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.i(TAG, "WriteThread end");
        }
    }

    private class EncodeThread extends Thread {
        @Override
        public void run() {
            Log.i(TAG, "EncodeThread start");

            while (true) {
                if (decodeFinished && encodeQueue.size() == 0) {
                    break;
                }
                BufferDecoded buffer = null;
                try {
                    buffer = encodeQueue.take();
                } catch (InterruptedException e) {
                    Log.e(TAG, "EncodeThread InterruptedException=" + e);
                }

                if (buffer != null) {
                    encodeToMp3(buffer);
                }
            }
            encodeFinished = true;

            if (writeThread != null) {
                writeThread.interrupt();
            }

            Log.i(TAG, "EncodeThread end");
        }
    }

    private class DecodeThread extends Thread {
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                Log.i(TAG, "DecodeThread start");

                while (true) {
                    int outputBufIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, -1);
                    if (outputBufIndex >= 0) {
                        ByteBuffer buffer = encodedOutputBuffers[outputBufIndex];
                        decodeSize += bufferInfo.size;
                        buffer.order(ByteOrder.nativeOrder());
                        java.nio.ShortBuffer shortBuffer = buffer.asShortBuffer();

                        short[] leftBuffer = null;
                        short[] rightBuffer = null;
                        short[] pcm = null;

                        if (channels == 2) {
                            pcm = new short[shortBuffer.remaining()];
                            shortBuffer.get(pcm);
                        } else {
                            leftBuffer = new short[shortBuffer.remaining()];
                            rightBuffer = leftBuffer;
                            shortBuffer.get(leftBuffer);
                            Log.e(TAG, "single channel leftBuffer.length = " + leftBuffer.length);
                        }

                        buffer.clear();

                        BufferDecoded bufferDecoded = new BufferDecoded();
                        bufferDecoded.leftBuffer = leftBuffer;
                        bufferDecoded.rightBuffer = rightBuffer;
                        bufferDecoded.pcm = pcm;
                        bufferDecoded.channels = channels;
                        bufferDecoded.pts = bufferInfo.presentationTimeUs;
                        encodeQueue.put(bufferDecoded);

                        mMediaCodec.releaseOutputBuffer(outputBufIndex, false);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.e(TAG, "DecodeThread get BUFFER_FLAG_END_OF_STREAM");
                            decodeFinished = true;
                            break;
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        encodedOutputBuffers = mMediaCodec.getOutputBuffers();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat oformat = mMediaCodec.getOutputFormat();
                        Log.d(TAG, "Output format has changed to " + oformat);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (encodeThread != null) {
                encodeThread.interrupt();
            }
            long endTime = System.currentTimeMillis();
            Log.i(TAG, "DecodeThread finished time=" + (endTime - startTime) / 1000);
        }
    }

    public void convertToMp3(String srcPath, String mp3Path) {

        long startTime = System.currentTimeMillis();
        long endTime;

        encodeThread = new EncodeThread();
        writeThread = new WriteThread(mp3Path);
        DecodeThread decodeThread = new DecodeThread();

        encodeThread.start();
        writeThread.start();
        prepareDecode(srcPath);
        decodeThread.start();
        readSampleData();

        try {
            writeThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "convertToMp3 InterruptedException=" + e);
        }

        double mReadSize = readSize / 1024.0 / 1024.0;
        double mDecodeSize = decodeSize / 1024.0 / 1024.0;
        double mEncodeSize = encodeSize / 1024.0 / 1024.0;
        Log.i(TAG, "readSize=" + mReadSize + ", decodeSize=" + mDecodeSize + ",encodeSize=" + mEncodeSize);

        endTime = System.currentTimeMillis();
        Log.i(TAG, "convertToMp3 finish time=" + (endTime - startTime) / 1000);
    }

    private void prepareDecode(String path) {
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(path);
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat mMediaFormat = mediaExtractor.getTrackFormat(i);
                Log.i(TAG, "prepareDecode get mMediaFormat=" + mMediaFormat);

                String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    mMediaCodec = MediaCodec.createDecoderByType(mime);
                    mMediaCodec.configure(mMediaFormat, null, null, 0);
                    mediaExtractor.selectTrack(i);
                    inSampleRate = mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    break;
                }
            }
            mMediaCodec.start();

            bufferInfo = new BufferInfo();
            rawInputBuffers = mMediaCodec.getInputBuffers();
            encodedOutputBuffers = mMediaCodec.getOutputBuffers();
            Log.i(TAG, "--channel=" + channels + "--sampleRate=" + inSampleRate);

            mp3Lame = new Mp3LameBuilder()
                    .setInSampleRate(inSampleRate)
                    .setOutChannels(channels)
                    .setOutBitrate(128)
                    .setOutSampleRate(inSampleRate)
                    .setQuality(9)
                    .setVbrMode(Mp3LameBuilder.VbrMode.VBR_MTRH)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSampleData() {
        boolean rawInputEOS = false;

        while (!rawInputEOS) {
            for (int i = 0; i < rawInputBuffers.length; i++) {
                int inputBufIndex = mMediaCodec.dequeueInputBuffer(-1);
                if (inputBufIndex >= 0) {
                    ByteBuffer buffer = rawInputBuffers[inputBufIndex];
                    int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        rawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        readSize += sampleSize;
                        presentationTimeUs = mediaExtractor.getSampleTime();
                    }
                    mMediaCodec.queueInputBuffer(inputBufIndex, 0,
                            sampleSize, presentationTimeUs,
                            rawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!rawInputEOS) {
                        mediaExtractor.advance();
                    } else {
                        break;
                    }
                } else {
                    Log.e(TAG, "wrong inputBufIndex=" + inputBufIndex);
                }
            }
        }
    }

    private void encodeToMp3(BufferDecoded buffer) {

        if (buffer == null || buffer.pts == lastPts) {
            return;
        }
        lastPts = buffer.pts;

        int bufferLength = buffer.pcm.length / 2;
        if (mp3buf == null) {
            mp3buf = new byte[(int) (bufferLength * 1.25 + 7200)];
        }
        if (bufferLength > 0) {
            int bytesEncoded;
            if (channels == 2) {
                bytesEncoded = mp3Lame.encodeBufferInterLeaved(buffer.pcm, bufferLength, mp3buf);
            } else {
                bytesEncoded = mp3Lame.encode(buffer.leftBuffer, buffer.leftBuffer, bufferLength, mp3buf);
            }
            Log.d(TAG, "mp3Lame encodeSize=" + bytesEncoded);

            if (bytesEncoded > 0) {
                BufferEncoded be = new BufferEncoded();
                be.buffer = mp3buf;
                be.length = bytesEncoded;
                try {
                    writeQueue.put(be);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                encodeSize += bytesEncoded;
            }
        }
    }
}
