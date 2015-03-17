package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.uiutil.Globals;
import info.guardianproject.securereaderinterface.uiutil.Utility;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class UploadsChooseActivity extends FragmentActivityWithMenu {
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "UploadsActivity";
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_uploads_choose);
		
		//setMenuIdentifier(R.menu.activity_downloads);
		setDisplayHomeAsUp(true);
		setActionBarTitle(getString(R.string.abc_action_mode_done));
		
		init();
	}
	
	private void init() {	
		// imports
		Button btnImportPhoto = (Button) findViewById(R.id.btnImportPhoto);
        Button btnImportVideo = (Button) findViewById(R.id.btnImportVideo);
        Button btnImportAudio = (Button) findViewById(R.id.btnImportAudio);
        
        btnImportPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleImport(Globals.MEDIA_TYPE.PHOTO);
            }
        });
        
        btnImportVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleImport(Globals.MEDIA_TYPE.VIDEO);
            }
        });
        
        btnImportAudio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleImport(Globals.MEDIA_TYPE.AUDIO);
            }
        });
        
        // captures
        Button btnCapturePhoto = (Button) findViewById(R.id.btnCapturePhoto);
        Button btnCaptureVideo = (Button) findViewById(R.id.btnCaptureVideo);
        Button btnCaptureAudio = (Button) findViewById(R.id.btnCaptureAudio);
        Button btnCaptureText = (Button) findViewById(R.id.btnCaptureText);
        
        btnCapturePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleCapture(Globals.MEDIA_TYPE.PHOTO);
            }
        });
        
        btnCaptureVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	handleCapture(Globals.MEDIA_TYPE.VIDEO);
            }
        });
        
        btnCaptureAudio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	handleCapture(Globals.MEDIA_TYPE.AUDIO);
            }
        });
        
        btnCaptureText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	handleTextCapture();
            }
        });
	}
	
	@SuppressLint("InlinedApi")
	private void handleImport(Globals.MEDIA_TYPE mediaType) {
		Intent intent = null;
		int requestId = Globals.REQUEST_FILE_IMPORT;
		
		 // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // filter intent search by mediaType (photo, video, audio)
        intent.setType(Utility.getIntentMediaType(mediaType));

        //String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
        // Apply is async and fine for UI thread. commit() is synchronous
        //mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Constants.PREFS_CALLING_CARD_ID, cardMediaId).apply();
        ((Activity) mContext).startActivityForResult(intent, requestId);
	}
	
	
	private void handleCapture(Globals.MEDIA_TYPE mediaType) {
		Intent intent = null;
		int requestCode = -1;	
		
		if (mediaType == Globals.MEDIA_TYPE.PHOTO) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(LOGTAG, "Unable to make image file");
                return;
            }

            mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Globals.EXTRA_MEDIA_FILE_LOCATION, photoFile.getAbsolutePath()).apply();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            requestCode = Globals.REQUEST_PHOTO_CAPTURE;

        } else if (mediaType == Globals.MEDIA_TYPE.VIDEO) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            requestCode = Globals.REQUEST_VIDEO_CAPTURE;
            
        } else if (mediaType == Globals.MEDIA_TYPE.AUDIO) {
            intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            requestCode = Globals.REQUEST_AUDIO_CAPTURE;
        }

        if (null != intent && intent.resolveActivity(mContext.getPackageManager()) != null) {
            ((Activity) mContext).startActivityForResult(intent, requestCode);
        }
	}
	
	private void handleTextCapture() {
		Intent uploadsActivityIntent = new Intent(this, UploadsActivity.class);
    	startActivity(uploadsActivityIntent);
	}
	
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }
    
    @SuppressLint("NewApi") @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(LOGTAG, "onActivityResult, requestCode:" + requestCode + ", resultCode: " + resultCode);

        String path = null;
        Globals.MEDIA_TYPE mediaType = null;

        if (resultCode == RESULT_OK) {
            if(requestCode == Globals.REQUEST_AUDIO_CAPTURE) {
                Uri uri = intent.getData();
                path = Utility.getRealPathFromURI(getApplicationContext(), uri);
                mediaType = Globals.MEDIA_TYPE.AUDIO;

                Log.d(LOGTAG, "onActivityResult, audio path:" + path);

            } else if(requestCode == Globals.REQUEST_PHOTO_CAPTURE) {
                path = this.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(Globals.EXTRA_MEDIA_FILE_LOCATION, null);
                mediaType = Globals.MEDIA_TYPE.PHOTO;

                Log.d(LOGTAG, "onActivityResult, photo path:" + path);

            } else if(requestCode == Globals.REQUEST_VIDEO_CAPTURE) {
                Uri uri = intent.getData();
                path = Utility.getRealPathFromURI(getApplicationContext(), uri);
                mediaType = Globals.MEDIA_TYPE.VIDEO;

                Log.d(LOGTAG, "onActivityResult, video path:" + path);

            }  else if (requestCode == Globals.REQUEST_FILE_IMPORT) {
                Uri uri = intent.getData();
                // Will only allow stream-based access to files
                if (Build.VERSION.SDK_INT >= 19) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                path = uri.toString();
                mediaType = Utility.getMediaTypeByPath(path);

                Log.d(LOGTAG, "onActivityResult, imported file path:" + path);
            }

            if (null == path) {
                Log.d(LOGTAG, "onActivityResult: Invalid file on import or capture");
                Toast.makeText(getApplicationContext(), R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            } else if (null == mediaType) {
                Log.d(LOGTAG, "onActivityResult: Invalid Media Type");
                Toast.makeText(getApplicationContext(), R.string.error_invalid_media_type, Toast.LENGTH_SHORT).show();
            } else {       	
            	Intent uploadsActivityIntent = new Intent(this, UploadsActivity.class);
            	uploadsActivityIntent.putExtra(Globals.EXTRA_MEDIA_FILE_TYPE, mediaType.ordinal());
            	uploadsActivityIntent.putExtra(Globals.EXTRA_MEDIA_FILE_PATH, path);
            	startActivity(uploadsActivityIntent);            	
            }
        }
    }

	@Override
	protected boolean useLeftSideMenu() {
		return true;
	}
}