package com.namsung.xgpsmanager;

import android.os.Handler;
import android.os.Looper;

import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.data.SatellitesInfo;
import com.namsung.xgpsmanager.data.SatellitesInfoMap;
import com.namsung.xgpsmanager.utils.Constants;
import com.namsung.xgpsmanager.utils.DLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by hjlee on 2018. 8. 20..
 */

public class XGPSParser {
    private static final String TAG = XGPSParser.class.getSimpleName();

    public static final int XGPS_MODE_1 = 1; 	// for 150
    public static final int XGPS_MODE_2 = 2;	// for 160
    public static final int XGPS_MODE_3 = 3;    // for 500
    public static final int XGPS_MODE_4 = 4;    // else (190, 170)

    public final static int TYPE_NO_FIX = 1;
    public final static int TYPE_2D_FIX = 2;
    public final static int TYPE_3D_FIX = 3;

    private NmeaParser mNmeaParser;
    private XGPSListener mXgpsListener;
    private boolean mIsADSBMode = false;
    private static XGPSParser instance;
    private volatile BaseParser mCurrentParser;

    public static XGPSParser getInstance() {
        if (instance == null) {
            instance = new XGPSParser();
        }
        return instance;
    }

    private XGPSParser() {
        mNmeaParser = new NmeaParser();
    }

    private LogData selectedLogData;
    public void setLogData(LogData data) {
        selectedLogData = data;
        if (mNmeaParser != null) {
            mNmeaParser.setLogData(selectedLogData);
        }
    }

    public void setListener(XGPSListener listener) {
        mXgpsListener = listener;
        mNmeaParser.setListener(listener);
    }

    public void setParserMode(String modelName) {
        int parserMode = XGPS_MODE_4;
        if (modelName.contains("XGPS150"))
            parserMode = XGPS_MODE_1;
        else if (modelName.contains("XGPS160"))
            parserMode = XGPS_MODE_2;
        else if (modelName.contains("XGPS500") || modelName.startsWith("XGPS360")  || modelName.startsWith("DashPro"))
            parserMode = XGPS_MODE_3;
        mNmeaParser.setParserMode(parserMode);
    }


    public int getParserMode() {
        return mNmeaParser.getParserMode();
    }

    public void parserBridge(byte[] buffer, int length) {
        try {
            // TEST CODE
//            DLog.e(new String(buffer).substring(0, length));
//            String str = "";
//            for (int i = 0; i < length; i++) {
//                str += String.format("0x%x ", buffer[i]);
//            }
//            DLog.e(str);
            mNmeaParser.handleInputStream(buffer, length);
            mCurrentParser = mNmeaParser;
            mIsADSBMode = false;
        }catch(Exception e) {
            DLog.e("parsing error : " + e.getMessage());
        }
    }

    public void requestFirmwareVersion() {
        if (mCurrentParser == null || !CommonValue.getInstance().BT_CONNECTED) {
            return;
        }
        if (CommonValue.getInstance().gpsManager != null)
            CommonValue.getInstance().gpsManager.sendCommandToDevice(Constants.cmd160_fwVersion, null, 0);
    }

    public boolean IsAdsbMode() {
        return mIsADSBMode;
    }

    public void setResponse(int value) {
        mNmeaParser.setResponse(value);
    }

    public int getResponse() {
        return mNmeaParser.getResponse();
    }

    // interface for others

    private BaseParser getCurrentParser() {
        if(mCurrentParser == null)
            mCurrentParser = mNmeaParser;
        return mCurrentParser;
    }

    public String getFirmwareVersion() {
        return getCurrentParser().getFirmwareVersion();
    }

    public int avgUsableSatSNR() {
        return getCurrentParser().avgUsableSatSNR();
    }

    public SatellitesInfoMap getSatellitesInfoMap() {
        return getCurrentParser().getSatellitesInfoMap();
    }

    public HashMap<Integer, SatellitesInfo> getDictOfSatInfo(int systemId) {
        return getCurrentParser().getDictOfSatInfo(systemId, 1);
    }

    public ArrayList<String> getSatsUsedInPosCalc(int systemId) {
        return getCurrentParser().getSatsUsedInPosCalc(systemId);
    }

    public HashMap<Integer, SatellitesInfo> getDictOfSatGlonassInfo() {
        return getCurrentParser().getDictOfSatGlonassInfo();
    }

