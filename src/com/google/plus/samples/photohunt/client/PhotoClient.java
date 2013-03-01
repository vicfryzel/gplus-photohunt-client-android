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

package com.google.plus.samples.photohunt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.google.plus.samples.photohunt.Endpoints;
import com.google.plus.samples.photohunt.model.Photo;
import com.google.plus.samples.photohunt.model.Theme;
import com.google.plus.samples.photohunt.tasks.FetchJsonTask;
import com.google.plus.samples.photohunt.tasks.FetchJsonTask.FetchCallback;

/**
 * API interface for interacting with the PhotoHunt backend.
 */
public class PhotoClient {

    private static final String VOTE_JSON = "{ \"photoId\":\"%d\"}";
    
    /**
     * Fetch the list of PhotoHunt {@Theme}s.
     * 
     * @param startIndex The starting index of the list of themes.
     * @param count The maximum number of themes returned.
     * @param callback The callback used to deliver the results.
     * @return The {@code AsyncTask} executed to perform the fetch.
     */
    public void getThemes(int startIndex, int count, FetchCallback<List<Theme>> callback) {
        String url = String.format(Endpoints.THEME_LIST, startIndex, count);
        FetchJsonTask<List<Theme>> task = new FetchJsonTask<List<Theme>>(url, callback){
            { mReturnType = new TypeToken<ArrayList<Theme>>() {}.getType(); }
        };
        
        task.execute();
    }
    
    /**
     * Fetch the active {@link Theme}.
     * 
     * @param callback The callback used to deliver the results.
     */
    public void getActiveTheme(final FetchCallback<Theme> callback) {
    	getThemes(0, 1, new FetchCallback<List<Theme>>() {
            public void onSuccess(List<Theme> result) {
                if (result != null
                	&& result.size() > 0) {
                	callback.onSuccess(result.get(0));
                } else {
                	callback.onSuccess(null);
                }
            }

            public void onError(List<Theme> result) {
                if (result != null
                    && result.size() > 0) {
                    callback.onSuccess(result.get(0));
                } else {
                    callback.onSuccess(null);
                }
            }
    	});
    }
    
    /**
     * Method used to perform a vote on a {@link Photo}.
     * 
     * @param photoId The id of the photo to vote for.
     * @param callback The callback used to deliver the result.
     */
    public void vote(Long photoId, final FetchCallback<Photo> callback) {
        final String url = Endpoints.PHOTO_VOTE;
        final byte[] voteJson = String.format(VOTE_JSON, photoId).getBytes();
        
        FetchJsonTask<Photo> task = new FetchJsonTask<Photo>(url, callback, Photo.class) {
            {  mRequestMethod = "PUT";  mRequestBody = voteJson; }
        };

        task.execute();
    }
    
    /**
     * Method used to delete a {@link Photo}.
     * 
     * @param photoId The id of the photo to delete.
     * @param callback The callback used to deliver the result.
     */
    public void delete(Long photoId, final FetchCallback<Void> callback) {
        String url = String.format(Endpoints.PHOTO, photoId);
        FetchJsonTask<Void> task = new FetchJsonTask<Void>(url, callback, Void.class) {
            { mRequestMethod = "DELETE"; }
        };

        task.execute();
    }
    
    /**
     * Method used to disconnect the current users account from Google.  We make a call to the
     * PhotoHunt service rather than calling PlusClient.revokeAndDisconnect() locally to ensure
     * that the client and server stay in sync.
     * 
     * @param callback The callback used to deliver the result.
     */
    public void disconnectAccount(final FetchCallback<Void> callback) {
        FetchJsonTask<Void> task = new FetchJsonTask<Void>(Endpoints.API_DISCONNECT, callback, Void.class) {
            { mRequestMethod = "POST"; }
        };

        task.execute();
    }
}
