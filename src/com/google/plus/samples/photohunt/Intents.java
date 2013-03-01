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

import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusShare;
import com.google.plus.samples.photohunt.model.Photo;
import com.google.plus.samples.photohunt.model.Theme;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.Locale;

/**
 * Helpers to generate Intents used in the PhotoHunt app.
 */
public class Intents {

    /** 
     * {@link android.content.Intent} extra used to pass the Google+ call to action type when 
     * creating an interactive post.
     */
    public static final String ACTION_EXTRA = "action";
    
    /** {@link android.content.Intent} extra used to pass a PhotoHunt Theme id. */
    public static final String THEME_ID_EXTRA = "themeId";

    /** {@link android.content.Intent} extra used to pass a PhotoHunt Photo id. */
    public static final String PHOTO_ID_EXTRA = "photoId";

    private static final String TMP_PHOTO_FILENAME = "photohunt.jpg";

    /**
     * Return an Intent to invoke an Google+ share dialog to create an interactive post promoting 
     * a users photo on PhotoHunt.
     * 
     * @param activity the {@link android.content.Context} for the {@link android.content.Intent}.
     * @param meta the {@link com.google.plus.samples.photohunt.model.Photo} to share in the interactive post.
     * @param plusClient the {@link com.google.android.gms.plus.PlusClient} to use to create the 
     *                  interactive post.
     * @return the interactive post Intent.
     */
    public static Intent getInteractiveIntent(Activity activity, Photo meta, PlusClient plusClient) {
        return getInteractiveIntent(activity, meta, plusClient, null, false);
    }

    /**
     * Return an Intent to invoke an Google+ share dialog to create an interactive post promoting 
     * a users photo on PhotoHunt.
     * 
     * @param activity the {@link android.content.Context} for the {@link android.content.Intent}.
     * @param metadata the {@link com.google.plus.samples.photohunt.model.Photo} to share in the interactive post.
     * @param plusClient the {@link com.google.android.gms.plus.PlusClient} to use to create the 
     *                  interactive post.
     * @param activeTheme the currently active PhotoHunt {@link com.google.plus.samples.photohunt.model.Theme}.
     * @param vote whether to promote the photo with a Vote call to action.
     * @return the interactive post Intent.
     */
    public static Intent getInteractiveIntent(Activity activity, Photo metadata, PlusClient plusClient,
            Theme activeTheme, boolean vote) {
        Uri photoContentUri = Uri.parse(metadata.photoContentUrl);
        
        // Include the theme name in the share text.
        String shareText = activity.getString(R.string.vote_share_text);
        if (activeTheme != null) {
            shareText = String.format(activity.getString(R.string.vote_share_theme_text),
                    generateThemeTag(activeTheme.displayName));
        }

        // Create an interactive post builder with the call to action metadata.
        PlusShare.Builder builder = new PlusShare.Builder(activity, plusClient);

        // Create the deep link URL
        String deepLink = "/?id=" + metadata.id;
        
        if (vote) {
            // Add the call-to-action metadata.
            builder.addCallToAction("VOTE", photoContentUri, metadata.voteCtaUrl);
            
            // Set the target deep-link ID (for mobile use).
            builder.setContentDeepLinkId(deepLink + "&action=VOTE", null, null, null);
        } else {
            // Set the share content type to text/plain explicitly if we are not
            // creating an interactive post.
            builder.setType("text/plain");
            
            // Set the target deep-link ID (for mobile use).
			builder.setContentDeepLinkId(deepLink, null, null, null);
        }

        // Set the target url (for desktop use).
        builder.setContentUrl(photoContentUri);

        // Set the pre-filled message.
        builder.setText(shareText);

        return builder.getIntent();
    }

    /**
     * Create a hash tag from a title.
     * 
     * Strips white space, forces lower case and prepends the # character.
     * 
     * @param title from which to create the hash tag.
     * @return the hash tag string.
     */
    private static String generateThemeTag(String title) {
        return "#" + title.replaceAll("[\\s,]", "").toLowerCase(Locale.getDefault());
    }

    /**
     * Create an {@link android.content.Intent} to capture an image from the device camera.
     * 
     * @return the camera intent.
     */
    public static Intent getCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoImageUri());
        return intent;
    }

    /**
     * Get the URI of the temporary file used to capture a PhotoHunt image.
     * 
     * @return the URI of the temporary file.
     */
    public static Uri getPhotoImageUri() {
        File f = new File(Environment.getExternalStorageDirectory(), TMP_PHOTO_FILENAME);
        return Uri.fromFile(f);
    }
    
    /**
     * Create an {@link android.content.Intent} to display a Google+ user's profile in the Google+
     * app.
     * 
     * @param profileUrl Google+ profile URL of the user.
     * @return the {@link android.content.Intent} to invoke.
     */
    public static Intent getPlusUserIntent(String profileUrl) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl));
    }

}
