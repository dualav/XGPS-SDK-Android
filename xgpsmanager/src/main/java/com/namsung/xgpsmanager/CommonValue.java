package com.namsung.xgpsmanager;


import android.content.Context;

import com.namsung.xgpsmanager.utils.SharedPrefHelper;

public class CommonValue {
	// pref key
	public static String PREF_KEY_SPEED = "key_speed";
	public static String PREF_KEY_ALTITUDE = "key_altitude";
	public static String PREF_KEY_POSITION = "key_position";
	public static String PREF_KEY_TRAFFIC_DISPLAY = "key_traffic_display";

	public static String MODEL_XGPS190 = "XGPS190";
	public static String MODEL_XGPS170D = "XGPS170D";
	public static String MODEL_XGPS170 = "XGPS170";
	public static String MODEL_XGPS160 = "XGPS160";
	public static String MODEL_XGPS150 = "XGPS150";
	public static String MODEL_XGPS360 = "XGPS360";
	public static String MODEL_DASHPRO = "DashPro";
	public static String MODEL_XGPS500 = "XGPS500";

	// common
	public Boolean BT_CONNECTED = false;
	public volatile BluetoothGpsManager gpsManager = null;

	public int Current_Page = 0;
	public int Select_Speed = 0;
	public int Select_Altitude = 0;
	public int Select_Position = 0;
	public int mTrafficDisplayUp = 0;
	public int mLEDBrightnessLevel = 0;

	public Boolean NDK_FirmwareReceive = false;
	public int AHRS_Status_Check_Value = 0;

	public Boolean feet=true;
	public Boolean meters=false;

	public Boolean isPWR = false;
	public String firmwareString;
	private String mDeviceName;

	public float offSetPitch;
	public float offsetRoll;

	private volatile static CommonValue instance = null;

	// led bright set value
	public int led_bright;
	public int led_activity;

	public static CommonValue getInstance() {
		synchronized(CommonValue.class) {
			if (null == instance) {
				instance = new CommonValue();
			}
		}
		return instance;
	}

	// 설정 파일저장
	public void savePreference(Context context) {
		SharedPrefHelper.setInt(context, PREF_KEY_SPEED, Select_Speed);
		SharedPrefHelper.setInt(context, PREF_KEY_ALTITUDE, Select_Altitude);
		SharedPrefHelper.setInt(context, PREF_KEY_POSITION, Select_Position);
		SharedPrefHelper.setInt(context, PREF_KEY_TRAFFIC_DISPLAY, mTrafficDisplayUp);
	}

	public void loadPreference(Context context)  {
		Select_Speed = SharedPrefHelper.getInt(context, PREF_KEY_SPEED, 0);
		Select_Altitude = SharedPrefHelper.getInt(context, PREF_KEY_ALTITUDE, 0);
		Select_Position = SharedPrefHelper.getInt(context, PREF_KEY_POSITION, 2);
		mTrafficDisplayUp = SharedPrefHelper.getInt(context, PREF_KEY_TRAFFIC_DISPLAY, 0);
	}

	public void setDeviceName(String deviceName) {
		this.mDeviceName = deviceName;
	}

	public String getDeviceName() {
		return mDeviceName;
	}

	public String getModelName() {
		if (mDeviceName == null)
			return null;
		if (mDeviceName.contains(MODEL_XGPS190))
			return MODEL_XGPS190;
		else if (mDeviceName.contains(MODEL_XGPS170D))
			return MODEL_XGPS170D;
		else if (mDeviceName.contains(MODEL_XGPS170))
			return MODEL_XGPS170;
		else if (mDeviceName.contains(MODEL_XGPS160))
			return MODEL_XGPS160;
		else if (mDeviceName.contains(MODEL_XGPS150))
			return MODEL_XGPS150;
		else if (mDeviceName.contains(MODEL_XGPS360) || mDeviceName.contains(MODEL_DASHPRO))
			return MODEL_XGPS360;
		else if (mDeviceName.contains(MODEL_XGPS500))
			return MODEL_XGPS500;
		return null;
	}

	public String getPositionString(int degree, float mins, String direction) {
		String latitudeString;
		switch(Select_Position){
			default:
			case 0 :
				latitudeString = degree + "˚"
						+ String.format("%.0f'", Math.floor(mins))
						+ String.format("%1.3f\"", (mins - Math.floor(mins)) * 60)
						+ direction;
				break;
			case 1 :
				latitudeString = degree + "˚" + String.format("%.4f", mins) + "'" + direction;
				break;
			case 2 :
				latitudeString = String.format("%.6f˚", (degree + (mins/60.0f))) + direction;
				break;
		}
		return latitudeString;
	}

	public String getPositionString(double location, String positive, String negative) {
		String dir = (location > 0) ? positive : (location < 0) ? negative : "-";
		String dmsString = "";
		switch(Select_Position){
			default:
			case 0 : {
				int degree = (int) Math.abs(location);
				int mins = (int) ((Math.abs(location) - degree) * 60.0f);
				float secs = (float) ((((Math.abs(location) - degree) * 60.0f) - (double) mins) * 60.0f);
				dmsString = String.format("%d˚ %d' %.3f\" %s", degree, mins, secs, dir);
			}
				break;
			case 1 : {
				int degree = (int) Math.abs(location);
				double mins = ((Math.abs(location) - degree) * 60.0f);
				dmsString = String.format("%d˚ %.3f' %s", degree, mins, dir);
			}
				break;
			case 2 : {
				double degrees = Math.abs(location);
				dmsString = String.format("%.6f˚ %s", degrees, dir);
			}
				break;
		}
		return dmsString;
	}

	public String getAltitudeString(String altitudeString, int fixTyle) {
		String altitudeWituUnit;
		float altitude = 0;
		try {
			altitude = Float.parseFloat(altitudeString);
		} catch (NumberFormatException e ) {

		} catch (Exception e ) {}

		if( fixTyle == 3 ) {
			switch (CommonValue.getInstance().Select_Altitude) {
				default:
				case 0:
					altitudeWituUnit = String.format("%.2f", altitude * 3.2808399f) + " ft";
					break;
				case 1:
					altitudeWituUnit = String.format("%.2f", altitude) + " m";
					break;
			}
		} else{
			altitudeWituUnit = "Need one more satellite..";
		}
		return altitudeWituUnit;
	}

	public String getSpeedString(String speedString) {
		String speedWithUnit;
		float speed = 0;
		try {
			speed = Float.parseFloat(speedString);
		} catch (NumberFormatException e) {}
		switch( CommonValue.getInstance().Select_Speed ){
			default:
			case 0 :
				speedWithUnit = String.format("%.2f knots", speed); // 노트
				break;
			case 1 :
				speedWithUnit = String.format("%.2f", (speed * 1.852f * 0.621371192f))+" mph";
				break;
			case 2 :
				speedWithUnit = String.format("%.0f", speed * 1.852f) +" km/h";
				break;
		}
		return speedWithUnit;
	}
}
