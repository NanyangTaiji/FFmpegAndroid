package com.frank.ffmpeg.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtil {

    private static final String YMDHMS = "yyyy-MM-dd HH:mm:ss";

    private TimeUtil() {}

    public static String getDetailTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat(YMDHMS, Locale.getDefault());
        Date date = new Date(time);
        return format.format(date);
    }

    public static long getLongTime(String time, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(YMDHMS, locale);
        try {
            Date dt = simpleDateFormat.parse(time);
            return dt.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static String addZero(int time) {
        if (time >= 0 && time <= 9) {
            return "0" + time;
        } else if (time >= 10) {
            return String.valueOf(time);
        }
        return "";
    }

    public static String getVideoTime(long t) {
        long time = t;
        if (time <= 0)
            return null;
        time /= 1000;
        int second;
        int minute = 0;
        int hour = 0;
        second = (int) (time % 60);
        time /= 60;
        if (time > 0) {
            minute = (int) (time % 60);
            hour = (int) (time / 60);
        }
        if (hour > 0) {
            return addZero(hour) + ":" + addZero(minute) + ":" + addZero(second);
        } else if (minute > 0) {
            return addZero(minute) + ":" + addZero(second);
        } else {
            return "00:" + addZero(second);
        }
    }

    public static long timeStrToLong(String timeStr) {
        String timeString = timeStr.replace('.', ':');
        String[] times = timeString.split(":");
        return (Integer.valueOf(times[0]) * 60 * 1000 +
                Integer.valueOf(times[1]) * 1000 +
                Integer.valueOf(times[2]));
    }
}
