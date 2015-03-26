package info.guardianproject.securereaderinterface;


import android.content.Context;
import android.os.Bundle;

public class VideoListActivity extends FragmentActivityWithMenu {
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "VideoListActivity";
	private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_video_list);
		
		setDisplayHomeAsUp(true);
		setActionBarTitle(getString(R.string.video_title));
	}

	@Override
	protected boolean useLeftSideMenu() {
		return true;
	}
}
