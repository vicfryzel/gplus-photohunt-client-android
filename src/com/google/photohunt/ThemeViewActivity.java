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

import static com.google.photohunt.PhotoListAdapter.FRIEND_PHOTOS_ID;
import static com.google.photohunt.PhotoListAdapter.MY_PHOTOS_ID;
import static com.google.photohunt.PhotoListAdapter.THEME_PHOTOS_ID;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.gson.reflect.TypeToken;
import com.google.photohunt.model.Photo;
import com.google.photohunt.model.Theme;
import com.google.photohunt.model.User;
import com.google.photohunt.tasks.FetchJsonTaskLoader;
import com.google.photohunt.tasks.SendPhotoTask;
import com.google.photohunt.widget.PinnedHeaderListView;

/**
 * Lists and renders the photos for a theme.  Handles the upload of photos to a theme.
 */
public class ThemeViewActivity extends BaseActivity {

    /** Tag to communicate with the {@link ThemeSelectDialog}. */
    private static final String SELECT_THEME_TAG = "SELECT_THEME_TAG";

    /** Loader id for the list of {@link Theme}s. */
    private static final int THEME_LIST_ID = 100;

    /** Activity result code for image capture. */
    private static final int REQUEST_CODE_IMAGE_CAPTURE = 6000;

    /** Activity result code for image gallery select. */
    private static final int REQUEST_CODE_IMAGE_SELECT = 6001;

    /** Id of the currently displayed theme. */
    private Long mThemeId;

    /** Currently displayed theme. */
    private Theme mTheme;

    /** List of all themes. */
    private List<Theme> mThemes;

    /** List of all photos for the current theme. */
    private List<Photo> mThemePhotos;

    /** List of photos by friends of the current user in the current theme. */
    private List<Photo> mFriendPhotos;

    /** List of the current users photos in the current theme. */
    private List<Photo> mMyPhotos;

    /** Loader fetching the list of themes. */
    private FetchJsonTaskLoader<List<Theme>> mThemeListLoader;

    /** Loader fetching the list of all photos for the current theme. */
    private FetchJsonTaskLoader<List<Photo>> mThemePhotosLoader;

    /** Loader fetching the list of photos by friends of the current user. */
    private FetchJsonTaskLoader<List<Photo>> mFriendPhotosLoader;

    /** Loader fetching the list of photos by the current user. */
    private FetchJsonTaskLoader<List<Photo>> mMyPhotosLoader;

    private LoaderManager mLoaderMgr;
    private PhotoListAdapter mPhotoListAdapter;
    private ListView mPhotoListView;

