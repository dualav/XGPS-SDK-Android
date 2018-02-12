package com.namsung.xgpssdksample;

import android.support.v4.app.Fragment;

import com.namsung.xgpsmanager.XGPSManager;
import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;

import java.util.ArrayList;

/**
 * Created by cnapman on 2017. 11. 20..
 */

public class BaseFragment extends Fragment {
    protected XGPSManager xgpsManager;

    public void onBackPressed() {}

    public void connected(boolean isConnect, int error) {}
    public void updatePositionInfo() {}
    public void updateSatellitesInfo() {}
    public void updateSettings(boolean positionEnable, boolean overWrite) {}
    public void getLogListComplete(ArrayList<LogData> logList) {}
    public void getLogDetailProgress(int bulkCount) {}
    public void getLogDetailComplete(ArrayList<LogBulkData> logBulkList) {}
}