    public ArrayList<String> getSatsUsedInPosCalcGlonass() {
        return getCurrentParser().getSatsUsedInPosCalcGlonass();
    }

    public int getFixType() {
        return getCurrentParser().getFixType();
    }

    public float getLatitude() {
        return getCurrentParser().getLatitude();
    }

    public float getLongitude() {
        return getCurrentParser().getLongitude();
    }

    public String getLatitudeString() {
        return getCurrentParser().getLatitudeString();
    }

    public int getLatitudeDegree() {
        return getCurrentParser().getLatitudeDegree();
    }

    public float getLatitudeMins() {
        return getCurrentParser().getLatitudeMins();
    }

    public String getLatitudeDirection() {
        return getCurrentParser().getLatitudeDirection();
    }

    public int getLatitudeSign() {
        if ("S".equals(getCurrentParser().getLatitudeDirection()))
            return -1;
        return 1;
    }

    public int getLongitudeSign() {
        if ("W".equals(getCurrentParser().getLongitudeDirection()))
            return -1;
        return 1;
    }

    public String getLongitudeString() {
        return getCurrentParser().getLongitudeString();
    }

    public int getLongitudeDegree() {
        return getCurrentParser().getLongitudeDegree();
    }

    public float getLongitudeMins() {
        return getCurrentParser().getLongitudeMins();
    }

    public String getLongitudeDirection() {
        return getCurrentParser().getLongitudeDirection();
    }

    public String getTime() {
        return getCurrentParser().getTime();
    }

    public String getAltitude() {
        return getCurrentParser().getAltitude();
    }

    public String getSpeed() {
        return getCurrentParser().getSpeed();
    }

    public String getHeading() {
        return getCurrentParser().getHeading();
    }

    public boolean getIsCharging() {
        return getCurrentParser().getIsCharging();
    }

    public boolean getIsDGPS() {
        return getCurrentParser().getIsDGPS();
    }

    public float getPdop() {
        return getCurrentParser().getPdop();
    }

    public float getVdop() {
        return getCurrentParser().getVdop();
    }

    public float getHdop() {
        return getCurrentParser().getHdop();
    }

    public int getSatellitesNum() {
        return getCurrentParser().getSatellitesNum();
    }

    public float getBatteryVoltage() {
        return getCurrentParser().getBatteryVoltage();
    }

    public int getChargingCurrent() {
        return getCurrentParser().getChargingCurrent();
    }

    public int getDeltaX() {
        return getCurrentParser().getDeltaX();
    }

    public int getDeltaY() {
        return getCurrentParser().getDeltaY();
    }

    public void setCalResultFlags(int flag) {
        getCurrentParser().setCalResultFlags(flag);
    }

    public int getCalResultFlags() {
        return getCurrentParser().getCalResultFlags();
    }

    public float getCalFieldStrength() {
        return getCurrentParser().getCalFieldStrength();
    }

    public void initNMEAParser() {
        mNmeaParser.initNMEAParser();
    }

    public byte[] getFwDataRead() {
        return mNmeaParser.pFwReadBuf;
    }

    public void initFwDataRead() {
        mNmeaParser.pFwReadBuf = null;
    }

    public void resetDevice(byte[] buf) {
//        parser.firmwareVerString = Constants.UNKNOWNSTRING;
        if( mNmeaParser.rsp160_cmd == Constants.cmd160_fwRsp && mNmeaParser.rsp160_buf[0] == Constants.cmd160_fwUpdate ){
            DLog.i(TAG, "returned cs=" + mNmeaParser.rsp160_buf[2]+" "+mNmeaParser.rsp160_buf[3]);
            if( mNmeaParser.rsp160_buf[1] == (byte)0x11 ){
                DLog.i(TAG, "Issuing cpu reset command");

                // Reset the MCU
                buf[0] = (byte)0xAB; // addr H
                buf[1] = (byte)0xCD; // addr L

                if( !CommonValue.getInstance().gpsManager.sendCommandToDevice(Constants.cmd160_fwUpdate, buf, 7, true) ){
                    DLog.i(TAG, "reset command failed. Please reset the unit manually");
                }
            }
            else if( mNmeaParser.rsp160_buf[1] == (byte)0xEE ){
                DLog.i(TAG, "Update verify checksum failure");
            }
            else{
                DLog.i(TAG, "Update verify unknown failure <"+mNmeaParser.rsp160_buf[1]+">");
            }
        }
    }
}
