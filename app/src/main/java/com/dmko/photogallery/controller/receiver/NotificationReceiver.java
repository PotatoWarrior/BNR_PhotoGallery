package com.dmko.photogallery.controller.receiver;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.dmko.photogallery.controller.notification.AlarmPollService;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received result " + getResultCode());

        if (getResultCode() != Activity.RESULT_OK) {
            Log.i(TAG, "onReceive: notification cancelled");
            return;
        }

        Log.i(TAG, "onReceive: showing notification");

        int requestCode = intent.getIntExtra(AlarmPollService.REQUEST_CODE, 0);
        Notification notification = intent.getParcelableExtra(AlarmPollService.NOTIFICATION);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(requestCode, notification);
    }
}
