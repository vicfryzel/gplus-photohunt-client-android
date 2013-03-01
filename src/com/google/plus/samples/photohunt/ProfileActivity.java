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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.moments.Moment;
import com.google.android.gms.plus.model.moments.MomentBuffer;
import com.google.android.imageloader.ImageLoader;
import com.google.plus.samples.photohunt.model.User;

/**
 * Handles rendering profile data and the PhotoHunt activity stream for a user.
 */
public class ProfileActivity extends BaseActivity implements PlusClient.OnMomentsLoadedListener {

    private static final String TAG = ProfileActivity.class.getSimpleName();

    private TextView mProfileText;
    private ImageView mProfileImageView;
    private ListView mImageListView;
    
    /** Displays the list of user activities retrieved from Google+. */
    private MomentListAdapter mMomentListAdapter;
    
    /** List of activities retrieved from Google+. */
    private ArrayList<Moment> mListItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        mProfileText = (TextView) findViewById(R.id.profile_text);
        mProfileImageView = (ImageView) findViewById(R.id.profile_photo);
        mImageListView = (ListView) findViewById(R.id.activity_stream);

        mListItems = new ArrayList<Moment>();
        mMomentListAdapter = new MomentListAdapter(this, android.R.layout.simple_list_item_1,
                mListItems);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setAuthenticatedProfile(User profile) {
        super.setAuthenticatedProfile(profile);

        if (profile != null) {
            if (mPlus.isAuthenticated()) {
                mImageListView.setAdapter(mMomentListAdapter);
                
                // Load the activities for the currently authenticated user from the PlusClient.
                // PlusClient notifies us of completion via onMomentsLoaded().
                mPlus.getClient().loadMoments(this);
            }

            // Load the users PhotoHunt profile name.
            mProfileText.setText(profile.googleDisplayName);
            
            // Load the users PhotoHunt profile image.
            if (profile.googlePublicProfilePhotoUrl != null) {
                mImageLoader.bind(mProfileImageView, profile.getProfileUrl(),
                        new ImageLoader.Callback() {
                            @Override
                            public void onImageLoaded(ImageView view, String url) {
                                mProfileImageView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onImageError(ImageView view, String url, Throwable e) {
                                mProfileImageView.setVisibility(View.INVISIBLE);
                                Log.e(TAG, e.getMessage(), e);
                            }
                        });
            }

            // Record the users activity in Google Analytics passing the PhotoHunt profile id.
            EasyTracker.getInstance().setContext(this);
            Tracker tracker = EasyTracker.getInstance().getTracker();
            tracker.set("&uid", profile.id.toString());
            tracker.trackView("viewMoments");
        } else {
            // Close this ProfileActivity if the user signed out.
            finish();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mImageListView.setAdapter(null);
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();
        mImageListView.setAdapter(null);
    }

    /**
     * Callback to notify that the currently authenticated users activities have been retrieved.
     * 
     * @param status the status returned from the request to fetch moments.
     * @param moments an immutable buffer containing the activities of the user.
     * @param nextPageToken a token to retrieve older user activities.
     * @param updated the time at which this collection of moments was last updated. Formatted as an RFC 3339 timestamp.
     */
    @Override
    public void onMomentsLoaded(ConnectionResult status, MomentBuffer moments,
            String nextPageToken, String updated) {
        if (status.getErrorCode() == ConnectionResult.SUCCESS) {
            try {
                for (Moment moment : moments) {
                    // Make the activities available to our adapter.
                    // Each moment must be frozen in order to persist it outside of the
                    // MomentBuffer.
                    mListItems.add(moment.freeze());
                }
            } finally {
                moments.close();
            }

            mMomentListAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "Error when loading moments: " + status.getErrorCode());
        }
    }

    /**
     * Array adapter that maintains a Moment list.
     */
    private class MomentListAdapter extends ArrayAdapter<Moment> {
        private ArrayList<Moment> items;

        public MomentListAdapter(Context ctx, int textViewResourceId, ArrayList<Moment> objects) {
            super(ctx, textViewResourceId, objects);
            items = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.moment_list_item, null);
            }
            Moment moment = items.get(position);
            if (moment != null) {
                TextView typeView = (TextView)v.findViewById(R.id.moment_type);
                TextView titleView = (TextView)v.findViewById(R.id.moment_title);
                typeView.setText("Voted");

                if (moment.getTarget() != null) {
                    titleView.setText(moment.getTarget().getName());
                }
            }

            return v;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpTo(this, new Intent(this, ThemeViewActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
