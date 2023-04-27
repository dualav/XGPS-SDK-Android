package com.namsung.xgpsmanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.namsung.xgpsmanager.data.SatellitesInfo;
import com.namsung.xgpsmanager.data.SatellitesInfoMap;
import com.namsung.xgpsmanager.data.XGPSError;
import com.namsung.xgpsmanager.utils.Constants;
import com.namsung.xgpsmanager.utils.DLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cnapman on 2017. 11. 16..
 */

public class XGPSManager {
    private static XGPSManager mXGPSManager;
    private static final String TAG = "XGPSManager";

    public static final int XGPS_MODE_1 = 1; 	// for 150
    public static final int XGPS_MODE_2 = 2;	// for 160
    public static final int XGPS_MODE_3 = 3;    // for 500
    public static final int XGPS_MODE_4 = 4;    // else

    public final static int MODE_POSITION_DMS = 0; // position display degree / minutes / seconds
    public final static int MODE_POSITION_DM = 1; // position display degree / minutes
    public final static int MODE_POSITION_DEGREE = 2; // position display degree

    public final static int TYPE_NO_FIX = 1;
    public final static int TYPE_2D_FIX = 2;
    public final static int TYPE_3D_FIX = 3;

    private int mCurrentXGPSMode = XGPS_MODE_1;
    private BluetoothAdapter bluetoothAdapter = null;
    private ArrayList<BluetoothDevice> mDeviceList = null;
    private String mModelName = null;
    private BluetoothDevice mSelectedDevice = null;
    private Context mContext = null;
    private NmeaParser parser = null;
    private boolean enabled = false;
    private boolean blocked = false;
    private boolean isConnected = false;
    private int mRemainRetryCount = Constants.BLUETOOTH_CONNECTING_RETRY;
    private ConnectedGps connectedGps;

    private XGPSListener xgpsListener;
    private HandlerThread mConnectingThread = null;    // 초기 연결 시도, 끊어진 후 연결 재시도
    private Handler mConnectingHandler;

    public static XGPSManager getInstance(Context context, XGPSListener listener) {
        if (mXGPSManager == null)
            mXGPSManager = new XGPSManager(context, listener);
        return mXGPSManager;
    }

    public XGPSManager(Context context, XGPSListener listener) {
        mContext = context;
        xgpsListener = listener;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDeviceList = new ArrayList<>();
        initialize();

        if (!bluetoothAdapter.isEnabled()) {
            isConnected = false;
            if (xgpsListener != null)  xgpsListener.connected(false, XGPSError.ERR_BLUETOOTH_ENABLE);
            return;
        }

        onResume();
    }

    public void onResume() {
        mRemainRetryCount = Constants.BLUETOOTH_CONNECTING_RETRY;
        if (isConnected)
            return;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Pattern p1 = Pattern.compile(".*(XGPS[1-9][0-9]0).*");
        Pattern p2 = Pattern.compile("(DashPro)-[0-9]*");
        Matcher m;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName == null || deviceName.length() == 0)
                    continue;

