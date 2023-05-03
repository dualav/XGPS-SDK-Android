# XGPS-SDK-Android

[![Release](https://jitpack.io/v/jitpack/android-example.svg)](https://jitpack.io/#jitpack/android-example)
![](https://img.shields.io/badge/language-java-orange.svg?style=flat)
![Platform](https://img.shields.io/badge/platform-Android-lightgrey.svg?style=flat)
![](https://img.shields.io/badge/sdk-aar-orange.svg?style=flat)
![](https://img.shields.io/badge/version-2.0-blue.svg?style=flat)

This project provides the Android SDK and example source code that XGPS150/XGPS160/XGPS500/DashPro(XGPS360)  exchanges data with Android device via Bluetooth connection.

## Installation

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    implementation 'com.github.dualav:XGPS-SDK-Android:2.0.0-beta'
}
```

## How to use 

```Java
public class MainActivity extends AppCompatActivity implements XGPSListener {
    ...
    XGPSManager xgpsManager;
    ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        xgpsManager = new XGPSManager(this, this);
    }
    
    @Override
    protected void onDestroy() {
        ...
        xgpsManager.onDestroy();
    }
    
    // handling listener 
    @Override
    void updateLocationInfo() {
        ...
        latitude.setText(xgpsManager.getLatitude(XGPSManager.MODE_POSITION_DEGREE));
        longitude.setText(xgpsManager.getLongitude(XGPSManager.MODE_POSITION_DEGREE));
        altitude.setText(xgpsManager.getAltitude(Constants.MODE_ALTITUDE_FEET));
        heading.setText(xgpsManager.getHeadingString());
        speed.setText(xgpsManager.getSpeed(Constants.MODE_SPEED_KPH));
        utc.setText(xgpsManager.getUTC());
    }
    
    @Override
    void updateSatellitesInfo(int systemId) {
        // Because of frequent calls, updating the UI every time can degrade app performance. It is recommended that the UI be updated only when necessary.
        String numberOfSatellites = "";
        HashMap<Integer, SatellitesInfo> satellitesMap = (HashMap<Integer, SatellitesInfo>) xgpsManager.getSatellitesInfoMap().getAll(systemId);
        Iterator<Integer> it = satellitesMap.keySet().iterator();
        while (it.hasNext()) {
            Integer key = it.next();
            if (numberOfSatellites.length() > 0) {
                numberOfSatellites += ", ";
            }
            numberOfSatellites += key;
        }
        String avgSNR = String.valueOf(xgpsManager.getAverageSNRInUse());
        String inUseList = String.join(", ", xgpsManager.getSatellitesInUse(systemId));
        ...
    }
    ...

```

## Methods
will be updated

## Sample project 

https://github.com/dualav/xgpsSample


