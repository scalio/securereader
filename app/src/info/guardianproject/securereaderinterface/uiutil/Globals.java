package info.guardianproject.securereaderinterface.uiutil;

/**
 * Created by micahjlucas on 03/10/15.
 */
public class Globals {

    // EULA
    public static final String ASSET_EULA         = "EULA";
    public static final String PREF_EULA_ACCEPTED = "eula.accepted";

    // request Codes used for media import and capture
    
    public static final int REQUEST_PHOTO_IMPORT = 100;
    public static final int REQUEST_VIDEO_IMPORT = 101;
    public static final int REQUEST_AUDIO_IMPORT = 102;
    
    public static final int REQUEST_PHOTO_CAPTURE = 103;
    public static final int REQUEST_VIDEO_CAPTURE = 104;
    public static final int REQUEST_AUDIO_CAPTURE = 105;
    public static final int REQUEST_TEXT_CAPTURE = 106;
    
    public static final int REQUEST_FILE_IMPORT   = 107;

    // intent extras
    public static final String EXTRA_MEDIA_FILE_LOCATION   	= "rz_extra_media_file_location";
    public static final String EXTRA_MEDIA_FILE_PATH 		= "rz_extra_media_file_path";
    public static final String EXTRA_MEDIA_FILE_TYPE   	 	= "rz_extra_media_file_type";

    // prefs
    public static final String PREF_FILE_KEY            = "archive_pref_key";

    public static final String PREF_FIRST_RUN           = "archive_pref_first_run";

    public static final String PREF_USE_TOR             = "archive_pref_use_tor";
    public static final String PREF_SHARE_TITLE         = "archive_pref_share_title";
    public static final String PREF_SHARE_DESCRIPTION   = "archive_pref_share_description";
    public static final String PREF_SHARE_AUTHOR        = "archive_pref_share_author";
    public static final String PREF_SHARE_TAGS          = "archive_pref_share_tags";
    public static final String PREF_SHARE_LOCATION      = "archive_pref_share_location";
    public static final String PREF_LICENSE_URL         = "archive_pref_share_license_url";

    public final static String SITE_ARCHIVE             = "archive"; //Text, Audio, Photo, Video
    
    public static enum MEDIA_TYPE {
        PHOTO, VIDEO, AUDIO, TEXT;
    }
}

