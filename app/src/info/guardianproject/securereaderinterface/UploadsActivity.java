package info.guardianproject.securereaderinterface;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.Globals;
import info.guardianproject.securereaderinterface.uiutil.Utility;
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

public class UploadsActivity extends FragmentActivityWithMenu
{
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "UploadsActivity";
	private Context mContext;
	
	Button btnUpload;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_uploads);
		
		//setMenuIdentifier(R.menu.activity_downloads);
		setDisplayHomeAsUp(true);
		setActionBarTitle(getString(R.string.abc_action_bar_home_description));
		
		init();
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return true;
	}
	
	private void init() {
        Button btnImport = (Button) findViewById(R.id.btnImport);
        Button btnCapture = (Button) findViewById(R.id.btnCapture);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        
        btnImport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // ACTION_OPEN_DOCUMENT is the new API 19 action for the Android file manager
                Intent intent;
                int requestId = Globals.REQUEST_FILE_IMPORT;
                if (Build.VERSION.SDK_INT >= 19) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                }

                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                //String cardMediaId = mCardModel.getStoryPath().getId() + "::" + mCardModel.getId() + "::" + MEDIA_PATH_KEY;
                // Apply is async and fine for UI thread. commit() is synchronous
                //mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Constants.PREFS_CALLING_CARD_ID, cardMediaId).apply();
                ((Activity) mContext).startActivityForResult(intent, requestId);
            }
        });

        btnCapture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = null;
                int requestId = -1;
                Spinner spCaptureOptions = (Spinner) findViewById(R.id.spCaptureOptions);
                Globals.MEDIA_TYPE mediaType = getSelectedMediaType(spCaptureOptions.getSelectedItemPosition());

                if (mediaType == Globals.MEDIA_TYPE.AUDIO) {
                    intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    requestId = Globals.REQUEST_AUDIO_CAPTURE;

                } else if (mediaType == Globals.MEDIA_TYPE.IMAGE) {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File photoFile;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.e(LOGTAG, "Unable to make image file");
                        return;
                    }

                    mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString(Globals.EXTRA_FILE_LOCATION, photoFile.getAbsolutePath()).apply();
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    requestId = Globals.REQUEST_IMAGE_CAPTURE;

                } else if (mediaType == Globals.MEDIA_TYPE.VIDEO) {
                    intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    requestId = Globals.REQUEST_VIDEO_CAPTURE;
                }

                if (null != intent && intent.resolveActivity(mContext.getPackageManager()) != null) {
                    ((Activity) mContext).startActivityForResult(intent, requestId);
                }
            }
        });
        
        btnUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(mContext, "uploading...", Toast.LENGTH_SHORT).show();
            }
        });
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

    private Globals.MEDIA_TYPE getSelectedMediaType(int pos) {
        // set image as default TODO set to null and display error
    	Globals.MEDIA_TYPE mediaType = Globals.MEDIA_TYPE.IMAGE;

        switch (pos) {
            case 0:
                mediaType = Globals.MEDIA_TYPE.IMAGE;
                break;
            case 1:
                mediaType = Globals.MEDIA_TYPE.VIDEO;
                break;
            case 2:
                mediaType = Globals.MEDIA_TYPE.AUDIO;
                break;
        }

        return mediaType;
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

            } else if(requestCode == Globals.REQUEST_IMAGE_CAPTURE) {
                path = this.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString(Globals.EXTRA_FILE_LOCATION, null);
                mediaType = Globals.MEDIA_TYPE.IMAGE;

                Log.d(LOGTAG, "onActivityResult, image path:" + path);

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
                mediaType = Utility.getMediaType(path);

                Log.d(LOGTAG, "onActivityResult, imported file path:" + path);
            }

            if (null == path) {
                Log.d(LOGTAG, "onActivityResult: Invalid file on import or capture");
                Toast.makeText(getApplicationContext(), R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            } else if (null == mediaType) {
                Log.d(LOGTAG, "onActivityResult: Invalid Media Type");
                Toast.makeText(getApplicationContext(), R.string.error_invalid_media_type, Toast.LENGTH_SHORT).show();
            } else {
                // create media
                //Media media = new Media(this, path, mediaType);

            	/*
                Intent reviewMediaIntent = new Intent(this, ReviewMediaActivity.class);
                reviewMediaIntent.putExtra(Globals.EXTRA_CURRENT_MEDIA_ID, media.getId());
                startActivity(reviewMediaIntent);*/
            	
            	ImageView ivMedia = (ImageView) findViewById(R.id.ivMedia);
            	Bitmap bm = Utility.getThumbnail(mContext, path, mediaType);
            	ivMedia.setImageBitmap(bm);
            	
            	if(null != bm) {
            		btnUpload.setVisibility(View.VISIBLE);
            	} else {
            		btnUpload.setVisibility(View.GONE);
            	}    
            }
            
            init();
        }
    }
}
