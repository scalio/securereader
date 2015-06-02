/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * Contains the internal constants that are used in the transfer manager.
 * As a general rule, modifying these constants should be done with care.
 */
public class Constants {

    /** Tag used for debugging/logging */
    public static final String TAG = "TransferManager";

    /** The column that used to be used for the HTTP method of the request */
    public static final String RETRY_AFTER_X_REDIRECT_COUNT = "method";

    /** The column that used to be used for the magic OTA update filename */
    public static final String OTA_UPDATE = "otaupdate";

    /** The column that used to be used to reject system filetypes */
    public static final String NO_SYSTEM_FILES = "no_system";

    /** The column that is used for the transfers's ETag */
    public static final String ETAG = "etag";

    /** The column that is used for the initiating app's UID */
    public static final String UID = "uid";

    /** The column that is used to remember whether the media scanner was invoked */
    public static final String MEDIA_SCANNED = "scanned";

    /** The intent that gets sent when the service must wake up for a retry */
    public static final String ACTION_RETRY = "android.intent.action.DOWNLOAD_WAKEUP";

    /** the intent that gets sent when clicking a successful transfer */
    public static final String ACTION_OPEN = "android.intent.action.DOWNLOAD_OPEN";

    /** the intent that gets sent when clicking an incomplete/failed transfer  */
    public static final String ACTION_LIST = "android.intent.action.DOWNLOAD_LIST";

    /** the intent that gets sent when deleting the notification of a completed transfer */
    public static final String ACTION_HIDE = "android.intent.action.DOWNLOAD_HIDE";

    /** The default base name for transfered files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_FILENAME = "transferfile";

    /** The default extension for html files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_HTML_EXTENSION = ".html";

    /** The default extension for text files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_TEXT_EXTENSION = ".txt";

    /** The default extension for binary files if we can't get one at the HTTP level */
    public static final String DEFAULT_DL_BINARY_EXTENSION = ".bin";

    public static final String PROVIDER_PACKAGE_NAME = "com.android.providers.transfers";

    /**
     * When a number has to be appended to the filename, this string is used to separate the
     * base filename from the sequence number
     */
    public static final String FILENAME_SEQUENCE_SEPARATOR = "-";

    /** Where we store transfered files on the external storage */
    public static final String DEFAULT_DL_SUBDIR = "/" + Environment.DIRECTORY_DOWNLOADS;

    /** A magic filename that is allowed to exist within the system cache */
    public static final String RECOVERY_DIRECTORY = "recovery";

    /** The default user agent used for transfers */
    public static final String DEFAULT_USER_AGENT;

    static {
        final StringBuilder builder = new StringBuilder();

        final boolean validRelease = !TextUtils.isEmpty(Build.VERSION.RELEASE);
        final boolean validId = !TextUtils.isEmpty(Build.ID);
        final boolean includeModel = "REL".equals(Build.VERSION.CODENAME)
                && !TextUtils.isEmpty(Build.MODEL);

        builder.append("AndroidTransferManager");
        if (validRelease) {
            builder.append("/").append(Build.VERSION.RELEASE);
        }
        builder.append(" (Linux; U; Android");
        if (validRelease) {
            builder.append(" ").append(Build.VERSION.RELEASE);
        }
        if (includeModel || validId) {
            builder.append(";");
            if (includeModel) {
                builder.append(" ").append(Build.MODEL);
            }
            if (validId) {
                builder.append(" Build/").append(Build.ID);
            }
        }
        builder.append(")");

        DEFAULT_USER_AGENT = builder.toString();
    }

    /** The MIME type of APKs */
    public static final String MIMETYPE_APK = "application/vnd.android.package";

    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 4096;

    /** The minimum amount of progress that has to be done before the progress bar gets updated */
    public static final int MIN_PROGRESS_STEP = 4096;

    /** The minimum amount of time that has to elapse before the progress bar gets updated, in ms */
    public static final long MIN_PROGRESS_TIME = 1500;

    /** The maximum number of rows in the database (FIFO) */
    public static final int MAX_DOWNLOADS = 1000;

    /**
     * The number of times that the transfer manager will retry its network
     * operations when no progress is happening before it gives up.
     */
    public static final int MAX_RETRIES = 5;

    /**
     * The minimum amount of time that the transfer manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MIN_RETRY_AFTER = 30; // 30s

    /**
     * The maximum amount of time that the transfer manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MAX_RETRY_AFTER = 24 * 60 * 60; // 24h

    /**
     * The maximum number of redirects.
     */
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.

    /**
     * The time between a failure and the first retry after an IOException.
     * Each subsequent retry grows exponentially, doubling each time.
     * The time is in seconds.
     */
    public static final int RETRY_FIRST_DELAY = 30;

    /** Enable separate connectivity logging */
    static final boolean LOGX = false;

    /** Enable verbose logging - use with "setprop log.tag.TransferManager VERBOSE" */
    private static final boolean LOCAL_LOGV = false;
    public static final boolean LOGV = LOCAL_LOGV && Log.isLoggable(TAG, Log.VERBOSE);

    /** Enable super-verbose logging */
    private static final boolean LOCAL_LOGVV = false;
    public static final boolean LOGVV = LOCAL_LOGVV && LOGV;

    public static final String STORAGE_AUTHORITY = "com.android.providers.transfers.documents";
    public static final String STORAGE_ROOT_ID = "transfers";
}