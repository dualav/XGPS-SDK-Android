package com.namsung.xgpsmanager.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by cnapman on 2017. 11. 24..
 */

public class LogData {
    private int sig;
    private int interval;
    private int startBlock;
    private int countEntry;
    private int countBlock;
    private String createDate;
    private String createTime;
    private String defaultFilename;
    private String localDateTimeString;
    private String localFilename = null;

    public LogData(int sig, int interval, int startBlock, int countEntry, int countBlock, String createDate, String createTime) {
        this.sig = sig;
        this.interval = interval;
        this.startBlock = startBlock;
        this.countEntry = countEntry;
        this.countBlock = countBlock;
        this.createDate = createDate;
        this.createTime = createTime;
    }

    private String utcToLocalDateTime(String date, String time, String pattern) {
        if (date == null || time == null || pattern == null)
            return null;
        String localTime = "";
        // utc -> device Time
        String utcTime = date+time;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date localDateTime = dateFormat.parse(utcTime);
            SimpleDateFormat localDateFormat = new SimpleDateFormat(pattern);
            localTime = localDateFormat.format(localDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return localTime;
    }

    public String getCreateDate() {
        return createDate;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getLocalTimeString() {
        if (localDateTimeString == null) {
            localDateTimeString = utcToLocalDateTime(createDate, createTime, "yyyy/MM/dd  HH:mm:ss");
        }
        return localDateTimeString;
    }

    public int getCountEntry() {
        return countEntry;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public int getCountBlock() {
        return countBlock;
    }

    public byte[] getRequestBuffer() {
        byte[] buffer = new byte[4];

        buffer[0] = (byte) (startBlock >> 8);
        buffer[1] = (byte) startBlock;
        buffer[2] = (byte) (countBlock >> 8);
        buffer[3] = (byte) countBlock;

        return buffer;
    }

    public void setLocalFilename(String filename) {
        this.localFilename = filename;
    }

    public String getLocalFilename() {
        return localFilename;
    }

    public String getDefaultFilename() {
        if (defaultFilename == null)
            defaultFilename = utcToLocalDateTime(createDate, createTime, "yyyyMMdd_HHmmss");
        return defaultFilename;
    }

    public int getInterval() {
        return interval;
    }
}
