package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.securereader.SyncServiceFeedFetcher.SyncServiceFeedFetchedCallback;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import info.guardianproject.yakreader.R;
import com.tinymission.rss.Feed;

public class HelpActivity extends FragmentActivityWithMenu
{
	public static String LOGTAG = "Big Buffalo";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (useLeftSideMenu())
			setDisplayHomeAsUp(true);
		setContentView(R.layout.activity_help);
		setMenuIdentifier(R.menu.activity_help);

		// Set up the action bar.
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		setActionBarTitle(getString(R.string.help_title));

		Button btnConnect = (Button) findViewById(R.id.btnConnectTor);
		btnConnect.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				App.getInstance().socialReader.connectTor(HelpActivity.this);
				//App.getInstance().socialReader.getSubscribedFeedItems(new FeedFetchedCallback()
				/*App.getInstance().socialReader.getSubscribedFeedItems(new SyncServiceFeedFetchedCallback()
				{
					@Override
					public void feedFetched(Feed _feed)
					{
					}
				}, true);*/
			}
		});

		Button btnTestPanic = (Button) findViewById(R.id.btnTestPanic);
		btnTestPanic.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(HelpActivity.this, PanicActivity.class);
				intent.putExtra("testing", true);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
			}
		});

		Button btnDone = (Button) findViewById(R.id.btnDone);
		btnDone.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				App.getSettings().setHasShownHelp(true);
				Intent intent = new Intent(HelpActivity.this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
				finish();
			}
		});
		if (useLeftSideMenu())
			btnDone.setVisibility(View.GONE);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return getIntent().getBooleanExtra("useLeftSideMenu", true);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		ViewGroup parent = (ViewGroup) (getWindow().getDecorView());
		View content = parent.getChildAt(0);
		this.performRotateTransition(parent, content);
	}

}
