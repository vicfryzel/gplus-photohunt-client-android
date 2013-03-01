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

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.google.plus.samples.photohunt.model.Theme;

/**
 * Dialog allowing the selection of a {@link Theme} for display in {@link ThemeViewActivity}.
 */
public class ThemeSelectDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ThemeViewActivity activity = (ThemeViewActivity) getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
		// Fetch the list of themes.
		final List<Theme> themes = activity.getThemes();
		final String[] items = new String[themes.size()];
		
		// Extract the titles for display in the list.
		for (int i = 0; i < themes.size(); i++) {
			Theme theme = themes.get(i);
			items[i] = theme.displayName;
		}
		
		// Create the dialog.
        builder.setTitle(R.string.dialog_select_theme)
        	.setItems(items, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int which) {
        			Theme selected = themes.get(which);
        			Theme current = activity.getSelectedTheme();
        			
        			// Update the selected theme if it is different from the current theme
        			if (current == null && selected == null
        				|| current.id == selected.id) {
        				activity.setSelectedTheme(themes.get(which));
        				activity.update();
        			}
        		}
        	});

        return builder.create();
    }
}
