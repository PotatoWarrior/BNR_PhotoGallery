package com.dmko.photogallery.controller.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.dmko.photogallery.controller.fragment.PhotoGalleryFragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }
}
