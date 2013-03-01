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

package com.google.plus.samples.photohunt.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.Scopes;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.plus.samples.photohunt.Endpoints;
import com.google.plus.samples.photohunt.HttpUtils;
import com.google.plus.samples.photohunt.model.User;

/**
 * Provides static utility methods to help make authenticated requests.
 */
public class AuthUtil {

    private static final String TAG = AuthUtil.class.getSimpleName();

    public static final String[] SCOPES = {
            Scopes.PLUS_LOGIN
    };

    public static final String[] VISIBLE_ACTIVITIES = {
            "http://schemas.google.com/AddActivity", "http://schemas.google.com/ReviewActivity"
    };

    private static final String SCOPE_STRING = "oauth2:" + TextUtils.join(" ", SCOPES);

    private static final String ACCESS_TOKEN_JSON = "{ \"access_token\":\"%s\"}";

    private static String sAccessToken = null;

    private static String sCookies = null;

    public static void setAuthHeaders(HttpURLConnection connection) {
        Log.d(TAG, "Authorization: OAuth " + sAccessToken);
        connection.setRequestProperty("Authorization", "OAuth " + sAccessToken);
        connection.setRequestProperty("Cookie", sCookies);
    }

    public static User authenticate(Context ctx, String account) {
        HttpURLConnection urlConnection = null;
        OutputStream outStream = null;
        String response = null;
        int statusCode = 0;

        try {
            URL url = new URL(Endpoints.API_CONNECT);

            sAccessToken = GoogleAuthUtil.getToken(ctx, account, AuthUtil.SCOPE_STRING);
            
            Log.v(TAG, "Authenticating at [" + url + "] with: " + sAccessToken);
            
            byte[] postBody = String.format(ACCESS_TOKEN_JSON, sAccessToken).getBytes();

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("User-Agent", Endpoints.USER_AGENT);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            outStream = urlConnection.getOutputStream();
            outStream.write(postBody);

            statusCode = urlConnection.getResponseCode();
            
            if (statusCode == 200) {
                User result = null;
                String[] cookies = urlConnection.getHeaderField("set-cookie").split(";");
                for (String cookie : cookies) {
                    if (cookie.trim().startsWith("JSESSIONID")) {
                        InputStream responseStream = urlConnection.getInputStream();
                        byte[] responseBytes = HttpUtils.getContent(responseStream).toByteArray();
                        response = new String(responseBytes, "UTF-8");
                        
                        if (!TextUtils.isEmpty(response)) {
                            result = new Gson().fromJson(response, User.class);
                        }
                        
                        sCookies = cookie;
                        break;
                    }
                }
                
                Log.v(TAG, "Authenticated: " + response);

                return result;
            } else { 
                response = HttpUtils.getErrorResponse(urlConnection);
                
                Log.w(TAG, "HTTP Status (" + statusCode + ") while authenticating: " + response);
                GoogleAuthUtil.invalidateToken(ctx, sAccessToken);
                return null;
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (GoogleAuthException e) {
            GoogleAuthUtil.invalidateToken(ctx, sAccessToken);
        } catch (JsonParseException jsonException) {
            Log.e(TAG, "Unable to parse the json response from: " + Endpoints.API_CONNECT,
                    jsonException);
            Log.e(TAG, "Response was: " + response);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        return null;
    }

    public static void invalidateSession() {
        sAccessToken = null;
        sCookies = null;
    }
}
