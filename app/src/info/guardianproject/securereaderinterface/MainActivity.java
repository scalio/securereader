package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings.SyncMode;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncService;
import info.guardianproject.securereader.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.ActionProviderFeedFilter;
import info.guardianproject.securereaderinterface.ui.ActionProviderShare;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.UICallbacks.OnCallbackListener;
import info.guardianproject.securereaderinterface.views.StoryListHintTorView;
import info.guardianproject.securereaderinterface.views.StoryListView;
import info.guardianproject.securereaderinterface.views.StoryListHintTorView.OnButtonClickedListener;
import info.guardianproject.yakreader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

// HockeyApp SDK
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

public class MainActivity extends ItemExpandActivity implements OnSharedPreferenceChangeListener
{
	public static String INTENT_EXTRA_SHOW_THIS_TYPE = "info.guardianproject.securereaderinterface.showThisFeedType";
	public static String INTENT_EXTRA_SHOW_THIS_FEED = "info.guardianproject.securereaderinterface.showThisFeedId";
	public static String INTENT_EXTRA_SHOW_THIS_ITEM = "info.guardianproject.securereaderinterface.showThisItemId";

	public static String LOGTAG = "MainActivity";
	
	// HockeyApp SDK
	public static String APP_ID = "NOT FOR MASTER BRANCH";

	private boolean mIsInitialized;
	private long mShowItemId;
	private long mShowFeedId;
	private FeedFilterType mShowFeedFilterType;
	SocialReader socialReader;

	OnCallbackListener mCallbackListener;

	/*
	 * The action bar menu item for the "TAG" option. Only show this when a feed
	 * filter is set.
	 */
	MenuItem mMenuItemTag;
	boolean mShowTagMenuItem;
	MenuItem mMenuItemShare;
	MenuItem mMenuItemFeed;

	ActionProviderFeedFilter mAPFeedFilter;
	FeedFilterType mFeedFilterType;
	Feed mFeed;
	ActionProviderShare mShareActionProvider;

	StoryListView mStoryListView;

	boolean mIsLoading;
	private SyncMode mCurrentSyncMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mCurrentSyncMode = App.getSettings().syncMode();

		// We do a little song and dance number here - This activity's theme is
		// set to NoActionBar in the manifest, but here we change to default app
		// theme again and request the action bar. This is because, at first
		// startup, the system will show a screen with default action bar and
		// default background. We don't want that. Instead we want to show solid
		// color (same as lock screen background) and no action bar. See
		// AppThemeNoActionBar theme for more information.
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setTheme(R.style.AppTheme);

		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();

		addUICallbackListener();

		setContentView(R.layout.activity_main);
		setMenuIdentifier(R.menu.activity_main);

		mStoryListView = (StoryListView) findViewById(R.id.storyList);
		mStoryListView.setListener(this);

		socialReader = ((App) getApplicationContext()).socialReader;
		socialReader.setSyncServiceListener(new SyncService.SyncServiceListener()
		{
			@Override
			public void syncEvent(SyncService.SyncTask syncTask)
			{
				Log.v(LOGTAG, "Got a syncEvent");
				if (syncTask.type == SyncService.SyncTask.TYPE_FEED && syncTask.status == SyncService.SyncTask.FINISHED)
				{
					refreshListIfCurrent(syncTask.feed);
				}
			}
		});

		createFeedSpinner();
		updateList(FeedFilterType.ALL_FEEDS, null);
		
