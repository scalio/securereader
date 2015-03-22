package info.guardianproject.securereaderinterface.uiutil;

/**
 * Created by micahjlucas on 03/10/15.
 */
public class Global {
	
    // prefs
    public static final String PREF_USER_NAME 	  	= "rz_pref_user_name";
    public static final String PREF_USER_PASSWORD 	= "rz_pref_user_password";
    public static final String PREF_SHARED_FILE_KEY = "rz_pref_shared_file_key";
    
    // request codes used for media import and capture
    public static final int REQUEST_PHOTO_IMPORT  = 100;
    public static final int REQUEST_VIDEO_IMPORT  = 101;
    public static final int REQUEST_AUDIO_IMPORT  = 102;
    
    public static final int REQUEST_PHOTO_CAPTURE = 103;
    public static final int REQUEST_VIDEO_CAPTURE = 104;
    public static final int REQUEST_AUDIO_CAPTURE = 105;
    public static final int REQUEST_TEXT_CAPTURE  = 106;
    
    public static final int REQUEST_FILE_IMPORT   = 107;

    // intent extras
    public static final String EXTRA_MEDIA_FILE_LOCATION   	= "rz_extra_media_file_location";
    public static final String EXTRA_MEDIA_FILE_PATH 		= "rz_extra_media_file_path";
    public static final String EXTRA_MEDIA_FILE_TYPE   	 	= "rz_extra_media_file_type";
    
    public static enum MEDIA_TYPE {
        PHOTO, VIDEO, AUDIO, TEXT;
    }
    
    //radio 
    public static final String RZ_RADIO_URI = "http://icecast.xs4all.nl:8000/zamaneh";
}

