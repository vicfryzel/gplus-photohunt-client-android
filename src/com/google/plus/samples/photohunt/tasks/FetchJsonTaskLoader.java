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

package com.google.plus.samples.photohunt.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.plus.samples.photohunt.HttpUtils;
import com.google.plus.samples.photohunt.auth.AuthUtil;

/**
 * Loads photos from the photohunt backend.
 */
public class FetchJsonTaskLoader<T> extends AsyncTaskLoader<T> {
	
    private static final String TAG = FetchJsonTaskLoader.class.getSimpleName();
    
	private final String mUrl;
    private T mResult;
    
    protected Type mReturnType;
    
    protected Exception mException;
    
    public FetchJsonTaskLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }
    
    public FetchJsonTaskLoader(Context context, String url, Type returnType) {
        this(context, url);
        
        mReturnType = returnType;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        
        if (mResult != null) {
        	deliverResult(mResult);
        } else if (!TextUtils.isEmpty(mUrl)) {
        	forceLoad();
        } else {
        	deliverResult(null);
        }
    }

    @Override 
    protected void onStopLoading() {
        cancelLoad();
    }

	@Override
	public T loadInBackground() {
        T result = null;

        try {
            result = fetchData();
        } catch (Exception e) {
            if (!isReset()) {
                mException = e;
            }
        }

        return result;
	}

	protected T fetchData() throws IOException {
		HttpURLConnection urlConnection = null;

		try {
			URL url = new URL(mUrl);

			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setUseCaches(true);
			urlConnection.setRequestMethod("GET");

			AuthUtil.setAuthHeaders(urlConnection);

			return onPostFetch(HttpUtils.getContent(urlConnection
					.getInputStream()));
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	@SuppressWarnings("unchecked")
    protected T onPostFetch(ByteArrayOutputStream content) throws IOException {
		try {
			String response = new String(content.toByteArray(), "UTF-8");
            Log.v(TAG, "Fetched " + mUrl + ": " + response);
			if (!TextUtils.isEmpty(response) && mReturnType != null) {
				return (T) new Gson().fromJson(response, mReturnType);
			}
		} catch (Exception jsonException) {
			Log.e(TAG, "Unable to parse the json response from: " + mUrl, jsonException);
			mException = jsonException;
		}

		return null;
	}
	
	public String getUrl() {
		return mUrl;
	}
}
