package com.dmko.photogallery.controller.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.dmko.photogallery.controller.fragment.PhotoPageFragment;

public class PhotoPageActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }
    public static Intent newIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(uri);
        return intent;
    }
}
