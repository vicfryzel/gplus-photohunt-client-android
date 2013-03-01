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

package com.google.photohunt.model;

import static android.text.TextUtils.isEmpty;

import com.google.photohunt.Endpoints;

/**
 * Represents a photo posted to a theme.
 */
public class Photo {


    /**
     * Primary identifier of this Photo.
     */
    public Long id;

    /**
     * ID of the User who owns this Photo.
     */
    public Long ownerUserId;

    /**
     * Display name of the User who owns this Photo.
     */
    public String ownerDisplayName;

    /**
     * Profile URL of the User who owns this Photo.
     */
    public String ownerProfileUrl;

    /**
     * Profile photo of the User who owns this Photo.
     */
    public String ownerProfilePhoto;

    /**
     * ID of the Theme to which this Photo belongs.
     */
    public Long themeId;

    /**
     * Display name of the Theme to which this Photo belongs.
     */
    public String themeDisplayName;

    /**
     * Number of votes this Photo has received.
     */
    public int numVotes;

    /**
     * True if the current user has already voted this Photo.
     */
    public boolean voted;

    /**
     * Date this Photo was uploaded to PhotoHunt.
     */
    public String created;

    /**
     * URL for full-size image of this Photo.
     */
    public String fullsizeUrl;

    /**
     * URL for thumbnail image of this Photo.
     */
    public String thumbnailUrl;

    /**
     * URL for vote call to action on this photo.
     */
    public String voteCtaUrl;

    /**
     * URL for interactive posts and deep linking to this photo.
     */
    public String photoContentUrl;
    
    public boolean hasAuthor(User profile) {
        return ownerUserId == profile.id;
    }
    
    public boolean hasTheme(Theme theme) {
        return themeId != theme.id;
    }

    public String getUri() {
        return String.format(Endpoints.PHOTO, id);
    }

}
