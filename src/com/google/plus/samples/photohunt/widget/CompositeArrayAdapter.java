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
import android.widget.BaseAdapter;

/**
 * A general purpose adapter that is composed of multiple arrays.
 */
public abstract class CompositeArrayAdapter<T> extends BaseAdapter {

    private static final int INITIAL_CAPACITY = 2;

    public static class Partition {
        boolean showIfEmpty;

        String header;
        List<?> list;
        boolean dirty;

        public Partition(boolean showIfEmpty) {
            this.showIfEmpty = showIfEmpty;
        }

        public boolean getShowIfEmpty() {
            return showIfEmpty;
        }

        public boolean hasHeader() {
            return header != null;
        }

		public String getHeader() {
			return header;
		}
		
    }

    private final Context mContext;
    private Partition[] mPartitions;
    private boolean[] mHeaderVisibility;
    private int mSize = 0;

    public CompositeArrayAdapter(Context context) {
        this(context, INITIAL_CAPACITY);
    }

    public CompositeArrayAdapter(Context context, int initialCapacity) {
        mContext = context;
        mPartitions = new Partition[initialCapacity];
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Registers a partition. The list for that partition can be set later.
     * Partitions should be added in the order they are supposed to appear in the
     * list.
     */
    public void addPartition(boolean showIfEmpty) {
        addPartition(new Partition(showIfEmpty));
    }

    public void addPartition(Partition partition) {
        if (mSize >= mPartitions.length) {
            int newCapacity = mSize + 2;
            Partition[] newAdapters = new Partition[newCapacity];
            System.arraycopy(mPartitions, 0, newAdapters, 0, mSize);
            mPartitions = newAdapters;
        }
        mPartitions[mSize++] = partition;
    }

    public void removePartition(int partitionIndex) {
        System.arraycopy(
        		mPartitions, 
        		partitionIndex + 1, 
        		mPartitions, 
        		partitionIndex,
                mSize - partitionIndex - 1);
        mSize--;
    }

    public void clearPartitions() {
    	mSize = 0;
    }
	
    public void markAllDirty() {
    	for (int i = 0; i < mSize; i++) {
			setDirty(i, true);
    	}
    }
    
	public void setDirty(int partitionIndex, boolean dirty) {
		mPartitions[partitionIndex].dirty = dirty;
	}
	
	public boolean isDirty(int partitionIndex) {
		return mPartitions[partitionIndex].dirty;
	}
    
    public void setHeader(int partitionIndex, String header) {
    	mPartitions[partitionIndex].header = header;
    }

    public void setShowIfEmpty(int partitionIndex, boolean flag) {
        mPartitions[partitionIndex].showIfEmpty = flag;
    }

    public Partition getPartition(int partitionIndex) {
        if (partitionIndex >= mSize) {
            throw new ArrayIndexOutOfBoundsException(partitionIndex);
        }
        return mPartitions[partitionIndex];
    }

    public int getPartitionCount() {
        return mSize;
    }
    
    public int getPartitionSize(int partitionIndex) {
		Partition partition = getPartition(partitionIndex);
    	int result = partition.list.size();
    	
		if (partition.hasHeader()) {
    		result++;
    	}
		
		return result;
    }
    
    public int getPartitionVisibleSize(int partitionIndex) {
		Partition partition = getPartition(partitionIndex);
		int result = 0;
		
		if (partition.list != null) {
			result = partition.list.size();
		}
		
		if (partition.hasHeader()
			&& (result > 0 || partition.showIfEmpty)) {
			result++;
		}
		
		return result;
    }

    /**
     * Returns true if the specified partition was configured to have a header.
     */
    public boolean hasHeader(int partition) {
        return mPartitions[partition].hasHeader();
    }

    /**
     * Returns the total number of list items in all partitions.
     */
    public int getCount() {
    	int result = 0;
    	
    	for (int i = 0; i < mSize; i++) {
			result = result + getPartitionVisibleSize(i);
    	}
    	
        return result;
    }

    /**
     * Returns the list for the given partition
     */
    public List<T> getList(int partition) {
        return (List<T>) mPartitions[partition].list;
    }

    /**
     * Changes the list for an individual partition.
     */
    public void changeList(int partition, List<T> list) {
        List<?> prevList = mPartitions[partition].list;
        if (prevList != list) {
            mPartitions[partition].list = list;
        }
    }

    /**
     * Returns true if the specified partition has no cursor or an empty cursor.
     */
    public boolean isPartitionEmpty(int partition) {
        List<?> list = mPartitions[partition].list;
        return list == null || list.size() == 0;
    }

    /**
     * Given a list position, returns the index of the corresponding partition.
     */
    public int getPartitionForPosition(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                return i;
            }
            start = end;
        }
        return -1;
    }

    /**
     * Given a list position, return the offset of the corresponding item in its
     * partition.  The header, if any, will have offset -1.
     */
    public int getOffsetInPartition(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader()) {
                    offset--;
                }
                return offset;
            }
            start = end;
        }
        return -1;
    }

    /**
     * Returns the first list position for the specified partition.
     */
    public int getPositionForPartition(int partition) {
        int position = 0;
        for (int i = 0; i < partition; i++) {
            position += getPartitionVisibleSize(i);
        }
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return getItemViewTypeCount() + 1;
    }

    /**
     * Returns the overall number of item view types across all partitions. An
     * implementation of this method needs to ensure that the returned count is
     * consistent with the values returned by {@link #getItemViewType(int,int)}.
     */
    public int getItemViewTypeCount() {
        return 1;
    }

    /**
     * Returns the view type for the list item at the specified position in the
     * specified partition.
     */
    protected int getItemViewType(int partition, int position) {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start  + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader() && offset == 0) {
                    return IGNORE_ITEM_VIEW_TYPE;
                }
                return getItemViewType(i, position);
            }
            start = end;
        }

        throw new ArrayIndexOutOfBoundsException(position);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader()) {
                    offset--;
                }
                View view;
                if (offset == -1) {
                    view = getHeaderView(i, (List<T>) mPartitions[i].list, convertView, parent);
                } else {
                    view = getView(i, (List<T>) mPartitions[i].list, position, convertView, parent);
                }
                if (view == null) {
                    throw new NullPointerException("View should not be null, partition: " + i
                            + " position: " + offset);
                }
                return view;
            }
            start = end;
        }

        throw new ArrayIndexOutOfBoundsException(position);
    }

    /**
     * Returns the header view for the specified partition, creating one if needed.
     */
    protected View getHeaderView(
    		int partition,
    		List<T> list,
    		View convertView,
            ViewGroup parent) {
    	return null;
    }

    /**
     * Returns an item view for the specified partition, creating one if needed.
     */
    protected abstract View getView(
    		int partition,
    		List<T> list,
    		int position,
    		View convertView,
            ViewGroup parent);

    public T getItem(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader()) {
                    offset--;
                }
                if (offset == -1) {
                    return null;
                }
                List<?> list = mPartitions[i].list;
                return (T) list.get(offset);
            }
            start = end;
        }

        return null;
    }

    /**
     * Returns the item ID for the specified list position.
     */
    public long getItemId(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader()) {
                    offset--;
                }
                if (offset == -1) {
                    return 0;
                }
                
                return getItemId(position, (List<T>) mPartitions[i].list, offset);
            }
            start = end;
        }

        return 0;
    }
    
    protected long getItemId(int position, List<T> list, int offset) {
    	return 0L;
    }

    /**
     * Returns false if any partition has a header.
     */
    @Override
    public boolean areAllItemsEnabled() {
        for (int i = 0; i < mSize; i++) {
            if (mPartitions[i].hasHeader()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true for all items except headers.
     */
    @Override
    public boolean isEnabled(int position) {
        int start = 0;
        for (int i = 0; i < mSize; i++) {
            int end = start + getPartitionVisibleSize(i);
            if (position >= start && position < end) {
                int offset = position - start;
                if (mPartitions[i].hasHeader() && offset == 0) {
                    return false;
                } else {
                    return isEnabled(i, offset);
                }
            }
            start = end;
        }

        return false;
    }

    /**
     * Returns true if the item at the specified offset of the specified
     * partition is selectable and clickable.
     */
    protected boolean isEnabled(int partition, int position) {
        return true;
    }

}
