package com.frank.ffmpeg.model;

/**
 * LRC line model representing a single line of lyrics with timing information
 */
public class LrcLine implements Comparable<LrcLine> {

    private String timeString;
    private long startTime;
    private long endTime;
    private String content;

    // Default constructor
    public LrcLine() {
    }

    // Constructor with parameters
    public LrcLine(String timeString, long startTime, long endTime, String content) {
        this.timeString = timeString;
        this.startTime = startTime;
        this.endTime = endTime;
        this.content = content;
    }

    // Getters and Setters
    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public int compareTo(LrcLine another) {
        if (another == null) {
            return 1;
        }
        return Long.compare(this.startTime, another.startTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LrcLine lrcLine = (LrcLine) obj;
        return startTime == lrcLine.startTime &&
                endTime == lrcLine.endTime &&
                (timeString != null ? timeString.equals(lrcLine.timeString) : lrcLine.timeString == null) &&
                (content != null ? content.equals(lrcLine.content) : lrcLine.content == null);
    }

    @Override
    public int hashCode() {
        int result = timeString != null ? timeString.hashCode() : 0;
        result = 31 * result + Long.hashCode(startTime);
        result = 31 * result + Long.hashCode(endTime);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LrcLine{" +
                "timeString='" + timeString + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", content='" + content + '\'' +
                '}';
    }
}
