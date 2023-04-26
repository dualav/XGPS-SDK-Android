package com.namsung.xgpsmanager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.utils.Constants;
import com.namsung.xgpsmanager.utils.DLog;
import com.namsung.xgpsmanager.utils.SharedPrefHelper;
import com.namsung.xgpsmanager.utils.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BluetoothGpsManager {
    private static final String LOG_TAG = "XGPS160";
    public static final String ACTION_START_GPS_PROVIDER = "com.namsung.xgpsmanager.intent.action.START_GPS_PROVIDER";
    public static final String ACTION_STOP_GPS_PROVIDER = "com.namsung.xgpsmanager.intent.action.STOP_GPS_PROVIDER";

    private BluetoothSocket gpsSocket;
    private String gpsDeviceAddress;
    private BluetoothDevice currentDevice;
    public boolean enabled = false;
    public ScheduledExecutorService connectionAndReadingPool;
    private List<NmeaListener> nmeaListeners = Collections.synchronizedList(new LinkedList<NmeaListener>());
    public ConnectedGps connectedGps;
    private int disableReason = 0;
    private Notification connectionProblemNotification;
    private Context appContext;
    private NotificationManager notificationManager;
    private int maxConnectionRetries;
    private int nbRetriesRemaining;
    public boolean connected = false;
    private LocationManager locationManager;
    private byte[] ndk_firmware_buff = new byte[512];
    Timer FirmwareTimer;
    TimerTask FirmwareCheck;
    private static int Tick_Count = 0;
    private XGPSListener xgpsListener;
    private XGPSParser mXgpsParser;

    private HandlerThread mNtripThread = null;
    private Handler mNtripHandler;
    private boolean isNtripRunning = false;
//    public static boolean isSaving = false;        // TODO : TEST CODE

    public void setLogData(LogData data) {
        if (mXgpsParser != null)
            mXgpsParser.setLogData(data);
    }


    public class ConnectedGps extends Thread {
        private final BluetoothSocket socket;
        private InputStream in;
        private OutputStream out;
        private PrintStream out2;
        private boolean ready = true;
        final char[] buf = new char[4096];
        BufferedReader reader = null;
//        OutputStream oos;

        public ConnectedGps(BluetoothSocket socket) {

            DLog.v("initialize ConnectedGps");
            this.socket = socket;
            OutputStream tmpOut = null;
            PrintStream tmpOut2 = null;

            try {
                in = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                if (tmpOut != null) {
                    tmpOut2 = new PrintStream(tmpOut, false, "US-ASCII");
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while getting socket streams", e);
            }
            out = tmpOut;
            out2 = tmpOut2;


            // TODO : TEST CODE
//            try {
//                File mBinFile = new File(appContext.getExternalFilesDir(null).toString() + "/socket.binary");
//
//                if (!mBinFile.getParentFile().exists())
//                    mBinFile.getParentFile().mkdirs();
//                mBinFile.createNewFile();
//                //fullNameFile.setWritable(true, false);
//
//                oos = new FileOutputStream(mBinFile);
//                mBinFile.setReadable(true, true);
//                mBinFile.setWritable(true, true);
//            } catch (IOException e) {
//
//
//            }

//            oos.flush();
//            oos.close();


            // TODO : file read test
            /*try {
                File sdcard = Environment.getExternalStorageDirectory();
                File file = new File(sdcard, "nmea.txt");
                reader = new BufferedReader(new FileReader(file));
            } catch (IOException e) {
                DLog.e("IOException in file read");
            }*/
        }

        public void run() {
            try {
                // TODO : original code
                //reader = new BufferedReader( new InputStreamReader(in, "ISO-8859-1"));
                int bytes;
                byte[] buffer = new byte[4096];
                DLog.v("enabled " + enabled);
                while(enabled ){
                    if( in.available() > 0 ) {
//                    if(in != null && in.available() > 0 ) {  // TODO : TEST CODE
                        bytes = in.read(buffer);
                        if (bytes > 0) {
                            mXgpsParser.parserBridge(buffer, bytes);
                            // TODO : TEST CODE
//                            try {
//                                if (isSaving) {
//                                    oos.write(buffer, 0, bytes);
//                                }
//                            } catch (IOException e) {
//                            }
//                            parser.parseADSB_DATA(buffer, bytes);
                        }
                        //o parser.parseTempAdsb( buffer, reader.readLine() + "\r\n", bytes);
                    }
                    connected = true;
                    ready = true;
                }
                  // TODO : test code
                /*String line;
                int bytes;
                byte[] buffer = new byte[4096];
                while(enabled) {
                    if (in.available() > 0) {
                        bytes = in.read(buffer);
                        if (bytes > 0) {
                            if ((line = reader.readLine()) != null) {
                                mXgpsParser.parserBridge(line.getBytes(), line.length());
                            }
                        }
                    }
                    connected = true;
                    ready = true;
                }*/
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while getting data", e);
            } finally {
                // cleanly closing everything...
                this.close();
                disableIfNeeded();
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        // xgps190 펌웨어 버전 정보 요청 명령어 보내기
        public void writeToXGPS190(byte[] buffer, int len){ //NDK_FirmwareCommandLength
            try {
                /*
                do {
                    Thread.sleep(100);
                } while ((enabled) && (!ready));
                */
                byte xbuf[] = new byte[len];
                System.arraycopy(buffer, 0, xbuf, 0, len);
                //xbuf[len] = 0;

                if ((enabled) && (ready)) {
                    int tt = xbuf.length;
                    out.write(xbuf);
                    out.flush();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
                close();
            }
        }

        public Boolean sendCommandToDevice(int cmd, byte[] arg, int argLen, boolean isWaitResponse) {
            try {
//                do {
//                    Thread.sleep(10);
//                } while ((enabled) && (!ready));

                int i;
                int size = 0;
                byte cs = 0;
                byte xbuf[] = new byte[argLen+5];

                xbuf[0] = (byte) (0x88);
                xbuf[1] = (byte) (0xEE);
                xbuf[2] = (byte) (argLen + 1); // length
                xbuf[3] = (byte) cmd;

                if (argLen > 0) {
                    if ( arg == null || argLen > 240 ) {
                        return false;
                    }
                    for( i=0; i<argLen; i++ )
                    {
                        xbuf[4+i] = arg[i];
                    }
                }

                size = 4+argLen;
                for ( i = 0; i < size; i++ ) {
                    cs += xbuf[i];
                }
                xbuf[size++] = cs;

                // ios     :  136 : 238 : 1 : 10
                // android : -120 : -18 : 1 : 10
                mXgpsParser.setResponse(-1);
                if ((enabled) && (ready)) {
//                    StringBuilder debugString = new StringBuilder("");
//                    for (int index = 0; index < size; index++) {
//                        debugString.append(String.format("%02x ", (byte)xbuf[index]));
//                    }
//                    DLog.v("write xbuf : " + debugString.toString());
                    out.write(xbuf);
                    out.flush();

                    // if listner is not null wait response
                    if (isWaitResponse) {
                        int timeout = 100;
                        do {
                            Thread.sleep(10);
                            if (mXgpsParser.getResponse() == -1)
                                timeout--;
                            else
                                break;
                        } while (timeout > 0);
                        if (timeout > 0)
                            return true;
                        else {
                            DLog.v("response timeout");
                            return false;
                        }
                    }
                    else {
                        return true;
                    }
                }
                else{
                    return false;
                }

            } catch (IOException e) {
                DLog.e("Exception during write", e);
                close();
            }
            catch (InterruptedException e) {
                DLog.e("Exception during write", e);
            }

            return false;
        }

        public Boolean writeBufferToDevice(byte[] buffer, int argLen) {
            try {
                do {
                    Thread.sleep(10);
                } while ((enabled) && (!ready));

                if ((enabled) && (ready)) {
//                    StringBuilder debugString = new StringBuilder("");
//                    for (int index = 0; index < argLen; index++) {
//                        debugString.append(String.format("%02x ", (byte)buffer[index]));
//                    }
//                    DLog.v(LOG_TAG, "writeBufferToDevice buffer : " + debugString + ", " + argLen);
                    out.write(buffer);
                    out.flush();

                    return true;
                }
                else{
                    return false;
                }

            } catch (IOException e) {
                DLog.e(LOG_TAG, "Exception during write", e);
            }
            catch (InterruptedException e) {
                DLog.e(LOG_TAG, "Exception during write", e);
            }

            return false;
        }

        // xgps160 명령어 보내기
        public Boolean writeToXGPS160(int cmd, int item, byte[] buf, int bufLen ) {
            try {
                do {
                    Thread.sleep(100);
                } while ((enabled) && (!ready));

                int size = 0;
                byte cs = 0;
                byte xbuf[] = new byte[256];

                xbuf[0] = (byte) (0x88);
                xbuf[1] = (byte) (0xEE);
                xbuf[2] = (byte) (bufLen + 1); // length
                xbuf[3] = (byte) cmd;

                if (bufLen > 0) {
                    if (buf == null) {
                        return false;
                    }
                    if (bufLen > 248) {
                        return false;
                    }
                    System.arraycopy(buf, 0, xbuf, 4, bufLen);
                }

                size = 4+bufLen;
                for (int i = 0; i < size; i++) {
                    cs += xbuf[i];
                }
                xbuf[size] = cs;


                if ((enabled) && (ready)) {
                    Log.i("", "xbuf: "+(xbuf[0]&0xFF)+": "+(xbuf[1]& 0xFF)+": "+(xbuf[2]& 0xFF)+": "+(xbuf[3]& 0xFF)+": "+(xbuf[4]& 0xFF));
                    out.write(xbuf);
                    out.flush();
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }
            catch (InterruptedException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }

            return true;
        }



        public void close() {
            if (!connected)
                return;

            connected = false;
            ready = false;

            if (xgpsListener != null)
                xgpsListener.connected(false, 0);

//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                public void run() {
//
//                    blueToothConnetFail();
//                }
//            });
//
//            ready = false;
//            connected = false;
            try {
                Log.i(LOG_TAG, "closing Bluetooth GPS output stream");
                in.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while closing GPS NMEA output stream", e);
            } finally {
                try {
                    Log.i(LOG_TAG, "closing Bluetooth GPS input streams");
                    out2.close();
                    out.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "error while closing GPS input streams", e);
                } finally {
                    try {
                        Log.i(LOG_TAG, "closing Bluetooth GPS socket");
                        socket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "error while closing GPS socket", e);
                    }
                }
            }

            if (connectionAndReadingPool != null && !connectionAndReadingPool.isShutdown()) {
                DLog.v("connectionAndReadingPool shutdown");
                connectionAndReadingPool.shutdown();
            }
        }
    }

    public void sendCommandToDevice(int cmd, byte[] buffer, int bufLen) {
        sendCommandToDevice(cmd, buffer, bufLen, false);
    }

    public boolean sendCommandToDevice(int cmd, byte[] buffer, int bufLen, boolean isWaitResponse) {
        if (cmd == Constants.cmd160_logList) {
            XGPSParser.getInstance().initNMEAParser();
        }
        if (connectedGps != null)
            return connectedGps.sendCommandToDevice(cmd, buffer, bufLen, isWaitResponse);
        return false;
    }

    public BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }

    public void sendToXGPS160Cmd(int cmd, int item, byte[] buf, int bufLen) {
        connectedGps.writeToXGPS160(cmd, item, buf, bufLen);
    }

    public void sendToXGPS190Cmd(byte[] buf, int bufLen){
        if (connectedGps != null)
        connectedGps.writeToXGPS190(buf, bufLen);
    }

    public void setDeviceAddress(String deviceAddress) {
        this.gpsDeviceAddress = deviceAddress;
    }

    public BluetoothGpsManager(Service callingService, String deviceAddress, int maxRetries, XGPSListener listener) {
        DLog.v("BluetoothGpsManager");
//        NDK_parser = new AdsbParserJava();
        xgpsListener = listener;
        mXgpsParser = XGPSParser.getInstance();
        mXgpsParser.setListener(listener);
//        CommonValue.getInstance().parser = this.parser;
        this.gpsDeviceAddress = deviceAddress;
        this.maxConnectionRetries = maxRetries;
        this.nbRetriesRemaining = 1 + maxRetries;
        this.appContext = callingService.getApplicationContext();
        locationManager = (LocationManager)callingService.getSystemService(Context.LOCATION_SERVICE);
        notificationManager = (NotificationManager) callingService  .getSystemService(Context.NOTIFICATION_SERVICE);
        connectionProblemNotification = new Notification();
//        parser.setLocationManager(locationManager);
        Intent stopIntent = new Intent(ACTION_STOP_GPS_PROVIDER);
        PendingIntent stopPendingIntent = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopPendingIntent = PendingIntent.getService(appContext,0, stopIntent, PendingIntent.FLAG_MUTABLE);
        } else {
            stopPendingIntent = PendingIntent.getService(appContext,0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        connectionProblemNotification.contentIntent = stopPendingIntent;
        Intent restartIntent = new Intent( ACTION_START_GPS_PROVIDER);

        mNtripThread = new HandlerThread("NtripHandler");
        mNtripThread.start();
        mNtripHandler = new Handler(mNtripThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    if (isNtripRunning) {
                        return;
                    }
                    isNtripRunning = true;
                    final String host = SharedPrefHelper.getString(appContext, Constants.KEY_PREF_SERVER, null);
                    final String port = SharedPrefHelper.getString(appContext, Constants.KEY_PREF_PORT, null);
                    final String user = SharedPrefHelper.getString(appContext, Constants.KEY_PREF_USER, null);
                    final String password = SharedPrefHelper.getString(appContext, Constants.KEY_PREF_PASSWORD, null);
                    final int mode = SharedPrefHelper.getInt(appContext, Constants.KEY_PREF_MODE, Constants.DEFAULT_NTRIP_MODE);
                    if (host != null && port != null && user != null && password != null) {
                        String mountPoint = (String)msg.obj;
                        NtripClient.onCreate(BluetoothGpsManager.this, host, port, user, password, mode, mountPoint);
                    }
                    isNtripRunning = false;
                }
            }
        };
    }


    public void startNtrip(String mountpoint) {
        Message msg = new Message();
        msg.what = 0;
        msg.obj = mountpoint;
        if (isNtripRunning) {
            mNtripHandler.sendMessageDelayed(msg, 1000);
        }
        else {
            mNtripHandler.sendMessage(msg);
        }
    }

    public void stopNtrip() {
        mNtripHandler.removeCallbacksAndMessages(null);
        NtripClient.onDestroy();
    }


    private void onDestroyNtripThread() {
        stopNtrip();
        if (mNtripThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mNtripThread.quitSafely();
            }else {
                mNtripThread.quit();
            }
            mNtripThread = null;
        }
    }

    /**
     * @return
     */
    public int getDisableReason() {
        return disableReason;
    }

    public boolean isRunning() {
        return !(connectionAndReadingPool.isShutdown() || connectionAndReadingPool.isTerminated());
    }

    /**
     * @return true if the bluetooth GPS is enabled
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void disable() {
        enabled = false;
    }

    public void onDestroy() {
        DLog.v("onDestroy");
//        if (connectedGps != null && connected) {
//            connectedGps.close();
//        }
        if (connectionAndReadingPool != null && !connectionAndReadingPool.isShutdown()) {
            DLog.v("connectionAndReadingPool shutdown");
            connectionAndReadingPool.shutdown();
        }
        onDestroyNtripThread();
    }

    /**
     * Enables the bluetooth GPS Provider.
     *
     * @return
     */
    public synchronized boolean enable() {
        Log.i("", "enable() 들어옴");
        notificationManager.cancel(R.string.service_closed_because_connection_problem_notification_title);
        if (!enabled) {
            Log.i(LOG_TAG, "enabling Bluetooth GPS manager");
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                Log.e(LOG_TAG, "Device does not support Bluetooth");
            }
            else if (!bluetoothAdapter.isEnabled()) {
                Log.e(LOG_TAG, "Bluetooth is not enabled");
            }
            else if (gpsDeviceAddress != null){
                final BluetoothDevice gpsDevice = bluetoothAdapter.getRemoteDevice(gpsDeviceAddress);
                currentDevice = gpsDevice;
                CommonValue.getInstance().setDeviceName(currentDevice.getName());

                if (gpsDevice == null) {
                    Log.e(LOG_TAG, "GPS device not found");
                }
                else {
                    try {
                        gpsSocket = gpsDevice.createInsecureRfcommSocketToServiceRecord(UUID
                                .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error during connection", e);
                        gpsSocket = null;
                    }



                    if (gpsSocket == null) {
                        Log.e(LOG_TAG,"Error while establishing connection: no socket");
                    } else {

                        Runnable connectThread = new Runnable() {
                            @Override
                            public void run() {
                                try {
									Log.i(LOG_TAG, "current device: "+ gpsDevice.getName() + "--"+ gpsDevice.getAddress());

                                    if ((bluetoothAdapter.isEnabled() ) && (nbRetriesRemaining > 0)) {

                                        try {
                                            if (connectedGps != null) {
                                                connectedGps.close();
                                                connectedGps = null;
                                            }
                                            if ((gpsSocket != null)
                                                    && ((connectedGps == null) || (connectedGps.socket != gpsSocket))) {
                                                Log.i(LOG_TAG, "trying to close old socket");
                                                gpsSocket.close();
                                            }
                                        } catch (IOException e) {
                                            Log.e(LOG_TAG, "Error during disconnection",e);
                                        }


                                        try {
                                            gpsSocket = gpsDevice .createInsecureRfcommSocketToServiceRecord(UUID
                                                    .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                        } catch (IOException e) {
                                            Log.e(LOG_TAG, "Error during connection", e);
                                            gpsSocket = null;
                                        }


                                        if (gpsSocket == null) {
                                            Log.e(LOG_TAG, "Error while establishing connection: no socket");
                                        }
                                        else {
                                            if (bluetoothAdapter.isDiscovering()) {
                                                bluetoothAdapter.cancelDiscovery();
                                            }
                                            Log.i(LOG_TAG, "connecting to socket");
                                            gpsSocket.connect();
                                            Log.i(LOG_TAG, "connected to socket");

                                            nbRetriesRemaining = 1 + maxConnectionRetries;
                                            notificationManager .cancel(R.string.connection_problem_notification_title);
                                            Log.i(LOG_TAG, "starting socket reading task");
                                            connectedGps = new ConnectedGps( gpsSocket);
                                            enabled = true;
                                            connected = true;
                                            connectionAndReadingPool .execute(connectedGps);
                                            Log.i(LOG_TAG, "socket reading thread started");

                                            if (xgpsListener != null)
                                                xgpsListener.connected(true, 0);

                                        }
                                    }
                                } catch (IOException connectException) {
                                    // Unable to connect
                                    Log.e(LOG_TAG,"error while connecting to socket",connectException);

                                } catch( Exception e) {
                                    Log.e(LOG_TAG,"error while connecting to socket " + e.getMessage());
                                } finally {
                                    disableIfNeeded();
                                }
                            }
                        };

                        Log.i(LOG_TAG, "Bluetooth GPS manager enabled");
                        Log.i(LOG_TAG, "starting notification thread");
                        Log.i(LOG_TAG, "starting connection and reading thread");

                        connectionAndReadingPool = Executors.newSingleThreadScheduledExecutor();
                        Log.i(LOG_TAG, "starting connection to socket task");
                        connectionAndReadingPool.scheduleWithFixedDelay( connectThread, 100, 5000, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
        return this.enabled;
    }


    private synchronized void disableIfNeeded(){
        DLog.v("disableIfNeeded " + enabled + ", remain " + nbRetriesRemaining);
        if (enabled){
            if (nbRetriesRemaining > 0){
                // Unable to connect
                Log.e(LOG_TAG, "Unable to establish connection");
                connectionProblemNotification.when = System.currentTimeMillis();
//                String pbMessage = appContext.getResources().getQuantityString(R.plurals.connection_problem_notification, nbRetriesRemaining, nbRetriesRemaining);
//                connectionProblemNotification.setLatestEventInfo(appContext,
//                        appContext.getString(R.string.connection_problem_notification_title),
//                        pbMessage,
//                        connectionProblemNotification.contentIntent);
                connectionProblemNotification.number = 1 + maxConnectionRetries - nbRetriesRemaining;
                notificationManager.notify(R.string.connection_problem_notification_title, connectionProblemNotification);
            }
        }
    }

    public boolean addNmeaListener(NmeaListener listener) {
        if (!nmeaListeners.contains(listener)) {
            Log.i(LOG_TAG, "adding new NMEA listener");
            nmeaListeners.add(listener);
        }
        return true;
    }

    public void removeNmeaListener(NmeaListener listener) {
        Log.i(LOG_TAG, "removing NMEA listener");
        nmeaListeners.remove(listener);
    }


    // 블루투스 연결관련 실패 UI업뎃
    public void blueToothConnetFail() {
        if (xgpsListener != null)
            xgpsListener.connected(false, 0);
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            public void run() {
//                CommonValue.getInstance().fragmentOne.notConnectedUI();
//                CommonValue.getInstance().fragmentTwo.notConnectedUI();
//                CommonValue.getInstance().fragmentThree.notConnectedUI();
//                CommonValue.getInstance().BT_CONNECTED = false;
//                connected = false;
//                enabled = false;
//            }
//        });
    }



    public void enableMockLocationProvider(String gpsName){

//        if (parser != null){
//            parser.enableMockLocationProvider(gpsName, true);
//        }
    }

    // for ntrip client

    public boolean writeBufferToDevice(byte[] buffer, int bufLen) {
        if (connectedGps != null)
            return connectedGps.writeBufferToDevice(buffer, bufLen);
        return false;
    }

    public void getGNSSData(final byte[] buffer, final int buffLen, int mode) {
        DLog.v("getGNSSData " + buffLen + ", mode : " + mode);
        if (mode == 1) {
            writeBufferToDevice(buffer, buffLen);
            xgpsListener.receiveNtripData(buffLen);
        }
        else if (mode == 2) {       // error
            xgpsListener.onNtripError(new String(buffer));
        }
        else if (buffLen > 0) {
            String mountpoint = new String(buffer);
            DLog.v("mountpoint " + mountpoint);
            ntripMountPoints(mountpoint);
        }
        else if (buffLen == 0 && mode == 0) {
            ntripMountPoints(null);
        }
    }

    private StringBuilder mountListString = new StringBuilder();
    private void ntripMountPoints (String data)
    {
        if (data != null) {
            mountListString.append(data);
        }
        else {
            Pattern regex = Pattern.compile("STR;.+;.+");
            Matcher m = regex.matcher(mountListString);
            String mountPoint = "";
            double shortestDistance = 100000;   // allow within 100km
            String savedMountPoint = SharedPrefHelper.getString(appContext, Constants.KEY_MOUNT_POINT, "");
            while(m.find()) {
                String matchString = m.group(0);
                DLog.v("match : " + matchString);
                String[] elements = matchString.split(";");
                if (elements.length < 11)
                    continue;
                try {
                    double latitude = Double.parseDouble(elements[9]);
                    double longitude = Double.parseDouble(elements[10]);
                    double distance = Tools.getDistance(latitude, longitude, XGPSParser.getInstance().getLatitude(), XGPSParser.getInstance().getLongitude(), "K");
                    if (savedMountPoint.equals(elements[1])) {
                        mountPoint = elements[1];
                    }
                    else if (distance <= shortestDistance && distance != 0 && mountPoint.equals("")) {
                        shortestDistance = distance;
                        mountPoint = elements[1];
                    }
                    Location location = new Location("");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    NtripClient.addMountPoint(elements[1], location);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mountPoint.length() > 0) {
                xgpsListener.getSelectedMountPoint(mountPoint);
            }
        }
    }
}
