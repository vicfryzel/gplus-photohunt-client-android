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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.imageloader.ImageLoader;
import com.google.plus.samples.photohunt.PlusClientFragment.OnSignInListener;
import com.google.plus.samples.photohunt.app.PhotoHuntApp;
import com.google.plus.samples.photohunt.auth.AuthUtil;
import com.google.plus.samples.photohunt.client.PhotoClient;
import com.google.plus.samples.photohunt.model.User;
import com.google.plus.samples.photohunt.tasks.FetchJsonTask.FetchCallback;

/**
 * Manages the authentication using Google sign-in and the PhotoHunt back end
 * service across all activities.
 * 
 * Authentication using Google sign-in is delegated to
 * {@link PlusClientFragment} which responds via the
 * {@link #onSignedIn(PlusClient)} method to indicated successful authentication
 * and the {@link #onSignInFailed()} method to indicate failure.
 * 
 * On successful authentication the corresponding OAuth token is passed to the
 * PhotoHunt back end service to be associated with a service specific profile.
 */
public abstract class BaseActivity extends SherlockFragmentActivity implements
		OnSignInListener, View.OnClickListener {

	/**
	 * Code used to identify the login request to the {@link PlusClientFragment}
	 * .
	 */
	public static final int REQUEST_CODE_PLUS_CLIENT_FRAGMENT = 0;

	/** Delegate responsible for handling Google sign-in. */
	protected PlusClientFragment mPlus;

	/** Used to retrieve the PhotoHunt back end session id. */
	private AsyncTask<Object, Void, User> mAuthTask;

	/** Client used to access the PhotoHunt API. */
	protected PhotoClient mPhotoClient;

	/** Image cache which manages asynchronous loading and caching of images. */
	protected ImageLoader mImageLoader;

	/** Person as returned by Google Play Services. */
	protected Person mPlusPerson;

	/** Profile as returned by the PhotoHunt service. */
	protected User mPhotoUser;

	/**
	 * Stores the pending click listener which should be executed if the user
	 * successfully authenticates. {@link #mPendingClick} is populated if a user
	 * performs an action which requires authentication but has not yet
	 * successfully authenticated.
	 */
	protected View.OnClickListener mPendingClick;

	/**
	 * Stores the {@link View} which corresponds to the pending click listener
	 * and is supplied as an argument when the action is eventually resolved.
	 */
	protected View mPendingView;

	/**
	 * Stores the @link com.google.android.gms.common.SignInButton} for use in
	 * the action bar.
	 */
	protected SignInButton mSignInButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		mPhotoClient = new PhotoClient();

		mImageLoader = ((PhotoHuntApp) getApplication()).getImageLoader();

		// Create the PlusClientFragment which will initiate authentication if
		// required.
		// AuthUtil.SCOPES describe the permissions that we are requesting of
		// the user to access
		// their information and write to their moments vault.
		// AuthUtil.VISIBLE_ACTIVITIES describe the types of moment which we can
		// read from or write
		// to the user's vault.
		mPlus = PlusClientFragment.getPlusClientFragment(this, AuthUtil.SCOPES,
				AuthUtil.VISIBLE_ACTIVITIES);

		mSignInButton = (SignInButton) getLayoutInflater().inflate(
				R.layout.sign_in_button, null);
		mSignInButton.setOnClickListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);

		// Delegate onActivityResult handling to PlusClientFragment to resolve
		// authentication
		// failures, eg. if the user must select an account or grant our
		// application permission to
		// access the information we have requested in AuthUtil.SCOPES and
		// AuthUtil.VISIBLE_ACTIVITIES
		mPlus.handleOnActivityResult(requestCode, responseCode, intent);
	}

	@Override
	public void onStop() {
		super.onStop();

		// Reset any asynchronous tasks we have running.
		resetTaskState();
	}

	/**
	 * Invoked when the {@link PlusClientFragment} delegate has successfully
	 * authenticated the user.
	 * 
	 * @param plusClient
	 *            The connected PlusClient which gives us access to the Google+
	 *            APIs.
	 */
	@Override
	public void onSignedIn(PlusClient plusClient) {
		if (plusClient.isConnected()) {
			mPlusPerson = plusClient.getCurrentPerson();

			// Retrieve the account name of the user which allows us to retrieve
			// the OAuth access
			// token that we securely pass over to the PhotoHunt service to
			// identify and
			// authenticate our user there.
			final String name = plusClient.getAccountName();

			// Asynchronously authenticate with the PhotoHunt service and
			// retrieve the associated
			// PhotoHunt profile for the user.
			mAuthTask = new AsyncTask<Object, Void, User>() {
				@Override
				protected User doInBackground(Object... o) {
					return AuthUtil.authenticate(BaseActivity.this, name);
				}

				@Override
				protected void onPostExecute(User result) {
					if (result != null) {
						setAuthenticatedProfile(result);
						executePendingActions();
						update();
					} else {
						setAuthenticatedProfile(null);
						mPlus.signOut();
					}
				}
			};

			mAuthTask.execute();
		}
	}

	/**
	 * Invoked when the {@link PlusClientFragment} delegate has failed to
	 * authenticate the user. Failure to authenticate will often mean that the
	 * user has not yet chosen to sign in.
	 * 
	 * The default implementation resets the PhotoHunt profile to null.
	 */
	@Override
	public void onSignInFailed() {
		update();
	}

	/**
	 * Invoked when the PhotoHunt profile has been successfully retrieved for an
	 * authenticated user.
	 * 
	 * @param profile
	 */
	public void setAuthenticatedProfile(User profile) {
		mPhotoUser = profile;
	}

	/**
	 * Update the user interface to reflect the current application state. This
	 * function is called whenever this Activity's state has been modified.
	 * 
	 * {@link BaseActivity} calls this method when user authentication succeeds
	 * or fails.
	 */
	public void update() {
		supportInvalidateOptionsMenu();
	}

	/**
	 * Execute actions which are pending; eg. because they were waiting for the
	 * user to authenticate.
	 */
	protected void executePendingActions() {
		// On successful authentication we resolve any pending actions
		if (mPendingClick != null) {
			mPendingClick.onClick(mPendingView);
			mPendingClick = null;
			mPendingView = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getSupportActionBar().setDisplayShowCustomEnabled(false);

		if (isAuthenticated()) {
			// Show the 'sign out' menu item only if we have an authenticated
			// PhotoHunt profile.
			menu.add(0, R.id.menu_item_sign_out, 0,
					getString(R.string.sign_out_menu_title)).setShowAsAction(
					MenuItem.SHOW_AS_ACTION_NEVER);

			menu.add(0, R.id.menu_item_disconnect, 0,
					getString(R.string.disconnect_menu_title)).setShowAsAction(
					MenuItem.SHOW_AS_ACTION_NEVER);
		} else if (!isAuthenticated() && !isAuthenticating()) {
			ActionBar.LayoutParams params = new ActionBar.LayoutParams(
					ActionBar.LayoutParams.WRAP_CONTENT,
					ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
			getSupportActionBar().setCustomView(mSignInButton, params);
			getSupportActionBar().setDisplayShowCustomEnabled(true);
		}

		return true;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.sign_in_button) {
			mPlus.signIn(REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_disconnect:
			mPhotoClient.disconnectAccount(new FetchCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					mPlus.signOut();
					// Invalidate the PhotoHunt session
					AuthUtil.invalidateSession();
				}

				@Override
				public void onError(Void result) {
					Toast toast = Toast.makeText(BaseActivity.this,
							getString(R.string.disconnect_failed),
							Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					toast.show();
				}
			});

			return true;

		case R.id.menu_item_sign_out:
			mPlus.signOut();
			// Invalidate the PhotoHunt session
			AuthUtil.invalidateSession();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Provides a guard to ensure that a user is authenticated.
	 */
	protected boolean requireSignIn() {
		if (!mPlus.isAuthenticated()) {
			mPlus.signIn(REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @return true if the user is currently authenticated through Google
	 *         sign-in and the the user's PhotoHunt profile has being fetched.
	 */
	public boolean isAuthenticated() {
		return mPlus.isAuthenticated() && mPhotoUser != null;
	}

	/**
	 * @return true if the user is currently being authenticated through Google
	 *         sign-in or if the the user's PhotoHunt profile is being fetched.
	 */
	public boolean isAuthenticating() {
		return mPlus.isConnecting() || mPlus.isAuthenticated()
				&& mPhotoUser == null;
	}

	/**
	 * Resets the state of asynchronous tasks used by this activity.
	 */
	protected void resetTaskState() {
		if (mAuthTask != null) {
			mAuthTask.cancel(true);
			mAuthTask = null;
		}
	}

}
