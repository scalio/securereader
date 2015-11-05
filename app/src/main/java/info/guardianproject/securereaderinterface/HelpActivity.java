package info.guardianproject.securereaderinterface;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.R;

public class HelpActivity extends FragmentActivityWithMenu
{
	public static final String LOGTAG = "HelpActivity";
	public static final boolean LOGGING = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		setMenuIdentifier(R.menu.activity_help);

		if (useLeftSideMenu())
			setDisplayHomeAsUp(true);

		// Set up the action bar.
		setActionBarTitle(getString(R.string.help_title));

		Button btnConnect = (Button) findViewById(R.id.btnConnectTor);
		btnConnect.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				App.getInstance().socialReader.connectProxy(HelpActivity.this);
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
		else
			getSupportActionBar().hide();
		
		// Update version display
		TextView tvVersion = (TextView) findViewById(R.id.tvVersion);
		tvVersion.setText("" + getString(R.string.app_name) + " " + getBuildVersion() + " - " + getBuildDate());
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

	private String getBuildVersion()
	{
		String ret = "unknown";
		try
		{
			ret = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return ret;
	}

	@SuppressLint("NewApi")
	private String getBuildDate()
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1)
		{
			return getBuildDateOld();
		}
		else
		{
			String ret = "unknown";
			try
			{
				long time = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
				ret = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			return ret;
		}
	}

	private String getBuildDateOld()
	{
		String buildDate = "unknown";
		try
		{
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			buildDate = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			zf.close();
		}
		catch (Exception e)
		{
		}
		return buildDate;
	}
}
