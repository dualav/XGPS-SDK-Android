package com.namsung.xgpsmanager;

import android.util.Log;

import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.data.SatellitesInfo;
import com.namsung.xgpsmanager.data.SatellitesInfoMap;
import com.namsung.xgpsmanager.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hjlee on 2018. 8. 21..
 */

public class BaseParser {
    private static final String TAG = BaseParser.class.getSimpleName();
    protected XGPSListener mXgpsListener;

    // setting value
    protected String mFirmwareVersion = Constants.UNKNOWNSTRING;

    protected int mDeltaX = 0;
    protected int mDeltaY = 0;
    protected int calResultFlags;
    protected float calFieldStrength;


    // GPS info value
    // PRMC
    public boolean isCharging = false;
    public int mChargingCurrent = 0;
    public float batteryVoltage=0;
    public boolean isDGPS = false;

    protected float latitude = 0.0f;
    protected float longitude = 0.0f;
    public float latMins=0;
    public int latDeg=0;
    public float lonMins=0;
    public int lonDeg=0;
    public String latDirString="";
    public String lonDirString="";
    public String time = null;
    public String latString = null;
    public String longString = null;
    public String altString = null;
    public String speedString = null;
    public String headingString = null;
    public int fixType = 0;
    public float pdopfloat = 0;
    public float vdopfloat = 0;
    public float hdopfloat = 0;
    public int satellitesNum = 0;
    public Integer dictOfSatInfoKey = 0;
    public Integer dictOfSatInfoKeyNDK = 0;
    public int TotalSatCount = 0;
    public int numOfSatInUse = 0;
    public int numOfSatInUseGlonass = 0;
    public String UsedSatData = null;
	public int numOfSatInView = 0, numOfSatInViewGlonass = 0;
    public HashMap<Integer, ArrayList<String>> satellitesUsedMap = new HashMap<>();
//    public ArrayList<String> satsUsedInPosCalc = new ArrayList<>();
    public ArrayList<String> satsUsedInPosCalcGlonass = new ArrayList<>();
//    public ConcurrentHashMap<Integer, SatellitesInfo> dictOfSatInfo = new ConcurrentHashMap<>();
//    public ConcurrentHashMap<Integer, SatellitesInfo> dictOfSatInfoGlonass = new ConcurrentHashMap<>();
//	protected ConcurrentHashMap<Integer, SatellitesInfo> tempSatInfoList = new ConcurrentHashMap<>();
//    protected ConcurrentHashMap<Integer, SatellitesInfo> tempSatInfoGlonassList = new ConcurrentHashMap<>();

    public SatellitesInfoMap satellitesInfoMap = new SatellitesInfoMap();
    protected SatellitesInfoMap tempSatellitesInfoMap = new SatellitesInfoMap();
//    protected HashMap<Integer, HashMap<Integer, SatellitesInfo>> tempSatellitesInfoMap = new HashMap<>();
    public HashMap<Integer, Integer> numOfSatInViewMap = new HashMap<>();

    protected LogData selectedLogData;
    public void setLogData(LogData data) {
        selectedLogData = data;
    }

