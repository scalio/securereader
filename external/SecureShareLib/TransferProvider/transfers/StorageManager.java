/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.transfers;

import static com.android.providers.downloads.Constants.LOGV;
import static com.android.providers.downloads.Constants.TAG;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import libcore.io.ErrnoException;
import libcore.io.Libcore;
import libcore.io.StructStat;

/**
 * Manages the storage space consumed by Transfers Data dir. When space falls below
 * a threshold limit (set in resource xml files), starts cleanup of the Transfers data dir
 * to free up space.
 */
class StorageManager {
    /** the max amount of space allowed to be taken up by the transfers data dir */
    private static final long sMaxtransferDataDirSize =
            Resources.getSystem().getInteger(R.integer.config_downloadDataDirSize) * 1024 * 1024;

    /** threshold (in bytes) beyond which the low space warning kicks in and attempt is made to
     * purge some transfered files to make space
     */
    private static final long sTransferDataDirLowSpaceThreshold =
            Resources.getSystem().getInteger(
                    R.integer.config_downloadDataDirLowSpaceThreshold)
                    * sMaxtransferDataDirSize / 100;

    /** see {@link Environment#getExternalStorageDirectory()} */
    private final File mExternalStorageDir;

    /** see {@link Environment#getTransferCacheDirectory()} */
    private final File mSystemCacheDir;

    /** The transfered files are saved to this dir. it is the value returned by
     * {@link Context#getCacheDir()}.
     */
    private final File mTransferDataDir;

    /** how often do we need to perform checks on space to make sure space is available */
    private static final int FREQUENCY_OF_CHECKS_ON_SPACE_AVAILABILITY = 1024 * 1024; // 1MB
    private int mBytesTransferedSinceLastCheckOnSpace = 0;

    /** misc members */
    private final Context mContext;

    public StorageManager(Context context) {
        mContext = context;
        mTransferDataDir = getTransferDataDirectory(context);
        mExternalStorageDir = Environment.getExternalStorageDirectory();
        mSystemCacheDir = Environment.getDownloadCacheDirectory();
        startThreadToCleanupDatabaseAndPurgeFileSystem();
    }

    /** How often should database and filesystem be cleaned up to remove spurious files
     * from the file system and
     * The value is specified in terms of num of transfers since last time the cleanup was done.
     */
    private static final int FREQUENCY_OF_DATABASE_N_FILESYSTEM_CLEANUP = 250;
    private int mNumTransfersSoFar = 0;

    synchronized void incrementNumTransfersSoFar() {
        if (++mNumTransfersSoFar % FREQUENCY_OF_DATABASE_N_FILESYSTEM_CLEANUP == 0) {
            startThreadToCleanupDatabaseAndPurgeFileSystem();
        }
    }
    /* start a thread to cleanup the following
     *      remove spurious files from the file system
     *      remove excess entries from the database
     */
    private Thread mCleanupThread = null;
    private synchronized void startThreadToCleanupDatabaseAndPurgeFileSystem() {
        if (mCleanupThread != null && mCleanupThread.isAlive()) {
            return;
        }
        mCleanupThread = new Thread() {
            @Override public void run() {
                removeSpuriousFiles();
                trimDatabase();
            }
        };
        mCleanupThread.start();
    }

    void verifySpaceBeforeWritingToFile(int destination, String path, long length)
            throws StopRequestException {
        // do this check only once for every 1MB of transfered data
        if (incrementBytesTransferedSinceLastCheckOnSpace(length) <
                FREQUENCY_OF_CHECKS_ON_SPACE_AVAILABILITY) {
            return;
        }
        verifySpace(destination, path, length);
    }

