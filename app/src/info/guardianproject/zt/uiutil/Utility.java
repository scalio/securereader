package info.guardianproject.zt.uiutil;

/**
 * Created by micahjlucas on 12/16/14.
 */

import info.guardianproject.zt.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.Toast;


public class Utility {
	public static final String LOGTAG = "Utility";
	
    public static Global.MEDIA_TYPE getMediaTypeByPath(String mediaPath) {
    	if (mediaPath == null) {
            return null;
        }
    	
        // makes comparisons easier
        mediaPath = mediaPath.toLowerCase(Locale.ENGLISH);

        String result;
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mediaPath);
        result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        if (result == null) {
            if (mediaPath.endsWith("wav")) {
                result = "audio/wav";
            } else if (mediaPath.endsWith("mp3")) {
                result = "audio/mpeg";
            } else if (mediaPath.endsWith("3gp") || mediaPath.endsWith("3gpp")) {
                result = "video/3gpp";
            } else if (mediaPath.endsWith("mp4")) {
                result = "video/mp4";
            } else if (mediaPath.endsWith("jpg")) {
                result = "image/jpeg";
            } else if (mediaPath.endsWith("png")) {
                result = "image/png";
            } else {
                // for imported files
                result = mediaPath;
            }
        }

        if (result.contains("audio")) {
            return Global.MEDIA_TYPE.AUDIO;
        } else if(result.contains("image")) {
            return Global.MEDIA_TYPE.PHOTO;
        } else if(result.contains("video")) {
            return Global.MEDIA_TYPE.VIDEO;
        }

