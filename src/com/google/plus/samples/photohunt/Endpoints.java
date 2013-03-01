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

import java.util.Properties;

import android.util.Log;

/**
 * The PhotoHunt API endpoints.
 */
public class Endpoints {

    private static final String TAG = Endpoints.class.getSimpleName();
    
    /** The UserAgent string supplied with all PhotoHunt HTTP requests. */
    public static final String USER_AGENT = "PhotoHunt Agent";

    /** The protocol and hostname used to access the PhotoHunt service. */
    public static final String API_HOST;

    /** The URL root used to access the PhotoHunt API. */
    public static final String API_ROOT;
    
    /** The link that is embedded when sharing a photo on Google+. */
    public static final String API_PHOTO_LINK;
    
    /** The API URL used to retrieve metadata about a single photo. */
    public static final String PHOTO;
    
    /** The API URL used to retrieve an upload URL for a photo. */
    public static final String PHOTO_UPLOAD;
    
    /** The API URL used to vote for a photo. */
    public static final String PHOTO_VOTE;

    /** The API URL used to retrieve the list of themes. */
    public static final String THEME_LIST;

    /** The API URL used to retrieve the photos for a theme. */
    public static final String THEME_PHOTO_LIST;
    
    /** The API URL used to retrieve the photos my friends have submitted to a theme. */
    public static final String FRIENDS_PHOTO_LIST;

    /** The API URL used to retrieve the photos a user has uploaded for a theme. */
    public static final String USER_THEME_PHOTO_LIST;

    /** The API URL used to retrieve the photos a user has uploaded to PhotoHunt. */
    public static final String USER_PHOTO_LIST;
 
    /** The API URL used to connect to the PhotoHunt service. */
    public static final String API_CONNECT;

    /** The API URL used to disconnect from the PhotoHunt service. */
    public static final String API_DISCONNECT;

    public static final String ME_ID = "me";
    
    static {
        Properties config = new Properties();
        String apiHost = null;
        
        try {
            config.load(Endpoints.class.getClassLoader().getResourceAsStream("config.properties"));
 
            apiHost = config.getProperty("api_host");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load configuration properties file", e);
        } finally {
            API_HOST = apiHost;
            
            if (API_HOST != null) {
                API_ROOT = API_HOST + "/api";
                
                API_PHOTO_LINK = API_ROOT + "/image?id=%s";
                
                API_CONNECT = API_ROOT + "/connect";
                API_DISCONNECT = API_ROOT + "/disconnect";
                THEME_LIST = API_ROOT + "/themes?startIndex=%s&count=%s";
                PHOTO_UPLOAD = API_ROOT + "/images";
                PHOTO = API_ROOT + "/photos?photoId=%s";
                THEME_PHOTO_LIST = API_ROOT + "/photos?themeId=%s";
                USER_PHOTO_LIST = API_ROOT + "/photos?userId=%s";
                USER_THEME_PHOTO_LIST = API_ROOT + "/photos?userId=%s&themeId=%s";
                FRIENDS_PHOTO_LIST = API_ROOT + "/photos?userId=%s&themeId=%s&friends=true";
                PHOTO_VOTE = API_ROOT + "/votes";
            } else {
                API_ROOT = null;
                
                API_PHOTO_LINK = null;
                
                API_CONNECT = null;
                API_DISCONNECT = null;
                THEME_LIST = null;
                PHOTO_UPLOAD = null;
                PHOTO = null;
                THEME_PHOTO_LIST = null;
                USER_PHOTO_LIST = null;
                USER_THEME_PHOTO_LIST = null;
                FRIENDS_PHOTO_LIST = null;
                PHOTO_VOTE = null;
            }
        }
    }
}
