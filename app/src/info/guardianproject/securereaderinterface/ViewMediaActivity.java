package info.guardianproject.securereaderinterface;

import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import info.guardianproject.securereaderinterface.R;
import android.view.Window;
import com.tinymission.rss.MediaContent;

public class ViewMediaActivity extends FragmentActivityWithMenu // implements
// OnTouchListener
{
	public static final String LOGTAG = "ViewMediaActivity";
	public static final boolean LOGGING = false;
	
	private static int ACTION_BAR_SHOW_DELAY = 2500; // How long to show the
														// action bar until it
														// does away

	private Handler mHandler;

	@Override
	protected boolean useLeftSideMenu()
	{
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		setContentView(R.layout.activity_view_media);
		setMenuIdentifier(R.menu.activity_view_media);
		this.setDisplayHomeAsUp(true);

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

	private final Runnable hideActionBarRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			hideActionBar();
		}
	};
	private GestureDetector mGestureDetector;

	public void showActionBar()
	{
		mHandler.removeCallbacks(hideActionBarRunnable);
		getSupportActionBar().show();
		mHandler.postDelayed(hideActionBarRunnable, ACTION_BAR_SHOW_DELAY);
	}

	public void hideActionBar()
	{
		getSupportActionBar().hide();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		createGestureDetector();
		showActionBar();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		if (mGestureDetector != null)
		{
			mGestureDetector.onTouchEvent(ev);
		}
		return super.dispatchTouchEvent(ev);
	}

	public void createGestureDetector()
	{
		if (mGestureDetector == null)
		{
			mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
			{
				private static final int SWIPE_MIN_DISTANCE = 40;
				private static final int SWIPE_MAX_OFF_PATH = 20;

				@Override
				public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
				{
					try
					{
						if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
							return false;

						// bottom to up swipe
						// if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE)
						// {
						// finish();
						// return true;
						// }
						else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE)
						{
							showActionBar();
							return true;
						}
					}
					catch (Exception e)
					{
						// nothing
					}
					return false;
				}
			});
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		finish();
	}
	
	
}
