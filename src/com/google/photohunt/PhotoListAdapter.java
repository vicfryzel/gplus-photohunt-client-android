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

package com.google.photohunt;

import java.util.List;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.imageloader.ImageLoader;
import com.google.photohunt.app.PhotoHuntApp;
import com.google.photohunt.client.ClickCallback;
import com.google.photohunt.model.Photo;
import com.google.photohunt.model.Theme;
import com.google.photohunt.model.User;
import com.google.photohunt.widget.PinnedHeaderArrayAdapter;

/**
 * List adapter which displays the photos of a {@link Theme} in three sections.
 * 
 * <ul>
 *   <li>My Photos - The photos the current user has uploaded to the selected theme.</li>
 *   <li>Photos by Friends - The photos that the friends of the current user have uploaded to the
 *   selected theme.</li>
 *   <li>All Photos - All photos for the selected theme.</li>
 * </ul>
 * 
 * The adapter keeps track of the currently active theme and the current user in order to enable
 * the correct user interface elements, such as the vote and delete buttons, for each photo.
 */
public class PhotoListAdapter extends PinnedHeaderArrayAdapter<Photo> {

    /** Partition id for 'All Photos' section. */
    public static final int THEME_PHOTOS_ID = 2;

    /** Partition id for 'Photos by Friends' section. */
    public static final int FRIEND_PHOTOS_ID = 1;

    /** Partition id for 'My Photos' section. */
    public static final int MY_PHOTOS_ID = 0;

    /** Default number of partitions to create. */
    public static final int INITIAL_PARTITIONS = 3;

    private BaseActivity mBaseActivity;

    private LayoutInflater mInflater;

    /** Image cache for this adapter. */
    private ImageLoader mImageLoader;

    /** {@link User} to refer to when rendering the user interface. */
    private User mActiveProfile;

    /** Most recently published PhotoHunt {@link Theme} which can therefore receive votes. */
    private Theme mActiveTheme;

    /** {@link Theme} to which the displayed photos belong. */
    private Theme mTheme;

    public PhotoListAdapter(BaseActivity activity) {
        super(activity, INITIAL_PARTITIONS);
        mBaseActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mImageLoader = ((PhotoHuntApp) activity.getApplication()).getImageLoader();

        addPartition(new Partition(false));
        setHeader(MY_PHOTOS_ID, activity.getString(R.string.my_photos));

        addPartition(new Partition(false));
        setHeader(FRIEND_PHOTOS_ID, activity.getString(R.string.photos_from_friends));

        addPartition(new Partition(false));
        setHeader(THEME_PHOTOS_ID, activity.getString(R.string.all_photos));
    }

    /**
     * Set the {@link Theme} to which the displayed photos belong.
     * 
     * @param theme
     */
    public void setTheme(Theme theme) {
        mTheme = theme;
    }

    /**
     * Set the currently active {@link Theme}.  In other words the {@link Theme} which is open for
     * new photos and votes.
     * 
     * @param activeTheme
     */
    public void setActiveTheme(Theme activeTheme) {
        mActiveTheme = activeTheme;
    }

    /**
     * Set the active user profile which determines which actions will be available on each photo.
     * 
     * @param activeProfile
     */
    public void setActiveProfile(User activeProfile) {
        mActiveProfile = activeProfile;
    }

    @Override
    protected View getView(final int partition, List<Photo> list, int position, View convertView,
            ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.photo_list_item, null);

