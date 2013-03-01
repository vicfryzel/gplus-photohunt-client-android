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

package com.google.plus.samples.photohunt.deeplink;

import java.util.Locale;

import com.google.android.gms.plus.PlusShare;
import com.google.plus.samples.photohunt.Intents;
import com.google.plus.samples.photohunt.ViewImageActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


/**
 * Parse incoming deep-links from Google+
 */
public class DeepLinkActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri deepLinkId = Uri.parse(PlusShare.getDeepLinkId(getIntent()));
        if ("/".equals(deepLinkId.getPath())) {
            String photoId = deepLinkId.getQueryParameter("id");
            String action = deepLinkId.getQueryParameter("action");
            
            Intent viewImageIntent = new Intent();
            viewImageIntent.setClass(this, ViewImageActivity.class);
            viewImageIntent.putExtra(Intents.PHOTO_ID_EXTRA, photoId);
            
            if (action != null && action.toLowerCase(Locale.getDefault()).equals("vote")) {
            	viewImageIntent.putExtra(Intents.ACTION_EXTRA, "vote");
            }
            
            startActivity(viewImageIntent);
        }
        finish();
    }

}