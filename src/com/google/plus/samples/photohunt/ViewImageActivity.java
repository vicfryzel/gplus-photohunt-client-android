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

import static android.text.TextUtils.isEmpty;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.plus.samples.photohunt.client.ClickCallback;
import com.google.plus.samples.photohunt.model.Photo;
import com.google.plus.samples.photohunt.model.Theme;
import com.google.plus.samples.photohunt.model.User;
import com.google.plus.samples.photohunt.tasks.FetchJsonTask;
import com.google.plus.samples.photohunt.tasks.FetchJsonTask.FetchCallback;

/**
 * Allow users to view a single photo from the stream. Users that click on a
 * deep-linked photo from the Google+ stream will also land here.
 */
public class ViewImageActivity extends BaseActivity {

    private String mImageId;

    private String mAction;

    private Photo mPhoto;

    private List<Theme> mThemes;

    private Theme mActiveTheme;

    private ImageView mPhotoView;

    private Button mPromoteButton;

    private Button mVoteButton;

    private ImageButton mDeleteButton;

    private TextView mVoteCount;

    private TextView mAuthorName;

    private ImageView mAuthorImage;

    private FetchJsonTask<Photo> mImageTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_list_item);

        mPhotoView = (ImageView) findViewById(R.id.photo_item);
        mPromoteButton = (Button) findViewById(R.id.promote_button);
        mVoteButton = (Button) findViewById(R.id.vote_button);
        mVoteCount = (TextView) findViewById(R.id.vote_count);
        mDeleteButton = (ImageButton) findViewById(R.id.delete_button);
        mAuthorName = (TextView) findViewById(R.id.author_name);
        mAuthorImage = (ImageView) findViewById(R.id.author_image);

        if (null != getIntent() && getIntent().hasExtra(Intents.PHOTO_ID_EXTRA)) {
            mImageId = (String) getIntent().getExtras().get(Intents.PHOTO_ID_EXTRA);
        }

        if (null != getIntent() && getIntent().hasExtra(Intents.ACTION_EXTRA)) {
            // Record the deep link action if it is provided
            mAction = (String) getIntent().getExtras().get(Intents.ACTION_EXTRA);
        }

        mPhotoClient.getThemes(0, 50, new FetchCallback<List<Theme>>() {
            public void onSuccess(List<Theme> themes) {
                mThemes = themes;

                if (mThemes != null && mThemes.size() > 0) {
                    mActiveTheme = mThemes.get(0);
                } else {
                    mActiveTheme = null;
                }

                update();
            }
        });

        if (mImageId != null) {
            String imageUrl = String.format(Endpoints.PHOTO, mImageId);
            mImageTask = new FetchJsonTask<Photo>(imageUrl) {
                { mReturnType = Photo.class; }
                
                @Override
                protected void onSuccess(Photo result) {
                    mPhoto = result;

                    if (mAction.equals("vote")) {
                        // If a deep link was provided, execute the action
                        vote(mVoteButton);
                    } else {
                        update();
                    }

                    trackAnalytics();
                }
            };

            mImageTask.execute();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        update();
    }

    public void update() {
        super.update();

        if (mPhoto != null) {
            mVoteCount.setText(getString(R.string.vote_count, mPhoto.numVotes));

            if (mPhoto.ownerUserId != null) {
                mAuthorName.setText(mPhoto.ownerDisplayName);

                if (!isEmpty(mPhoto.ownerProfilePhoto)) {
                    mImageLoader.bind(mAuthorImage, mPhoto.ownerProfilePhoto, null);

                    mAuthorImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mPhoto.ownerProfilePhoto)));
                        }
                    });
                }
            } else {
                mAuthorName.setText(getString(R.string.unknown_user));
            }

            mImageLoader.bind(mPhotoView, mPhoto.thumbnailUrl, null);
            
            final boolean isActive = mActiveTheme != null && mPhoto.hasTheme(mActiveTheme);
            
            if (isActive && (mPhotoUser == null || mPhoto != null
                    && !mPhoto.hasAuthor(mPhotoUser)
                    && mPhoto.voted)) {
                mVoteButton.setEnabled(true);

                mVoteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        vote(view);
                    }
                });
            } else {
                mVoteButton.setEnabled(false);
            }

            if (mPhotoUser != null && mPhoto.hasAuthor(mPhotoUser)) {
                mDeleteButton.setVisibility(View.VISIBLE);

                mDeleteButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(final View view) {
                        if (!mPlus.isAuthenticated()) {
                            requireSignIn();
                            mPendingClick = this;
                            mPendingView = view;
                            return;
                        }

                        if (mPhotoUser != null && mPhoto.hasAuthor(mPhotoUser)) {
                            mPhotoClient.delete(mPhoto.id, new ClickCallback<Void>(
                                    ViewImageActivity.this, view, R.string.delete_success,
                                    R.string.delete_failure));
                            view.setEnabled(false);
                            finish();
                        }
                    }
                });
            } else {
                mDeleteButton.setVisibility(View.INVISIBLE);
            }

            mPromoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mPlus.isAuthenticated()) {
                        requireSignIn();
                        mPendingClick = this;
                        mPendingView = view;
                        return;
                    }

                    Intent interactivePostIntent = Intents.getInteractiveIntent(
                            ViewImageActivity.this, mPhoto, mPlus.getClient(), getPhotoTheme(),
                            isActive);
                    startActivityForResult(interactivePostIntent, 0);
                }
            });
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

    @Override
    public void setAuthenticatedProfile(User profile) {
        super.setAuthenticatedProfile(profile);

        trackAnalytics();
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();

        trackAnalytics();
    }

    private void trackAnalytics() {
        EasyTracker.getInstance().setContext(this);
        Tracker tracker = EasyTracker.getInstance().getTracker();

        if (mPhotoUser != null) {
            tracker.set("&uid", mPhotoUser.id.toString());
        } else {
            tracker.set("&uid", null);
        }

        if (mPhoto != null) {
            tracker.trackView("image/" + mPhoto.id);
        }
    }

    private void vote(View view) {
        if (!mPlus.isAuthenticated()) {
            requireSignIn();
            mPendingClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vote(view);
                }
            };
            mPendingView = view;
            return;
        }
        
        if (mPhoto != null && mPhotoUser != null && !mPhoto.hasAuthor(mPhotoUser)
                && mPhoto.voted) {
            // Submit the vote.
            mPhotoClient.vote(mPhoto.id, new ClickCallback<Photo>(this, view,
                    R.string.vote_success, R.string.vote_failure) {
                @Override
                public void onError(Photo photo) {
                    super.onError(photo);

                    mPhoto.numVotes -= 1;
                    mPhoto.voted = false;
                    update();
                }
            });
            view.setEnabled(false);

            // Optimistic update.
            mPhoto.numVotes += 1;
            mPhoto.voted = true;
            update();
        }
    }

    private Theme getPhotoTheme() {
        for (Theme theme : mThemes) {
            if (mPhoto != null && mPhoto.hasTheme(theme)) {
                return theme;
            }
        }

        return null;
    }
}