            holder = new ViewHolder();
            holder.authorImage = (ImageView) convertView.findViewById(R.id.author_image);
            holder.authorText = (TextView) convertView.findViewById(R.id.author_name);
            holder.itemImageView = (ImageView) convertView.findViewById(R.id.photo_item);
            holder.voteCount = (TextView) convertView.findViewById(R.id.vote_count);
            holder.voteButton = (Button) convertView.findViewById(R.id.vote_button);
            holder.deleteButton = (ImageButton) convertView.findViewById(R.id.delete_button);
            holder.promoteButton = (Button) convertView.findViewById(R.id.promote_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Photo metadata = getItem(position);
        
        // Photo is active if the photo theme matches the active theme.
        // Only active photos can be voted for.
        final boolean isActive = (mTheme != null 
                && mActiveTheme != null 
                && mActiveTheme.id == mTheme.id);
        
        // Set the users name and profile image if they are available
        if (!TextUtils.isEmpty(metadata.ownerDisplayName)) {
            holder.authorText.setText(metadata.ownerDisplayName);

            if (!TextUtils.isEmpty(metadata.ownerProfilePhoto)) {
                mImageLoader.bind(this, holder.authorImage, metadata.ownerProfilePhoto);
                holder.authorImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String profileUrl = metadata.ownerProfilePhoto;
                        getContext().startActivity(Intents.getPlusUserIntent(profileUrl));
                    }
                });
            }
        } else {
            holder.authorText.setText(mBaseActivity.getString(R.string.unknown_user));
        }

        // Start the ImageLoader retrieving the photo
        if (!TextUtils.isEmpty(metadata.thumbnailUrl)) {
            mImageLoader.bind(this, holder.itemImageView, metadata.thumbnailUrl);
        }

        // Display the delete button if the active user matches the author of the photo
        if (mActiveProfile != null && metadata.hasAuthor(mActiveProfile)) {

            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setEnabled(true);

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View view) {
                    if (!mBaseActivity.mPlus.isAuthenticated()) {
                        mBaseActivity.requireSignIn();
                        mBaseActivity.mPendingClick = this;
                        mBaseActivity.mPendingView = view;
                        return;
                    }

                    final List<Photo> photoList = PhotoListAdapter.this.getList(partition);
                    final int position = photoList.indexOf(metadata);

                    mBaseActivity.mPhotoClient.delete(metadata.id, new ClickCallback<Void>(
                            mBaseActivity, view, R.string.delete_success, R.string.delete_failure) {
                        @Override
                        public void onError(Void v) {
                            super.onError(v);

                            // Rollback on failure
                            photoList.add(position, metadata);
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onSuccess(Void v) {
                            // Initiate a reload of the metadata
                            markAllDirty();
                            mBaseActivity.update();
                        }
                    });

                    view.setEnabled(false);

                    // Optimistic update
                    photoList.remove(metadata);
                    notifyDataSetChanged();
                }
            });

        } else {
            holder.deleteButton.setVisibility(View.INVISIBLE);
        }

        holder.voteCount.setText(getContext().getString(R.string.vote_count, metadata.numVotes));

        if (isActive && (mActiveProfile == null || !metadata.hasAuthor(mActiveProfile)
                && !metadata.voted)) {
            holder.voteButton.setEnabled(true);

            holder.voteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mBaseActivity.mPlus.isAuthenticated()) {
                        mBaseActivity.requireSignIn();
                        mBaseActivity.mPendingClick = this;
                        mBaseActivity.mPendingView = view;
                        return;
                    }

                    if (!isActive || !(mActiveProfile != null
                            && !metadata.hasAuthor(mActiveProfile)
                            && !metadata.voted)) {
                        return;
                    }

                    // Submit the vote.
                    mBaseActivity.mPhotoClient.vote(metadata.id, new ClickCallback<Photo>(
                            mBaseActivity, view, R.string.vote_success, R.string.vote_failure) {
                        @Override
                        public void onError(Photo photo) {
                            super.onError(photo);

                            // Rollback on failure
                            metadata.numVotes -= 1;
                            metadata.voted = false;
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onSuccess(Photo photo) {
                            // Initiate a reload of the metadata
                            markAllDirty();
                            mBaseActivity.update();
                        }
                    });

                    view.setEnabled(false);

                    // Optimistic update
                    metadata.numVotes += 1;
                    metadata.voted = true;
                    notifyDataSetChanged();
                }
            });
        } else {
            holder.voteButton.setEnabled(false);
        }

        // Add a promote button to create a Google+ share
        holder.promoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mBaseActivity.mPlus.isAuthenticated()) {
                    mBaseActivity.requireSignIn();
                    mBaseActivity.mPendingClick = this;
                    mBaseActivity.mPendingView = view;
                    return;
                }

                Intent interactivePostIntent = Intents.getInteractiveIntent(mBaseActivity,
                        metadata, mBaseActivity.mPlus.getClient(), mTheme, isActive);
                mBaseActivity.startActivityForResult(interactivePostIntent, 0);
            }
        });

        return convertView;
    }

    @Override
    protected View getHeaderView(int partition, List<Photo> list, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.photo_list_header, null);
        }

        TextView header = (TextView)convertView.findViewById(R.id.header_title);
        header.setText(this.getPartition(partition).getHeader());

        return convertView;
    }

    static class ViewHolder {
        ImageView authorImage;
        TextView authorText;
        ImageView itemImageView;
        TextView voteCount;
        Button voteButton;
        ImageButton deleteButton;
        Button promoteButton;
    }
}
