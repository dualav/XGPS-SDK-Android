# XGPS-SDK-Android

![](https://img.shields.io/badge/language-java-orange.svg?style=flat)
![Platform](https://img.shields.io/badge/platform-Android-lightgrey.svg?style=flat)
![](https://img.shields.io/badge/sdk-aar-orange.svg?style=flat)
![](https://img.shields.io/badge/version-1.0-blue.svg?style=flat)

This project provides the Android SDK and example source code that XGPS150/XGPS160/XGPS500  exchanges data with Android device via Bluetooth connection.

# Contents

* [Requirements](#requirements)
* [Features](#features)
* [Permissions](#permissions)
* [Installation](#installation)
* [Usage](#usage)
* [Product](#product)


# Requirements

* OS : Above Android 4.3 (API Level 18)
* Bluetooth : activate
* External Bletooth GPS Device : XGPS150 / XGPS160 / XGPS500

# Features

* Bluetooth connection with XGPS Series via SPP communication
* parsing NMEA sentence
* parsing XGPS custom command

# Permissions

* SDK include the following permission 
```xml
    <!-- bluetooth communication -->
    <uses-permission android:name="android.permission.BLUETOOTH" />     
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    
    <!-- in order to replace the android's internal GPS location -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" />
```

* If you use MockLocation and Android OS is above 6.0, you should do check self permission.
```Java
protected void onCreate(Bundle savedInstanceState) {
    …
    // user accept the location permission request at runtime.
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSIONS_LOCATION);
        }
    }
    …
}
```

* for the result from requesting location permissions
```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch(requestCode) {
        case REQUEST_CODE_PERMISSIONS_LOCATION:
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
            break;
        default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
```

# Installation

for using the XGPS SDK in your Android Studio, follow the steps below. (you can choose one method)
### method 1 : adding aar file directly
1. copy a xgpsManager.aar to your project. (ex. myproject/library/xgpsManager.aar)
2. open the application's 'build.gradle'.
3. adding repositories and dependencies like below.
```gradle
repositories {
    flatDir {
        dirs 'library/'     // aar file path
    }
}

dependencies {
    …
    compile(name: 'xgpsManager', ext: 'aar')
}
```

### method 2 : using dialog command in Android studio 
1. File > New > New Module > Import .JAR/.AAR Package                               
![](https://dl.dropboxusercontent.com/s/zaqm48mq21qpld1/new_module.png)
2. File name : as xgpsManager.aar, Subproject name: as xgpsManager
![](https://dl.dropboxusercontent.com/s/3sbileqadj3d00b/modulename.png)
3. Open Project structure (using Ctrl+Enter/⌘+Enter)
4. dependency > + > Module dependency                                  
![](https://dl.dropboxusercontent.com/s/wr0fd8mpyg1ljfi/project_structure.png)
5. choose xgpsManager' module                                                 
![](https://dl.dropboxusercontent.com/s/whmc4p0jyr1jiab/chhose_modules.png)


## Usage

### XGPSManager 

| Public constructors | Parameters   | |
| ------------------- |:------------ | -----   |
| XGPSManager         | Context      |         |
|                     | XGPSListener | The callback that the result receive. This value may be null. |


| Public methods      | Return           | Parameters   |   |
| -------------       |:-------------:| -----:| ---- |
| onResume            | void          |       | retry to start bluetooth connection thread     |
| isConnected         | boolean       |       | returns bluetooth connection status.     |
| setListener         | void    | XGPSListener | The callback that the result receive. This value may be null. |
| setMockEnable       | void | enable | true is MockLocationProvider as gps provider, false is disable |
| sendCommandToDevice | void | cmd | custom command for xgps. you can see the Constants list. |
|                     |      | buffer | byte buffer to transfer to xgps devices. |
|                     |      | bufLen | buffer's length |
| getBatteryLevel     | float || get battery level. min value is 0.0, max value is 1.0 |
| isCharging          | boolean | | return battery is charging or not. | 
| getFirmwareVersion  | String | | return firmware version as String |
| getLatitude         | String | mode | latitude as String. mode can be MODE_POSITION_DMS, MODE_POSITION_DM, MODE_POSITION_DEGREE| 
| getLatitude         | float | | latitude as float in degree |
| getLongitude        | String | mode | longitude as String. mode can be MODE_POSITION_DMS, MODE_POSITION_DM, MODE_POSITION_DEGREE| 
| getLongitude        | float |  | longitude as float in degree |
| getAltitude         | String | mode | altitude as String. mode can be MODE_ALTITUDE_FEET, MODE_ALTITUDE_METER | 
| getAltitude         | float | | altitude as float in meter |
| getUTC              | String | | |
| getSpeed            | String  | mode | speed as String. mode can be MODE_SPEED_KNOTS, MODE_SPEED_KPH, MODE_SPEED_MPH |
| getSpeed            | float | | speed as float in knots |
| getHeadingString    | String | | heading as String in degree |
| getHeading          | float | | heading as float |
| getHDOP | int | | |
| getVDOP | int | | |
| getPDOP | int | | |
| getSatellitesMap | HashMap | | GPS satellites information. see the SatellitesInfo class |
| getGlonassSatellitesMap | HashMap | | GLONASS satellites information. see the SatellitesInfo class. / not supported XGPS150 |
| getFixType | int | | return TYPE_NO_FIX, TYPE_2D_FIX, TYPE_3D_FIX |
| getAvailableDevices | ArrayList |  | get bonded XGPS series bluetooth devices |
| onDestroy | void | | | 


### XGPSListener

|Public methods |     |     |
| ------------- | --- | --- |
|abstract void |    connecting(BluetoothDevice device) | Called when bluetooth try to connect. |
|abstract void |    connected(boolean isConnect, int error) | Called when bluetooth connection changed. |
|abstract void |    updateLocationInfo() | Called when get location information from device |
|abstract void |    updateSatellitesInfo() | Called when get satellites information from device. |
|abstract void |    updateSettings(boolean positionEnable, boolean overWrite) | Called when get settings changed. |
|abstract void |    getLogListComplete(ArrayList<LogData> logList) | Called when get log list complete. / not supported XGPS150 |
|abstract void |    getLogDetailProgress(int bulkCount) | Called when get detail log download progress. / not supported XGPS150 |
|abstract void |    getLogDetailComplete(ArrayList<LogBulkData> logBulkList) | Called when get detail log download complete. / not supported XGPS150 |
|abstract void |    throwException(Exception e) | Called when occur exception. |
    
    
### Mock Location

In order to replace internal GPS location to external XGPS device, you can use XGPSManager.setMockEnable method. 
But in above Android 6.0, you should change the mock provider permissions in developer option.
Please go to the developer option > Select mock location app > select your app as provider.


* Please reference the xgpsSample project to see detail.

## Product
![](http://gps.dualav.com/wp-content/uploads/xgps150_HeaderImage.jpg) ![](http://gps.dualav.com/wp-content/uploads/xgps160_HeaderImage.jpg)


http://gps.dualav.com/
