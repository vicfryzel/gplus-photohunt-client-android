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

package com.google.plus.samples.photohunt.widget;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public abstract class PinnedHeaderArrayAdapter<T> extends CompositeArrayAdapter<T> 
		implements PinnedHeaderListView.PinnedHeaderAdapter {

	public PinnedHeaderArrayAdapter(Context context) {
		super(context);
	}
	
    public PinnedHeaderArrayAdapter(Context context, int initialCapacity) {
        super(context, initialCapacity);
    }

	@Override
	public int getPinnedHeaderCount() {
        return getPartitionCount();
	}

	@Override
	public View getPinnedHeaderView(
			int viewIndex,
			View convertView,
			ViewGroup parent) {
		int headerCount = -1;
    	for (int i = 0; i < getPartitionCount(); i++) {
    		if (getPartition(i).hasHeader()) {
    			headerCount++;
    		}
    		
    		if (headerCount == viewIndex) {
    			return getHeaderView(i, (List<T>) getPartition(viewIndex).list, convertView, parent);
    		}
    	}
    	
    	throw new ArrayIndexOutOfBoundsException("No pinned header at index " + viewIndex);
	}

	@Override
	public void configurePinnedHeaders(PinnedHeaderListView listView) {
        int size = getPartitionCount();

        int topPosition = listView.getPositionAt(0);
        int partition = getPartitionForPosition(topPosition);
        
		if (partition >= 0) {
			for (int i = 0; i < size; i++) {
				if (isHeaderVisible(partition)) {
					listView.setHeaderInvisible(i, isPartitionEmpty(i));
				}
			}

			if (isHeaderVisible(partition)) {
				int headerHeight = listView.getPinnedHeaderHeight(partition);

				int visiblePosition = listView.getPositionAt(headerHeight);

				if (getPartitionForPosition(visiblePosition) == partition) {
					listView.setHeaderPinnedAtTop(partition, 0);
				} else {
					listView.setFadingHeader(partition, topPosition, true);
				}
			}
		}
	}

	private boolean isHeaderVisible(int partition) {
		return hasHeader(partition) 
            && (getPartition(partition).getShowIfEmpty() 
            	|| !isPartitionEmpty(partition));
	}

	@Override
	public int getScrollPositionForHeader(int viewIndex) {
		int headerCount = 0;
    	for (int i = 0; i < getPartitionCount(); i++) {
    		if (getPartition(i).hasHeader()) {
                if (headerCount == viewIndex) {
                    return getPositionForPartition(i);
                }
                
                headerCount++;
    		}
    	}
    	
    	throw new ArrayIndexOutOfBoundsException("No pinned header at index " + viewIndex);
	}
	
}
