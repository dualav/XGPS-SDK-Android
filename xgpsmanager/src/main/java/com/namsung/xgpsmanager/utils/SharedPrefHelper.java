package com.namsung.xgpsmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

public class SharedPrefHelper {

	private final static String INI_FILENAME_CONFIG = "config";

	public static SharedPreferences getPreference(Context context) {
		return context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
	}

	public static void setString(Context context,String key,String value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putString(key, value);
		edit.apply();
	}

	public static String getString(Context context,String key,String non){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		return pref.getString(key,non);
	}

	public static void setInt(Context context,String key,int value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putInt(key, value);
		edit.apply();
	}

	public static int getInt(Context context,String key,int non){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		return pref.getInt(key,non);
	}

	public static void setBool(Context context,String key,boolean value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putBoolean(key, value);
		edit.apply();
	}
	public static boolean getBool(Context context,String key,boolean non){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		return pref.getBoolean(key,non);
	}

	public static void setFloat(Context context,String key,float value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putFloat(key, value);
		edit.apply();
	}

	public static float getFloat(Context context,String key,float non){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		return pref.getFloat(key,non);
	}

	public static void setDouble(Context context, String key, double value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putLong(key, Double.doubleToLongBits(value));
		edit.apply();
	}

	public static double getDouble(Context context, String key){
		long retValue;
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		try {
			retValue = pref.getLong(key, 0);
		} catch (ClassCastException e) {
			retValue = 0;
		}
		return Double.longBitsToDouble(retValue);
	}

	public static void setLong(Context context,String key,long value){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.putLong(key, value);
		edit.apply();
	}

	public static long getLong(Context context,String key,long non){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		return pref.getLong(key,non);
	}

	public static void setArray(Context context, String key, String[] array) {
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < array.length; i++) {
			jsonArray.put(array[i]);
		}
		SharedPreferences.Editor edit = pref.edit();
		edit.putString(key, jsonArray.toString());
		edit.apply();
	}

	public static String[] getArray(Context context, String key) {
		String[] array = null;
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		try {
		    JSONArray jsonArray = new JSONArray(pref.getString(key, "[]"));
		    array = new String[jsonArray.length()];
		    for (int i = 0; i < jsonArray.length(); i++) {
		         array[i] = jsonArray.getString(i);
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return array;
	}

	public static void removeKey(Context context,String key){
		SharedPreferences pref = context.getSharedPreferences(INI_FILENAME_CONFIG, 0);
		SharedPreferences.Editor edit = pref.edit();
		edit.remove(key);
		edit.apply();
	}
}


