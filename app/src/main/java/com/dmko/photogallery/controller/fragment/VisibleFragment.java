package com.dmko.photogallery.controller.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.dmko.photogallery.controller.notification.AlarmPollService;

public abstract class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";

    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Cancelling notification ");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(AlarmPollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification, intentFilter, AlarmPollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }
}
