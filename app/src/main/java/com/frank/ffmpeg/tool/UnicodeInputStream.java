package com.frank.ffmpeg.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/*
FileInputStream fileStream = new FileInputStream("unknown-encoding.txt");
UnicodeInputStream unicodeStream = new UnicodeInputStream(fileStream, "UTF-8");

// Get the detected encoding
String encoding = unicodeStream.getEncoding();
System.out.println("Detected encoding: " + encoding);

// Now read the content using the detected encoding
InputStreamReader reader = new InputStreamReader(unicodeStream, encoding);
// Read text content normally...
 */

public class UnicodeInputStream extends InputStream {

    private static final int BOM_SIZE = 4;

    private String encoding;
    private boolean isInitialed = false;
    private final PushbackInputStream internalIn;
    private final String defaultEnc;

    public UnicodeInputStream(InputStream in, String defaultEnc) {
        this.defaultEnc = defaultEnc;
        internalIn = new PushbackInputStream(in, BOM_SIZE);
    }

    public String getEncoding() {
        if (!isInitialed) {
            try {
                init();
            } catch (IOException ex) {
                throw new IllegalStateException("getEncoding error=" + ex.getMessage());
            }
        }
        return encoding;
    }

    /**
     * Read-ahead four bytes and check for BOM marks. Extra bytes are unread
     * back to the stream, only BOM bytes are skipped.
     */
    private void init() throws IOException {
        if (isInitialed) {
            return;
        }

        byte[] bom = new byte[BOM_SIZE];
        int n = internalIn.read(bom, 0, bom.length);
        int unread;

        if (bom[0] == (byte) 0x00 && bom[1] == (byte) 0x00
                && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF) {
            encoding = "UTF-32BE";
            unread = n - 4;
        } else if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE
                && bom[2] == (byte) 0x00 && bom[3] == (byte) 0x00) {
            encoding = "UTF-32LE";
            unread = n - 4;
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB
                && bom[2] == (byte) 0xBF) {
            encoding = "UTF-8";
            unread = n - 3;
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF) {
            encoding = "UTF-16BE";
            unread = n - 2;
        } else if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            encoding = "UTF-16LE";
            unread = n - 2;
        } else {
            encoding = defaultEnc;
            unread = n;
        }

        if (unread > 0) {
            internalIn.unread(bom, n - unread, unread);
        }
        isInitialed = true;
    }

    @Override
    public void close() throws IOException {
        isInitialed = false;
        internalIn.close();
    }

    @Override
    public int read() throws IOException {
        if (!isInitialed) {
            init();
        }
        return internalIn.read();
    }
}
