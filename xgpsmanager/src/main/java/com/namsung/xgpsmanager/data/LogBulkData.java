package com.namsung.xgpsmanager.data;

/**
 * Created by cnapman on 2017. 11. 24..
 */

public class LogBulkData {
    private String date;
    private Float latitude;
    private Float longitude;
    private Float altitude;
    private long speed;
    private int tod;
    private String todString;

    public LogBulkData(String date, Float latitude, Float longitude, Float altitude, long speed, int tod, String todString) {
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.tod = tod;
        this.todString = todString;
    }

    public String getDate() {
        return date;
    }

    public String getTodString() {
        return todString;
    }

    public int getTod() {
        return tod;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public Float getAltitude() {
        return altitude;
    }

    public long getSpeed() {
        return speed;
    }
}
