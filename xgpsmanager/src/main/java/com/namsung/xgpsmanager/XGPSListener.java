package com.namsung.xgpsmanager;

import android.bluetooth.BluetoothDevice;

import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.data.SettingInfo;

import java.util.ArrayList;

/**
 * Created by cnapman on 2017. 11. 20..
 */

public interface XGPSListener {
    void connecting(BluetoothDevice device);
    void connected(boolean isConnect, int error);
    void updateGPSVoltage();
    void updateFirmwareInfo();
    void updateLocationInfo();
    void updateConfidenceInfo();
    void updateSatellitesInfoADSB();
    void updateSatellitesInfo(int systemId);
    void updateCalibrationInfo();
    void updateADSBStatus(boolean isSelected);
    void updateTrafficInfo();
    void updateSettings(SettingInfo info);
    void updateDrivingMode(int mode);
    // for skypro gps
    void getLogListComplete(ArrayList<LogData> logList);
    void getLogDetailProgress(final int bulkCount);
    void getLogDetailComplete(final ArrayList<LogBulkData> logBulkList);
    void throwException(Exception e);
    void getSelectedMountPoint(String mountpoint);
    void receiveNtripData(long length);
    void onNtripError(String error);
}
