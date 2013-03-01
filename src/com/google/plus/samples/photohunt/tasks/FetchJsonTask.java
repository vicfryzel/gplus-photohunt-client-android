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
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.plus.samples.photohunt.Endpoints;
import com.google.plus.samples.photohunt.HttpUtils;
import com.google.plus.samples.photohunt.auth.AuthUtil;

/**
 * @param <T>
 */
public class FetchJsonTask<T> extends AsyncTask<Void, Void, T> {
	
    private static final String TAG = FetchJsonTask.class.getSimpleName();

    private final String mUrl;
    protected Exception mException;

    protected String mRequestMethod = "GET";
    protected byte[] mRequestBody = null;
    protected Type mReturnType;

    private final FetchCallback<T> mCallback;
    
    public static class FetchCallback<T> {

        public void onSuccess(T result) { /* Do nothing. */ }

        public void onError(T result) { /* Do nothing. */ }

    }
    
    public FetchJsonTask(String fetchUrl) {
        this(fetchUrl, null);
    }
    
    public FetchJsonTask(String fetchUrl, FetchCallback<T> callback) {
        mUrl = fetchUrl;
        mCallback = callback;
    }
    
    public FetchJsonTask(String fetchUrl, FetchCallback<T> callback, Type returnType) {
        this(fetchUrl, callback);
        
        mReturnType = returnType;
    }

    @Override
    protected T doInBackground(Void... params) {
        T result = null;

        try {
            result = fetchData();
        } catch (Exception e) {
            if (!isCancelled()) {
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
            urlConnection.setRequestProperty("User-Agent", Endpoints.USER_AGENT);
            urlConnection.setUseCaches(true);
            urlConnection.setRequestMethod(mRequestMethod);
            
            AuthUtil.setAuthHeaders(urlConnection);
            
            if (mRequestBody != null) {
                urlConnection.setAllowUserInteraction(false);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                
                OutputStream outStream = urlConnection.getOutputStream();
                outStream.write(mRequestBody);
            }

            return onPostFetch(HttpUtils.getContent(urlConnection.getInputStream()));
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
            Log.e(TAG, "Unable to parse the json response from: " + mUrl,
                    jsonException);
        }

        return null;
    }

    protected void onPostExecute(T result) {
        if (null != mException || (null == result && mReturnType != null)) {
            onError(result);
        } else {
            onSuccess(result);
        }
    }

    protected void onError(T result) {
        Log.w(TAG, "Error fetching data (" + mUrl + ")", mException);
        mCallback.onError(result);
    }

    protected void onSuccess(T result) {
        mCallback.onSuccess(result);
    }

    protected Exception getException() {
        return mException;
    }

    protected String getFetchUrl() {
        return mUrl;
    }

}
