package com.namsung.xgpssdksample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.namsung.xgpsmanager.XGPSManager;
import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by cnapman on 2017. 11. 24..
 */

public class TripDetailFragment extends BaseFragment implements View.OnClickListener {
    private final String TAG = "TripDetailFragment";
    private LogData logData;
    private ArrayList<LogBulkData> mLogBulkList = new ArrayList<>();

    private EditText mLogFilename;
    private TextView mStartedTime;
    private TextView mDurationTime;
    private TextView mStoppedTime;
    private TextView mDataPoints;
    private TextView mSpeed;

    private ProgressBar mProgressBar;

    private boolean isMadeKML = false;

    public static TripDetailFragment newInstance(XGPSManager xgpsManager, LogData logdata) {
        TripDetailFragment tripDetailFragment = new TripDetailFragment();
        tripDetailFragment.xgpsManager = xgpsManager;
        tripDetailFragment.logData = logdata;
        return tripDetailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_detail, container, false);

        mLogFilename = view.findViewById(R.id.et_filename);
        mLogFilename.setEnabled(false);
        mStartedTime = view.findViewById(R.id.tv_started_time);
        mDurationTime = view.findViewById(R.id.tv_duration_time);
        mStoppedTime = view.findViewById(R.id.tv_stopped_time);
        mDataPoints = view.findViewById(R.id.tv_data_points);
        mSpeed = view.findViewById(R.id.tv_speed);

        Button mBtnDelete = view.findViewById(R.id.btn_delete_log);
        Button mBtnShare = view.findViewById(R.id.btn_share_log);
        Button mBtnEditFilename = view.findViewById(R.id.btn_edit_log_name);
        mBtnDelete.setOnClickListener(this);
        mBtnShare.setOnClickListener(this);
        mBtnEditFilename.setOnClickListener(this);

        mProgressBar = view.findViewById(R.id.pb_waiting);
        mProgressBar.setVisibility(View.VISIBLE);

