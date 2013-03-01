/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.plus.samples.photohunt;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

/**
 * Displays an about message including the version number of the project release.
 *
 */
public class AboutActivity extends SherlockFragmentActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        TextView photoHuntVersion = (TextView)findViewById(R.id.photo_hunt_version);
        TextView playServicesVersion = (TextView)findViewById(R.id.play_services_version);
        
        try {
            // Get the version number of this package.
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            photoHuntVersion.setText(pInfo.versionName);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to determine package version: ", e);
        }

        try {
            // Get the version number of the Google Play Services package on which this package
            // depends.
            PackageInfo pInfo = getPackageManager().getPackageInfo("com.google.android.gms", 0);
            playServicesVersion.setText(pInfo.versionName);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to determine play services version: ", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().setContext(this);
        Tracker tracker = EasyTracker.getInstance().getTracker();
        tracker.trackView("viewAbout");
    }

}
