package com.frank.ffmpeg.tool;

import com.frank.ffmpeg.model.LrcInfo;
import com.frank.ffmpeg.model.LrcLine;
import com.frank.ffmpeg.util.TimeUtil;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class LrcParser {
    private final LrcInfo lrcInfo = new LrcInfo();
    private final ArrayList<LrcLine> lrcLineList = new ArrayList<>();

    public LrcInfo readLrc(String path) {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            String charsetName = getCharsetName(path);
            inputStream = new FileInputStream(path);
            if (charsetName.toLowerCase(Locale.getDefault()).equals("utf-8")) {
                inputStream = new UnicodeInputStream(inputStream, charsetName);
                charsetName = ((UnicodeInputStream) inputStream).getEncoding();
            }
            inputStreamReader = charsetName != null && !charsetName.isEmpty() ?
                    new InputStreamReader(inputStream, charsetName) :
                    new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            while (true) {
                String str = reader.readLine();
                if (str == null) break;
                if (!str.isEmpty()) {
                    decodeLine(str);
                }
            }
            lrcInfo.setLrcLineList(lrcLineList);
            return lrcInfo;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (inputStreamReader != null) inputStreamReader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getCharsetName(String path) {
        String code = "GBK";
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream(new FileInputStream(path));
            switch ((bin.read() << 8) + bin.read()) {
                case 0xefbb:
                    code = "UTF-8";
                    break;
                case 0xfffe:
                    code = "Unicode";
                    break;
                case 0xfeff:
                    code = "UTF-16BE";
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return code;
    }

    private boolean match(String line) {
        return line.length() > 4 && line.lastIndexOf("]") > 4;
    }

    private void decodeLine(String str) {
        if (str.startsWith("[ti:")) {
            if (match(str))
                lrcInfo.setTitle(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[ar:")) {
            if (match(str))
                lrcInfo.setArtist(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[al:")) {
            if (match(str))
                lrcInfo.setAlbum(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[au:")) {
            if (match(str))
                lrcInfo.setAuthor(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[by:")) {
            if (match(str))
                lrcInfo.setCreator(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[re:")) {
            if (match(str))
                lrcInfo.setEncoder(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[ve:")) {
            if (match(str))
                lrcInfo.setVersion(str.substring(4, str.lastIndexOf("]")));
        } else if (str.startsWith("[offset:")) {
            if (str.lastIndexOf("]") > 8) {
                String offset = str.substring(8, str.lastIndexOf("]"));
                try {
                    lrcInfo.setOffset(Integer.parseInt(offset));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else {
            String timeExpress = "\\[(\\d{1,2}:\\d{1,2}\\.\\d{1,2})]|\\[(\\d{1,2}:\\d{1,2})]";
            Pattern pattern = Pattern.compile(timeExpress);
            java.util.regex.Matcher matcher = pattern.matcher(str);
            long currentTime = 0;
            while (matcher.find()) {
                int groupCount = matcher.groupCount();
                String currentTimeStr = "";
                for (int index = 0; index < groupCount; index++) {
                    String timeStr = matcher.group(index);
                    if (index == 0) {
                        currentTimeStr = timeStr.substring(1, timeStr.length() - 1);
                        currentTime = TimeUtil.timeStrToLong(currentTimeStr);
                    }
                }
                String[] content = pattern.split(str);
                String currentContent = "";
                if (content.length > 0) {
                    currentContent = content[content.length - 1];
                }
                LrcLine lrcLine = new LrcLine();
                lrcLine.setTimeString(currentTimeStr);
                lrcLine.setStartTime(currentTime);
                lrcLine.setEndTime(currentTime + 10 * 1000);
                lrcLine.setContent(currentContent);
                lrcLineList.add(lrcLine);
            }
        }
    }
}
