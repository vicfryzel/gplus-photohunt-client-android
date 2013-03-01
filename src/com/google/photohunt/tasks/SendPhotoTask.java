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

package com.google.photohunt.tasks;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.photohunt.Endpoints;
import com.google.photohunt.HttpUtils;
import com.google.photohunt.auth.AuthUtil;
import com.google.photohunt.model.Photo;

/**
 * Uploads photos to PhotoHunt.
 */
public abstract class SendPhotoTask extends AsyncTask<String, Void, Photo> {

    private static final String BOUNDARY = "------boundary1";

    private static final String NEWLINE = "\r\n";

    private static final String START_BOUNDARY = "--" + BOUNDARY;

    private static final String END_BOUNDARY = "--" + BOUNDARY + "--" + NEWLINE;

    private static final String TAG = SendPhotoTask.class.getSimpleName();

    private long mThemeId;

    private Exception mException;

    protected SendPhotoTask(long themeId) {
        mThemeId = themeId;
    }

    @Override
    protected Photo doInBackground(String... params) {
        Photo result = null;
        try {
            result = sendData(params[0]);
        } catch (Exception e) {
            if (!isCancelled()) {
                mException = e;
            }
        }

        return result;
    }

    protected Photo sendData(String localImageUri) {
        String uploadUrl = fetchUploadUrl();
        Photo result = null;
        String responseBody = null;

        HttpURLConnection conn = null;
        DataOutputStream outStream = null;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap uploadBitmap = BitmapFactory.decodeFile(localImageUri, options);
            ExifInterface exif = new ExifInterface(localImageUri); 
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
    
            uploadBitmap = fixOrientation(uploadBitmap, exifOrientation);

            conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            AuthUtil.setAuthHeaders(conn);

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("User-Agent", Endpoints.USER_AGENT);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            outStream = new DataOutputStream(conn.getOutputStream());
            outStream.writeBytes(START_BOUNDARY + NEWLINE);
            outStream.writeBytes("Content-Disposition: form-data; name=\"themeId\"" + NEWLINE);
            outStream.writeBytes(NEWLINE + mThemeId + NEWLINE);

            outStream.writeBytes(START_BOUNDARY + NEWLINE);
            outStream.writeBytes("Content-Disposition: form-data; "
                    + "name=\"image\"; filename=\"image.jpg\"" + NEWLINE);
            outStream.writeBytes("Content-Type: application/octet-stream" + NEWLINE);

            outStream.writeBytes(NEWLINE);
            uploadBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream);
            outStream.writeBytes(NEWLINE);
            outStream.writeBytes(END_BOUNDARY);

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                responseBody = HttpUtils.getContent(conn.getInputStream()).toString("UTF-8");
                result = new Gson().fromJson(responseBody, Photo.class);
            } else {
                Log.w(TAG, "Failed to upload image [" + localImageUri + "]: error code: " + responseCode);
                Log.w(TAG, "Error response: " + HttpUtils.getErrorResponse(conn));
            }
        } catch (JsonParseException jsonException) {
            Log.e(TAG, "Unable to parse the json response from: " + uploadUrl, jsonException);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (outStream != null) {
                try {
                    outStream.flush();
                    outStream.close();
                    outStream = null;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        Log.v(TAG, "Upload image [" + localImageUri + "]: " + responseBody);
        
        return result;
    }

    protected void onPostExecute(Photo result) {
        if (result == null || mException != null) {
            onError();
        } else {
            onSuccess(result);
        }
    }

    protected void onError() {
        Log.w(TAG, "Error sending data.", mException);
    }

    protected abstract void onSuccess(Photo result);

    protected Exception getException() {
        return mException;
    }

    private String fetchUploadUrl() {
        HttpURLConnection urlConnection = null;
        String uploadUrl = null;
        
        try {
            urlConnection = (HttpURLConnection) new URL(Endpoints.PHOTO_UPLOAD).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Content-Length", "0");
            urlConnection.setFixedLengthStreamingMode(0);
            AuthUtil.setAuthHeaders(urlConnection);

            int responseCode = urlConnection.getResponseCode();
            
            if (responseCode != 200) {
                Log.e(TAG, "Unable to fetch upload URL (" + Endpoints.PHOTO_UPLOAD + "): "
                        + responseCode);
                return null;
            }

            InputStream is = urlConnection.getInputStream();
            uploadUrl = new String(HttpUtils.getContent(is).toByteArray(), "UTF-8");
            
            Log.v(TAG, "Obtained an upload URL: " + uploadUrl);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return uploadUrl;
    }

    public Bitmap fixOrientation(Bitmap original, int adjustment) {
        Bitmap result = null;
        int rotation;

        switch (adjustment) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotation = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotation = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotation = 270;
                break;
            default:
                return original;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        result = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(),
                matrix, true);

        return result;
    }
}