    public void setListener(XGPSListener listener) {
        mXgpsListener = listener;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public SatellitesInfoMap getSatellitesInfoMap() {
        return satellitesInfoMap;
    }

    public HashMap<Integer, SatellitesInfo> getDictOfSatInfo(int systemId, int signalId) {
//        return dictOfSatInfo;
        return (HashMap) satellitesInfoMap.get(systemId, signalId);
    }

    public ArrayList<String> getSatsUsedInPosCalc(int systemId) {
//        return satsUsedInPosCalc;
        return satellitesUsedMap.get(systemId); // TODO : test for GPS
    }

    public HashMap<Integer, SatellitesInfo> getDictOfSatGlonassInfo() {
//        return dictOfSatInfoGlonass;
        return (HashMap) satellitesInfoMap.get(2, 1);
    }

    public ArrayList<String> getSatsUsedInPosCalcGlonass() {
//        return satsUsedInPosCalcGlonass;
        return satellitesUsedMap.get(2); // TODO : test for GPS
    }

    public int avgUsableSatSNR() {
        SatellitesInfoMap clonedMap = satellitesInfoMap.clone();

        List<Integer> systems = new ArrayList<>(clonedMap.keySet());
        int numInUse = 0;
        int sumSatStrength=0;
        for (Integer system : systems) {
            List<Integer> signals = clonedMap.getKeyList(system);
            Map<Integer, SatellitesInfo> integratedMap = new HashMap<>();
            for (Integer signal : signals) {
                List<Integer> keys = clonedMap.getKeyList(system, signal);
                for (Integer key : keys) {
                    SatellitesInfo value = (SatellitesInfo) clonedMap.get(system, signal, key);
                    if (value == null)  continue;
                    if (integratedMap.containsKey(key)) {
                        if (integratedMap.get(key).SNR < value.SNR) {
                            integratedMap.put(key, value);
                        }
                    } else {
                        integratedMap.put(key, value);
                    }
                }
            }
            for (int i = 0; i < satellitesUsedMap.get(system).size(); i++) {
                int sat = Integer.parseInt( satellitesUsedMap.get(system).get(i));
                if (integratedMap.containsKey(sat)) {
                    sumSatStrength += integratedMap.get(sat).SNR;
                }
            }
            numInUse += satellitesUsedMap.get(system).size();
        }
//        Log.d("avg", "sumSatStrength : " + sumSatStrength + ", numInUse : " + numInUse);
        return (numInUse!=0)?(sumSatStrength / numInUse):0;

/*
        if (numOfSatInUse == 0 && numOfSatInUseGlonass == 0)
            return 0;

        int sumSatStrength=0, avgInt=0;
        float avgSNR=0.0f, avgSNRGlonass=0.0f ;

        for(Map.Entry<Integer, SatellitesInfo> entry : satellitesInfoMap.get(1).entrySet()) {
            Integer key = entry.getKey();
            SatellitesInfo value = entry.getValue();
            for (int i = 0; i < satellitesUsedMap.get(1).size(); i++) {

                if (key == Integer.parseInt( satellitesUsedMap.get(1).get(i)) ) {
                    sumSatStrength += value.SNR;
                }
            }
        }


        // 숫자가 아닐수 잇음
        if (numOfSatInUse == 0)
            avgSNR = 0;
        else
            avgSNR = sumSatStrength / numOfSatInUse;
//		DLog.i(TAG, "avgSNR: "+avgSNR);

        sumSatStrength=0;

        for(Map.Entry<Integer, SatellitesInfo> entry : satellitesInfoMap.get(2).entrySet()) {
            Integer key = entry.getKey();
            SatellitesInfo value = entry.getValue();
            for (int i = 0; i < satsUsedInPosCalcGlonass.size(); i++) {

                if (key == Integer.parseInt( satsUsedInPosCalcGlonass.get(i)) ) {
                    sumSatStrength += value.SNR;
                }
            }
        }

        // 숫자가 아닐수 잇음
        if (numOfSatInUseGlonass == 0)
            avgSNRGlonass = 0;
        else
            avgSNRGlonass = sumSatStrength / numOfSatInUseGlonass;
//		DLog.i(TAG, "avgSNRGlonass: "+avgSNRGlonass);

        if (avgSNRGlonass == 0) {
            avgInt = (int) avgSNR;
        }
        else
            avgInt = (int) ((avgSNR+avgSNRGlonass)/2);

        return avgInt;*/
    }

    public int getFixType() {
        return fixType;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public String getLatitudeString() {
        return latString;
    }

    public int getLatitudeDegree() {
        return latDeg;
    }

    public float getLatitudeMins() {
        return latMins;
    }

    public String getLatitudeDirection() {
        return latDirString;
    }

    public String getLongitudeString() {
        return longString;
    }

    public int getLongitudeDegree() {
        return lonDeg;
    }

    public float getLongitudeMins() {
        return lonMins;
    }

    public String getLongitudeDirection() {
        return lonDirString;
    }

    public String getTime() {
        return time;
    }

    public String getAltitude() {
        return altString;
    }

    public String getSpeed() {
        return speedString;
    }

    public String getHeading() {
        return headingString;
    }

    public boolean getIsCharging() {
        return isCharging;
    }

    public boolean getIsDGPS() {
        return isDGPS;
    }

    public float getPdop() {
        return pdopfloat;
    }

    public float getVdop() {
        return vdopfloat;
    }

    public float getHdop() {
        return hdopfloat;
    }

    public int getSatellitesNum() {
        return satellitesNum;
    }

    public float getBatteryVoltage() {
        return batteryVoltage;
    }

    public int getChargingCurrent() {
        return mChargingCurrent;
    }

    public int getDeltaX() {
        return mDeltaX;
    }

    public int getDeltaY() {
        return mDeltaY;
    }

    public void setCalResultFlags(int flag) {
        calResultFlags = flag;
    }

    public int getCalResultFlags() {
        return calResultFlags;
    }

    public float getCalFieldStrength() {
        return calFieldStrength;
    }

    public float parseNmeaSpeed(String speed,String metric){
        float meterSpeed = 0.0f;
        if (speed != null && metric != null && !speed.equals("") && !metric.equals("")){
            float temp1 = Float.parseFloat(speed)/3.6f;
            if (metric.equals("K")){
                meterSpeed = temp1;
            } else if (metric.equals("N")){
                meterSpeed = temp1*1.852f;
            }
        }
        return meterSpeed;
    }

    protected int getSystemIdFromTalkId(char talkId) {
        int GNSSSYSTEMID_GPS = 1;
        int GNSSSYSTEMID_GLONASS = 2;
        int GNSSSYSTEMID_GALILEO = 3;
        int GNSSSYSTEMID_BEIDOU = 4;
        int GNSSSYSTEMID_QZSS = 5;
        int GNSSSYSTEMID_NAVIC = 6;
        int GNSSSYSTEMID_UNKNOWN = 7;
        switch (talkId) {
            case 'P' :  return GNSSSYSTEMID_GPS;
            case 'L' :  return GNSSSYSTEMID_GLONASS;
            case 'A' :  return GNSSSYSTEMID_GALILEO;
            case 'B' :  return GNSSSYSTEMID_BEIDOU;
            case 'Q':   return GNSSSYSTEMID_QZSS;
            case 'I':   return GNSSSYSTEMID_NAVIC;
            default:    return GNSSSYSTEMID_UNKNOWN;
        }
    }
}
