package info.guardianproject.securereaderinterface.uiutil;

/**
 * Created by micahjlucas on 12/16/14.
 */

import info.guardianproject.securereader.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
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
	
    public static Globals.MEDIA_TYPE getMediaType(String mediaPath) {
        // makes comparisons easier
        mediaPath = mediaPath.toLowerCase();

        String result;
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(mediaPath);
        result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        if (result == null) {
            if (mediaPath.endsWith("wav")) {
                result = "audio/wav";
            } else if (mediaPath.toLowerCase().endsWith("mp3")) {
                result = "audio/mpeg";
            } else if (mediaPath.endsWith("3gp")) {
                result = "audio/3gpp";
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
            return Globals.MEDIA_TYPE.AUDIO;
        } else if(result.contains("image")) {
            return Globals.MEDIA_TYPE.IMAGE;
        } else if(result.contains("video")) {
            return Globals.MEDIA_TYPE.VIDEO;
        }

        return null;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        if (contentUri == null) {
            return null;
        }

        // work-around to handle normal paths
        if (contentUri.toString().startsWith(File.separator)) {
            return contentUri.toString();
        }

        // work-around to handle normal paths
        if (contentUri.toString().startsWith("file://")) {
            return contentUri.toString().split("file://")[1];
        }

        // TODO deal with document providers
        // path of form : content://com.android.providers.media.documents/document/video:183

        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
    
    public static Bitmap getThumbnail(Context context, String filePath, Globals.MEDIA_TYPE mediaType) {
        Bitmap thumbnail = null;
        String thumbnailFilePath;
        
        Uri uri = Uri.parse(filePath);
        String lastSegment = uri.getLastPathSegment();
        boolean isDocumentProviderUri = filePath.contains("content:/") && (lastSegment.contains(":"));
        
        if (mediaType == Globals.MEDIA_TYPE.AUDIO) {
            thumbnail = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        } else if (mediaType == Globals.MEDIA_TYPE.IMAGE) {
            if (isDocumentProviderUri) {
                // path of form : content://com.android.providers.media.documents/document/video:183
                // An Android Document Provider URI. Thumbnail already generated
                // TODO Because we need Context we can't yet override this behavior at MediaFile#getThumbnail
                long id = Long.parseLong(Uri.parse(filePath).getLastPathSegment().split(":")[1]);
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            } else {
                if (filePath.contains("content:/")) {
                	filePath = Utility.getRealPathFromURI(context, uri);
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
        } else if (mediaType == Globals.MEDIA_TYPE.VIDEO) {
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
                    	filePath = Utility.getRealPathFromURI(context, uri);
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
}