                // save default value
                if ((p1.matcher(deviceName).matches() || p2.matcher(deviceName).matches()) && BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
                    mDeviceList.add(device);
                }
            }
        }

        if (mDeviceList.size() > 0) {
            mSelectedDevice = mDeviceList.get(0);
            DLog.v("selected device : " + mSelectedDevice.getAddress());
            if (mSelectedDevice.getName() != null) {
                m = p1.matcher(mSelectedDevice.getName());
                if (m.matches()) {
                    mModelName = m.group(1);
                } else {
                    m = p2.matcher(mSelectedDevice.getName());
                    if (m.matches()) {
                        mModelName = m.group(1);
                    }
                }
            }
        }

        if (mSelectedDevice == null) {
            isConnected = false;
            if (xgpsListener != null)   xgpsListener.connected(false, XGPSError.ERR_NO_BONDED_DEVICE);
            return;
        }
        // if paired device is one, continue to connect
        connect();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setListener(XGPSListener listener) {
        DLog.d(TAG, "setListener : " + listener);
        xgpsListener = listener;
        if (parser != null)
            parser.setListener(xgpsListener);
    }

    private void initialize() {
        mConnectingThread = new HandlerThread("ConnectingHandler");
        mConnectingThread.start();
        mConnectingHandler = new Handler(mConnectingThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {     // for connecting thread
                    if (blocked)    return;
                    blocked = true;
                    DLog.d(TAG,"handleMessage connectingThread");
                    if (!(bluetoothAdapter.isEnabled())) {
                        if (xgpsListener != null)   xgpsListener.connected(false, XGPSError.ERR_BLUETOOTH_ENABLE);
                        postDelayed(retryConnectingThread, 5000);
                    }
                    else if (!socketConnecting())
                        postDelayed(retryConnectingThread, 5000);
                    else {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mConnectingHandler.sendEmptyMessage(2);
                            }
                        }, 200);
                    }
                    blocked = false;
                    DLog.d(TAG,"end of handleMessage");
                }
                else if (msg.what == 1) {
                    DLog.d(TAG,"handleMessage connectedGps");
                    BluetoothSocket gpsSocket = (BluetoothSocket) msg.obj;
                    connectedGps = new ConnectedGps(gpsSocket, "manager");
                    connectedGps.start();
//                        post(connectedGps);
                }
                else if (msg.what == 2) {
                    DLog.d(TAG,"handleMessage fw version");
                    sendCommandToDevice(Constants.cmd160_fwVersion, null, 0);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mConnectingHandler.sendEmptyMessage(3);
                        }
                    }, 200);
                }
                else if (msg.what == 3) {
                    DLog.d(TAG,"handleMessage settings");
                    getSettings();
                }
                else if (msg.what == 4) {
                    DLog.v(TAG, "send empty command");
                    sendCommandToDevice(Constants.cmd160_ack, null, 0);
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mConnectingHandler.sendEmptyMessage(4);
                        }
                    }, 2000);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        mContext.registerReceiver(XGPSReceiver, filter);

        this.parser = new NmeaParser(xgpsListener, 10f);
        LocationManager locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
//        parser.setLocationManager(locationManager);

    }

    public int getXgpsMode() {
        return mCurrentXGPSMode;
    }

    private void connect() {
        if (mSelectedDevice.getName().contains("XGPS150"))
            mCurrentXGPSMode = XGPS_MODE_1;
        else if (mSelectedDevice.getName().contains("XGPS160"))
            mCurrentXGPSMode = XGPS_MODE_2;
        else if (mSelectedDevice.getName().contains("XGPS500") || mSelectedDevice.getName().startsWith("XGPS360") || mSelectedDevice.getName().startsWith("DashPro"))
            mCurrentXGPSMode = XGPS_MODE_3;
        else
            mCurrentXGPSMode = XGPS_MODE_4;
        parser.setParserMode(mCurrentXGPSMode);

        if (!mConnectingHandler.hasMessages(0)) {
            mConnectingHandler.sendEmptyMessage(0);
        }
//        mConnectingHandler.sendEmptyMessage(4);
    }

    public void setMockEnable(boolean enable) {
//        if (enable) {
//            if (parser != null) parser.enableMockLocationProvider("gps", true);
//        }
//        else {
//            if (parser != null) parser.disableMockLocationProvider();
//        }
    }

    private BroadcastReceiver XGPSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                DLog.v("@@@@@@@@@@@", "BT found");
            } else if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                DLog.v("@@@@@@@@@@@", "BT Connected");
//                NtripClient.onCreate(XGPSManager.this);
            } else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                DLog.v("@@@@@@@@@@@", "BT Disconnected");