        String filename = logData.getLocalFilename();
        if (filename == null)
            filename = logData.getDefaultFilename();
        mLogFilename.setText(filename);
        return view;
    }

    @Override
    public void getLogDetailProgress(int count) {
        int percent = count * 100 / logData.getCountEntry();
        Log.i(TAG, "get log detail percent " + percent);
    }

    @Override
    public void getLogDetailComplete(final ArrayList<LogBulkData> logBulkList) {
        mLogBulkList.clear();
        mLogBulkList.addAll(logBulkList);
        mProgressBar.setVisibility(View.GONE);
        if (logBulkList == null || (logBulkList != null && logBulkList.size() == 0)) {
            mStartedTime.setText(Constants.UNKNOWNSTRING);
            mDurationTime.setText(Constants.UNKNOWNSTRING);
            mStoppedTime.setText(Constants.UNKNOWNSTRING);
            mDataPoints.setText(Constants.UNKNOWNSTRING);
            mSpeed.setText(Constants.UNKNOWNSTRING);
            return;
        }
        LogBulkData startData = logBulkList.get(0);
        LogBulkData endData = logBulkList.get(logBulkList.size() - 1);
        mStartedTime.setText(startData.getDate() + " " + startData.getTodString());
        mStoppedTime.setText(endData.getDate() + " " + endData.getTodString());
        mDataPoints.setText(String.valueOf(logBulkList.size()));

        // duration
        int duration = endData.getTod() - startData.getTod();
        mDurationTime.setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_delete_log:
                AlertDialog.Builder deleteCheckDialog = new AlertDialog.Builder(getActivity());
                deleteCheckDialog.setTitle(R.string.app_name)
                        .setMessage(R.string.delete_from_skypro)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // delete log bulk
                                xgpsManager.sendCommandToDevice(Constants.cmd160_logDelBlock, logData.getRequestBuffer(), 4);
                            }
                        }).show();
                break;
            case R.id.btn_share_log:
                if (mLogBulkList.size() == 0)
                    return;
                // make kml file once
                if (isMadeKML == false) {

                    final Dialog dialog = new Dialog(getActivity());
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    dialog.setTitle(R.string.making_kml);

                    Thread pThread = new Thread(new Runnable(){
                        @Override
                        public void run(){
                            try{
                                makeKMLString();
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    public void run() {
                                        dialog.cancel();
                                        showMailIntent();
                                    }
                                });

                            }catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }
                    });
                    pThread.start();

                    isMadeKML = true;
                }
                else
                    showMailIntent();
                break;
            case R.id.btn_edit_log_name:
                mLogFilename.setEnabled(true);
                mLogFilename.setFocusableInTouchMode(true);
                mLogFilename.requestFocus();
                if (!mLogFilename.isCursorVisible())
                    mLogFilename.setCursorVisible(true);
                // show keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                mLogFilename.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_ENTER:

                                mLogFilename.setFocusableInTouchMode(false);
                                mLogFilename.setCursorVisible(false);
                                mLogFilename.clearFocus();
                                InputMethodManager immhide = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                                immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                logData.setLocalFilename(mLogFilename.getText().toString());

                                return true;
                            default:
                                break;

                        }
                        return false;
                    }
                });
                break;
        }
    }

    private void makeKMLString() {

        // write kml to file
        StringBuilder kmlString= new StringBuilder();
        kmlString.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kmlString.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n");

        kmlString.append("\t<ExtendedData>\n");
        kmlString.append("\t\t<Data name=\"GPSModelName\">\n");
        kmlString.append("\t\t\t<value>Dual XGPS160</value>\n");
        kmlString.append("\t\t</Data>\n");
        kmlString.append("\t</ExtendedData>\n");

        kmlString.append("\t<Placemark>\n");
        kmlString.append("\t\t<gx:Track>\n");
        kmlString.append("\t\t\t<altitudeMode>absolute</altitudeMode>\n");
        // end of header

        String whenStr;
        String gxCordStr;
        String whenTag;
        String gxCoordTag;
        float latFloat, altFloat = 0, longFloat;
        for (int i = 0; i < mLogBulkList.size(); i++) {

            LogBulkData bulkData = mLogBulkList.get(i);

            // longitude
            longFloat = bulkData.getLongitude();
            if (longFloat < 0)
                longFloat = (longFloat + 360)* -1;


            // latitude
            latFloat = bulkData.getLatitude();
            if (latFloat < 0)
                latFloat = (latFloat + 360)* -1;

            if (altFloat < 101350)
                altFloat = bulkData.getAltitude();

            whenStr = bulkData.getDate().replace("/", "-") + "T" + bulkData.getTodString() + "Z";
            gxCordStr = "<gx:coord>" + longFloat + " "+ latFloat + " " + String.format("%.1f", altFloat) + "</gx:coord>";
            whenTag = "\t\t\t\t<when>" + whenStr + "</when>";
            gxCoordTag = gxCordStr;

            kmlString.append(whenTag);
            kmlString.append(gxCoordTag);
            kmlString.append("\n");
        }
        // end of body

        kmlString.append("\t\t</gx:Track>\n");
        kmlString.append("\t</Placemark>\n");
        kmlString.append("</kml>\n");

        // end of tail
        // /////////////////////////////////////////////////////

        // make a file to local cache directory
        try {
            String szSendFilePath = getActivity().getCacheDir().getAbsolutePath();
            String filename = logData.getLocalFilename();
            if (filename == null)
                filename = logData.getDefaultFilename();
            File fullNameFile = new File(szSendFilePath+"/" + filename + ".kml");

            if (!fullNameFile.getParentFile().exists())
                fullNameFile.getParentFile().mkdirs();
            fullNameFile.createNewFile();
            fullNameFile.setReadable(true, false);
            //fullNameFile.setWritable(true, false);

            OutputStream oos = new FileOutputStream(fullNameFile);
            fullNameFile.setReadable(true, true);
            fullNameFile.setWritable(true, true);
            oos.write( kmlString.toString().getBytes() );

            oos.flush();
            oos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMailIntent() {
        try {
            String szSendFilePath = getActivity().getCacheDir().getAbsolutePath();
            logData.setLocalFilename(mLogFilename.getText().toString());
            String filename = logData.getLocalFilename();
            if (filename == null)
                filename = logData.getDefaultFilename();

            File fullNameFile = new File(szSendFilePath+"/" + filename + ".kml");

//            final Uri fileUri = Uri.fromFile(fullNameFile);
            Uri fileUri = FileProvider.getUriForFile(getContext(), "com.namsung.xgpssdksample.fileprovider", fullNameFile);

            // share
            Intent mail = new Intent(Intent.ACTION_SEND);
            mail.setType("plain/text");

            mail.putExtra(Intent.EXTRA_EMAIL,"");
            mail.putExtra(Intent.EXTRA_SUBJECT, fullNameFile);
            mail.putExtra(Intent.EXTRA_TEXT, "I'll send KML file.");
            mail.putExtra(Intent.EXTRA_STREAM, fileUri);

            startActivityForResult(Intent.createChooser(mail, "Send email..."), 10);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
