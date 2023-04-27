package com.namsung.xgpsmanager.utils;

/**
 * Created by hjlee on 09/07/2019.
 */
public class Tools {

    private static double deg2rad(double deg) {
        return deg * Math.PI / 180.0;
    }

    private static double rad2deg(double rad) {
        return rad * 180.0 / Math.PI;
    }

    public static double getDistance (double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit.equals("K")) {
            dist = dist * 1.609344;
        }
        else if (unit.equals("N")) {
            dist = dist * 0.8684;
        }
        return dist;
    }
}