    /** AsyncTask used to upload photos to the PhotoHunt service. */
    private SendPhotoTask mSendTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_view_activity);
        setSupportProgressBarIndeterminateVisibility(false);

        mLoaderMgr = getSupportLoaderManager();

        mThemes = new ArrayList<Theme>();
        mMyPhotos = new ArrayList<Photo>();
        mFriendPhotos = new ArrayList<Photo>();
        mThemePhotos = new ArrayList<Photo>();

        mPhotoListAdapter = new PhotoListAdapter(this);
        mPhotoListAdapter.changeList(MY_PHOTOS_ID, mMyPhotos);
        mPhotoListAdapter.changeList(FRIEND_PHOTOS_ID, mFriendPhotos);
        mPhotoListAdapter.changeList(THEME_PHOTOS_ID, mThemePhotos);

        mPhotoListView = (PinnedHeaderListView) findViewById(R.id.theme_images_view);
        mPhotoListView.setAdapter(mPhotoListAdapter);

        // Set the desired theme to display if it was set in the calling Intent.
        // For example, if we deep linked to a theme.
        if (null != getIntent() && getIntent().hasExtra(Intents.THEME_ID_EXTRA)) {
            mThemeId = (Long) getIntent().getExtras().get(Intents.THEME_ID_EXTRA);
        }

        Bundle bundle = new Bundle();

        // Initialise the Loaders
        mThemePhotosLoader = (FetchJsonTaskLoader<List<Photo>>) mLoaderMgr.initLoader(THEME_PHOTOS_ID,
                bundle, new PhotoCallbacks(THEME_PHOTOS_ID, mThemePhotos));
        mMyPhotosLoader = (FetchJsonTaskLoader<List<Photo>>) mLoaderMgr.initLoader(MY_PHOTOS_ID,
                bundle, new PhotoCallbacks(MY_PHOTOS_ID, mMyPhotos));
        mFriendPhotosLoader = (FetchJsonTaskLoader<List<Photo>>) mLoaderMgr.initLoader(
                FRIEND_PHOTOS_ID, bundle, new PhotoCallbacks(FRIEND_PHOTOS_ID, mFriendPhotos));

        bundle.putString("url", String.format(Endpoints.THEME_LIST, 0, 50));
        mThemeListLoader = (FetchJsonTaskLoader<List<Theme>>) mLoaderMgr.initLoader(THEME_LIST_ID,
                bundle, new ThemeListCallbacks());

        update();
    }
    
    @Override
    public void setAuthenticatedProfile(User profile) {
        super.setAuthenticatedProfile(profile);
        
        // User has successfully authenticated; reconfigure the Loaders with the user id and
        // refresh the data.
        mPhotoListAdapter.setActiveProfile(profile);
        mPhotoListAdapter.setDirty(MY_PHOTOS_ID, true);
        mPhotoListAdapter.setDirty(FRIEND_PHOTOS_ID, true);
        configurePhotoLoaders();
        
        // Update the analytics if the user has signed in
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

        if (mTheme != null) {
            tracker.trackView("theme/" + mTheme.id.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);

        switch (requestCode) {
            case REQUEST_CODE_IMAGE_CAPTURE:
                if (responseCode == RESULT_OK) {
                    sendImage(Intents.getPhotoImageUri().getPath(), mTheme.id);
                }
                break;
            case REQUEST_CODE_IMAGE_SELECT:
                if (responseCode == RESULT_OK && intent != null && intent.getData() != null) {
                    String imageUriString = intent.getDataString();
                    Uri imageUri = intent.getData();

                    if ("content".equals(imageUri.getScheme())) {
                        Cursor cursor = getContentResolver()
                                .query(imageUri, new String[] { Media.DATA }, null, null, null);
                        int column_index = cursor.getColumnIndexOrThrow(Media.DATA);
                        cursor.moveToFirst();

                        imageUriString = cursor.getString(column_index);
                    }

                    sendImage(imageUriString, mTheme.id);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_upload:
                startCameraIntent();
                return true;

            case R.id.menu_item_gallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                        REQUEST_CODE_IMAGE_SELECT);
                return true;

            case android.R.id.home:
            case R.id.menu_item_theme_select:
                DialogFragment themeDialog = new ThemeSelectDialog();
                themeDialog.show(getSupportFragmentManager(), SELECT_THEME_TAG);
                return true;

            case R.id.menu_item_profile:
                Intent profileIntent = new Intent();
                profileIntent.setClass(this, ProfileActivity.class);
                startActivity(profileIntent);
                return true;

            case R.id.menu_item_refresh:
                mThemeListLoader.forceLoad();
                mPhotoListAdapter.setDirty(THEME_PHOTOS_ID, true);
                mPhotoListAdapter.setDirty(FRIEND_PHOTOS_ID, true);
                mPhotoListAdapter.setDirty(MY_PHOTOS_ID, true);
                update();
                return true;

            case R.id.menu_item_about:
                Intent aboutIntent = new Intent();
                aboutIntent.setClass(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Theme active = getActiveTheme(mThemes);
        
        if (isAuthenticated()) {
            // Only allow image upload, gallery select or profile view if the user is 
            // authenticated.
            if (active != null && mTheme != null && active.id == mTheme.id) {
                menu.add(0, R.id.menu_item_upload, 0, getString(R.string.upload_menu_title))
                        .setIcon(android.R.drawable.ic_menu_camera)
                        .setShowAsAction(
                                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

                menu.add(0, R.id.menu_item_gallery, 0, getString(R.string.gallery_menu_title))
                        .setIcon(android.R.drawable.ic_menu_gallery)
                        .setShowAsAction(
                                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }

            menu.add(0, R.id.menu_item_profile, 0, getString(R.string.profile_menu_title))
                    .setIcon(R.drawable.ic_action_profile)
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (active != null) {
            // Only allow theme selection if we have at least one theme.
            menu.add(0, R.id.menu_item_theme_select, 0, getString(R.string.change_theme_menu_title))
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        menu.add(0, R.id.menu_item_refresh, 0, getString(R.string.refresh_menu_title))
                .setIcon(R.drawable.ic_popup_sync_2)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, R.id.menu_item_about, 0, getString(R.string.about_menu_title))
                .setIcon(R.drawable.ic_popup_sync_2)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    private void startCameraIntent() {
        ConnectivityManager connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!mWifi.isConnected()) {
            Toast.makeText(this, getString(R.string.toast_connect_wifi), Toast.LENGTH_LONG).show();
        }

        Intent cameraIntent = Intents.getCameraIntent();
        startActivityForResult(cameraIntent, REQUEST_CODE_IMAGE_CAPTURE);
    }

    private void sendImage(String imageUri, Long id) {
        mSendTask = new SendPhotoTask(id) {
            @Override
            protected void onSuccess(Photo result) {
                Toast.makeText(ThemeViewActivity.this, getString(R.string.upload_success),
                        Toast.LENGTH_LONG).show();
                mSendTask = null;
                update();
            }

            @Override
            protected void onError() {
                Toast.makeText(ThemeViewActivity.this, getString(R.string.upload_failure),
                        Toast.LENGTH_LONG).show();
                mSendTask = null;
                update();
            }
        };

        mSendTask.execute(imageUri);
        update();
    }

    @Override
    protected void resetTaskState() {
        if (mSendTask != null) {
            mSendTask.cancel(true);
            mSendTask = null;
        }
    }

    @Override
    public void update() {
        super.update();

        // Force the load of any dirty list partitions.
        if (mPhotoListAdapter.isDirty(THEME_PHOTOS_ID)) {
            mThemePhotosLoader.forceLoad();
        }

        if (mPhotoListAdapter.isDirty(MY_PHOTOS_ID)) {
            mMyPhotosLoader.forceLoad();
        }

        if (mPhotoListAdapter.isDirty(FRIEND_PHOTOS_ID)) {
            mFriendPhotosLoader.forceLoad();
        }

        if (mTheme != null) {
            getSupportActionBar().setTitle(mTheme.displayName);

            if (mThemes != null && mThemes.size() > 1) {
                // Enable the home button for theme selection.
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // Update the progress bar based on whether we are uploading a photo.
        setSupportProgressBarIndeterminateVisibility(mSendTask != null);

        mPhotoListAdapter.notifyDataSetChanged();
    }

    public void setSelectedTheme(Theme theme) {
        trackAnalytics();
        mTheme = theme;
        mPhotoListAdapter.setTheme(theme);

        if (mTheme != null) {
            mThemeId = mTheme.id;
        } else {
            mThemeId = null;
        }

        configurePhotoLoaders();
    }

    public Theme getSelectedTheme() {
        return mTheme;
    }
    

    private Theme getActiveTheme(List<Theme> themes) {
        if (themes != null && themes.size() > 0) {
            return themes.get(0);
        } else {
            return null;
        }
    }

    public List<Theme> getThemes() {
        return mThemes;
    }

    /**
     * Ensure that the correct photo loaders are running based on whether we
     * have a theme selected and whether the user is authenticated.
     */
    private void configurePhotoLoaders() {
        String themePhotosUrl = null;
        String myPhotosUrl = null;
        String friendPhotosUrl = null;

        if (mTheme != null) {
            themePhotosUrl = String.format(Endpoints.THEME_PHOTO_LIST, mTheme.id);

            if (!isAuthenticating() && mPhotoUser != null) {
                myPhotosUrl = String.format(Endpoints.USER_THEME_PHOTO_LIST, Endpoints.ME_ID,
                        mTheme.id);

                friendPhotosUrl = String.format(Endpoints.FRIENDS_PHOTO_LIST, Endpoints.ME_ID,
                        mTheme.id);
            }
        }

        mThemePhotosLoader = restartLoader(mLoaderMgr, THEME_PHOTOS_ID, mThemePhotosLoader,
                new PhotoCallbacks(THEME_PHOTOS_ID, mThemePhotos), themePhotosUrl);
        mMyPhotosLoader = restartLoader(mLoaderMgr, MY_PHOTOS_ID, mMyPhotosLoader,
                new PhotoCallbacks(MY_PHOTOS_ID, mMyPhotos), myPhotosUrl);
        mFriendPhotosLoader = restartLoader(mLoaderMgr, FRIEND_PHOTOS_ID, mFriendPhotosLoader,
                new PhotoCallbacks(FRIEND_PHOTOS_ID, mFriendPhotos), friendPhotosUrl);
    }

    private class ThemeListCallbacks implements LoaderManager.LoaderCallbacks<List<Theme>> {

        @Override
        public Loader<List<Theme>> onCreateLoader(int i, Bundle bundle) {
            String url = bundle.getString("url");

            return new FetchJsonTaskLoader<List<Theme>>(ThemeViewActivity.this, url) {
                { mReturnType = new TypeToken<ArrayList<Theme>>() {}.getType(); }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Theme>> loader, List<Theme> themes) {
            mThemes = themes;

            Theme active = getActiveTheme(mThemes);
            if (active != null) {
                if (mThemeId == null) {
                    // If mThemeId has not been set we default it to the currently active them.
                    // Otherwise we assume it was selected by the user explicitly.
                    setSelectedTheme(active);
                } else if (mTheme == null) {
                    for (Theme theme : mThemes) {
                        if (mThemeId == theme.id) {
                            setSelectedTheme(theme);
                            break;
                        }
                    }
                }

                mPhotoListAdapter.setActiveTheme(active);
            } else {
                mThemeId = null;
                setSelectedTheme(null);
                mPhotoListAdapter.setActiveTheme(null);
            }

            update();
        }

        @Override
        public void onLoaderReset(Loader<List<Theme>> loader) {
            mThemes = null;
            mThemeId = null;
            setSelectedTheme(null);
            mPhotoListAdapter.setActiveTheme(null);
            update();
        }

    }

    /**
     * Updates a {@link Photo} list backing one of the partitions of the PhotoListAdapter.
     */
    private class PhotoCallbacks implements LoaderManager.LoaderCallbacks<List<Photo>> {

        int mId;
        List<Photo> mList;
        
        public PhotoCallbacks(int id, List<Photo> list) {
            mId = id;
            mList = list;
        }
        
        @Override
        public Loader<List<Photo>> onCreateLoader(int i, Bundle bundle) {
            String url = bundle.getString("url");

            return new FetchJsonTaskLoader<List<Photo>>(ThemeViewActivity.this, url) {
                { mReturnType = new TypeToken<ArrayList<Photo>>() {}.getType(); }
            };
        }

        @Override
        public void onLoadFinished(Loader<List<Photo>> loader, List<Photo> photos) {
            mList.clear();

            if (photos != null) {
                mList.addAll(photos);
            }

            mPhotoListAdapter.setDirty(mId, false);
            mPhotoListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Photo>> loader) {
            mList.clear();
        }

    }

    /** Restarts the FetchJsonTaskLoader if the URL being fetched has changed. */
    private static <T> FetchJsonTaskLoader<T> restartLoader(LoaderManager loaderMgr, int id,
            FetchJsonTaskLoader<T> loader, LoaderManager.LoaderCallbacks<T> callbacks, String url) {
        FetchJsonTaskLoader<T> result = loader;
        Bundle bundle = new Bundle();
        bundle.putString("url", url);

        if (!TextUtils.equals(url, loader.getUrl())) {
            result = (FetchJsonTaskLoader<T>) loaderMgr.restartLoader(id, bundle, callbacks);
        }

        return result;
    }

}
