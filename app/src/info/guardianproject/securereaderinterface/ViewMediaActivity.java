package info.guardianproject.securereaderinterface;

import android.os.Bundle;
import android.view.MenuItem;
import info.guardianproject.securereaderinterface.R;
import com.tinymission.rss.MediaContent;

public class ViewMediaActivity extends FragmentActivityWithMenu // implements
// OnTouchListener
{
	public static final String LOGTAG = "ViewMediaActivity";
	public static final boolean LOGGING = false;
	
	@Override
	protected boolean useLeftSideMenu()
	{
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentViewNoBase(R.layout.activity_view_media);
		setMenuIdentifier(R.menu.activity_view_media);
		this.setDisplayHomeAsUp(true);
		this.setUsePullDownActionBar(true);
		
		// Send the URI along to the fragment, so we know what to show!
		//
		try
		{
			ViewMediaFragment fragment = (ViewMediaFragment) this.getSupportFragmentManager().findFragmentById(R.id.view_media_fragment);
			if (fragment != null)
			{
				Bundle parameters = this.getIntent().getBundleExtra("parameters");
				if (parameters != null)
				{
					MediaContent mediaContent = (MediaContent) parameters.getSerializable("media");
					fragment.setMediaContent(mediaContent);
				}
			}
		}
		catch (Exception ex)
		{
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		finish();
	}
	
	
}
