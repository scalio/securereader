package info.guardianproject.securereaderinterface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class LockableActivity extends SherlockFragmentActivity {

	private boolean mLockedInOnPause;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LocalBroadcastManager.getInstance(this).registerReceiver(mLockReceiver, new IntentFilter(App.LOCKED_BROADCAST_ACTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(mUnlockReceiver, new IntentFilter(App.UNLOCKED_BROADCAST_ACTION));
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			 getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
			 WindowManager.LayoutParams.FLAG_SECURE);
		}
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mLockReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mUnlockReceiver);
	}

	@Override
	protected void onStart()
	{
		if (!mLockedInOnPause)
			App.getInstance().onActivityResume(this);
		super.onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (!mLockedInOnPause)
			App.getInstance().onActivityPause(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		PowerManager pm =(PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm.isScreenOn() == false)
		{
			mLockedInOnPause = true;
			App.getInstance().onActivityPause(this);
		}
	}

	@Override
	protected void onResume()
	{
		if (mLockedInOnPause)
			App.getInstance().onActivityResume(this);
		mLockedInOnPause = false;
		super.onResume();
	}
	
	BroadcastReceiver mLockReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			LockableActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					onLocked();
				}
			});
		}
	};
	
	BroadcastReceiver mUnlockReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			LockableActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					onUnlocked();
				}
			});
		}
	};
	private View mContentView;

	private boolean mHasResult;

	private int mRequestCode;

	private int mResultCode;

	private Intent mReturnedIntent;
	
	protected void onLocked()
	{
		mContentView.setVisibility(View.INVISIBLE);
	}
	
	protected void onUnlocked()
	{
		mContentView.setVisibility(View.VISIBLE);
		if (mHasResult)
		{
			mHasResult = false;
			onUnlockedActivityResult(mRequestCode, mResultCode, mReturnedIntent);
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		ViewGroup parent = (ViewGroup) (getWindow().getDecorView());
		mContentView = parent.getChildAt(0);
	}

	@Override
	public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		return true;
	}

	@Override
	final protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent)
	{
		super.onActivityResult(requestCode, resultCode, returnedIntent);
		if (App.getInstance().isActivityLocked())
		{
			mHasResult = true;
			mRequestCode = requestCode;
			mResultCode = resultCode;
			mReturnedIntent = returnedIntent;
		}
		else
		{
			onUnlockedActivityResult(requestCode, resultCode, returnedIntent);
		}
	}
	
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		
	}
}