		// HockeyApp SDK
		checkForUpdates();
	}

	private void createFeedSpinner()
	{
		mAPFeedFilter = new ActionProviderFeedFilter(this);
		getSupportActionBar().setCustomView(mAPFeedFilter.onCreateActionView());
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		removeUICallbackListener();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// If we are in the process of displaying the lock screen isFinishing is
		// actually
		// true, so avoid extra work!
		if (!isFinishing())
		{
			// If we have not shown help yet, open that on top
			if (!App.getSettings().hasShownHelp())
			{
				Intent intent = new Intent(this, HelpActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra("useLeftSideMenu", false);
				startActivity(intent);
			}
			else
			{
				if (!mIsInitialized)
				{
					mIsInitialized = true;
					UICallbacks.setFeedFilter(FeedFilterType.ALL_FEEDS, 0, MainActivity.this);
					getSupportActionBar().show();
				}
			}

		}

		addSettingsChangeListener();

		// Called with flags of which item to show?
		Intent intent = getIntent();
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_ITEM) && intent.hasExtra(INTENT_EXTRA_SHOW_THIS_FEED))
		{
			this.mShowFeedId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_FEED, 0);
			this.mShowItemId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_ITEM, 0);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_FEED);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_ITEM);
		}
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_TYPE))
		{
			this.mShowFeedFilterType = (FeedFilterType) intent.getSerializableExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
		}
		else if (socialReader.getDefaultFeedId() >= 0) 
		{
			this.mShowFeedId = socialReader.getDefaultFeedId();
		}
		else
		{
			this.mShowFeedFilterType = null;
		}

		if (this.mShowFeedFilterType != null)
		{
			Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_TYPE was set, show type " + this.mShowFeedFilterType.toString());
			UICallbacks.setFeedFilter(this.mShowFeedFilterType, -1, MainActivity.this);
			this.mShowFeedFilterType = null;
		}
		else if (this.mShowFeedId != 0)
		{
			Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_FEED was set, show feed id " + this.mShowFeedId);
			UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, this.mShowFeedId, MainActivity.this);
		}

		// Resume sync if we are back from Orbot
		updateTorView();
		
		// HockeyApp SDK
		checkForCrashes();
	}
	
	// HockeyApp SDK
	private void checkForCrashes() {
		CrashManager.register(this, APP_ID);
	}	
	
	// HockeyApp SDK
	private void checkForUpdates() {
		//Remove this for store builds!
		UpdateManager.register(this, APP_ID);
	}	

	@Override
	public void onPause()
	{
		super.onPause();
		removeSettingsChangeListener();
	}
	
	@Override
	protected void onAfterResumeAnimation()
	{
		super.onAfterResumeAnimation();
		if (!isFinishing() && App.getSettings().hasShownHelp())
		{
			boolean willShowMenuHint = false;
			if (mLeftSideMenu != null)
				willShowMenuHint = mLeftSideMenu.showMenuHintIfNotShown();
			if (socialReader.getFeedsList().size() > 0)
			{
				if (willShowMenuHint)
				{
					// Allow the menu animation some time before we start the
					// heavy work!
					mStoryListView.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							refreshList();
						}
					}, 6000);
				}
				else
				{
					refreshList();
				}
			}
		}

	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void syncSpinnerToCurrentItem()
	{
		if (mFeedFilterType == FeedFilterType.ALL_FEEDS)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_all_feeds));
		else if (mFeedFilterType == FeedFilterType.POPULAR)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_popular));
		else if (mFeedFilterType == FeedFilterType.SHARED)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_shared_stories));
		else if (mFeedFilterType == FeedFilterType.FAVORITES)
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_favorites));
		else if (mFeed != null)
			mAPFeedFilter.setCurrentTitle(mFeed.getTitle());
		else
			mAPFeedFilter.setCurrentTitle(getString(R.string.feed_filter_all_feeds));
	}

	private Feed getFeedById(long idFeed)
	{
		ArrayList<Feed> items = socialReader.getSubscribedFeedsList();
		for (Feed feed : items)
		{
			if (feed.getDatabaseId() == idFeed)
				return feed;
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		// Find the tag menu item. Only to be shown when feed filter is set!
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		mMenuItemTag.setVisible(mShowTagMenuItem);

		// Locate MenuItem with ShareActionProvider
		mMenuItemShare = menu.findItem(R.id.menu_share);
		if (mMenuItemShare != null)
		{
			mShareActionProvider = new ActionProviderShare(this);
			mShareActionProvider.setFeed(mFeed);
			mMenuItemShare.setActionProvider(mShareActionProvider);
		}
		// Locate MenuItem with ShareActionProvider
		// mMenuItemFeed = menu.findItem(R.id.menu_feed);
		// if (mMenuItemFeed != null)
		// {
		// mMenuItemFeed.setActionProvider(new ActionProviderFeedFilter(this));
		// }

		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_tag:
		{
			showTagSearchPopup(getSupportActionBar().getCustomView());
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	private void setTagItemVisible(boolean bVisible)
	{
		mShowTagMenuItem = bVisible && App.UI_ENABLE_TAGS;
		if (mMenuItemTag != null)
		{
			mMenuItemTag.setVisible(mShowTagMenuItem);
			if (Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
		}
	}

	@SuppressLint("NewApi")
	private void addUICallbackListener()
	{
		mCallbackListener = new UICallbackListener()
		{
			@SuppressLint("NewApi")
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				Feed feed = null;
				boolean visibleTags = false;
				if (type == FeedFilterType.SINGLE_FEED)
				{
					feed = getFeedById(feedId);
					if (feed != null)
					{
						visibleTags = true;
					}
				}
				setTagItemVisible(visibleTags);
				updateList(type, feed);
			}

			@Override
			public void onItemFavoriteStatusChanged(Item item)
			{
				// An item has been marked/unmarked as favorite. Update the list
				// of favorites to pick
				// up this change!
				if (mFeedFilterType == FeedFilterType.FAVORITES)
					refreshList();
			}

			@Override
			public void onCommand(int command, Bundle commandParameters)
			{
				switch (command)
				{
				case R.integer.command_add_feed_manual:
				{
					// First add it to reader!
					App.getInstance().socialReader.addFeedByURL(commandParameters.getString("uri"), MainActivity.this.mFeedFetchedCallback);
					refreshList();
					break;
				}
				}
			}

			@Override
			public void onRequestResync(Feed feed)
			{
				onResync(feed, (mFeedFilterType == FeedFilterType.ALL_FEEDS || mFeedFilterType == FeedFilterType.SINGLE_FEED)
						&& mFeed == feed); // Only show
																	// spinner
																	// if
																	// updating
																	// current
																	// feed
			}
		};
		UICallbacks.getInstance().addListener(mCallbackListener);
	}

	private void removeUICallbackListener()
	{
		if (mCallbackListener != null)
			UICallbacks.getInstance().removeListener(mCallbackListener);
		mCallbackListener = null;
	}

	private void addSettingsChangeListener()
	{
		App.getSettings().registerChangeListener(this);
	}

	private void removeSettingsChangeListener()
	{
		App.getSettings().unregisterChangeListener(this);
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = getLayoutInflater();
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.story_search_by_tag, null, false), this.mStoryListView.getWidth(),
					this.mStoryListView.getHeight(), true);

			ListView lvTags = (ListView) mMenuPopup.getContentView().findViewById(R.id.lvTags);

			String[] rgTags = new String[0];
			// rgTags[0] = "#one";
			// rgTags[1] = "#two";
			// rgTags[2] = "#three";
			// rgTags[3] = "#four";

			ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.story_search_by_tag_item, R.id.tvTag, rgTags);
			lvTags.setAdapter(adapter);
			lvTags.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
				{
					String tag = (String) arg0.getAdapter().getItem(position);
					UICallbacks.setTagFilter(tag, null);
					mMenuPopup.dismiss();
				}
			});

			EditText editTag = (EditText) mMenuPopup.getContentView().findViewById(R.id.editTag);
			editTag.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
				{
					if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH)
					{
						UICallbacks.setTagFilter(v.getText().toString(), null);
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			mMenuPopup.showAsDropDown(anchorView);
			mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuPopup.dismiss();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onResync()
	{
		if (mFeedFilterType == FeedFilterType.SHARED)
			refreshList();
		else
			onResync(mFeed, true);
	}

	private void onResync(Feed feed, boolean showLoadingSpinner)
	{
		if (socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_TOR)
		{
			socialReader.connectTor(this);
		}

		if (socialReader.isOnline() == SocialReader.ONLINE)
		{
			setIsLoading(showLoadingSpinner);
			if (feed == null)
				socialReader.manualSyncSubscribedFeeds(mFeedFetchedCallback);
			else
				socialReader.manualSyncFeed(feed, mFeedFetchedCallback);
		}
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		if (mMenuItemFeed != null)
			mMenuItemFeed.setVisible(!fullscreen);
		if (mMenuItemShare != null)
			mMenuItemShare.setVisible(!fullscreen);

		if (!fullscreen)
		{
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			setDisplayHomeAsUp(false);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			setDisplayHomeAsUp(true);
		}
	}

	private void setIsLoading(boolean isLoading)
	{
		mIsLoading = isLoading;
		if (mStoryListView != null)
			mStoryListView.setIsLoading(mIsLoading);
		updateTorView();
	}

	private void showError(String error)
	{
		if (mStoryListView != null)
		{
			if (TextUtils.isEmpty(error))
				mStoryListView.hideError();
			else
				mStoryListView.showError(error);
		}
	}
	
	private UpdateFeedListTask mUpdateListTask;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	private void updateList(FeedFilterType feedFilterType, Feed optionalFeed)
	{
		boolean isUpdate = (feedFilterType == mFeedFilterType);
		
		mFeedFilterType = feedFilterType;
		mFeed = optionalFeed;
		
		if (!isUpdate)
			setIsLoading(true);
		
		if (mUpdateListTask != null)
			mUpdateListTask.cancel(true);
		mUpdateListTask = new UpdateFeedListTask(this, mFeedFilterType, optionalFeed, isUpdate);
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		//	mUpdateListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		//else
			mUpdateListTask.execute();

		syncSpinnerToCurrentItem();
		if (mShareActionProvider != null)
			mShareActionProvider.setFeed(mFeed);
		mStoryListView.setCurrentFeed(mFeed);
	}

	private void refreshList()
	{
		updateList(mFeedFilterType, mFeed);
	}
	
	private void refreshListIfCurrent(Feed feed)
	{
		if (mFeedFilterType == FeedFilterType.ALL_FEEDS)
		{
			refreshList();
		}
		else if (mFeedFilterType == FeedFilterType.SINGLE_FEED && mFeed != null && mFeed.getDatabaseId() == feed.getDatabaseId())
		{
			mFeed = feed;
			refreshList();
		}
	}
	
	private void checkShowStoryFullScreen(ArrayList<Item> items)
	{
		if (mShowItemId != 0)
		{
			Log.v(LOGTAG, "Loaded feed and INTENT_EXTRA_SHOW_THIS_ITEM was set to " + mShowItemId + ". Try to show it");
			for (int itemIndex = 0; itemIndex < items.size(); itemIndex++)
			{
				Item item = items.get(itemIndex);
				if (item.getDatabaseId() == mShowItemId)
				{
					Log.v(LOGTAG, "Found item at index " + itemIndex);
					this.openStoryFullscreen(items, itemIndex, mStoryListView.getListView(), null);
				}
			}
			mShowFeedId = 0;
			mShowItemId = 0;
		}
	}

	private ArrayList<Item> flattenFeedArray(ArrayList<Feed> listOfFeeds)
	{
		ArrayList<Item> items = new ArrayList<Item>();
		if (listOfFeeds != null)
		{
			Iterator<Feed> itFeed = listOfFeeds.iterator();
			while (itFeed.hasNext())
			{
				Feed feed = itFeed.next();
				Log.v(LOGTAG, "Adding " + feed.getItemCount() + " items");
				items.addAll(feed.getItems());
			}
		}
		Log.v(LOGTAG, "There are " + items.size() + " items total");
		return items;
	}

	private boolean showErrorForFeed(Feed feed, boolean onlyRemoveIfAllOk)
	{
		if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_404)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_404));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_bad_url));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_unknown));
		}
		else
		{
			this.showError(null);
			return false;
		}
		return true;
	}

	private final FeedFetchedCallback mFeedFetchedCallback = new FeedFetchedCallback()
	{
		@Override
		public void feedFetched(Feed _feed)
		{
			Log.v(LOGTAG, "feedFetched Callback");
			refreshListIfCurrent(_feed);
		}
	};
	private StoryListHintTorView mTorView;


	@Override
	protected boolean onCommand(int command, Bundle commandParameters)
	{
		if (command == R.integer.command_resync)
		{
			onResync();
			return true;
		}
		return super.onCommand(command, commandParameters);
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
		if (resIdHeader == R.layout.story_list_hint_tor)
		{
			mTorView = (StoryListHintTorView) headerView;
			updateTorView();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.v(LOGTAG, "The setting " + key + " has changed.");
		// if (Settings.KEY_SYNC_MODE.equals(key))
		// {
		// }
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		// Probably opening a popup (Feed Spinner). Remember what sync mode was
		// set to when we open.
		if (!hasFocus)
		{
			mCurrentSyncMode = App.getSettings().syncMode();
		}
		else
		{
			if (mCurrentSyncMode != App.getSettings().syncMode())
			{
				mCurrentSyncMode = App.getSettings().syncMode();
				refreshList();
			}
		}
	}

	private void updateTorView()
	{
		if (mTorView == null)
			return;

		if (!App.getSettings().requireTor() || mIsLoading)
		{
			mTorView.setVisibility(View.GONE);
		}
		else
		{
			mTorView.setOnButtonClickedListener(new OnButtonClickedListener()
			{
				private StoryListHintTorView mView;

				@Override
				public void onNoNetClicked()
				{
					int onlineMode = App.getInstance().socialReader.isOnline();
					mView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
							onlineMode == SocialReader.ONLINE);
				}

				@Override
				public void onGoOnlineClicked()
				{
					onResync();
				}

				public OnButtonClickedListener init(StoryListHintTorView view)
				{
					mView = view;
					return this;
				}
			}.init(mTorView));
			int onlineMode = App.getInstance().socialReader.isOnline();
			mTorView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
					onlineMode == SocialReader.ONLINE);
			mTorView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, -1, this);
	}

	class UpdateFeedListTask extends ThreadedTask<Void, Void, ArrayList<Feed>>
	{
		private Context mContext;
		private FeedFilterType mFeedFilterType;
		private Feed mFeed;
		private boolean mIsUpdate;

		public UpdateFeedListTask(Context context, FeedFilterType feedFilterType, Feed feed, boolean isUpdate)
		{
			mContext = context;
			mFeedFilterType = feedFilterType;
			mFeed = feed;
			mIsUpdate = isUpdate;
		}

		@Override
		protected ArrayList<Feed> doInBackground(Void... values)
		{
			Log.v(LOGTAG, "UpdateFeedListTask: doInBackground");

			ArrayList<Feed> listOfFeeds = null;

			if (mFeedFilterType == FeedFilterType.SHARED)
			{
				listOfFeeds = socialReader.getAllShared();
			}
			else if (mFeedFilterType == FeedFilterType.FAVORITES)
			{
				listOfFeeds = socialReader.getAllFavorites();
			}
			else if (mFeedFilterType == FeedFilterType.ALL_FEEDS || mFeed == null)
			{
				Log.v(LOGTAG, "UpdateFeedsTask: all subscribed");
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getSubscribedFeedItems());
			}
			else
			{
				Log.v(LOGTAG, "UpdateFeedsTask: " + mFeed.getTitle());
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getFeed(mFeed));
			}
			return listOfFeeds;
		}

		@Override
		protected void onPostExecute(ArrayList<Feed> result)
		{
			Log.v(LOGTAG, "RefreshFeedsTask: finished");

			switch (mFeedFilterType)
			{
			case ALL_FEEDS:
			case SINGLE_FEED:
			{
				Feed _feed = result.get(0);
				if (mFeed != null && mFeed.getDatabaseId() == _feed.getDatabaseId())
					mFeed = _feed; // need to update to get NetworkPullDate

				// Any errors to show?
				showError(null);
				for (Feed feed : result)
				{
					if (feed != null)
					{
						if (showErrorForFeed(feed, !mIsLoading))
							break;
					}
				}

				int headerViewId = 0;
				ArrayList<Item> items = result.get(0).getItems();
				if (items == null || items.size() == 0)
				{
					headerViewId = R.layout.story_list_hint_tor;
				}
				mStoryListView.updateItems(mContext, items, headerViewId, mIsUpdate);
				checkShowStoryFullScreen(items);
			}
				break;

			case FAVORITES:
			{
				ArrayList<Item> favoriteItems = new ArrayList<Item>();
				if (result != null)
				{
					Iterator<Feed> itFeed = result.iterator();
					while (itFeed.hasNext())
					{
						Feed feed = itFeed.next();
						favoriteItems.addAll(feed.getItems());
					}
				}

				boolean fakedItems = false;
				if (favoriteItems.size() == 0)
				{
					// No real favorites, so we fake some by randomly picking
					// Items from
					// the "all items" feed
					fakedItems = true;

					Feed allSubscribed = App.getInstance().socialReader.getSubscribedFeedItems();
					if (allSubscribed != null)
					{
						ArrayList<Item> allSubscribedItems = allSubscribed.getItems();

						// Truncate to random 5 items
						Collections.shuffle(allSubscribedItems);
						favoriteItems.addAll(allSubscribedItems.subList(0, Math.min(5, allSubscribedItems.size())));
					}
				}

				boolean shouldShowAddFavoriteHint = fakedItems || favoriteItems == null || favoriteItems.size() == 0;
				mStoryListView.updateItems(mContext, favoriteItems, shouldShowAddFavoriteHint ? R.layout.story_list_hint_add_favorite : 0, mIsUpdate);
				break;
			}

			case POPULAR:
				// TODO
				break;

			case SHARED:
			{
				ArrayList<Item> items = flattenFeedArray(result);
				boolean shouldShowNoSharedHint = (items == null || items.size() == 0);
				mStoryListView.updateItems(mContext, items, shouldShowNoSharedHint ? R.layout.story_list_hint_no_shared : 0, mIsUpdate);
				checkShowStoryFullScreen(items);
			}
				break;
			}
			setIsLoading(false);
		}
	}

	@Override
	protected void onUnlocked() {
		super.onUnlocked();
		socialReader = ((App) getApplicationContext()).socialReader;
		createFeedSpinner();
		updateList(FeedFilterType.ALL_FEEDS, null);
	}
	
	
}
