package info.guardianproject.securereaderinterface;

import java.util.ArrayList;

import info.guardianproject.securereader.Settings.SyncMode;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.UICallbacks.OnCallbackListener;
import info.guardianproject.securereaderinterface.uiutil.ActivitySwitcher;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.FeedFilterView;
import info.guardianproject.securereaderinterface.views.FeedFilterView.FeedFilterViewCallbacks;
import info.guardianproject.securereaderinterface.widgets.CheckableButton;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class FragmentActivityWithMenu extends LockableActivity implements FeedFilterViewCallbacks, OnCallbackListener
{
	public static final String LOGTAG = "FragmentActivityWithMenu";
	public static final boolean LOGGING = false;
	
	private KillReceiver mKillReceiver;
	private SetUiLanguageReceiver mSetUiLanguageReceiver;
	private WipeReceiver mWipeReceiver;
	private int mIdMenu;
	private Menu mOptionsMenu;
	private boolean mDisplayHomeAsUp = false;

	/**
	 * The main menu that will host all content links.
	 */
	protected View mLeftSideMenu;
	protected DrawerLayout mDrawerLayout;
	protected ActionBarDrawerToggle mDrawerToggle;
	
	private ArrayList<Runnable> mDeferredCommands = new ArrayList<Runnable>();
	private Toolbar mToolbar;


	protected void setMenuIdentifier(int idMenu)
	{
		mIdMenu = idMenu;
	}

	protected boolean useLeftSideMenu()
	{
		return true;
	}

	@Override
	public void setContentView(int layoutResID) 
	{
		View view = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
		ViewStub stub = (ViewStub)view.findViewById(R.id.content_root);
		if (stub != null)
		{
			stub.setLayoutResource(layoutResID);
			stub.inflate();
			super.setContentView(view);
		}
		else
		{
			super.setContentView(layoutResID);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setBackgroundDrawable(null);

		UICallbacks.getInstance().addListener(this);

		mKillReceiver = new KillReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mKillReceiver, new IntentFilter(App.EXIT_BROADCAST_ACTION));
		mSetUiLanguageReceiver = new SetUiLanguageReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
		mWipeReceiver = new WipeReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mWipeReceiver, new IntentFilter(App.WIPE_BROADCAST_ACTION));
	}
	
	@Override
	public void onContentChanged() 
	{
		super.onContentChanged();
	    mToolbar = (Toolbar) findViewById(R.id.toolbar);
	    if (mToolbar != null)
	    {
	    	setSupportActionBar(mToolbar);
	    	getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    	getSupportActionBar().setDisplayShowTitleEnabled(false);
	    	setActionBarTitle(getSupportActionBar().getTitle());
	    }
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
		if (mDrawerLayout != null)
		{
			if (!useLeftSideMenu())
			{
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			}
			else
			{
				mLeftSideMenu = mDrawerLayout.findViewById(R.id.left_drawer);
				mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0)
				{
					@Override
					public void onDrawerClosed(View drawerView) {
						super.onDrawerClosed(drawerView);
						runDeferredCommands();
					}

					@Override
					public void onDrawerStateChanged(int newState) {
						super.onDrawerStateChanged(newState);
						if (newState == DrawerLayout.STATE_DRAGGING)
						{
							if (mMenuViewHolder != null)
							{
								mMenuViewHolder.viewFeedFilter.post(new Runnable()
								{
									@Override
									public void run()
									{
										mMenuViewHolder.viewFeedFilter.setSelectionAfterHeaderView();	
									}
								});
							}
							mMenuViewHolder.viewFeedFilter.invalidateViews();
							new UpdateTorStatusTask().execute();	
						}

					}
					
				};
				mDrawerLayout.setDrawerListener(mDrawerToggle);
				mDrawerToggle.syncState();
			}
		}
		
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (mDrawerToggle != null)
			mDrawerToggle.syncState();
	}

	public void setDisplayHomeAsUp(boolean displayHomeAsUp)
	{
		mDisplayHomeAsUp = displayHomeAsUp;
		if (displayHomeAsUp)
		{
			if (mDrawerToggle != null)
			{
				mDrawerToggle.setDrawerIndicatorEnabled(false);
			}
			else
			{
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			}
		}
		else
		{
			if (mDrawerToggle != null)
			{
				mDrawerToggle.setDrawerIndicatorEnabled(true);
			}
			else
			{
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
			}
		}
	}

	public void setActionBarTitle(CharSequence title)
	{
		if (mToolbar != null)
		{
			TextView tvTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
			if (tvTitle != null)
			{
				tvTitle.setText(title);
				tvTitle.setSelected(true);
			}
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		if (mLeftSideMenu != null)
			initializeMenu();
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume()
	{
		super.onResume();
		if (!isFinishing())
		{
			if (Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
			refreshMenu();
		}
	}

	private final class KillReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			finish();
		}
	}

	private final class SetUiLanguageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{

				@Override
				public void run()
				{
					recreateNowOrOnResume();
				}
			});
		}
	}

	private final class WipeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{
				@Override
				public void run()
				{
					onWipe();
				}
			});
		}
	}

	/**
	 * Override this to react to a wipe!
	 */
	protected void onWipe()
	{

	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		UICallbacks.getInstance().removeListener(this);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mKillReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mSetUiLanguageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mWipeReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (mIdMenu == 0)
			return false;
		mOptionsMenu = menu;
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(mIdMenu, menu);
		
		getMenuInflater().inflate(R.menu.overflow_main, menu);
		
		colorizeMenuItems();
		return true;
	}

	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	private void colorizeMenuItems()
	{
		if (mOptionsMenu == null || getSupportActionBar() == null)
			return;
		for (int i = 0; i < mOptionsMenu.size(); i++)
		{
			MenuItem item = mOptionsMenu.getItem(i);
			Drawable d = item.getIcon();
			if (d != null)
			{
				d.mutate();
				UIHelpers.colorizeDrawable(getSupportActionBar().getThemedContext(), R.attr.colorControlNormal, d);
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerToggle != null)
			mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mDisplayHomeAsUp && item.getItemId() == android.R.id.home)
		{
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			this.startActivity(intent);
			this.overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
			return true;
		}
		
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		switch (item.getItemId())
		{
		case R.id.menu_panic:
		{
			Intent intent = new Intent(this, PanicActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		}

		case R.id.menu_add_post:
		{
			Intent intent = new Intent(this, AddPostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			return true;
		}

		case R.id.menu_media_downloads:
		{
			UICallbacks.handleCommand(this, R.integer.command_downloads, null);
			return true;
		}

		case R.id.menu_manage_feeds:
		{
			UICallbacks.handleCommand(this, R.integer.command_feed_add, null);
			return true;
		}

		case R.id.menu_preferences:
		{
			UICallbacks.handleCommand(this, R.integer.command_settings, null);
			return true;
		}

		case R.id.menu_about:
		{
			UICallbacks.handleCommand(this, R.integer.command_help, null);
			return true;
		}

		case R.id.menu_share_app:
		{
			UICallbacks.handleCommand(this, R.integer.command_shareapp, null);
			return true;
		}

		case R.id.menu_lock_app:
		{
			App.getInstance().socialReader.lockApp();
			return true;
		}
		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void initializeMenu()
	{
		View menu = mLeftSideMenu;
		((FeedFilterView)menu.findViewById(R.id.viewFeedFilter)).setFeedFilterViewCallbacks(this);
		performRotateTransition((ViewGroup)mDrawerLayout.getParent());
		refreshMenu();
	}

	@SuppressLint("NewApi")
	protected void performRotateTransition(final ViewGroup container)
	{
		// Get bitmap from intent!!!
		Bitmap bmp = App.getInstance().getTransitionBitmap();
		if (bmp != null)
		{
			getWindow().setBackgroundDrawableResource(R.drawable.background_news);

			final ImageView snap = new ImageView(this);
			snap.setImageBitmap(bmp);
			container.addView(snap);
			snap.bringToFront();

			if (Build.VERSION.SDK_INT >= 11)
			{
				container.setLayerType(View.LAYER_TYPE_HARDWARE, null);
				snap.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			}
			else
			{
				container.setDrawingCacheEnabled(true);
				snap.setDrawingCacheEnabled(true);
			}

			// Animate!!!
			ActivitySwitcher.animationOut(container, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener()
			{
				@Override
				public void onAnimationFinished()
				{
					container.removeView(snap);
					App.getInstance().putTransitionBitmap(null);

					ActivitySwitcher.animationIn(container, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener()
					{
						@Override
						public void onAnimationFinished()
						{
							container.post(new Runnable()
							{
								@Override
								@SuppressLint("NewApi")
								public void run()
								{
									if (Build.VERSION.SDK_INT >= 11)
									{
										container.setLayerType(View.LAYER_TYPE_NONE, null);
									}
									else
									{
										container.setDrawingCacheEnabled(false);
									}

									container.clearAnimation();
									getWindow().setBackgroundDrawable(null);
									onAfterResumeAnimation();
								}
							});
						}
					});
				}
			});
		}
		else
		{
			onAfterResumeAnimation();
		}
	}

	protected void onAfterResumeAnimation()
	{
		// Override this to start doing stuff after the animation is complete
	}

	private class MenuViewHolder
	{
		public CheckableButton btnTorStatus;
		public CheckableButton btnShowPhotos;
		public FeedFilterView viewFeedFilter;
	}

	private MenuViewHolder mMenuViewHolder;

	protected void refreshMenu()
	{
		if (mLeftSideMenu != null)
		{
			new UpdateMenuFeedsTask().execute();
		}
	}

	private void createMenuViewHolder()
	{
		if (mMenuViewHolder == null)
		{
			mMenuViewHolder = new MenuViewHolder();
			View menuView = mLeftSideMenu;
			mMenuViewHolder.btnTorStatus = (CheckableButton) menuView.findViewById(R.id.btnMenuTor);
			mMenuViewHolder.btnShowPhotos = (CheckableButton) menuView.findViewById(R.id.btnMenuPhotos);
			mMenuViewHolder.viewFeedFilter = (FeedFilterView) menuView.findViewById(R.id.viewFeedFilter);
			
			// Hookup events
			mMenuViewHolder.btnTorStatus.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (App.getSettings().requireTor())
					{
						if (App.getInstance().socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_TOR)
						{
							mDrawerLayout.closeDrawers();
							App.getInstance().socialReader.connectTor(FragmentActivityWithMenu.this);
						}
					}
				}
			});
			
			mMenuViewHolder.btnShowPhotos.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (App.getSettings().syncMode() == SyncMode.LetItFlow)
						App.getSettings().setSyncMode(SyncMode.BitWise);
					else
						App.getSettings().setSyncMode(SyncMode.LetItFlow);
					mMenuViewHolder.btnShowPhotos.setChecked(App.getSettings().syncMode() == SyncMode.LetItFlow);
				}
			});
		}
	}
	
	class UpdateMenuFeedsTask extends ThreadedTask<Void, Void, Void>
	{
		private ArrayList<Feed> feeds;

		@Override
		protected Void doInBackground(Void... values)
		{
			createMenuViewHolder();
			feeds = App.getInstance().socialReader.getSubscribedFeedsList();
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			mMenuViewHolder.viewFeedFilter.updateList(feeds);
		}
	}

	class UpdateTorStatusTask extends ThreadedTask<Void, Void, Void>
	{
		private boolean isUsingTor;
		private boolean showImages;
		private boolean isOnline;

		@Override
		protected Void doInBackground(Void... values)
		{
			createMenuViewHolder();
			isUsingTor = App.getInstance().socialReader.useTor();
			isOnline = App.getInstance().socialReader.isTorOnline();
			showImages = (App.getSettings().syncMode() == SyncMode.LetItFlow);
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// Update TOR connection status
			//
			if (isUsingTor)
			{
				mMenuViewHolder.btnTorStatus.setChecked(isOnline);
				mMenuViewHolder.btnTorStatus.setText(isOnline ? R.string.menu_tor_connected : R.string.menu_tor_not_connected);
			}
			else
			{
				mMenuViewHolder.btnTorStatus.setChecked(false);
				mMenuViewHolder.btnTorStatus.setText(R.string.menu_tor_not_connected);
			}
			mMenuViewHolder.btnShowPhotos.setChecked(showImages);
		}
	}

	@Override
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, data);
		if (requestCode == UICallbacks.RequestCode.CREATE_CHAT_ACCOUNT.Value)
		{
			if (resultCode == RESULT_OK)
			{
				App.getSettings().setChatUsernamePasswordSet();
				// Then redirect somewhere?
			}
		}
	}
	
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			LayoutInflater mParent = (LayoutInflater) super.getSystemService(name);
			LayoutInflater inflater = mParent.cloneInContext(mParent.getContext());
			inflater.setFactory(new LayoutFactoryWrapper(this));
			return inflater;
		}
		return super.getSystemService(name);
	}

	@Override
	public void receiveShare() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.handleCommand(FragmentActivityWithMenu.this, R.integer.command_receiveshare, null);
			}
		});
	}
	
	@Override
	public void viewFavorites() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.setFeedFilter(FeedFilterType.FAVORITES, 0, FragmentActivityWithMenu.this);
			}
		});
	}

	@Override
	public void viewPopular() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.setFeedFilter(FeedFilterType.POPULAR, 0, FragmentActivityWithMenu.this);
			}
		});
	}

	@Override
	public void viewDownloads() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.handleCommand(FragmentActivityWithMenu.this, R.integer.command_downloads, null);
			}
		});
	}

	@Override
	public void viewShared() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.setFeedFilter(FeedFilterType.SHARED, 0, FragmentActivityWithMenu.this);
			}
		});
	}

	@Override
	public void viewFeed(final Feed feedToView) {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				if (feedToView == null)
					UICallbacks.setFeedFilter(FeedFilterType.ALL_FEEDS, 0, this);
				else
					UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, feedToView.getDatabaseId(), this);
				UICallbacks.handleCommand(FragmentActivityWithMenu.this, R.integer.command_news_list, null);
			}
		});
	}

	@Override
	public void addNew() {
		waitForMenuCloseAndRunCommand(new Runnable()
		{
			@Override
			public void run()
			{
				UICallbacks.handleCommand(FragmentActivityWithMenu.this, R.integer.command_feed_add, null);
			}
		});
	}
	
	private void waitForMenuCloseAndRunCommand(Runnable runnable)
	{
		mDeferredCommands.add(runnable);
		if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START))
		{
			mDrawerLayout.closeDrawer(Gravity.START);
		}
		else
		{
			runDeferredCommands();
		}
	}
	
	private void runDeferredCommands()
	{
		for (Runnable r : mDeferredCommands)
			r.run();
		mDeferredCommands.clear();
	}
	
	@Override
	public void onFeedSelect(FeedFilterType type, long feedId, Object source)
	{
		if (mMenuViewHolder != null)
			mMenuViewHolder.viewFeedFilter.invalidateViews();
	}
	
	@Override
	public void onTagSelect(String tag)
	{
	}

	@Override
	public void onRequestResync(Feed feed)
	{
	}

	@Override
	public void onItemFavoriteStatusChanged(Item item)
	{
	}

	@Override
	public void onCommand(int command, Bundle commandParameters)
	{
	}
	
	/**
	 * This is a shortcut to {@link App#getCurrentFeedFeedFilterType()} }
	 * @return the currently displayed feed type
	 */
	protected FeedFilterType getCurrentFeedFilterType()
	{
		return App.getInstance().getCurrentFeedFilterType();
	}
	
	/**
	 * This is a shortcut to {@link App#getCurrentFeed()} }
	 * @return the currently displayed feed (if any)
	 */
	protected Feed getCurrentFeed()
	{
		return App.getInstance().getCurrentFeed();
	}
}