    void verifySpace(int destination, String path, long length) throws StopRequestException {
        resetBytesTransferedSinceLastCheckOnSpace();
        File dir = null;
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "in verifySpace, destination: " + destination +
                    ", path: " + path + ", length: " + length);
        }
        if (path == null) {
            throw new IllegalArgumentException("path can't be null");
        }
        switch (destination) {
            case Downloads.Impl.DESTINATION_CACHE_PARTITION:
            case Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING:
            case Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE:
                dir = mTransferDataDir;
                break;
            case Downloads.Impl.DESTINATION_EXTERNAL:
                dir = mExternalStorageDir;
                break;
            case Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION:
                dir = mSystemCacheDir;
                break;
            case Downloads.Impl.DESTINATION_FILE_URI:
                if (path.startsWith(mExternalStorageDir.getPath())) {
                    dir = mExternalStorageDir;
                } else if (path.startsWith(mTransferDataDir.getPath())) {
                    dir = mTransferDataDir;
                } else if (path.startsWith(mSystemCacheDir.getPath())) {
                    dir = mSystemCacheDir;
                }
                break;
         }
        if (dir == null) {
            throw new IllegalStateException("invalid combination of destination: " + destination +
                    ", path: " + path);
        }
        findSpace(dir, length, destination);
    }

    /**
     * finds space in the given filesystem (input param: root) to accommodate # of bytes
     * specified by the input param(targetBytes).
     * returns true if found. false otherwise.
     */
    private synchronized void findSpace(File root, long targetBytes, int destination)
            throws StopRequestException {
        if (targetBytes == 0) {
            return;
        }
        if (destination == Downloads.Impl.DESTINATION_FILE_URI ||
                destination == Downloads.Impl.DESTINATION_EXTERNAL) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                throw new StopRequestException(Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR,
                        "external media not mounted");
            }
        }
        // is there enough space in the file system of the given param 'root'.
        long bytesAvailable = getAvailableBytesInFileSystemAtGivenRoot(root);
        if (bytesAvailable < sTransferDataDirLowSpaceThreshold) {
            /* filesystem's available space is below threshold for low space warning.
             * threshold typically is 10% of transfer data dir space quota.
             * try to cleanup and see if the low space situation goes away.
             */
            discardPurgeableFiles(destination, sTransferDataDirLowSpaceThreshold);
            removeSpuriousFiles();
            bytesAvailable = getAvailableBytesInFileSystemAtGivenRoot(root);
            if (bytesAvailable < sTransferDataDirLowSpaceThreshold) {
                /*
                 * available space is still below the threshold limit.
                 *
                 * If this is system cache dir, print a warning.
                 * otherwise, don't allow transfering until more space
                 * is available because transfermanager shouldn't end up taking those last
                 * few MB of space left on the filesystem.
                 */
                if (root.equals(mSystemCacheDir)) {
                    Log.w(Constants.TAG, "System cache dir ('/cache') is running low on space." +
                            "space available (in bytes): " + bytesAvailable);
                } else {
                    throw new StopRequestException(Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR,
                            "space in the filesystem rooted at: " + root +
                            " is below 10% availability. stopping this transfer.");
                }
            }
        }
        if (root.equals(mTransferDataDir)) {
            // this transfer is going into transfers data dir. check space in that specific dir.
            bytesAvailable = getAvailableBytesInTransfersDataDir(mTransferDataDir);
            if (bytesAvailable < sTransferDataDirLowSpaceThreshold) {
                // print a warning
                Log.w(Constants.TAG, "Transfers data dir: " + root +
                        " is running low on space. space available (in bytes): " + bytesAvailable);
            }
            if (bytesAvailable < targetBytes) {
                // Insufficient space; make space.
                discardPurgeableFiles(destination, sTransferDataDirLowSpaceThreshold);
                removeSpuriousFiles();
                bytesAvailable = getAvailableBytesInTransfersDataDir(mTransferDataDir);
            }
        }
        if (bytesAvailable < targetBytes) {
            throw new StopRequestException(Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR,
                    "not enough free space in the filesystem rooted at: " + root +
                    " and unable to free any more");
        }
    }

    /**
     * returns the number of bytes available in the transfers data dir
     * TODO this implementation is too slow. optimize it.
     */
    private long getAvailableBytesInTransfersDataDir(File root) {
        File[] files = root.listFiles();
        long space = sMaxtransferDataDirSize;
        if (files == null) {
            return space;
        }
        int size = files.length;
        for (int i = 0; i < size; i++) {
            space -= files[i].length();
        }
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "available space (in bytes) in transfers data dir: " + space);
        }
        return space;
    }

    private long getAvailableBytesInFileSystemAtGivenRoot(File root) {
        StatFs stat = new StatFs(root.getPath());
        // put a bit of margin (in case creating the file grows the system by a few blocks)
        long availableBlocks = (long) stat.getAvailableBlocks() - 4;
        long size = stat.getBlockSize() * availableBlocks;
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "available space (in bytes) in filesystem rooted at: " +
                    root.getPath() + " is: " + size);
        }
        return size;
    }

    File locateDestinationDirectory(String mimeType, int destination, long contentLength)
            throws StopRequestException {
        switch (destination) {
            case Downloads.Impl.DESTINATION_CACHE_PARTITION:
            case Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE:
            case Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING:
                return mTransferDataDir;
            case Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION:
                return mSystemCacheDir;
            case Downloads.Impl.DESTINATION_EXTERNAL:
                File base = new File(mExternalStorageDir.getPath() + Constants.DEFAULT_DL_SUBDIR);
                if (!base.isDirectory() && !base.mkdir()) {
                    // Can't create transfer directory, e.g. because a file called "transfer"
                    // already exists at the root level, or the SD card filesystem is read-only.
                    throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                            "unable to create external transfers directory " + base.getPath());
                }
                return base;
            default:
                throw new IllegalStateException("unexpected value for destination: " + destination);
        }
    }

    File getTransferDataDirectory() {
        return mTransferDataDir;
    }

    public static File getTransferDataDirectory(Context context) {
        return context.getCacheDir();
    }

    /**
     * Deletes purgeable files from the cache partition. This also deletes
     * the matching database entries. Files are deleted in LRU order until
     * the total byte size is greater than targetBytes
     */
    private long discardPurgeableFiles(int destination, long targetBytes) {
        if (true || Constants.LOGV) {
            Log.i(Constants.TAG, "discardPurgeableFiles: destination = " + destination +
                    ", targetBytes = " + targetBytes);
        }
        String destStr  = (destination == Downloads.Impl.DESTINATION_SYSTEMCACHE_PARTITION) ?
                String.valueOf(destination) :
                String.valueOf(Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE);
        String[] bindArgs = new String[]{destStr};
        Cursor cursor = mContext.getContentResolver().query(
        		Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                null,
                "( " +
        		Downloads.Impl.COLUMN_STATUS + " = '" + Downloads.Impl.STATUS_SUCCESS + "' AND " +
        		Downloads.Impl.COLUMN_DESTINATION + " = ? )Downloads",
                bindArgs,
                Downloads.Impl.COLUMN_LAST_MODIFICATION);
        if (cursor == null) {
            return 0;
        }
        long totalFreed = 0;
        try {
            final int dataIndex = cursor.getColumnIndex(Downloads.Impl._DATA);
            while (cursor.moveToNext() && totalFreed < targetBytes) {
                final String data = cursor.getString(dataIndex);
                if (TextUtils.isEmpty(data)) continue;

                File file = new File(data);
                if (Constants.LOGV) {
                    Log.d(Constants.TAG, "purging " + file.getAbsolutePath() + " for "
                            + file.length() + " bytes");
                }
                totalFreed += file.length();
                file.delete();
                long id = cursor.getLong(cursor.getColumnIndex(Downloads.Impl._ID));
                mContext.getContentResolver().delete(
                        ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id),
                        null, null);
            }
        } finally {
            cursor.close();
        }
        if (true || Constants.LOGV) {
            Log.i(Constants.TAG, "Purged files, freed " + totalFreed + " for " +
                    targetBytes + " requested");
        }
        return totalFreed;
    }

    /**
     * Removes files in the systemcache and transfers data dir without corresponding entries in
     * the transfers database.
     * This can occur if a delete is done on the database but the file is not removed from the
     * filesystem (due to sudden death of the process, for example).
     * This is not a very common occurrence. So, do this only once in a while.
     */
    private void removeSpuriousFiles() {
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "in removeSpuriousFiles");
        }
        // get a list of all files in system cache dir and transfers data dir
        List<File> files = new ArrayList<File>();
        File[] listOfFiles = mSystemCacheDir.listFiles();
        if (listOfFiles != null) {
            files.addAll(Arrays.asList(listOfFiles));
        }
        listOfFiles = mTransferDataDir.listFiles();
        if (listOfFiles != null) {
            files.addAll(Arrays.asList(listOfFiles));
        }
        if (files.size() == 0) {
            return;
        }
        Cursor cursor = mContext.getContentResolver().query(
        		Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                new String[] { Downloads.Impl._DATA }, null, null, null);
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filename = cursor.getString(0);
                    if (!TextUtils.isEmpty(filename)) {
                        if (LOGV) {
                            Log.i(Constants.TAG, "in removeSpuriousFiles, preserving file " +
                                    filename);
                        }
                        files.remove(new File(filename));
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // delete files owned by us, but that don't appear in our database
        final int myUid = android.os.Process.myUid();
        for (File file : files) {
            final String path = file.getAbsolutePath();
            try {
                final StructStat stat = Libcore.os.stat(path);
                if (stat.st_uid == myUid) {
                    if (Constants.LOGVV) {
                        Log.d(TAG, "deleting spurious file " + path);
                    }
                    file.delete();
                }
            } catch (ErrnoException e) {
                Log.w(TAG, "stat(" + path + ") result: " + e);
            }
        }
    }

    /**
     * Drops old rows from the database to prevent it from growing too large
     * TODO logic in this method needs to be optimized. maintain the number of transfers
     * in memory - so that this method can limit the amount of data read.
     */
    private void trimDatabase() {
        if (Constants.LOGV) {
            Log.i(Constants.TAG, "in trimDatabase");
        }
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                    new String[] { Downloads.Impl._ID },
                    Downloads.Impl.COLUMN_STATUS + " >= '200'", null,
                    Downloads.Impl.COLUMN_LAST_MODIFICATION);
            if (cursor == null) {
                // This isn't good - if we can't do basic queries in our database,
                // nothing's gonna work
                Log.e(Constants.TAG, "null cursor in trimDatabase");
                return;
            }
            if (cursor.moveToFirst()) {
                int numDelete = cursor.getCount() - Constants.MAX_DOWNLOADS;
                int columnId = cursor.getColumnIndexOrThrow(Downloads.Impl._ID);
                while (numDelete > 0) {
                    Uri transferUri = ContentUris.withAppendedId(
                    		Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, cursor.getLong(columnId));
                    mContext.getContentResolver().delete(transferUri, null, null);
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    numDelete--;
                }
            }
        } catch (SQLiteException e) {
            // trimming the database raised an exception. alright, ignore the exception
            // and return silently. trimming database is not exactly a critical operation
            // and there is no need to propagate the exception.
            Log.w(Constants.TAG, "trimDatabase failed with exception: " + e.getMessage());
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private synchronized int incrementBytesTransferedSinceLastCheckOnSpace(long val) {
        mBytesTransferedSinceLastCheckOnSpace += val;
        return mBytesTransferedSinceLastCheckOnSpace;
    }

    private synchronized void resetBytesTransferedSinceLastCheckOnSpace() {
        mBytesTransferedSinceLastCheckOnSpace = 0;
    }
}
