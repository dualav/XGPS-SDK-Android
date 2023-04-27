package com.namsung.xgpsmanager;
import android.location.Location;

import java.util.ArrayList;

/**
 * Created by cnapman on 2018. 4. 16..
 */

public class NtripClient {
    private static ArrayList<String> mMountPoints = new ArrayList<>();
    private static ArrayList<Location> mMountPointLocations = new ArrayList<>();
    static {
        System.loadLibrary("NtripTool");
    }

    public static void addMountPoint(String mountpoint, Location location) {
        mMountPoints.add(mountpoint);
        mMountPointLocations.add(location);
    }

    public static ArrayList<String> getMountPoinsts() {
        return mMountPoints;
    }

    public static ArrayList<Location> getMountPointLocations() {
        return mMountPointLocations;
    }

    public static native void onCreate(BluetoothGpsManager manager, String host, String port, String user, String password, int mode, String mountpoint);
    public static native void onDestroy();
    public static native boolean isReady();
}
