package com.namsung.xgpsmanager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        XGPSManager manager = new XGPSManager(appContext, new XGPSListener() {
            @Override
            public void connecting(BluetoothDevice device) {

            }

            @Override
            public void connected(boolean isConnect, int error) {
                assertEquals(isConnect, false);
            }

            @Override
            public void updateLocationInfo() {

            }

            @Override
            public void updateSatellitesInfo(int systemId) {

            }

            @Override
            public void updateDrivingMode(int mode) {

            }


            @Override
            public void getLogListComplete(ArrayList<LogData> logList) {

            }

            @Override
            public void getLogDetailProgress(int bulkCount) {

            }

            @Override
            public void getLogDetailComplete(ArrayList<LogBulkData> logBulkList) {

            }

            @Override
            public void throwException(Exception e) {

            }
        });

        assertNotEquals(null, manager);

        assertEquals("com.namsung.xgpsmanager.test", appContext.getPackageName());
    }
}
