
package com.namsung.xgpsmanager;

import android.text.TextUtils.SimpleStringSplitter;

import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.data.SatellitesInfo;
import com.namsung.xgpsmanager.data.SettingInfo;
import com.namsung.xgpsmanager.utils.Constants;
import com.namsung.xgpsmanager.utils.DLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NmeaParser extends BaseParser {

	private String TAG = "XGPSManager";
	private int parserMode = 1;

	// for log
	private volatile int logBulkRecodeCnt = 0;
	private volatile int logBulkOffset = 0;
	private volatile int prevAddr = 0;
	private ArrayList<LogData> logList = new ArrayList<>();
	private ArrayList<LogBulkData> logBulkList = new ArrayList<>();
	private final ArrayList<Object> logRawDataList = new ArrayList<>();
	private final TreeMap<Integer, ArrayList<Object>> logRawDataMap = new TreeMap<>();
	private boolean isGetGPGSA = false;
	private boolean isGetGNGSA = false;
//
//

//	/*
//	 * 여기서부터 NMEA 파싱 변수들
//	 */
//	public static String firmwareVerString = Constants.UNKNOWNSTRING;
//	public static String deviceNameString = null;
//	public static String altString = null;
//	private static String latString = null;
//	private static String longString = null;
//	public static String speedString = null;
//	public static String headingString = null;
//	private static int numOfSatInUseGlonass = 0;
//	private static int numOfSatInUse = 0;
//	private static ArrayList<String> satsUsedInPosCalcGlonass = new ArrayList<String>();
//	private static ArrayList<String> satsUsedInPosCalc = new ArrayList<String>();
//
//	public static float batteryVoltage = 0;
//	public boolean isCharging = false;
//
//
//	private int numOfSatInView = 0, numOfSatInViewGlonass = 0;
//	public static ConcurrentHashMap<Integer, SatellitesInfo> dictOfSatInfoGlonass = new ConcurrentHashMap<>();
//	public static ConcurrentHashMap<Integer, SatellitesInfo> dictOfSatInfo = new ConcurrentHashMap<>();
//	private ConcurrentHashMap<Integer, SatellitesInfo> tempSatInfoList = new ConcurrentHashMap<>();
//	private ConcurrentHashMap<Integer, SatellitesInfo> tempSatInfoGlonassList = new ConcurrentHashMap<>();
//
//
//	// PRMC
//	public static String latMinsString = "";
//	public static float latMins = 0;
//	public static int latDeg = 0;
//	public static String lonMinsString = "";
//	public static float lonMins = 0;
//	public static int lonDeg = 0;
//	public static String latDirString = "";
//	public static String lonDirString = "";
//	// End 파싱 멤버변수
//	// //////////////////////////////////////////////////////////////////////////////
//
	byte[] pFwReadBuf = null;
    public int mStreamingMode = SettingInfo.STREAM_MODE_NMEA;

	volatile int rsp160_cmd;
	volatile byte rsp160_buf[] = new byte[256];

//	private XGPSListener xgpsListener;
	private byte[] commandQueue = null;

//	private long totalCount = 0;
//	private long successCount = 0;

	// Start functions
	public NmeaParser() {
		DLog.d(TAG, "NmeaParse");
	}

	public void setParserMode(int parserMode) {
		this.parserMode = parserMode;
	}

	public int getParserMode() {
	    return parserMode;
    }

	public void handleInputStream(final byte[] buffer, final int length) {

		byte[] queueBuffer = null; // = ByteBuffer.wrap(buffer);
		byte[] validBuffer = new byte[length];
		if (buffer[2]  == (byte)0x85 && buffer[3] == (byte)4)	// debug point
			DLog.v("here");
		System.arraycopy(buffer, 0, validBuffer, 0, length);
		if (commandQueue != null) {
			queueBuffer = commandQueue;
		}
		queueBuffer = appendBuffer(queueBuffer, validBuffer);
		commandQueue = null;
		boolean isCommandRemain = false;

		do {
			ArrayList<byte[]> commands = separateCommands(queueBuffer);
			if (commands.size() == 0)
				break;
			if (commands.size() > 1) {
				isCommandRemain = true;
				queueBuffer = commands.get(1);
			}
			else
				isCommandRemain = false;
			parseInputStreams(commands.get(0));
		} while (isCommandRemain);
	}

	private byte[] appendBuffer(byte[] src, byte[] des) {
		int outLength = 0;
		int offset = 0;
		if (src != null)
			outLength += src.length;
		if (des != null)
			outLength += des.length;
		byte[] out = new byte[outLength];
		if (src != null) {
			System.arraycopy(src, 0, out, 0, src.length);
			offset = src.length;
		}
		if (des != null && offset >= 0) {
			System.arraycopy(des, 0, out, offset, des.length);
		}
		return out;
	}

	private byte[] getSubArray(byte[] buffer, int position, int length) {
		byte[] out = new byte[length];
		System.arraycopy(buffer, position, out, 0, length);
		return out;
	}

	private ArrayList<byte[]> separateCommands(byte[] buffer) {
		ArrayList<byte[]> buffers = new ArrayList<>();
		int offset = 0;
		int length = 0;
		int customOffset = -1;
		int nmeaOffset = -1;
		int nmeaLength = 0;
		int customLength = 0;

		for (int i = 1; i < buffer.length; i++) {
			if (buffer[i - 1] == (byte)'$' && (buffer[i] == (byte)'G' || buffer[i] == (byte)'X')) {
				nmeaOffset = i - 1;
			}
			if (buffer[i - 1] == (byte)'X' && buffer[i] == (byte)'C') {
				nmeaOffset = i - 1;
			}
			if (nmeaOffset != -1 && nmeaLength == 0 && buffer[i] == (byte)0x0a) {		// for nmea & XCFG
				nmeaLength = (i - nmeaOffset + 1);
				break;
			}
			if (nmeaOffset != -1 && nmeaLength == 0 && buffer[i] == (byte)'$') {
				nmeaLength = (i - nmeaOffset);
				break;
			}
			if (nmeaOffset != -1 && nmeaLength == 0 && (i+1 < buffer.length && buffer[i] == (byte)'X' && buffer[i+1] == (byte)'C')) {
				nmeaLength = (i - nmeaOffset);
				break;
			}
			if (buffer[i - 1] == (byte)0x88 && buffer[i] == (byte)0xee) {		//  && buffer[i+1] > 4
				if (nmeaOffset != -1 && nmeaLength == 0) {
					nmeaLength = (i - nmeaOffset - 1);
					break;
				}
				else {
					customOffset = i - 1;
					if (i + 1 < buffer.length - 1) {
						int cmdParamLen = (buffer[i + 1] & 0xff);
						if (cmdParamLen + 4 <= (buffer.length - i + 1)) {
							customLength = cmdParamLen + 4;		// 88, ee, len, cs
							break;
						}
					}
				}
			}
		}
		if (customLength == 0 && nmeaLength == 0) {
			commandQueue = getSubArray(buffer, 0, buffer.length);
			return buffers;
		}

		if (customLength == 0) {
			offset = nmeaOffset;
			length = nmeaLength;
		}
		else if (nmeaLength == 0) {
			offset = customOffset;
			length = customLength;
		}
		else {
			length = Math.min(nmeaLength, customLength);
		}
		buffers.add(getSubArray(buffer, offset, length));
		if (offset + length < buffer.length) {
			buffers.add(getSubArray(buffer, offset + length, buffer.length - (offset + length)));
		}

		return buffers;
	}

	private void parseInputStreams(byte[] buffer) {
		if (buffer[0] == (byte)0x88 && buffer[1] == (byte)0xee) {	// buffer.length > 4 &&
			parseCustomCommand(buffer);
		}
		else if (buffer[0] == (byte)'X' && buffer[1] == (byte)'C') {
			DLog.v("******************** XCFG " + buffer[0] + buffer[1] + buffer[2]);
		}
		else {
			parseNmeaSentence(new String(buffer));
		}
	}

	private void parseCustomCommand(byte[] buffer) {
		int cs = 0;
		for (int i = 0; i < buffer.length - 1; i++ ) {
			cs += (buffer[i] & 0xff);
		}
		cs = (cs & 0xff);
		if (cs != (int)(buffer[buffer.length-1] & 0xFF)) {
			DLog.w("custom command cuecksum error!");
			return;
		}

		// debug
		if (BuildConfig.DEBUG) {
			StringBuilder debugString = new StringBuilder("");
			for (int index = 0; index < buffer.length; index++) {
				debugString.append(String.format("%02x ", (byte) buffer[index]));
			}
			DLog.d(TAG, "custom : " + debugString.toString());
		}
		if (buffer.length < 5)
			return;

		// 로그리스트 벌크
		if (buffer[4] == (byte)Constants.cmd160_logReadBulk) {

			int addr;
			int dataSize;

			addr = (int) buffer[6] & 0xFF;
			addr <<= 8;
			addr |= (int) buffer[7] & 0xFF;
			addr <<= 8;
			addr |= (int) buffer[8] & 0xFF;
			if (logBulkOffset == 0) {
				logBulkOffset = addr;
			}

			dataSize = (int) buffer[9] & 0xFF;
			DLog.d(TAG, "cmd160_logReadBulk : " + String.format("%d (%x), dataSize : %d", addr, addr, dataSize));
			if (!logRawDataMap.isEmpty() && addr - logRawDataMap.lastKey() >= 220) {
				DLog.d("Skipped addr "  + addr );
			}

			// End-of-data 데이터 다 받음
			if ((addr == 0 && dataSize == 0) || logRawDataMap.size() * 5 == selectedLogData.getCountEntry()) {
				DLog.d(TAG, "로그리스트 벌크 다 받음 : "+logBulkRecodeCnt);
				/*if (logRawDataMap.size() * 5 < selectedLogData.getCountEntry()) { // TODO : 테스트 더 필요함.
					// 재요청
					Integer prevKey = -1;
					for(Integer key: logRawDataMap.keySet()){
						if (key - prevKey >= 220 && prevKey != -1) {
							int requestAddr = prevKey + 110;
							byte[] buff = new byte[4];
							buff[0] = (byte)(requestAddr>>16);// addr H
							buff[1] = (byte)(requestAddr>>8);// addr H
							buff[2] = (byte)(requestAddr&0xFF); // addr L
							buff[3] = 110;
//							CommonValue.getInstance().gpsManager.sendCommandToDevice(Constants.cmd160_logRead, buff, 4);
							CommonValue.getInstance().gpsManager.sendCommandToDevice(Constants.cmd160_logReadBulk, selectedLogData.getRequestBuffer(), 4);
							DLog.d("request again for " + requestAddr);
						}
						prevKey = key;
					}
				} else {*/
					this.decodeLogBulk();
					if (mXgpsListener != null)
						mXgpsListener.getLogDetailProgress(logRawDataMap.size() * 5);

					// 초기화
					logBulkRecodeCnt = 0;
					logBulkOffset = 0;
					logRawDataList.clear();
					logRawDataMap.clear();
//				}
			}
			// 받는중
			else {

				int p = 10;
				ArrayList<Object> dataList = new ArrayList<>();

				for (int i = 0; i < 5; i++) {
					if (buffer[p + 1] == 0) { // type 1
						LogEntry logEntry = new LogEntry();
						logEntry.seq = buffer[p];
						DLog.d(TAG, "sequence  : " + logEntry.seq);
						logEntry.type = buffer[p + 1];
						logEntry.date = (buffer[p + 2] & 0xff) + ((buffer[p + 3] & 0xff) << 8);
						logEntry.tod = (buffer[p + 4] & 0xff) + ((buffer[p + 5] & 0xff) << 8);
						logEntry.tod2 = buffer[p + 6];
						logEntry.lat[0] = buffer[p + 7];
						logEntry.lat[1] = buffer[p + 8];
						logEntry.lat[2] = buffer[p + 9];
						logEntry.lon[0] = buffer[p + 10];
						logEntry.lon[1] = buffer[p + 11];
						logEntry.lon[2] = buffer[p + 12];
						logEntry.alt[0] = buffer[p + 13];
						logEntry.alt[1] = buffer[p + 14];
						logEntry.alt[2] = buffer[p + 15];
						logEntry.spd[0] = buffer[p + 16];
						logEntry.spd[1] = buffer[p + 17];
						logEntry.heading = buffer[p + 18];
						logEntry.satnum = buffer[p + 19];
						logEntry.satsig = buffer[p + 20];
						logEntry.dop = buffer[p + 21];
						logRawDataList.add(logEntry);
						dataList.add(logEntry);
					} else if (buffer[p + 1] == 2) { // type 2
						Log2Entry logEntry = new Log2Entry();
						logEntry.seq = buffer[p];
						DLog.d(TAG, "sequence  : " + logEntry.seq);
						logEntry.type = buffer[p + 1];
						logEntry.date = (buffer[p + 2] & 0xff) + ((buffer[p + 3] & 0xff) << 8);
						logEntry.tod = (buffer[p + 4] & 0xff) + ((buffer[p + 5] & 0xff) << 8);
						logEntry.tod2 = buffer[p + 6];
						logEntry.lat[0] = buffer[p + 7];
						logEntry.lat[1] = buffer[p + 8];
						logEntry.lat[2] = buffer[p + 9];
						logEntry.lat[3] = buffer[p + 10];
						logEntry.lon[0] = buffer[p + 11];
						logEntry.lon[1] = buffer[p + 12];
						logEntry.lon[2] = buffer[p + 13];
						logEntry.lon[3] = buffer[p + 14];
						logEntry.alt[0] = buffer[p + 15];
						logEntry.alt[1] = buffer[p + 16];
						logEntry.alt[2] = buffer[p + 17];
						logEntry.spd[0] = buffer[p + 18];
						logEntry.spd[1] = buffer[p + 19];
						logEntry.heading = buffer[p + 20];
						logEntry.satsig = buffer[p + 21];
						logRawDataList.add(logEntry);
						dataList.add(logEntry);
					}
					p += 22;
					DLog.d("Hashmap index : " + ((addr - logBulkOffset)/22 + i));
				}

				if (!dataList.isEmpty()) {
					logRawDataMap.put(addr, dataList);
				}

				// 5개 담았으니 5개 다음부터 다시 담는다
				logBulkRecodeCnt += 5;
				DLog.d(TAG, "logBulkRecodeCnt = "+(logRawDataMap.size() * 5) + "/" + selectedLogData.getCountEntry());
                if (mXgpsListener != null) mXgpsListener.getLogDetailProgress(logRawDataMap.size() * 5);
			}
		}
		// 로그리스트
		else if (buffer[4] == (byte)Constants.cmd160_logListItem) {
			DLog.d(TAG, "cmd160_logListItem");

			int listIdx;
			int listTotal;

			listIdx = (int) buffer[6] & 0xff;
			listIdx <<= 8;
			listIdx |= (int) buffer[7] & 0xff;

			listTotal = (int) buffer[8] & 0xff;
			listTotal <<= 8;
			listTotal |= (int) buffer[9] & 0xff;

			////////////////////////////////////////////////////////////////////////////////////

			// 로그 리스트 응답끝
			if (listIdx == listTotal) {
				DLog.d(TAG, "log list end");
				// 리스트 날짜별 선택정렬 TODO: 이거 왜 하는걸까???  data  size 별로 정렬하는거 같은데?
//				if (logList.size() > 2) {
//
//					HashMap<String, String> map = new HashMap<String, String>();
//					int minidx = 0;
//					for (int i = 0; i < logList.size()-1; i++) {
//						minidx = i;
//						for (int j = i+1; j < logList.size(); j++) {
//
//							if(Long.parseLong((String) logList.get(i).get("dataSize"))  >  Long.parseLong((String) logList.get(j).get("dataSize")) )
//								minidx = j;
//							else
//								minidx = i;
//
//							if (minidx != i) {
//								map = logList.get(minidx);
//								logList.set(minidx, logList.get(i));
//								logList.set(i, (HashMap) map.clone());
//							}
//						}
//					}
//				}
                if (mXgpsListener != null) mXgpsListener.getLogListComplete(logList);
			} else {
				// 로그 리스트 받는중
				DLog.d(TAG, "log list add");

				int sig = (int) buffer[10] & 0xff;
				int interval = (int) buffer[11] & 0xff;
				int startDate = (int)((buffer[12] & 0xff) + ((buffer[13] & 0xff) * 256));

				long startTod = (long)((buffer[14] & 0xff) + ((buffer[15] & 0xff)  * 256) + ((buffer[16] & 0xff)  * 65536));

				int startBlock = (int)((buffer[18] & 0xff) + ((buffer[19] & 0xff) * 256));
				int countEntry = (int)((buffer[20] & 0xff) + ((buffer[21] & 0xff) * 256));
				int countBlock = (int)((buffer[22] & 0xff) + ((buffer[23] & 0xff) * 256));

				// 날짜 계산 //////////////////////////////
				int tmp, yy, mm, dd;

				tmp = startDate;
				yy = 2012 + tmp / 372;
				mm = 1 + (tmp % 372) / 31;
				dd = 1 + tmp % 31;
				String dateString = String.format("%04d%02d%02d", yy,mm,dd);

				// UTC 계산 ///////////////////////////////
				int hr, mn, ss;

				hr = (int) (startTod / 3600);
				mn = (int) ((startTod % 3600) / 60);
				ss = (int) (startTod % 60);
				String todString = String.format("%02d%02d%02d", hr,mn,ss);

				logList.add(new LogData(sig, interval, startBlock, countEntry, countBlock, dateString, todString));
			}
		}


		else if (buffer[4] == (byte)Constants.cmd160_logDelBlock) {
			DLog.d(TAG, "cmd160_logDelBlock");

			// 로그 리스트 지운다..  이후 리스트뷰 다시 호출한다
			// 2016. 9. 2
			// 로그 리스트에서 지우기 전에 앱에 저장된 내용이면 지우지 않도록 한다.
			// 따라서 화면 갱신도 할 필요가 없다.
			//if (CommonValue.getInstance().section3Fragment.isSaved(CommonValue.getInstance().section3Fragment.listViewSeletedIdx, (String) NmeaParser.logList.get(CommonValue.getInstance().section3Fragment.listViewSeletedIdx).get("title"))) {

			//}

			//else {

//			if( CommonValue.getInstance().isFwUpdateWorking ){
//				CommonValue.getInstance().isFwUpdateWorking = false;
//				//firmwareUp
//			} else{
//				logList.remove(CommonValue.getInstance().section3Fragment.listViewSeletedIdx);
//				DLog.d(TAG, "listViewSeletedIdx:  " + CommonValue.getInstance().section3Fragment.listViewSeletedIdx);
//				DLog.d(TAG, "logList.size():  " + logList.size());
//
//				new Handler(Looper.getMainLooper()).post(new Runnable() {
//					public void run() {
//						CommonValue.getInstance().section3Fragment.removeAndlistViewReload();
//					}
//				});
//			}
		}
		// 펌웨어 버전
		else if (buffer[4] == (byte)Constants.cmd160_fwVersion) {
			DLog.d(TAG, "cmd160_fwVersion");
			mFirmwareVersion = (int)(buffer[5]&0xFF)+"."+(int)(buffer[6]&0xFF)+"."+(int)(buffer[7]&0xFF);
		}
		// 셋팅
		else if (((int) buffer[4] == (byte)Constants.cmd160_getSettings )) {
			// 레코드 시작 셋팅
			DLog.d(TAG, "cmd160_getSettings");
			if (parserMode == XGPSParser.XGPS_MODE_2) {
				boolean positionEnable = ((buffer[5] & 0x40) == 0x40)?true:false;
				boolean overWrite = ((buffer[5] & 0x80) == 0x80)?true:false;
				int logInterval = buffer[6] & 0xff;
				SettingInfo setting = new SettingInfo(overWrite, positionEnable, -1, -1, logInterval, 10);
				if (mXgpsListener != null) mXgpsListener.updateSettings(setting);
			}
			else if (parserMode == XGPSParser.XGPS_MODE_3) {
//			receive example : 88 ee 6e 03 14 1a 11 19 11 30 01 00 00 f3 a5 05 0a ff ff 58 02 58 02 ff ff 00 19 01 45 fe 04 90 8d 6c e4 a2 10 98 04 e5 36 57 b4 41 83 4c 74 bf c4 48 a8 e4 a0 3b e3 a4 63 61 86 ff 03 00 ff ff ff 00 0a 03 01 00 00 00 00 00 00 ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff 88
//			[0] 88 ee : prefix
//			[2] 6e : length
//			[3] 03 14 : cmd (cmd160_fwRsp, cmd160_getSettings)
//			[5] 1a 11 19 11 : signature
//			[9] 30 01 00 00 : revision
//			[13] f3 : adc_offset
//			[14] a5 : adb_calibrated
//			[15] 05 : gpsOption (& 0x40)
//			[16] 0a : GpsRefreshRateX10
//			[17] ff ff : GpsForceHDOP
//			[19] 58 02 : GpsPowerTime
//			[21] 58 02 : BtPowerTime
//			[23] ff : LedBrightness
//			[24] ff : LedBrightness2
//			[25] 00 19 01 45 fe 04 : bdaddr[6];
//			[31] 90 8d 6c e4 a2 10 98 04 e5 36 57 b4 41 83 4c 74 bf c4 48 a8 e4 a0 3b e3 a4 63 61 86 ff 03 00 ff ff ff 00 : recentDev[7 * MaxRecentDevices]
//			[66] 0a : logInterval
//			[67] 03 : logType
//			[68] 01 : logOverWriteEnable
//			[69] 00 00 00 00 : logStorePosition
//			[73] 00 : opMode
//			[74] 00 : streamMode
//			[75] ff ff ff : rsvdb
//			[78] ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff ff : rsvd
//			[106] ff ff ff ff ff ff ff 88 : signature2 ~ 114byte
				if (buffer.length < 114)
					return;
				boolean isLogOverwrite = ((buffer[68] & 0x01) != 0)?true:false;
				boolean recordEnable = ((buffer[15] & 0x40) != 0)?true:false;
				int streamMode = buffer[74] & 0xff;
				int logType = buffer[67] & 0xff;
				int logInterval = buffer[66] & 0xff;
				int gpsRefreshRate = (buffer[16] & 0xff) / 10;

				mStreamingMode = streamMode;
				SettingInfo setting = new SettingInfo(isLogOverwrite, recordEnable, streamMode, logType, logInterval, gpsRefreshRate);
				if (mXgpsListener != null) mXgpsListener.updateSettings(setting);
			}
		}
		// logenable 을 던져도 logdisable로 응답이 옴....
		else if ((int) buffer[4] == (byte)Constants.cmd160_logEnable) {
			DLog.d(TAG, "cmd160_logEnable");
		}
		else if ((int) buffer[4] == (byte)Constants.cmd160_logDisable) {
			DLog.d(TAG, "cmd160_logDisable");
		}
		else if ((int) buffer[4] == (byte)Constants.cmd160_logOWEnable) {
			DLog.d(TAG, "cmd160_logOWEnable");
		}
		else if ((int) buffer[4] == (byte)Constants.cmd160_logOWDisable) {
			DLog.d(TAG, "cmd160_logOWDisable");
		}
		// for 500 trip
		else if ((int) buffer[4] == (byte)Constants.cmd160_fileList) {
			DLog.d(TAG, "cmd160_fileList");

//			typedef struct {
//				uint32_t	filesize;
//				uint32_t	fileattr;
//				uint32_t	filetime;
//				uint8_t		rsvd;
//				uint8_t		filenameLen;
//				//	char	filename[filenameLen];			//
//			} xgps_fileinfo_t;
//			logList.add(new LogData(sig, interval, startBlock, countEntry, countBlock, dateString, todString));

		}

		if (buffer[3] == (byte)Constants.cmd160_fwRsp) {
			DLog.d(TAG, "cmd160_fwRsp");
			rsp160_cmd = (int)buffer[3];
            for (int i = 4; i < buffer.length; i++) {
                rsp160_buf[i-4] = (byte)buffer[i];
            }
		}

		else if( buffer[3] == (byte)Constants.cmd160_fwData) {
			DLog.d(TAG, "cmd160_fwData");

			int cmdLength = (buffer[2] & 0xff) - 5;
			rsp160_cmd = (int)buffer[3];
			if( (buffer[2] & 0xff) > 2 ){
				int offset = 0;
				if (pFwReadBuf != null)
					offset += pFwReadBuf.length;
				byte[] validBuffer = new byte[offset + cmdLength];
				if (pFwReadBuf != null)
					System.arraycopy(pFwReadBuf, 0, validBuffer, 0, offset);
				System.arraycopy(buffer, 8, validBuffer, offset, cmdLength);
				pFwReadBuf = validBuffer;
			}
		}
		else if (buffer[3] == (byte)Constants.cmd160_fwDataR) {
			rsp160_cmd = (int)buffer[3];
		}
		else if (buffer[3] == (byte)Constants.cmd160_ack || buffer[3] == (byte)Constants.cmd160_nack) {
			rsp160_cmd = (int)buffer[3];
			if (buffer[4] == (byte)Constants.cmd160_gpsForceColdStart) {
			}
		}
		// 그 외 명령어
		else {
			StringBuilder debugString2 = new StringBuilder("");
//			for (int index = 0; index < buffer.length; index++) {
//				debugString2.append(String.format("%02x ", (byte)buffer[index]));
//			}
			DLog.d(TAG, "그 외 명령어 : " + debugString2.toString());
		}
	}

	// 로그 리스트들 초기화
	public void initNMEAParser() {
		commandQueue = null;
		logList.clear();
		logBulkList.clear();
	}


	private class LogEntry {
		byte	seq;	// sequence number of the record (wrap after 255)
		byte	type;	// 0= dataentry_t, others not defined yet.

		int	date;	// date: ((year-2012) * 12 + (month - 1)) * 31 + (day - 1)
		//  year  = 2012 + (dd/372)
		//  month = 1 + (dd % 372) / 31
		//  day   = 1 + dd % 31
		int	tod;	// 16 LSB of time of day in second
		byte	tod2;	// [0..3] 1/10 of second
		// [4]    1 MSB of the time of day
		// [5..7] reserved

		byte[]	lat = new byte[3];		// Latitude
		byte[]	lon = new byte[3];		// Longitude
		byte[]	alt = new byte[3];		// Altitude with 5 ft. unit
		byte[]	spd = new byte[2];		// speed over ground
		byte	heading;	// True north heading in 360/256 step
		byte	satnum;		// in view, in use
		byte	satsig;
		byte	dop;		// HDOP, VDOP
	}

	private class Log2Entry {
		byte	seq;	// sequence number of the record (wrap after 255)
		byte	type;	// 0= dataentry_t, others not defined yet.

		int	date;	// date: ((year-2012) * 12 + (month - 1)) * 31 + (day - 1)
		//  year  = 2012 + (dd/372)
		//  month = 1 + (dd % 372) / 31
		//  day   = 1 + dd % 31
		int	tod;	// 16 LSB of time of day in second
		byte	tod2;	// [0..3] 1/10 of second
		// [4]    1 MSB of the time of day
		// [5..7] reserved

		byte[]	lat = new byte[4];		// Latitude
		byte[]	lon = new byte[4];		// Longitude

		byte[]	alt = new byte[3];		// Altitude in cm
		byte[]	spd = new byte[2];		// speed over ground
		byte	heading;	// True north heading in 360/256 step
		byte	satsig;
	}


	private double getLatLon24bit(byte[] buf) {
		double kLatLonBitResolution = 2.1457672e-5;
		double  d;
		int r;

		r = (buf[0] & 0xFF);
		r <<= 8;
		r |= (buf[1] & 0xFF);
		r <<= 8;
		r |= (buf[2] & 0xFF);

		d = ((double)r) * kLatLonBitResolution;

		if((r & 0x800000) != 0) {	// is South / West ?
			d = -d;
		}

		return d;
	}

	private int getUInt24bit(byte[] buf) {
		int r;

		r = (buf[0] & 0xFF);
		r <<= 8;
		r |= (buf[1] & 0xFF);
		r <<= 8;
		r |= (buf[2] & 0xFF);

		return r;
	}

	private double getLatLon32bit(byte[] buf)
	{
		double  d;
		int r;

		r = (buf[0] & 0xFF);
		r <<= 8;
		r |= (buf[1] & 0xFF);
		r <<= 8;
		r |= (buf[2] & 0xFF);
		r <<= 8;
		r |= (buf[3] & 0xFF);

		d = ((double)r) * 0.000001;

		return d;
	}

	private String getDateString(int date) {
		int year = 2012 + date / 372;
		int month = 1 + (date % 372) / 31;
		int day = 1 + date % 31;
		return String.format("%04d/%02d/%02d", year, month, day);
	}

	private String getTodString(int tod, int milli) {
		int hour = tod / 3600;
		int minute = (tod % 3600) / 60;
		int second = tod % 60;

		return String.format("%02d:%02d:%02d.%d", hour, minute, second, milli);
	}

	///////////////////////////////////////////////////////////////////
	// 로그 벌크  다 받은후 디코드 함  ////////////////////////////////////
	private void decodeLogBulk() {
		logBulkList.clear();
		int date;
		int tod;
		int tod100milli;

		long spd;
		Double fLat;
		Double fLon;
		double fAlt;

		DLog.d(TAG, "decodeLogBulk - logBulkRecodeCnt:  "+logBulkRecodeCnt + ", arraySize ; " + logRawDataList.size());

		int logType = 0;
		if (logRawDataList.get(0).getClass() == Log2Entry.class)
			logType = 2;
		for (int i = 0; i < logRawDataList.size(); i++) {
			if (logType == 0) {
				LogEntry logEntry = (LogEntry) logRawDataList.get(i);
				tod = (logEntry.tod & 0xFFFF) | ((logEntry.tod2 & 0x10) << 12);
				tod100milli = logEntry.tod2 & 0x0F;
				fLat = getLatLon24bit(logEntry.lat);
				fLon = getLatLon24bit(logEntry.lon);
				fAlt = ((double)getUInt24bit(logEntry.alt)) * 5.0 / 3.2808399;	// 5feet unit -> meters
				spd = (((logEntry.spd[0] & 0xFF) << 8) | logEntry.spd[1]);
				date = (logEntry.date & 0xFFFF);
			}
			else if (logType == 2) {
				Log2Entry logEntry = (Log2Entry) logRawDataList.get(i);
				tod = (logEntry.tod & 0xFFFF) | ((logEntry.tod2 & 0x10) << 12);
				tod100milli = logEntry.tod2 & 0x0F;
				fLat = getLatLon32bit(logEntry.lat);
				fLon = getLatLon32bit(logEntry.lon);
				fAlt = ((double)getUInt24bit(logEntry.alt)) / 100.0f;	// 5feet unit -> meters
				spd = (((logEntry.spd[0] & 0xFF) << 8) | (logEntry.spd[1] & 0xff));
				date = (logEntry.date & 0xFFFF);
			}
			else
				break;

			if (fLat.compareTo(0.0) != 0 && fLon.compareTo(0.0) != 0 && date != 0xFFFF) {
				logBulkList.add(new LogBulkData(getDateString(date), fLat.floatValue(), fLon.floatValue(), (float)fAlt, spd, tod, getTodString(tod, tod100milli)));
			}
		}
		DLog.d(TAG, "logBulkList.size:   "+logBulkList.size());
        if (mXgpsListener != null) mXgpsListener.getLogDetailComplete(logBulkList);
	}
	//             로그 벌크 종료
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public void logBulkEndReceive() {
		DLog.d(TAG, "수동 처리에  의해 로그벌크 종료 확인");
//		this.decodeLogBulk(); 

		// 초기화
		logBulkRecodeCnt = 0;
		logRawDataList.clear();
	}

	boolean NMEAVerifyChecksum(  String sentence )
	{
		int        i;
		char    cs = 0;
		char vv = 0;
		char    ch;

		sentence = sentence.replace("$", "").replace("\r\n", "").replace("\n", "");

		String[] sentenceSplit = sentence.replace("$", "").split("\\*");
		if (sentenceSplit.length != 2)
			return false;

		for (i = 0;  i < sentenceSplit[0].length() ; i++) {
			ch = sentence.charAt(i);
			cs ^= ch;
		}

		vv = (char)Integer.parseInt(sentenceSplit[1], 16);
		if (cs == vv) {
			return true;
		}

		/*
		 * 			if( ch == '*' ) {
				String checkSumString =
				char[] validBuffer = new char[length];
				System.arraycopy(buffer, 0, validBuffer, 0, length);

				vv = (char)hexStrToInt( data, 2 );
				if( vv == cs ) {
					return true;
				}
				break;
			}
 */
//		DLog.e(String.format("CS %02x %02x", cs, vv));
		return false;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	//               NMEA 데이터 센텐스  파싱
	private void parseNmeaSentence(String gpsSentence)  {
		// for debug
//		DLog.d(TAG, gpsSentence);
		//String gpsSentence = data.toString();
		// 선행 정보 파싱, 배터리 상태 펌웨어 버전등
//		String nmeaSentence = null;

		/*
		String temp = new String(data, 0, 4);

		if( temp.equals("PGGA")){
			DLog.i(TAG, "PGGA");
		}
		*/
		if (!NMEAVerifyChecksum(gpsSentence)) {
			DLog.e("Checksum fail : " + gpsSentence);
			return;
		}

		Pattern xx = Pattern.compile("\\$([^*$]*)\\*([0-9A-F][0-9A-F])?[\r\n]*");
		Matcher m = xx.matcher(gpsSentence);

		if (m.matches()) {
//			nmeaSentence = m.group(0);
			String sentence = m.group(1);
//			String checkSum = m.group(2);
			SimpleStringSplitter splitter = new SimpleStringSplitter(',');
			splitter.setString(sentence);
			String command=null;

			try {
				command = splitter.next();
			} catch (Exception e) {
				// TODO: handle exception
				DLog.e(TAG, "파싱에러:", e);
			}

			// Driving / Pedestrian mode
			if (command.equals("XGPS")) {
				DLog.v("XGPS", gpsSentence);
				String pecfg = splitter.next();
				splitter.next();
				String mode = splitter.next();
				if (!pecfg.equals("PECFG")) {
					DLog.e(TAG, "Command error : " + gpsSentence);
				} else {
					if (mXgpsListener != null) {
						if (mode.equals("1")) {    // driving mode
							mXgpsListener.updateDrivingMode(1);
						} else if (mode.equals("2")) { // pedestrian mode
							mXgpsListener.updateDrivingMode(2);
						}
					}
				}
			}

			// ㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜ
			//
			// alt 정보
			else if (command.equals("GPGGA")) {

//				DLog.v("GGA", gpsSentence);
				// 명령어 0부터 9번째 되는게 고도임
				try {
//					splitter.next();
//					splitter.next();
//					splitter.next();
//					splitter.next();
//					splitter.next();
//					splitter.next();
//					splitter.next();
//					splitter.next();
//
//					altString = splitter.next();


					// UTC time of fix HHmmss.S
					String time = splitter.next();
					// latitude ddmm.M
					String lat = splitter.next();
					// direction (N/S)
					String latDir = splitter.next();
					// longitude dddmm.M
					String lon = splitter.next();
					// direction (E/W)
					String lonDir = splitter.next();

					String quality = splitter.next();
					// Number of satellites being tracked
					String nbSat = splitter.next();
					// Horizontal dilution of position (float)
					String hdop = splitter.next();
					// Altitude, Meters, above mean sea level
					String alt = splitter.next();
					altString = alt;

					if (quality.equals("2")) {
						isDGPS = true;
						DLog.v("DGPS");
					}
					else
						isDGPS = false;
				} catch (Exception e) {
//					DLog.e(TAG, "파싱에러:", e);
				}

			} // End PGGA (alt 정보)
			//
			// ㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗ

			// ㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜ
			//
			else if (command.endsWith("GSA")) {
				String[] items = sentence.split(",", -1);
				int systemId = 1; // default gps
				if (items.length < 17)		// not is gsa
					return;

				try {
					fixType = Integer.parseInt(items[2]);
					pdopfloat = Float.parseFloat(items[15]);
					hdopfloat = Float.parseFloat(items[16]);
					vdopfloat = Float.parseFloat(items[17]);
					if (items.length > 18) { // nmea 0183 4.11 version
						systemId = Integer.parseInt(items[18]);
					} else if (command.charAt(1) == 'N') {
						systemId = 2;
					}
				} catch (NumberFormatException e) {
//					DLog.e(TAG, "파싱에러:", e);
				}

				if (systemId < 1) {
					systemId = 1;
				}

				ArrayList<String> satsUsedInPosCalcLocal = new ArrayList<String>();
				for (int i = 3; i < 15; i++) {
					String satInDOP = items[i];

					if (satInDOP.length() > 0) {
						satsUsedInPosCalcLocal.add(satInDOP);
					}
				}
				numOfSatInUse = satsUsedInPosCalcLocal.size();
				isGetGPGSA = true;
				satellitesUsedMap.put(systemId, satsUsedInPosCalcLocal);
				if (mXgpsListener != null)
					mXgpsListener.updateConfidenceInfo();
			}
			else if (command.endsWith("GSV")) {
				int systemId = getSystemIdFromTalkId(command.charAt(1));
				if (systemId < 1) {
					systemId = 1;
				}
				if (satellitesUsedMap.get(systemId) == null) {
					return;
				}

				String[] items = gpsSentence.split(",|\\*");

				if (items.length < 4)
					return;

				try {
					int signalId = 0;
					if (items.length == 22) {
						signalId = Integer.parseInt(items[20]);
					}
					int num = Integer.parseInt(items[3]);
					numOfSatInViewMap.put(systemId, num);
					if (num != 0) {
						int numOfSatsInSentence;

						if (items.length < 10)
							numOfSatsInSentence = 1;
						else if (items.length < 14)
							numOfSatsInSentence = 2;
						else if (items.length < 19)
							numOfSatsInSentence = 3;
						else
							numOfSatsInSentence = 4;

						HashMap<Integer, SatellitesInfo> map;
						if (tempSatellitesInfoMap.get(systemId, signalId) == null || items[2].equals("1")) {
							map = new HashMap<>();
						} else {
							map = (HashMap<Integer, SatellitesInfo>) tempSatellitesInfoMap.get(systemId, signalId);
						}
//						if (tempSatellitesInfoMap.size() > 0 && tempSatellitesInfoMap.containsKey(systemId)) {
//							// 첫 시작센텐스라면 초기화
//							if (items[2].equals("1") && (signalId == 1 || signalId == 0)) {
//								tempSatellitesInfoMap.get(systemId).clear();
//							}
//							map = tempSatellitesInfoMap.get(systemId);
//						} else {
//							map = new HashMap<>();
//						}

						for (int i = 0; i < numOfSatsInSentence; i++) {
							String satNum = "0", satElev = "0", satAzi = "0", satSNR = "0";
							int index = i * 4 + 4;

							satNum = items[index].equals("")?"0":items[index];
							satElev = items[index + 1].equals("")?"0":items[index + 1];
							satAzi = items[index + 2].equals("")?"0":items[index + 2];
							satSNR = items[index + 3].equals("")?"0":items[index + 3];
							if (satNum.equals("0")) {
								continue;
							}

							SatellitesInfo satInfo = (new SatellitesInfo()).create(satElev, satAzi, satSNR, satellitesUsedMap.get(systemId).contains(satNum));

							if (satInfo != null) {
								int sat = Integer.parseInt(satNum);
								int snr = Integer.parseInt(satSNR);
								if (!map.containsKey(sat) || map.get(sat).SNR < snr) {
									map.put(sat, satInfo);
								}
//								tempSatellitesInfoMap.put(systemId, map);
								tempSatellitesInfoMap.put(systemId, signalId, map);
							}
						}
					}
					try {
						if (items[1].equals(items[2])) {   // the last message
//							satellitesInfoMap.put(systemId, (HashMap<Integer, SatellitesInfo>) tempSatellitesInfoMap.get(systemId).clone());
//							satellitesInfoMap.put(systemId, signalId, (HashMap<Integer, SatellitesInfo>) tempSatellitesInfoMap.get(systemId).clone());
							satellitesInfoMap.put(systemId, signalId, (HashMap) tempSatellitesInfoMap.get(systemId, signalId));
							if (mXgpsListener != null) mXgpsListener.updateSatellitesInfo(systemId);
						}
					} catch (NullPointerException e) {
						DLog.e(TAG, "NullPointerException:", e);
					}
				} catch (Exception e) {
//					DLog.e(TAG, "파싱에러:", e);
				}

			}
			//
			// ㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗ

			// ㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜ
			//
			// utc 와 방위각 날라옴
			else if (command.equals("GPRMC")) {
				
				try {
					
					// UTC time of fix HHmmss.S
					time = splitter.next();
					String[] timeSplit = time.split("\\.");
					if (timeSplit.length > 0 && timeSplit[0].length() == 6) {
						String sec = timeSplit[0];
						String milisec = "";

						if (timeSplit.length >  1) {
							milisec = "." + timeSplit[1];
						}
						time = sec.substring(0, 2) + ":" + sec.substring(2, 4) + ":"
								+ sec.substring(4,6) + milisec;
					}
					// fix status (A/V)
					String status = splitter.next();
					
					// latitude ddmm.M
					latString = splitter.next();
					String lat = latString;
//					latMinsString = "";
					if ( !(latString.length() <= 1)) {
						latDeg = (int) (Float.parseFloat(latString)/100);
						latMins = Float.parseFloat(latString) - (latDeg*100);
//						int dotIndex = latString.indexOf('.');
//						if (dotIndex != -1)
//							latMinsString = latString.substring(dotIndex - 2);
					}
					
					// direction (N/S)
					latDirString = splitter.next();
					String latDir = latDirString;
                    latitude = (latDeg + (latMins / 60.0f)) * (latDirString.equals("N")?1:-1);

					// longitude dddmm.M
					longString = splitter.next();
					String lon = longString;
//					lonMinsString = "";
					if ( !(longString.length() <= 1)) {
						lonDeg = (int) (Float.parseFloat(longString) / 100);
						lonMins = Float.parseFloat(longString) - (lonDeg * 100);
//						int dotIndex = longString.indexOf('.');
//						if (dotIndex != -1)
//							lonMinsString = longString.substring(dotIndex - 2);
					}
					
					// direction (E/W)
					lonDirString = splitter.next();
                    longitude = (lonDeg + (lonMins / 60.0f)) * (lonDirString.equals("E")?1:-1);

					String lonDir = lonDirString;
					// Speed over the ground in knots
					speedString = splitter.next();
					String speed = speedString;
					// Track angle in degrees True
					headingString = splitter.next();
					String bearing = headingString;
					
				} catch (Exception e) {
					DLog.d(TAG, gpsSentence);
					DLog.e(TAG, "파싱에러:", e);
				}
                if (mXgpsListener != null) mXgpsListener.updateLocationInfo();
				
				// IU 업데이트.. 0.1초에 한번씩 updateWithNewPosition
			/*	new Handler(Looper.getMainLooper()).post(new Runnable() {
					public void run() {
						// 0.1초에 한번
						CommonValue.getInstance().section1Fragment.updateInfomation();
					}
				});*/
			}
			// End GPRMC (utc 와 방위각 날라옴)
			// ㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗ

			//ㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜㅜ
			//      베터리 정보 
			else if (command.equals("GPPWR")) {
				// 150 $GPPWR,026A,0,1,1,0,00,5,S,60,212,000*7A
				// 160 $GPPWR,04C0,1,1,0,1,00,0,S,2B, 2 9 ,S00*73

				int vbat=0, vbat2=0 ;
				float bvolt, batLevel;

				String voltString = splitter.next();
				if (parserMode == XGPSParser.XGPS_MODE_2) {
					vbat = (Integer.parseInt(voltString, 16)) / 2;
				}
				else {
					vbat = Integer.parseInt(voltString.substring(0, 1));
					vbat <<= 4;
					vbat += Integer.parseInt(voltString.substring(1, 2));
					vbat <<= 8;

					vbat2 = Integer.parseInt(voltString.substring(2, 3), 16);
					vbat2 <<= 4;
					vbat2 += Integer.parseInt(voltString.substring(3, 4),16);

					vbat += vbat2;
				}


				if (vbat < 543)
					vbat = 543;
				if (vbat > 644)
					vbat = 644;

				bvolt = (float)vbat * 330.0f / 512.0f;
				batLevel = (float) (((bvolt / 100) - 3.5) / 0.65);

				if (batLevel > 1.0)
					batteryVoltage = 1.0f;
				else if (batLevel < 0)
					batteryVoltage = 0.0f;
				else
					batteryVoltage = batLevel;

				if (parserMode == XGPSParser.XGPS_MODE_1) {
					try {
					    // charing
                        if(  Integer.parseInt(splitter.next()) == 1 )
                            isCharging = true;
                        else
                            isCharging = false;
                        splitter.next();splitter.next();splitter.next();
						// 펌웨어
						splitter.next();
						SettingInfo settingInfo = new SettingInfo();
						String streamingRate = splitter.next();
						if (streamingRate.equals("5"))
                            settingInfo.setRefreshRate(5);
						else
						    settingInfo.setRefreshRate(1);

						splitter.next();splitter.next();
						mFirmwareVersion = splitter.next();
						mFirmwareVersion = String.format("%s.%s.%s", mFirmwareVersion.subSequence(0, 1),
								mFirmwareVersion.subSequence(1, 2),mFirmwareVersion.subSequence(2, 3));

                        if (mXgpsListener != null)
                            mXgpsListener.updateSettings(settingInfo);

                    } catch (Exception e) {
					}
				}
				else {
                    try {
                        splitter.next();splitter.next();splitter.next();
                        if(  Integer.parseInt(splitter.next()) == 1 )
                            isCharging = true;
                        else
                            isCharging = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
				if (mXgpsListener != null)
					mXgpsListener.updateGPSVoltage();
			}
			//           GPPWR  종료
			//ㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗㅗ

			else if (command.equals("GPVTG")) {
//				DLog.i(TAG, "GPVTG");
			} 
			else if (command.equals("GPGLL")) {
//				DLog.i(TAG, "GPGLL");
			} 
			else {
//				DLog.i(TAG, "아무 NMEA도 아님: " + command);
			}
		}
	}

	public NmeaParser(XGPSListener listener, float precision) {
		this.mXgpsListener = listener;
//		this.precision = precision;
		logRawDataList.clear();
	}

	public void setListener(XGPSListener listener) {
		mXgpsListener = listener;
	}

	public void setResponse(int value) {
		rsp160_cmd = value;
	}

	public int getResponse() {
		return rsp160_cmd;
	}

	private double parseNmeaLatitude(String lat,String orientation){
		double latitude = 0.0;
		if (lat != null && orientation != null && !lat.equals("") && !orientation.equals("")){
			double temp1 = Double.parseDouble(lat);
			double temp2 = Math.floor(temp1/100); 
			double temp3 = (temp1/100 - temp2)/0.6;
			if (orientation.equals("S")){
				latitude = -(temp2+temp3);
			} else if (orientation.equals("N")){
				latitude = (temp2+temp3);
			}
		}
		return latitude;
	}

	private double parseNmeaLongitude(String lon,String orientation){
		double longitude = 0.0;
		if (lon != null && orientation != null && !lon.equals("") && !orientation.equals("")){
			double temp1 = Double.parseDouble(lon);
			double temp2 = Math.floor(temp1/100); 
			double temp3 = (temp1/100 - temp2)/0.6;
			if (orientation.equals("W")){
				longitude = -(temp2+temp3);
			} else if (orientation.equals("E")){
				longitude = (temp2+temp3);
			}
		}
		return longitude;
	}

	private long parseNmeaTime(String time){
		long timestamp = 0;
		SimpleDateFormat fmt = new SimpleDateFormat("HHmmss.SSS");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			if (time != null){
				long now = System.currentTimeMillis();
				long today = now - (now %86400000L);
				long temp1;
				// sometime we don't have millisecond in the time string, so we have to reformat it 
				temp1 = fmt.parse(String.format((Locale)null,"%010.3f", Double.parseDouble(time))).getTime();
				long temp2 = today+temp1;
				// if we're around midnight we could have a problem...
				if (temp2 - now > 43200000L) {
					timestamp  = temp2 - 86400000L;
				} else if (now - temp2 > 43200000L){
					timestamp  = temp2 + 86400000L;
				} else {
					timestamp  = temp2;
				}
			}
		} catch (ParseException e) {
			DLog.e(TAG, "Error while parsing NMEA time", e);
		}
		return timestamp;
	}

	public ArrayList<String> getGPSSatellitesInUse() {
//		return satsUsedInPosCalc;
		return satellitesUsedMap.get(1);
	}

	public ArrayList<String> getGlonassSatellitesInUse() {
		return satsUsedInPosCalcGlonass;
	}
}
