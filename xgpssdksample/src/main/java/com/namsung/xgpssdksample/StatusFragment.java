package com.namsung.xgpssdksample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.namsung.xgpsmanager.XGPSManager;
import com.namsung.xgpsmanager.data.SatellitesInfo;
import com.namsung.xgpsmanager.utils.Constants;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cnapman on 2018. 2. 5..
 */

public class StatusFragment extends BaseFragment {
    private static StatusFragment statusFragment = null;
    private TextView deviceName, batteryInfo, firmwareVersion, fixType, latitude, longitude, altitude, heading, speed, utc, satellites, glonass;

    public static StatusFragment newInstance(XGPSManager xgpsManager) {
        if (statusFragment == null) {
            statusFragment = new StatusFragment();
        }
        statusFragment.xgpsManager = xgpsManager;
        return statusFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        deviceName = (TextView)view.findViewById(R.id.tv_device);
        batteryInfo = (TextView)view.findViewById(R.id.tv_battery);
        firmwareVersion = (TextView)view.findViewById(R.id.tv_firmware);
        fixType = (TextView) view.findViewById(R.id.tv_fixType);
        latitude = (TextView)view.findViewById(R.id.tv_latitude);
        longitude = (TextView)view.findViewById(R.id.tv_longitude);
        altitude = (TextView)view.findViewById(R.id.tv_altitude);
        heading = (TextView)view.findViewById(R.id.tv_heading);
        speed = (TextView)view.findViewById(R.id.tv_speed);
        utc = (TextView)view.findViewById(R.id.tv_utc);
        satellites = (TextView)view.findViewById(R.id.tv_sv_list);
        glonass = (TextView)view.findViewById(R.id.tv_glonass_list);

        CheckBox mockGpsCheckBox = view.findViewById(R.id.cb_mock_gps);
        mockGpsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int hasWriteContactsPermission = getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                            buttonView.setChecked(false);
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MainActivity.REQUEST_CODE_PERMISSIONS_LOCATION);
                        }
                        else {
                            buttonView.setText("disable Mock GPS");
                            xgpsManager.setMockEnable(true);
                        }
                    }
                }
                else {
                    buttonView.setText("enable Mock GPS");
                    xgpsManager.setMockEnable(false);
                }
            }
        });

        disconnectState();
        return view;
    }

    private void disconnectState() {
        deviceName.setText(R.string.disconnected);
        batteryInfo.setText(R.string.unkown);
        firmwareVersion.setText(R.string.unkown);
        fixType.setText(R.string.no_fix);
        latitude.setText(R.string.unkown);
        longitude.setText(R.string.unkown);
        altitude.setText(R.string.unkown);
        heading.setText(R.string.unkown);
        speed.setText(R.string.unkown);
        utc.setText(R.string.unkown);
        satellites.setText(R.string.unkown);
        glonass.setText(R.string.unkown);
    }

    private void connectInitialState() {
        fixType.setText(R.string.no_fix);
        latitude.setText(R.string.waiting);
        longitude.setText(R.string.waiting);
        altitude.setText(R.string.waiting);
        heading.setText(R.string.waiting);
        speed.setText(R.string.waiting);
        utc.setText(R.string.waiting);
        satellites.setText(R.string.waiting);
        glonass.setText(R.string.waiting);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (xgpsManager != null && xgpsManager.isConnected()) {
            deviceName.setText(xgpsManager.getDeviceName());
            firmwareVersion.setText(xgpsManager.getFirmwareVersion());
            if (xgpsManager.isCharging()) {
                batteryInfo.setText("charging");
            }
            else {
                batteryInfo.setText(String.format("%d%%", (int)(xgpsManager.getBatteryLevel()*100)));
            }
        }
    }

    @Override
    public void connected(final boolean isConnect, final int error) {
        if (isConnect) {
            deviceName.setText(xgpsManager.getDeviceName());
            firmwareVersion.setText(xgpsManager.getFirmwareVersion());
            if (xgpsManager.isCharging()) {
                batteryInfo.setText("charging");
            } else {
                batteryInfo.setText(String.format("%d%%", (int) (xgpsManager.getBatteryLevel() * 100)));
            }
            connectInitialState();
        }
        else {
            disconnectState();
        }
    }

    @Override
    public void updatePositionInfo() {
        if (firmwareVersion.getText().equals(Constants.UNKNOWNSTRING))
            firmwareVersion.setText(xgpsManager.getFirmwareVersion());
        if (xgpsManager.isCharging()) {
            batteryInfo.setText("charging");
        }
        else {
            batteryInfo.setText(String.format("%d%%", (int)(xgpsManager.getBatteryLevel()*100)));
        }
        int type = xgpsManager.getFixType();
        if (type == XGPSManager.TYPE_2D_FIX)
            fixType.setText(R.string.two_d_fix);
        else if (type == XGPSManager.TYPE_3D_FIX)
            fixType.setText(R.string.three_d_fix);
        else
            fixType.setText(R.string.no_fix);
        latitude.setText(xgpsManager.getLatitude(XGPSManager.MODE_POSITION_DEGREE));
        longitude.setText(xgpsManager.getLongitude(XGPSManager.MODE_POSITION_DEGREE));
        altitude.setText(xgpsManager.getAltitude(Constants.MODE_ALTITUDE_FEET));
        heading.setText(xgpsManager.getHeadingString());
        speed.setText(xgpsManager.getSpeed(Constants.MODE_SPEED_KPH));
        utc.setText(xgpsManager.getUTC());
    }

    @Override
    public void updateSatellitesInfo() {
        String numberOfSatellites = "";
        ConcurrentHashMap<Integer, SatellitesInfo> satellitesMap = xgpsManager.getSatellitesMap();
        Iterator<Integer> it = satellitesMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            if (numberOfSatellites.length() > 0) {
                numberOfSatellites += ", ";
            }
            numberOfSatellites += key;
        }
        satellites.setText(numberOfSatellites);

        ConcurrentHashMap<Integer, SatellitesInfo> glonassMap = xgpsManager.getGlonassSatellitesMap();
        numberOfSatellites = "";
        it = glonassMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            if (numberOfSatellites.length() > 0) {
                numberOfSatellites += ", ";
            }
            numberOfSatellites += key;
        }
        glonass.setText(numberOfSatellites);
    }
}
