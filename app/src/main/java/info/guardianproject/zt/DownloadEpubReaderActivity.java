package info.guardianproject.zt;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import info.guardianproject.zt.*;
import info.guardianproject.zt.ui.PackageHelper;
import info.guardianproject.zt.R;

public class DownloadEpubReaderActivity extends info.guardianproject.zt.FragmentActivityWithMenu implements OnClickListener
{
	public static final String LOGTAG = "DownloadEpubReaderActivity";
	public static final boolean LOGGING = false;
	
	private View mBtnClose;
	private View mBtnGetFromPlay;
	private View mBtnGetFromWeb;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Display home as up
		setDisplayHomeAsUp(true);

		setContentView(R.layout.activity_download_epub_reader);
		setMenuIdentifier(R.menu.activity_create_account);
		
		mBtnClose = findViewById(R.id.btnClose);
		mBtnGetFromPlay = findViewById(R.id.btnGetFromPlay);
		mBtnGetFromWeb = findViewById(R.id.btnGetFromWeb);
		
		mBtnClose.setOnClickListener(this);
		mBtnGetFromPlay.setOnClickListener(this);
		mBtnGetFromWeb.setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		if (v == mBtnClose)
		{
			finish();
		}
		else if (v == mBtnGetFromPlay)
		{
			Uri uri = Uri.parse(PackageHelper.URI_FBREADER_PLAY);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
			finish();
		}
		else if (v == mBtnGetFromWeb)
		{
			Uri uri = Uri.parse(PackageHelper.URI_FBREADER_WEB);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
			finish();
		}
	}
}