        return null;
    }
    
    public static String getIntentMediaType(Global.MEDIA_TYPE mediaType) {
    	String intentType = null;
    	
        if (mediaType == Global.MEDIA_TYPE.PHOTO) {
        	intentType = "image/*";
        } else if (mediaType == Global.MEDIA_TYPE.VIDEO){
        	intentType = "video/*";
        } else if (mediaType == Global.MEDIA_TYPE.AUDIO){
        	intentType = "audio/*";
        }

        return intentType;
    }
    
    public static Global.MEDIA_TYPE getMediaTypeByInt(int intMediaType) {
    	Global.MEDIA_TYPE mediaType = null;
    	
    	if (intMediaType == Global.MEDIA_TYPE.PHOTO.ordinal()) {
    		mediaType =  Global.MEDIA_TYPE.PHOTO;
        } else if(intMediaType == Global.MEDIA_TYPE.VIDEO.ordinal()) {
        	mediaType = Global.MEDIA_TYPE.VIDEO;
        } else if(intMediaType == Global.MEDIA_TYPE.AUDIO.ordinal()) {
        	mediaType = Global.MEDIA_TYPE.AUDIO;
        } else if(intMediaType == Global.MEDIA_TYPE.TEXT.ordinal()) {
        	mediaType = Global.MEDIA_TYPE.TEXT;
        }

        return mediaType;
    }
    

    public static void clearWebviewAndCookies(WebView webview, Activity activity) {
        CookieSyncManager.createInstance(activity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        if(webview != null) {
            webview.clearHistory();
            webview.clearCache(true);
            webview.clearFormData();
            webview.loadUrl("about:blank");
            webview.destroy();
        }
    }

    public static boolean stringNotBlank(String string) {
        return (string != null) && !string.equals("");
    }

    public static String stringArrayToCommaString(String[] strings) {
        if (strings.length > 0) {
            StringBuilder nameBuilder = new StringBuilder();

            for (String n : strings) {
                nameBuilder.append(n.replaceAll("'", "\\\\'")).append(",");
            }

            nameBuilder.deleteCharAt(nameBuilder.length() - 1);

            return nameBuilder.toString();
        } else {
            return "";
        }
    }

    public static String[] commaStringToStringArray(String string) {
        if (string != null) {
            return string.split(",");
        } else {
            return null;
        }
    }

    public static void toastOnUiThread(Activity activity, String message) {
        toastOnUiThread(activity, message, false);
    }

    public static void toastOnUiThread(Activity activity, String message, final boolean isLongToast) {
        final Activity _activity = activity;
        final String _msg = message;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(_activity.getApplicationContext(), _msg, isLongToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toastOnUiThread(FragmentActivity fragmentActivity, String message) {
        toastOnUiThread(fragmentActivity, message, false);
    }

    public static void toastOnUiThread(FragmentActivity fragmentActivity, String message, final boolean isLongToast) {
        final FragmentActivity _activity = fragmentActivity;
        final String _msg = message;
        fragmentActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(_activity.getApplicationContext(), _msg, isLongToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public static Bitmap getThumbnail(Context context, String filePath, Global.MEDIA_TYPE mediaType) {
        Bitmap thumbnail = null;
        String thumbnailFilePath;
        
        Uri uri = Uri.parse(filePath);
        String lastSegment = uri.getLastPathSegment();
        boolean isDocumentProviderUri = filePath.contains("content:/") && (lastSegment.contains(":"));
        
        if (mediaType == Global.MEDIA_TYPE.AUDIO) {
            thumbnail = BitmapFactory.decodeResource(context.getResources(), R.drawable.img_audio_wave);
        } else if (mediaType == Global.MEDIA_TYPE.PHOTO) {
            if (isDocumentProviderUri) {
                // path of form : content://com.android.providers.media.documents/document/video:183
                // An Android Document Provider URI. Thumbnail already generated
                // TODO Because we need Context we can't yet override this behavior at MediaFile#getThumbnail
                long id = Long.parseLong(Uri.parse(filePath).getLastPathSegment().split(":")[1]);
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            } else {
                if (filePath.contains("content:/")) {
                	filePath = Utility.getPathFromURI(context, uri);
                }
                File originalFile = new File(filePath);
                String fileName = originalFile.getName();
                String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
                thumbnailFilePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1) + tokens[0] + "_thumbnail.png";
                File thumbnailFile = new File(thumbnailFilePath);
                if (thumbnailFile.exists()) {
                    thumbnail = BitmapFactory.decodeFile(thumbnailFilePath);
                } else {
                    Bitmap bitMap = BitmapFactory.decodeFile(filePath);

                    try {
                        FileOutputStream thumbnailStream = new FileOutputStream(thumbnailFile);
                        thumbnail = ThumbnailUtils.extractThumbnail(bitMap, 400, 300); // FIXME figure out the real aspect ratio and size needed
                        thumbnail.compress(Bitmap.CompressFormat.PNG, 75, thumbnailStream); // FIXME make compression level configurable
                        thumbnailStream.flush();
                        thumbnailStream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (mediaType == Global.MEDIA_TYPE.VIDEO) {
            // path of form : content://com.android.providers.media.documents/document/video:183
            if (isDocumentProviderUri) {
                // An Android Document Provider URI. Thumbnail already generated

                long id = 0;
                id = Long.parseLong(lastSegment.split(":")[1]);
                return MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            } else {
                // Regular old File path
                try {
                    if (filePath.contains("content:/")) {
                    	filePath = Utility.getPathFromURI(context, uri);
                    }
                    File originalFile = new File(filePath);
                    String fileName = originalFile.getName();
                    String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
                    thumbnailFilePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1) + tokens[0] + "_thumbnail.png";
                    File thumbnailFile = new File(thumbnailFilePath);
                    if (thumbnailFile.exists()) {
                        thumbnail = BitmapFactory.decodeFile(thumbnailFilePath);
                    } else {
                        FileOutputStream thumbnailStream = new FileOutputStream(thumbnailFile);

                        thumbnail = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
                        if (thumbnail != null) {
                            thumbnail.compress(Bitmap.CompressFormat.PNG, 75, thumbnailStream); // FIXME make compression level configurable
                            thumbnailStream.flush();
                            thumbnailStream.close();
                        }
                    }
                } catch (IOException ioe) {
                    return null;
                }
            }
        }  else {
            Log.e(LOGTAG, "can't create thumbnail file for " + filePath + ", unsupported medium");
            thumbnail = null;
        }

        return thumbnail;
    }
    
    public static Boolean isUserLoggedIn(Context context) {
    	String username = SharedPrefHelper.getPref(context, Global.PREF_USER_NAME);
    	
    	// if user exists
    	if(null != username) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public static String getPathFromURI(Context context, Uri contentUri) {
        if (contentUri == null)
            return null;

        // work-around to handle normal paths
        if (contentUri.toString().startsWith(File.separator)) {
            return contentUri.toString();
        }

        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    
    
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi") public static String getImportPathFromURI(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    
    
}
