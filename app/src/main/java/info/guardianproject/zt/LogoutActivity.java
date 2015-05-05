package info.guardianproject.zt;

import info.guardianproject.zt.R;
import android.os.Bundle;

public class LogoutActivity extends FragmentActivityWithMenu
{
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "LogoutActivity";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_downloads);	
		
		// Display home as up
		setDisplayHomeAsUp(true);
		setMenuIdentifier(R.menu.activity_downloads);
		// Set up the action bar.
		setActionBarTitle(getString(R.string.title_activity_logout));
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return true;
	}
}
