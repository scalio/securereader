package info.guardianproject.securereaderinterface;


import info.guardianproject.securereaderinterface.uiutil.Global;
import info.guardianproject.securereaderinterface.uiutil.Utility;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class UploadsActivity extends FragmentActivityWithMenu {
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "UploadsActivity";
	private Context mContext;
	private boolean isText = false;
	
	Button btnUpload;
	ImageView ivMedia;
	EditText etMain;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		mContext = this;

		setContentView(R.layout.activity_uploads);
		
		//setMenuIdentifier(R.menu.activity_downloads);
		setDisplayHomeAsUp(true);
		setActionBarTitle(getString(R.string.upload_title));
		
		// init views
		ivMedia = (ImageView) findViewById(R.id.ivMedia);
		etMain = (EditText) findViewById(R.id.etMain);
		
		btnUpload = (Button) findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(mContext, "upload", Toast.LENGTH_SHORT).show();
            }
        });
		
		// get intent extras
		if(null != extras) {
			int intMediaType = extras.getInt(Global.EXTRA_MEDIA_FILE_TYPE);
			String filePath = extras.getString(Global.EXTRA_MEDIA_FILE_PATH);
			
			initMedia(filePath, Utility.getMediaTypeByInt(intMediaType));
		} else {
			isText = true;	
			initText();
		}
	}
	
	private void initMedia(String filePath, Global.MEDIA_TYPE mediaType) {
		// show imageview
		ivMedia.setVisibility(View.VISIBLE);
		
		Bitmap bm = Utility.getThumbnail(mContext, filePath, mediaType);	
    	ivMedia.setImageBitmap(bm);
	}
	
	private void initText() {
		// show textview
		etMain.setVisibility(View.VISIBLE);
	}

	@Override
	protected boolean useLeftSideMenu() {
		return true;
	}
   
}
