package com.dmko.photogallery.model;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetcher {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "ed52e8ff4c721d636ea964248cf2b642";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri END_POINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(Integer page) {
        String url = buildUrl(FETCH_RECENTS_METHOD, null, page);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, Integer page) {
        String url = buildUrl(SEARCH_METHOD, query, page);
        return downloadGalleryItems(url);
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            parseItems(items, jsonString);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON", e);
        }
        return items;
    }

    private String buildUrl(String method, String query, Integer page) {
        Uri.Builder builder = END_POINT.buildUpon()
                .appendQueryParameter("method", method);
        if (method.equals(SEARCH_METHOD)) {
            builder.appendQueryParameter("text", query);
            builder.appendQueryParameter("page", page.toString());
        } else if (method.equals(FETCH_RECENTS_METHOD)) {
            builder.appendQueryParameter("page", page.toString());
        }
        return builder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, String jsonString) throws JSONException {
        JsonElement jsonBody = new JsonParser().parse(jsonString);
        JsonObject photosJsonObject = jsonBody.getAsJsonObject().getAsJsonObject("photos");
        JsonArray photoJsonArray = photosJsonObject.getAsJsonArray("photo");
        Gson gson = new Gson();
        for (int i = 0; i < photoJsonArray.size(); i++) {
            GalleryItem photo = gson.fromJson(photoJsonArray.get(i), GalleryItem.class);
            if (photo.getUrl() == null) continue;
            items.add(photo);
        }
    }

}
