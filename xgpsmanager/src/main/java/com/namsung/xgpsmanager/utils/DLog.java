package com.namsung.xgpsmanager.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.namsung.xgpsmanager.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * <b>Log 와 같은 기능을 지원하는 클래스</b>
 * 
 * <p>
 * 테스트시에는 BuildConfig.DEBUG가 true 상태여서 logcat으로 로그를 분석할 수 있고, apk 생성하여 배포시에는
 * 자동으로 false로 변경되어 로그가 출력되지 않는다.
 * </p>
 * 
 * @author hjlee
 * 
 */
public class DLog {
	public static String TAG = "SkyProLog";
	public static boolean isDebug = BuildConfig.isLogEnable;        // use external directory and external storage file log & all log

    private static final short FILE_LINE_DEFAULT = 4;
//	public static boolean isDebug = true;
	
    /** Log Level Error **/
    public static void e(Context context, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.e(context.getClass().getSimpleName(),  getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
    
    public static void e(Context context, String message, Throwable throwable) {
    	if (isDebug || BuildConfig.isReleaseLogEnable) Log.e(context.getClass().getSimpleName(), getFileLine(FILE_LINE_DEFAULT) + " > " + message, throwable);
    }
 
    /** Log Level Warning **/
    public static void w(Context context, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.w(context.getClass().getSimpleName(), getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Information **/
    public static void i(Context context, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.i(context.getClass().getSimpleName(), getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Debug **/
    public static void d(Context context, String message) {
        if (isDebug) Log.d(context.getClass().getSimpleName(), getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Verbose **/
    public static void v(Context context, String message) {
        if (isDebug) Log.v(context.getClass().getSimpleName(), getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Error **/
    public static void e(String tag, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.e(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    public static void e(String tag, String message, Throwable throwable) {
    	if (isDebug || BuildConfig.isReleaseLogEnable) Log.e(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message, throwable);
    }
 

    /** Log Level Warning **/
    public static void w(String tag, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.w(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Warning **/
    public static void w(String tag, String message, Throwable e) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.w(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message + e.toString());
    }
 
    /** Log Level Information **/
    public static void i(String tag, String message) {
        if (isDebug || BuildConfig.isReleaseLogEnable) Log.i(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Debug **/
    public static void d(String tag, String message) {
        if (isDebug) Log.d(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
 
    /** Log Level Verbose **/
    public static void v(String tag, String message) {
        if (isDebug) Log.v(tag, getFileLine(FILE_LINE_DEFAULT) + " > " + message);
    }
    
    /** Log Level Error **/
    public static void e(String message) {
        e(TAG, message);
    }
 
    public static void e(String message, Throwable throwable) {
    	e(TAG, message, throwable);
    }
 

    /** Log Level Warning **/
    public static void w(String message) {
        w(TAG, message);
    }
 
    /** Log Level Warning **/
    public static void w(String message, Throwable throwable) {
        w(TAG, message, throwable);
    }
 
    /** Log Level Information **/
    public static void i(String message) {
        i(TAG, message);
    }
 
    /** Log Level Debug **/
    public static void d(String message) {
        d(TAG, message);
    }
 
    /** Log Level Verbose **/
    public static void v(String message) {
        v(TAG, message);
    }

    /** Log Level Error **/
    public static void enf(Context context, String message) {
        e(TAG, message);
//        if (isDebug)
//            Utils.saveFileLog(context, message);
    }

    /** Log Level Warning **/
    public static void wnf(Context context, String message) {
        w(TAG, message);
//        if (isDebug)
//            Utils.saveFileLog(context, message);
    }

    /** Log Level Information **/
    public static void inf(Context context, String message) {
        i(TAG, message);
//        if (isDebug)
//            Utils.saveFileLog(context, message);
    }

    /** Log Level Debug **/
    public static void dnf(Context context, String message) {
        d(TAG, message);
//        if (isDebug)
//            Utils.saveFileLog(context, message);
    }

    /** Log Level Verbose **/
    public static void vnf(Context context, String message) {
        v(TAG, message);
//        if (isDebug)
//            Utils.saveFileLog(context, message);
    }

    private static String getFileLine(int depth) {
        ++depth;
        return getFileName(depth) + ":" + getLineNumber(depth);
    }
    
    private static String getFileName(int depth) {
        try {
            StackTraceElement stack = Thread.currentThread().getStackTrace()[depth];
            return stack.getFileName();
        } catch (Throwable e) {
            Log.w(TAG, e);
        }
        return "FFF";
    }

    private static String getLineNumber(int depth) {
        try {
            StackTraceElement stack = Thread.currentThread().getStackTrace()[depth];
            return String.valueOf(stack.getLineNumber());
        } catch (Throwable e) {
            Log.w(TAG, e);
        }
        return "LLL";
    }

    public static void fileDump(Context context, byte[] data) {
        if (context == null || !isDebug)
            return;
        Random random = new Random();
        File logDirectory = new File(Environment.getExternalStorageDirectory() + "/" +  context.getPackageName() + "." + TAG + "Log");
        logDirectory.mkdirs();
        try{
            Date date = new Date( System.currentTimeMillis());
            SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
            String today = sdfDay.format(date);
            File logFile = new File(logDirectory + "/xhud_" + today + ".bin");   // random.nextInt(1000) +
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(data);
            fos.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}