//                NtripClient.onDestroy();
                if (connectedGps != null && connectedGps.socket != null) {
                    connectedGps.close();
                }
                mRemainRetryCount = Constants.BLUETOOTH_CONNECTING_RETRY;
                if (!mConnectingHandler.hasMessages(0)) {
                    DLog.d(TAG, "sendEmptyMessage 0");
                    mConnectingHandler.sendEmptyMessage(0);
                }
                DLog.v("@@@@@@@@@@@", "end of BT Disconnected");
            } else
                DLog.v("@@@@@@@@@@@", "BT Disconnect requested");
        }
    };

    private boolean socketConnecting() {
        DLog.d(TAG, "socketConnecting");
        BluetoothSocket gpsSocket;

        try {
            if (connectedGps != null) {
                gpsSocket = connectedGps.socket;
                connectedGps.close();
                if (gpsSocket != null) {
                    DLog.d(TAG, "trying to close old socket");
                    gpsSocket.close();
                }
            }

            if (xgpsListener != null) xgpsListener.connecting(mSelectedDevice);

            gpsSocket = mSelectedDevice.createInsecureRfcommSocketToServiceRecord(UUID
                    .fromString("00001101-0000-1000-8000-00805F9B34FB"));       // SerialPortServiceClass_UUID
            if (gpsSocket == null) {
                DLog.e(TAG, "Error while establishing connection: no socket");
            }
            else {
                bluetoothAdapter.cancelDiscovery();
                DLog.v(TAG, "connecting to socket");

                gpsSocket.connect();
                DLog.v(TAG, "connected to socket");
                DLog.v(TAG, "starting socket reading task");
                enabled = true;
                isConnected = true;
                Message message = new Message();
                message.what = 1;
                message.obj = gpsSocket;
                mConnectingHandler.sendMessage(message);
                if (xgpsListener != null) xgpsListener.connected(true, XGPSError.ERR_NO_ERROR);
                return true;
            }
        } catch (Exception e) {
            DLog.e(TAG, "Error during disconnection",e);
            isConnected = false;
            if (xgpsListener != null) xgpsListener.connected(false, XGPSError.ERR_FAIL_TO_CONNECT);
            int index = mDeviceList.indexOf(mSelectedDevice);
            if (index == -1) {
                mSelectedDevice = null;
            }
            else {
                if (index < mDeviceList.size() - 1)
                    index++;
                else
                    index = 0;
                mSelectedDevice = mDeviceList.get(index);
            }
        }
        return false;
    }

    private Runnable retryConnectingThread = new Runnable() {
        @Override
        public void run() {
            DLog.d(TAG, "connectingThread");
            if (mSelectedDevice != null && bluetoothAdapter != null)
                DLog.d(TAG, "current device: "+ mSelectedDevice.getName() + "--"+ mSelectedDevice.getAddress() + ", BTAdapter enable " + bluetoothAdapter.isEnabled());

            if (mRemainRetryCount < 0) {
                DLog.e(TAG, "fail to connect bluetooth.");

                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mContext != null)
                            Toast.makeText(mContext, "fail to connect bluetooth.", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }

            if (isConnected) {
                DLog.d(TAG, "isConnected " + isConnected);
                return;
            }
            mRemainRetryCount--;
            if (!mConnectingHandler.hasMessages(0)) {
                mConnectingHandler.sendEmptyMessage(0);
            }
        }
    };

    private class ConnectedGps extends Thread {

        private final BluetoothSocket socket;
        public final InputStream in;
        private final OutputStream out;
        private final PrintStream out2;
        private boolean ready = true;

        public ConnectedGps(BluetoothSocket socket, String name) {
            super(name);
            DLog.d(TAG, "start ConnectedGps " + socket.isConnected());
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            PrintStream tmpOut2 = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                if (tmpOut != null) {
                    tmpOut2 = new PrintStream(tmpOut, false, "US-ASCII");
                }
            } catch (IOException e) {
                DLog.e(TAG, "error while getting socket streams", e);
            }
            in = tmpIn;
            out = tmpOut;
            out2 = tmpOut2;
            parser.initNMEAParser();
        }

        // 스트림 받는 부분 //////////////////////////////////////////////////////////////////////////////
        public void run() {
            // Keep listening to the InputStream while connected
            try {
                int length = 0;
                byte[] buffer = new byte[4096];
                DLog.d(TAG, "socket reading thread started " + in.available() + ", connect?" + socket.isConnected());
//                reader = new BufferedReader( new InputStreamReader(in, "ISO-8859-1"));

                while(enabled){
                    if (in.available() > 0) {
                        try {
                            length = in.read(buffer);
                            if (length > 0) {
                                parser.handleInputStream(buffer, length);
                            }
                            // debug
//                            DLog.d(TAG, "raw : " + String.valueOf(buffer, 0, length));
                            ready = true;
                            isConnected = true;
                        } catch (Exception e) {
                            DLog.e(TAG, "error while handle input stream : " + e.getMessage());
                            String debugString = "";
                            for (int i = 0; i < Math.min(length, 20); i++) {
                                debugString += String.format("%02x ", (int)buffer[i]);
                            }
                            DLog.d(TAG, "length : " + length + ", buffer " + debugString);
                        }
                    }
//					else
//						SystemClock.sleep(CommonValue.getInstance().blueTooth_sleepTime);

                }
            } catch (IOException e) {
                DLog.e(TAG, "error while getting data", e);
            } catch (Exception e) {
                DLog.e(TAG, "error while getting data", e);
            } finally {
                // cleanly closing everything...
                DLog.e(TAG, "Close in inputstream thread");
//                this.close();
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        public Boolean sendCommandToDevice(int cmd, byte[] arg, int argLen, boolean isWaitResponse) {
            try {
                do {
                    Thread.sleep(10);
                } while ((enabled) && (!ready));

                int i;
                int size = 0;
                byte cs = 0;
                byte xbuf[] = new byte[256];

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
                parser.rsp160_cmd = -1;
                if ((enabled) && (ready)) {
                    StringBuilder debugString = new StringBuilder("");
                    for (int index = 0; index < size; index++) {
                        debugString.append(String.format("%02x ", (byte)xbuf[index]));
                    }
                    DLog.v(TAG, "write xbuf : " + debugString.toString());
                    out.write(xbuf);
                    out.flush();

                    // if listner is not null wait response
                    if (isWaitResponse) {
                        int timeout = 100;
                        do {
                            Thread.sleep(10);
                            if (parser.rsp160_cmd == -1)
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
                DLog.e(TAG, "Exception during write", e);
            }
            catch (InterruptedException e) {
                DLog.e(TAG, "Exception during write", e);
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

                // ios     :  136 : 238 : 1 : 10
                // android : -120 : -18 : 1 : 10

                if ((enabled) && (ready)) {
                    DLog.v(TAG, "xbuf: "+(xbuf[0]&0xFF)+": "+(xbuf[1]& 0xFF)+": "+(xbuf[2]& 0xFF)+": "+(xbuf[3]& 0xFF)+": "+(xbuf[4]& 0xFF));
                    out.write(xbuf);
                    out.flush();
                }

            } catch (IOException e) {
                DLog.e(TAG, "Exception during write", e);
            }
            catch (InterruptedException e) {
                DLog.e(TAG, "Exception during write", e);
            }

            return true;
        }

        public Boolean writeBufferToDevice(byte[] buffer, int argLen) {
            try {
                do {
                    Thread.sleep(10);
                } while ((enabled) && (!ready));

                int i;
                int size = 0;

                // ios     :  136 : 238 : 1 : 10
                // android : -120 : -18 : 1 : 10
                parser.rsp160_cmd = -1;
                if ((enabled) && (ready)) {
                    StringBuilder debugString = new StringBuilder("");
                    for (int index = 0; index < argLen; index++) {
                        debugString.append(String.format("%02x ", (byte)buffer[index]));
                    }
                    DLog.v(TAG, "writeBufferToDevice buffer : " + debugString + ", " + argLen);
                    out.write(buffer);
                    out.flush();

                    // if listner is not null wait response
//                    if (isWaitResponse) {
//                        int timeout = 100;
//                        do {
//                            Thread.sleep(10);
//                            if (parser.rsp160_cmd == -1)
//                                timeout--;
//                            else
//                                break;
//                        } while (timeout > 0);
//                        if (timeout > 0)
//                            return true;
//                        else {
//                            DLog.v("response timeout");
//                            return false;
//                        }
//                    }
//                    else {
                        return true;
//                    }
                }
                else{
                    return false;
                }

            } catch (IOException e) {
                DLog.e(TAG, "Exception during write", e);
            }
            catch (InterruptedException e) {
                DLog.e(TAG, "Exception during write", e);
            }

            return false;
        }



        public void close() {
            ready = false;
            enabled = false;
            isConnected = false;
//            parser.firmwareVerString = Constants.UNKNOWNSTRING;
            if (xgpsListener != null) xgpsListener.connected(false, XGPSError.ERR_NO_ERROR);
//            connected = false;
            try {
                DLog.i(TAG, "closing Bluetooth GPS output stream");
                in.close();
            } catch (IOException e) {
                DLog.e(TAG, "error while closing GPS NMEA output stream", e);
            } finally {
                try {
                    DLog.i(TAG, "closing Bluetooth GPS input streams");
                    out2.close();
                    out.close();
                } catch (IOException e) {
                    DLog.e(TAG, "error while closing GPS input streams", e);
                } finally {
                    try {
                        DLog.i(TAG, "closing Bluetooth GPS socket");
                        socket.close();
                    } catch (IOException e) {
                        DLog.e(TAG, "error while closing GPS socket", e);
                    }
                }
            }
            connectedGps = null;
            this.interrupt();
        }
    }

    public void sendCommandToDevice(int cmd, byte[] buffer, int bufLen) {
        sendCommandToDevice(cmd, buffer, bufLen, false);
    }

    public boolean sendCommandToDevice(int cmd, byte[] buffer, int bufLen, boolean isWaitResponse) {
        if (cmd == Constants.cmd160_logList) {
            parser.initNMEAParser();
        }

        if (connectedGps != null)
            return connectedGps.sendCommandToDevice(cmd, buffer, bufLen, isWaitResponse);
        return false;
    }

    public byte[] getFwDataRead() {
        if (parser != null) {
            return parser.pFwReadBuf;
        }
        return null;
    }

    public void initFwDataRead() {
        if (parser != null)
            parser.pFwReadBuf = null;
    }

    public void resetDevice(byte[] buf) {
//        parser.firmwareVerString = Constants.UNKNOWNSTRING;
        if( parser.rsp160_cmd == Constants.cmd160_fwRsp && parser.rsp160_buf[0] == Constants.cmd160_fwUpdate ){
            DLog.i(TAG, "returned cs=" + parser.rsp160_buf[2]+" "+parser.rsp160_buf[3]);
            if( parser.rsp160_buf[1] == (byte)0x11 ){
                DLog.i(TAG, "Issuing cpu reset command");

                // Reset the MCU
                buf[0] = (byte)0xAB; // addr H
                buf[1] = (byte)0xCD; // addr L

                if( !sendCommandToDevice(Constants.cmd160_fwUpdate, buf, 7, true) ){
                    DLog.i(TAG, "reset command failed. Please reset the unit manually");
                }
            }
            else if( parser.rsp160_buf[1] == (byte)0xEE ){
                DLog.i(TAG, "Update verify checksum failure");
            }
            else{
                DLog.i(TAG, "Update verify unknown failure <"+parser.rsp160_buf[1]+">");
            }
        }
    }

    public String getDeviceMacAddress() {
        if (mSelectedDevice == null)
            return null;

        return mSelectedDevice.getAddress();
    }

    public String getDeviceName() {
        if (mSelectedDevice == null)
            return null;

        return mSelectedDevice.getName();
    }

    public String getModelName() {
        if (mModelName == null)
            return Constants.UNKNOWNSTRING;
        return mModelName;
    }

    public BluetoothDevice getDevice() {
        return mSelectedDevice;
    }

    public void setDevice(BluetoothDevice device) {
        mSelectedDevice = device;
        connect();
    }

    public ArrayList<BluetoothDevice> getAvailableDevices() {
        return mDeviceList;
    }

    public float getBatteryLevel() {
        if (parser == null)
            return 0.0f;
        return parser.batteryVoltage;
    }

    public boolean isCharging() {
        if (parser == null)
            return false;
        return parser.isCharging;
    }

    public String getFirmwareVersion() {
        if (parser == null) {
            return null;
        }
//        if (parser.firmwareVerString.equals(Constants.UNKNOWNSTRING)) {
//            sendCommandToDevice(Constants.cmd160_fwVersion, null, 0);
//        }
        return null; //parser.firmwareVerString;
    }

    public void getSettings() {
        sendCommandToDevice(Constants.cmd160_getSettings, null, 0);
    }

    public String getLatitude(int mode) {
        if (parser == null)
            return Constants.UNKNOWNSTRING;

        String displayLatitude;
        // 위.경도
        if (mode == Constants.MODE_POSITION_DMS) {

            displayLatitude = parser.latDeg+"˚"+
                    String.format("%.0f'", Math.floor(parser.latMins) )+
                    String.format("%1.1f”", (parser.latMins - Math.floor(parser.latMins)) * 60) + parser.latDirString;
        }
//        else if (mode == Constants.MODE_POSITION_DM) {
//            displayLatitude = parser.latDeg + "˚" + parser.latMinsString + "'" + parser.latDirString;
//        }
        else {
            displayLatitude = String.format("%.5f˚", (parser.latDeg + (parser.latMins/60.0)))+
                    parser.latDirString;
        }
        return displayLatitude;
    }

    public float getLatitude() {
        if (parser == null)
            return 0.0f;

        int sign = (parser.latDirString.equals("N"))?1:-1;
        return (float)((parser.latDeg + (parser.latMins/60.0)) * sign);
    }

    public String getLongitude(int mode) {
        if (parser == null)
            return Constants.UNKNOWNSTRING;

        String displayLongitude;
        if (mode == Constants.MODE_POSITION_DMS) {
            displayLongitude = parser.lonDeg+"˚"+
                    String.format("%.0f'", Math.floor(parser.lonMins) )+
                    String.format("%1.1f”", (parser.lonMins - Math.floor(parser.lonMins)) * 60) + parser.lonDirString;

        }
//        else if (mode == Constants.MODE_POSITION_DM) {
//            displayLongitude = parser.lonDeg + "˚" + parser.lonMinsString + "'" + parser.lonDirString;
//        }
        else {
            displayLongitude = String.format("%.5f˚", (parser.lonDeg + (parser.lonMins/60.0)))+
                    parser.lonDirString;
        }
        return displayLongitude;
    }

    public float getLongitude() {
        if (parser == null)
            return 0.0f;

        int sign = (parser.lonDirString.equals("E"))?1:-1;
        return (float)((parser.lonDeg + (parser.lonMins/60.0)) * sign);
    }

    public String getAltitude(int mode) {
        String displayAltitude;
        float alt;
        try {
            alt = Float.parseFloat( parser.altString);
        } catch (Exception e) {
            return Constants.UNKNOWNSTRING;
        }
        if (mode == Constants.MODE_ALTITUDE_FEET)
            displayAltitude = String.format("%.2f",  alt * 3.2808399f) + " ft";
        else
            displayAltitude = String.format("%.2f",  alt)+" m";
        return displayAltitude;
    }

    public float getAltitude() {
        float alt = 0.0f;
//        try {
//            alt = Float.parseFloat( NmeaParser.altString);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return alt;
    }

    public String getUTC() {
        if (parser == null || parser.time == null || parser.time.length() == 0 || parser.time.length() < 6)
            return Constants.UNKNOWNSTRING;
        return parser.time.substring(0, 2) + ":" + parser.time.substring(2, 4)+":" + parser.time.substring(4);
    }

    public String getSpeed(int mode) {
        if (parser == null || parser.speedString == null || parser.speedString.length() == 0)
            return Constants.UNKNOWNSTRING;
        String displaySpeed;
        try {
            if (mode == Constants.MODE_SPEED_KNOTS)
                displaySpeed = String.format("%.0f knots",Float.parseFloat(parser.speedString)); // 노트
            else if(mode == Constants.MODE_SPEED_KPH)
                displaySpeed = String.format("%.0f", Float.parseFloat( parser.speedString) * 1.852f) +" kph";
            else
                displaySpeed = String.format("%.0f", (Float.parseFloat( parser.speedString) * 1.852f * 0.621371192f))+" mph";
        } catch (Exception e) {
            return Constants.UNKNOWNSTRING;
        }
        return displaySpeed;
    }

    public float getSpeed() {
        if (parser == null || parser.speedString == null || parser.speedString.length() == 0)
            return 0;
        return Float.parseFloat(parser.speedString);
    }

    public String getHeadingString() {
        String displayHeading = Constants.UNKNOWNSTRING;
//        try {
//            if (Float.parseFloat( NmeaParser.speedString) * 1.852f > 1.6f)
//                displayHeading = NmeaParser.headingString+"˚ T";
//        } catch (Exception e) {
//            return Constants.UNKNOWNSTRING;
//        }
        return displayHeading;
    }

    public float getHeading() {
//        try {
//            if (Float.parseFloat( NmeaParser.speedString) * 1.852f > 1.6f)
//                return Float.parseFloat(NmeaParser.headingString);
//        } catch (Exception e) {
//            return 0;
//        }
        return 0;
    }

    public int getHDOP() {
        float hdop = 0;
//        try {
//            hdop = Float.parseFloat(parser.hdopString);
//        } catch (Exception e) {
//            return -1;
//        }

        return (int)(((20.0 - hdop) / 20.0) * 100);
    }

    public int getVDOP() {
//        if (parser == null || parser.fixType == null || !parser.fixType.equals("3")) {
//            return -1;
//        }
        float vdop = 0;
//        try {
//            vdop = Float.parseFloat(parser.vdopString);
//        } catch (Exception e) {
//            return -1;
//        }

        return (int)(((20.0 - vdop) / 20.0) * 100);
    }

    public int getPDOP() {
//        if (parser == null || parser.fixType == null || !parser.fixType.equals("3")) {
//            return -1;
//        }
        float pdop = 0;
//        try {
//            pdop = Float.parseFloat(parser.pdopString);
//        } catch (Exception e) {
//            return -1;
//        }

        return (int)(((20.0 - pdop) / 20.0) * 100);
    }

    public synchronized HashMap<Integer, SatellitesInfo> getSatellitesMap() {
//        return parser.dictOfSatInfo;
        return (HashMap) parser.satellitesInfoMap.get(1, 1);
    }

    public SatellitesInfoMap getSatellitesInfoMap() {
        return parser.getSatellitesInfoMap();
    }

    public synchronized HashMap<Integer, SatellitesInfo> getGlonassSatellitesMap() {
//        return parser.dictOfSatInfoGlonass;
        return (HashMap) parser.satellitesInfoMap.get(2, 1);
    }

    public int getAverageSNRInUse() {
        if (parser == null)
            return 0;
        return parser.avgUsableSatSNR();
    }

    public ArrayList<String> getSatellitesInUse(int systemId) {
        if (parser == null)
            return null;
        return parser.getSatsUsedInPosCalc(systemId);
    }

    public ArrayList<String> getGPSSatellitesInUse() {
        if (parser == null)
            return null;
        return parser.getGPSSatellitesInUse();
    }

    public ArrayList<String> getGlonassSatellitesInUse() {
        if (parser == null)
            return null;
        return parser.getGlonassSatellitesInUse();
    }

    public int getFixType() {
        int fixType = 0;
//        try {
//            fixType = Integer.parseInt(parser.fixType);
//        } catch (Exception e) {
//            DLog.e(TAG, "fix type parsing error : " + e.getMessage());
//            e.printStackTrace();
//        }

        return fixType;
    }

    public void setEndLogBulk() {
        if (parser == null)
            return;
        parser.logBulkEndReceive();
    }

    public void onDestroy() {
        try {
            if (connectedGps != null) {
                BluetoothSocket gpsSocket = connectedGps.socket;
                connectedGps.close();
                if (gpsSocket != null) {
                    gpsSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            DLog.e(TAG, "onDestroy IOException " + e.getMessage());
        }

        try {
            mContext.unregisterReceiver(XGPSReceiver);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            DLog.e(TAG, "onDestroy IllegalArgumentException " + e.getMessage());
        }

        if (mConnectingThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mConnectingThread.quitSafely();
            }else {
                mConnectingThread.quit();
            }
            mConnectingThread = null;
        }
        mSelectedDevice = null;
        mDeviceList.clear();
        mXGPSManager = null;
    }

    public String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public int getStreamMode() {
        return parser.mStreamingMode;
    }

    // for ntrip client

    public boolean writeBufferToDevice(byte[] buffer, int bufLen) {
        if (connectedGps != null)
            return connectedGps.writeBufferToDevice(buffer, bufLen);
        return false;
    }

    public void getGNSSData(byte[] buffer, int buffLen) {
        DLog.v(TAG, "getGNSSData" + buffer[0] + ", len : " + buffLen);
        writeBufferToDevice(buffer, buffLen);
    }
}
