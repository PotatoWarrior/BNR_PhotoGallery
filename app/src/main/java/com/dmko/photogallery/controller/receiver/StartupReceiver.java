package com.dmko.photogallery.controller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.dmko.photogallery.controller.notification.JobPollService;
import com.dmko.photogallery.controller.notification.AlarmPollService;
import com.dmko.photogallery.model.QueryPreferences;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isNotificationsOn(context);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(TAG, "Setting new alarm after reboot " + isOn);
            AlarmPollService.setServiceAlarm(context, isOn);
        } else {
            Log.i(TAG, "Setting new job after reboot " + isOn);
            JobPollService.setJob(context, isOn);
        }
    }
}
