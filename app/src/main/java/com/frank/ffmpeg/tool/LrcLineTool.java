package com.frank.ffmpeg.tool;

import com.frank.ffmpeg.model.LrcLine;
import com.frank.ffmpeg.util.TimeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LrcLineTool {

    private LrcLineTool() {}

    private static List<LrcLine> createLine(String lrcLine) {
        try {
            if (lrcLine == null || lrcLine.isEmpty() ||
                    lrcLine.indexOf("[") != 0 || lrcLine.indexOf("]") != 9) {
                return null;
            }

            int lastIndexOfRightBracket = lrcLine.lastIndexOf("]");
            String content = lrcLine.substring(lastIndexOfRightBracket + 1);
            String times = lrcLine.substring(0, lastIndexOfRightBracket + 1).replace("[", "-")
                    .replace("]", "-");
            if (times.isEmpty() || !Character.isDigit(times.charAt(1))) return null;
            String[] arrTimes = times.split("-");
            List<LrcLine> listTimes = new ArrayList<>();
            for (String temp : arrTimes) {
                if (temp.trim().isEmpty()) {
                    continue;
                }
                LrcLine mLrcLine = new LrcLine();
                mLrcLine.setContent(content);
                mLrcLine.setTimeString(temp);
                long startTime = TimeUtil.timeStrToLong(temp);
                mLrcLine.setStartTime(startTime);
                listTimes.add(mLrcLine);
            }
            return listTimes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<LrcLine> getLrcLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        List<LrcLine> rows = new ArrayList<>();
        try {
            List<LrcLine> lrcLines = createLine(line);
            if (lrcLines != null && !lrcLines.isEmpty()) {
                rows.addAll(lrcLines);
            }
        } catch (Exception e) {
            return null;
        }
        return rows;
    }

    public static List<LrcLine> sortLyrics(List<LrcLine> lrcList) {
        Collections.sort(lrcList);
        if (!lrcList.isEmpty()) {
            int size = lrcList.size();
            for (int i = 0; i < size; i++) {
                LrcLine lrcLine = lrcList.get(i);
                if (i < size - 1) {
                    lrcLine.setEndTime(lrcList.get(i + 1).getStartTime());
                } else {
                    lrcLine.setEndTime(lrcLine.getStartTime() + 10 * 1000);
                }
            }
        }
        return lrcList;
    }
}
