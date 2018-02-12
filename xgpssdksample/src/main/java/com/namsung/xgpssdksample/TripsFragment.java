package com.namsung.xgpssdksample;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.namsung.xgpsmanager.XGPSListener;
import com.namsung.xgpsmanager.XGPSManager;
import com.namsung.xgpsmanager.data.LogBulkData;
import com.namsung.xgpsmanager.data.LogData;
import com.namsung.xgpsmanager.utils.Constants;

import java.util.ArrayList;

/**
 * Created by cnapman on 2018. 2. 5..
 */

public class TripsFragment extends BaseFragment {
    private static TripsFragment tripsFragment = null;
    private ListView mListView;
    private TripsListAdapter mListAdapter;
    private ProgressBar mProgressBar;
    private ArrayList<LogData> mLogList = new ArrayList<>();
    private TripDetailFragment detailFragment = null;

    public static TripsFragment newInstance(XGPSManager xgpsManager) {
        if (tripsFragment == null)
            tripsFragment = new TripsFragment();
        tripsFragment.xgpsManager = xgpsManager;
        return tripsFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trips, container, false);

        mListView = view.findViewById(R.id.listview_logs);
        mListAdapter = new TripsListAdapter();
        mListView.setAdapter(mListAdapter);

        mProgressBar = view.findViewById(R.id.pb_waiting);
        mProgressBar.setVisibility(View.VISIBLE);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogData logData = mLogList.get(position);
                xgpsManager.sendCommandToDevice(Constants.cmd160_logReadBulk, logData.getRequestBuffer(), 4);
                detailFragment = TripDetailFragment.newInstance(xgpsManager, logData);
                final FragmentManager childFragManager = getChildFragmentManager();
                final FragmentTransaction childFragTrans = childFragManager.beginTransaction();
                childFragTrans.add(R.id.frame_layout, detailFragment);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
            }
        });
        return view;
    }

    public void onBackPressed() {
        if (detailFragment != null && getChildFragmentManager().getBackStackEntryCount() > 0) {
            FragmentTransaction childFragTrans = getChildFragmentManager().beginTransaction();
            childFragTrans.remove(detailFragment);
            getChildFragmentManager().popBackStack();
            childFragTrans.commit();
            detailFragment = null;
        }
    }

    private class ViewHolder {
        public TextView createDateTime;
        public TextView isSaved;
    }

    public class TripsListAdapter extends BaseAdapter {

        public TripsListAdapter() {
        }

        @Override
        public int getCount() {
            return mLogList.size();
        }

        @Override
        public Object getItem(int position) {
            return mLogList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.trips_list_cell, null);

                holder = new ViewHolder();
                holder.createDateTime = (TextView) convertView.findViewById(R.id.tv_create_date_time);
                holder.isSaved = convertView.findViewById(R.id.tv_saved);

                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            LogData logData = mLogList.get(position);


            holder.createDateTime.setText(logData.getLocalTimeString());
            if (logData.getLocalFilename() != null) {
                holder.isSaved.setText(R.string.saved);
            }
            else {
                holder.isSaved.setText("");
            }

            return convertView;
        }
    }

    @Override
    public void getLogListComplete(final ArrayList<LogData> logList) {
        mProgressBar.setVisibility(View.GONE);
        mLogList.clear();
        mLogList.addAll(logList);
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void getLogDetailProgress(final int bulkCount) {
        if (detailFragment != null) {
            detailFragment.getLogDetailProgress(bulkCount);
        }
    }

    @Override
    public void getLogDetailComplete(final ArrayList<LogBulkData> logBulkList) {
        if (detailFragment != null) {
            detailFragment.getLogDetailComplete(logBulkList);
        }
    }
}
