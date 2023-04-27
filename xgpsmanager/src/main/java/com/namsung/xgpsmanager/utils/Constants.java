package com.namsung.xgpsmanager.utils;

/**
 * Created by cnapman on 2017. 11. 16..
 */

public class Constants {
    public final static int BLUETOOTH_CONNECTING_RETRY = 20;

    public final static int MAX_BUFFER_SIZE = 4096;

    public final static String UNKNOWNSTRING = "--";
    public final static String WAITING_SAT = "Waiting for sat data";

    public final static int MODE_ALTITUDE_FEET = 0;
    public final static int MODE_ALTITUDE_METER = 1;

    public final static int MODE_POSITION_DMS = 0; // position display degree / minutes / seconds
    public final static int MODE_POSITION_DM = 1; // position display degree / minutes
    public final static int MODE_POSITION_DEGREE = 2; // position display degree

    public final static int MODE_SPEED_KNOTS = 0;
    public final static int MODE_SPEED_MPH = 1;
    public final static int MODE_SPEED_KPH = 2;

    public final static String KEY_PREF_SPEED = "key_pref_speed";
    public final static String KEY_PREF_ALTITUDE = "key_pref_altitude";
    public final static String KEY_PREF_POSITION = "key_pref_position";
    public final static String KEY_PREF_ALWAYS_RECORD = "key_pref_always_record";
    public final static String KEY_PREF_OVERWRITE = "key_pref_overwrite";
    public final static String KEY_PREF_OLD_SDK = "key_pref_old_sdk";
    public final static String KEY_PREF_MARK_LIST = "key_mark_list";

    // ntrip network
    public final static String KEY_PREF_DGPS_NETWORK = "key_network";
    public final static String KEY_PREF_SERVER = "key_server";
    public final static String KEY_PREF_PORT = "key_port";
    public final static String KEY_PREF_USER = "key_user";
    public final static String KEY_PREF_PASSWORD = "key_password";
    public final static String KEY_PREF_MODE = "key_mode";
    public final static String KEY_AUTO_MOUNTPOINT = "key_auto_mountpoint";
    public final static String KEY_MOUNT_POINT = "key_mount_point";

    public final static String DEFAULT_NTRIP_HOST = "ap2.skylark.swiftnav.com";// "www.gnssdata.or.kr";
    public final static String DEFAULT_NTRIP_PORT = "2101";
    public final static String DEFAULT_NTRIP_USER = "dualpuck_testing"; //"gnss";
    public final static String DEFAULT_NTRIP_PASSWORD = "Hydg4qF9XR"; // "gnss";
    public final static int DEFAULT_NTRIP_MODE = 3;


    public final static int cmd160_ack=0;
    public final static int cmd160_nack=1;
    public final static int cmd160_response=2;
    public final static int cmd160_fwRsp=3;
    public final static int cmd160_fwData=4;
    public final static int cmd160_fwDataR=5;
    public final static int cmd160_fwErase=6;
    public final static int cmd160_fwUpdate=7;
    public final static int cmd160_fwBDADDR=8;
    public final static int cmd160_fwCancel=9;

    public final static int cmd160_streamStop=10;
    public final static int cmd160_streamResume=11;
    public final static int cmd160_logDisable=12;
    public final static int cmd160_logEnable=13;  // 전원 껏다켜도 로그기능 살아잇음
    public final static int cmd160_logOneshot=14; //  전원 켜면 로그 기능 꺼짐
    public final static int cmd160_logPause=15; // 로그 일시정지
    public final static int cmd160_logResume=16;// 로그 재시작
    public final static int cmd160_logInterval=17;  // 로그가 날아오는 hz 설정
    public final static int cmd160_logOWEnable=18;	// datalog over-write enable
    public final static int cmd160_logOWDisable=19;// datalog over-write disable

    public final static int cmd160_getSettings=20; // 기기의 모든 설정정보 가져옴
    public final static int cmd160_logReadBulk=21; // 로그 상세 가져옴
    public final static int cmd160_logList=22;     // 로그 리스트 헤더 정보만 가져옴
    public final static int cmd160_logListItem=23; // 리스트 중에 못받은것 개별로 가져오기 -> 별로 안쓰임
    public final static int cmd160_logRead=24;     // 로그 상세중에 못받은것 개별로 가져오기 -> 별로 안쓰임
    public final static int cmd160_logDelBlock=25; // 헤어에서 날아온 블럭정보가 잇는데 그 블럭 정보로 각각의 로그를 지울수 잇음
    public final static int cmd160_resetSettings=26;
    public final static int cmd160_fwVersion=27;
    public final static int cmd160_recentList=28;  // 블루투스 연결기기 목록 보여줌
    public final static int cmd160_recentDel=29;   // 그 기기 연결 끊음

    public final static int cmd160_gpsForceColdStart = 44;

    // XGPS500 COMMANDS
    public final static int cmd160_GetProductName = 202;
    public final static int cmd160_gpsSetRefreshRate = 71;

    public final static int cmd160_setPowerGps = 72;
    public final static int cmd160_setPowerBt = 73;

    public final static int cmd160_logTypeSet = 54;

    // commands added on 20160105
    public final static int cmd160_fileList = 80;
    public final static int cmd160_fileDelete = 81;
    public final static int cmd160_fileReadOpen = 82;
    public final static int cmd160_fileRead = 83;
    public final static int cmd160_fileWriteOpen = 84;
    public final static int cmd160_fileWrite = 85;
    public final static int cmd160_fileClose = 86;
    public final static int cmd160_fileDump = 87;
    //#endif

    //#if XGPS_MODEL == 165
    // commands for AHRS configuration
    public final static int cmd160_compassCalib = 90;
    public final static int cmd160_compassCalibInfo = 91;
    //#endif

    //#if XGPS_MODEL == 500
    public final static int cmd160_setStreamMode = 100;
    public final static int cmd160_getStreamMode = 101;
}
