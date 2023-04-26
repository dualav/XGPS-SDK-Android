package com.namsung.xgpssdksample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SatellitesInfoLayout extends LinearLayout {
    LayoutInflater mInflater;
    TextView mInViewTitle;
    TextView mInViewContent;
    TextView mInUseTitle;
    TextView mInUseContent;
    String systemTitle = null;
    private int systemId = 0;

    public SatellitesInfoLayout(Context context) {
        super(context);
        mInflater = LayoutInflater.from(context);
        init(context);
    }

    public SatellitesInfoLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = LayoutInflater.from(context);
        init(context);
    }

    private void init(Context context) {
        View v = mInflater.inflate(R.layout.satellites_cell, this, true);
        mInViewTitle = v.findViewById(R.id.in_view_title);
        mInUseTitle =  v.findViewById(R.id.in_use_title);
        mInViewContent = v.findViewById(R.id.in_view_content);
        mInUseContent = v.findViewById(R.id.in_use_content);
    }

    private String getSystemFromId(int systemId) {
        switch (systemId) {
            case 1:     return "GPS";
            case 2:     return "GLONASS";
            case 3:     return "GALILEO";
            case 4:     return "BEIDOU";
            case 5:     return "QZSS";
            case 6:     return "NAVIC";
            default:    return "UNKNOWN";
        }
    }

    public void setSystemId(int systemId) {
        this.systemId = systemId;

        mInViewTitle.setText(getResources().getString(R.string.in_view) + " " + getSystemFromId(systemId));
        mInUseTitle.setText(getResources().getString(R.string.in_use) + " " + getSystemFromId(systemId));
    }

    public int getSystemId() {
        return systemId;
    }

    public void setInViewValue(String value) {
        mInViewContent.setText(value);
    }

    public void setInUseValue(String value) {
        mInUseContent.setText(value);
    }

    public String getSystemTitle() {
        return systemTitle;
    }

}
