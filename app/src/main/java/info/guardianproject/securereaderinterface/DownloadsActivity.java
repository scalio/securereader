package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.R;
import android.os.Bundle;
import android.widget.ListView;

public class DownloadsActivity extends FragmentActivityWithMenu
{
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "DownloadsActivity";
	
	private ListView mListView;
	private DownloadsAdapter mListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_downloads);
		setMenuIdentifier(R.menu.activity_downloads);

		// Display home as up
		setDisplayHomeAsUp(true);
		
		// Set up the action bar.
		setActionBarTitle(getString(R.string.downloads_title));

		mListView = (ListView) findViewById(R.id.lvRoot);
		mListAdapter = DownloadsAdapter.getInstance(this);
		mListView.setAdapter(mListAdapter);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		mListAdapter.notifyDataSetChanged();
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return true;
	}

	// @Override
	// protected void onResume()
	// {
	// super.onResume();
	// ViewGroup parent = (ViewGroup) (getWindow().getDecorView());
	// View content = parent.getChildAt(0);
	// this.performRotateTransition(parent, content);
	// }

}
