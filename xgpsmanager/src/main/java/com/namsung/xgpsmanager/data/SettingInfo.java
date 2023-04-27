package com.namsung.xgpsmanager.data;

/**
 * Created by cnapman on 2018. 3. 6..
 */

public class SettingInfo {
    public final static int STREAM_MODE_NMEA = 0;
    public final static int STREAM_MODE_RXM = 1;
    public final static int STREAM_MODE_MIX = 2;

    public final static int LOG_TYPE_NATIVE = 0;
    public final static int LOG_TYPE_NMEA = 1;
    public final static int LOG_TYPE_GPX = 2;
    public final static int LOG_TYPE_KML = 3;
    public final static int LOG_TYPE_RXM = 4;


    private boolean isLogOverwrite;
    private boolean recordEnable;
    private int streamMode;
    private int logType;
    private int logInterval;
    private int gpsRefreshRate;

    public SettingInfo() {}

    public SettingInfo(boolean isLogOverwrite, boolean recordEnable, int streamMode, int logType, int logInterval, int gpsRefreshRate) {
        this.isLogOverwrite = isLogOverwrite;
        this.recordEnable = recordEnable;
        this.streamMode = streamMode;
        this.logType = logType;
        this.logInterval = logInterval;
        this.gpsRefreshRate = gpsRefreshRate;
    }

    public boolean isLogOverwrite() {
        return isLogOverwrite;
    }

    public boolean isRecordEnable() {
        return recordEnable;
    }

    public int getStreamMode() {
        return streamMode;
    }

    public int getLogType() {
        return logType;
    }

    public int getLogInterval() {
        return logInterval;
    }

    public void setRefreshRate(int rate) {
        gpsRefreshRate = rate;
    }

    public int getRefreshRate() {
        return gpsRefreshRate;
    }
}
