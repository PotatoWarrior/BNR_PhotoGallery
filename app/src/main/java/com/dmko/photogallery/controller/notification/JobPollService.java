package com.dmko.photogallery.controller.notification;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dmko.photogallery.R;
import com.dmko.photogallery.controller.activity.PhotoGalleryActivity;
import com.dmko.photogallery.model.FlickrFetcher;
import com.dmko.photogallery.model.GalleryItem;
import com.dmko.photogallery.model.QueryPreferences;

import java.util.List;

import static com.dmko.photogallery.controller.notification.AlarmPollService.ACTION_SHOW_NOTIFICATION;
import static com.dmko.photogallery.controller.notification.AlarmPollService.NOTIFICATION;
import static com.dmko.photogallery.controller.notification.AlarmPollService.PERM_PRIVATE;
import static com.dmko.photogallery.controller.notification.AlarmPollService.REQUEST_CODE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobPollService extends JobService {
    private static final String TAG = "JobPollService";
    private static final int JOB_ID = 1;
    private PollTask mTask;

    public static boolean isJobActive(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                Log.i(TAG, "isJobActive: true");
                return true;
            }
        }
        Log.i(TAG, "isJobActive: false");
        return false;
    }

    public static void setJob(Context context, boolean isOn) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        QueryPreferences.setNotificationsOn(context, isOn);
        if (isOn) {
            Log.i(TAG, "New job created");
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, JobPollService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000 * 60 * 15)
                    .setPersisted(true)
                    .build();
            scheduler.schedule(jobInfo);
        } else {
            Log.i(TAG, "Job cancelled");
            scheduler.cancel(JOB_ID);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Starting new Job");
        mTask = new PollTask();
        mTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Job stopped");
        if (mTask != null) {
            mTask.cancel(true);
        }
        return true;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        @Override
        protected Void doInBackground(JobParameters... params) {
            Context context = getApplicationContext();
            String query = QueryPreferences.getStoredQuery(context);
            String lastResultId = QueryPreferences.getLastResultId(context);

            List<GalleryItem> items;

            if (query == null) {
                items = new FlickrFetcher().fetchRecentPhotos(1);
            } else {
                items = new FlickrFetcher().searchPhotos(query, 1);
            }

            if (items.isEmpty()) {
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result");
            } else {
                Log.i(TAG, "Got a new result");
            }

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(context);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(context)
                    .setTicker(resources.getString(R.string.new_picture_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_picture_title))
                    .setContentText(resources.getText(R.string.new_picture_text))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            showBackgroundNotification(0, notification);

            QueryPreferences.setLastResultId(context, resultId);

            jobFinished(params[0], false);
            return null;
        }

        private void showBackgroundNotification(int requestCode, Notification notification) {
            Log.i(TAG, "showBackgroundNotification: Show notification");
            Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
            i.putExtra(REQUEST_CODE, requestCode);
            i.putExtra(NOTIFICATION, notification);
            sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
        }
    }
}
