package com.dmko.photogallery.controller.notification;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dmko.photogallery.R;
import com.dmko.photogallery.controller.activity.PhotoGalleryActivity;
import com.dmko.photogallery.model.FlickrFetcher;
import com.dmko.photogallery.model.GalleryItem;
import com.dmko.photogallery.model.QueryPreferences;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlarmPollService extends IntentService {
    private static final String TAG = "AlarmPollService";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);
    public static final String ACTION_SHOW_NOTIFICATION = "com.dmko.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.dmko.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context, AlarmPollService.class);
    }

    public AlarmPollService() {
        super(TAG);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent intent = AlarmPollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        QueryPreferences.setNotificationsOn(context, isOn);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = AlarmPollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!isNetworkConnectionAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetcher().fetchRecentPhotos(1);
        } else {
            items = new FlickrFetcher().searchPhotos(query, 1);
        }

        if (items.isEmpty()) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result");
        } else {
            Log.i(TAG, "Got a new result");
            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_picture_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getText(R.string.new_picture_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            showBackgroundNotification(0, notification);

            QueryPreferences.setLastResultId(this, resultId);
        }
    }

    private void showBackgroundNotification(int requestCode, Notification notification) {
        Log.i(TAG, "showBackgroundNotification: Show notification");
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }

    private boolean isNetworkConnectionAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }
}
