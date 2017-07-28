package com.dmko.photogallery.model;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String PREF_IS_NOTIFICATIONS_ON = "isNotificationsOn";

    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_RESULT_ID, null);
    }

    public static void setLastResultId(Context context, String id) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, id)
                .apply();
    }

    public static boolean isNotificationsOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_IS_NOTIFICATIONS_ON, false);
    }

    public static void setNotificationsOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_NOTIFICATIONS_ON, isOn)
                .apply();
    }
}
