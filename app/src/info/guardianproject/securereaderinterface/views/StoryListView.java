package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter.OnHeaderCreatedListener;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter.OnTagClickedListener;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.UICallbacks.OnCallbackListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.AppearingFrameLayout;
import info.guardianproject.securereaderinterface.widgets.AppearingRelativeLayout;
import info.guardianproject.securereaderinterface.widgets.HeightLimitedLinearLayout;
import info.guardianproject.securereaderinterface.widgets.SyncableListView;
import info.guardianproject.securereaderinterface.widgets.SyncableListView.OnPullDownListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import info.guardianproject.yakreader.R;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class StoryListView extends FrameLayout implements OnTagClickedListener, OnPullDownListener, OnHeaderCreatedListener 
{
	public interface StoryListListener
	{
		/**
		 * Called when list has been pulled down to resync.
		 */
		void onResync();

		void onStoryClicked(ArrayList<Item> items, int indexOfStory, View storyView);

		void onHeaderCreated(View headerView, int resIdHeader);

		void onListViewUpdated(ListView newList);
	}

	private TextView mTvTagResults;
	private View mBtnCloseTagSearch;

	private SyncableListView mListStories;
	private HeightLimitedLinearLayout mListHeader;
	private OnCallbackListener mCallbackListener;
	private StoryListAdapter mAdapter;
	private String mCurrentSearchTag;

	private StoryListListener mListener;
	private boolean mIsLoading;
	private AppearingFrameLayout mFrameLoading;
	private AppearingRelativeLayout mFrameError;
	private View mIvLoading;

	private long mOldItemId = Integer.MIN_VALUE;
	private int mOldY;
	
	public StoryListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	public StoryListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public StoryListView(Context context)
	{
		super(context);
		init();
	}

	private void init()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		View rootView = inflater.inflate(R.layout.story_list, this, true);

		mTvTagResults = (TextView) rootView.findViewById(R.id.tvTagResults);
		mBtnCloseTagSearch = rootView.findViewById(R.id.btnCloseTagSearch);
		mTvTagResults.setVisibility(View.GONE);
		mFrameLoading = (AppearingFrameLayout) rootView.findViewById(R.id.frameLoading);
		mFrameError = (AppearingRelativeLayout) rootView.findViewById(R.id.frameError);
		mFrameError.findViewById(R.id.ivErrorClose).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hideError();
			}
		});
		mIvLoading = rootView.findViewById(R.id.ivLoading);
		mBtnCloseTagSearch.setVisibility(View.GONE);
		mBtnCloseTagSearch.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Clear tag filter
				UICallbacks.setTagFilter(null, null);
			}
		});

		mListStories = (SyncableListView) rootView.findViewById(R.id.lvStories);
		if (mAdapter == null)
			createOrUpdateAdapter(getContext(), null, 0);
		mListStories.setAdapter(mAdapter);
		mListHeader = (HeightLimitedLinearLayout) rootView.findViewById(R.id.storyListHeader);
		mListHeader.setVisibility(View.INVISIBLE);
		mFrameLoading.setVisibility(View.GONE);
		mFrameError.setVisibility(View.GONE);
		hideHeaderControls(true);
		setIsLoading(mIsLoading);

		mListStories.setPullDownListener(this);

		mListStories.setOnScrollListener(new AbsListView.OnScrollListener()
		{
			private boolean isFlinging = false;
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				if (scrollState == OnScrollListener.SCROLL_STATE_FLING)
				{
					isFlinging = true;
					mAdapter.setDeferMediaLoading(true);
				}
				else
				{
					mAdapter.setDeferMediaLoading(false);
					if (isFlinging)
					{
						isFlinging = false;
						mAdapter.notifyDataSetChanged();
					}
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
			}
		});
		
		searchByTag(null);
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		addCallbackListener();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		removeCallbackListener();
		super.onDetachedFromWindow();
	}

	public void setListener(StoryListListener listener)
	{
		mListener = listener;
		if (mAdapter != null)
			mAdapter.setListener(mListener);
	}

	public ListView getListView()
	{
		return mListStories;
	}

	private void addCallbackListener()
	{
		mCallbackListener = new UICallbackListener()
		{
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				if (type != FeedFilterType.SINGLE_FEED)
				{
					mAdapter.setShowTags(false);
					mListStories.invalidateViews();
				}
				else
				{
					if (App.UI_ENABLE_TAGS)
						mAdapter.setShowTags(true);
					mListStories.invalidateViews();
				}
			}

			@Override
			public void onTagSelect(String tag)
			{
				searchByTag(tag);
			}

		};
		UICallbacks.getInstance().addListener(mCallbackListener);
	}

	private void removeCallbackListener()
	{
		if (mCallbackListener != null)
			UICallbacks.getInstance().removeListener(mCallbackListener);
		mCallbackListener = null;
	}

	private void searchByTag(String tag)
	{
		mCurrentSearchTag = tag;
		if (mCurrentSearchTag == null)
		{
			mTvTagResults.setVisibility(View.GONE);
			mBtnCloseTagSearch.setVisibility(View.GONE);
		}
		else
		{
			mTvTagResults.setText(UIHelpers.setSpanBetweenTokens(getContext().getString(R.string.story_item_short_tag_results, tag), "##",
					new ForegroundColorSpan(getContext().getResources().getColor(R.color.accent))));
			mTvTagResults.setVisibility(View.VISIBLE);
			mBtnCloseTagSearch.setVisibility(View.VISIBLE);
		}
		mAdapter.setTagFilter(tag);
	}

	@Override
	public void onTagClicked(String tag)
	{
		UICallbacks.setTagFilter(tag, null);
	}

	private int mHeaderState; // 0 = hidden, 1 = shown, 2 = fully shown, 3 = No net
	private Feed mFeed;

	@Override
	public void onListPulledDown(int heightVisible)
	{
		int newState = 1;
		if (heightVisible == 0)
			newState = 0;
		else if (heightVisible == mListHeader.getHeight())
			newState = 2;

		// Offline mode?
		if (mHeaderState == 0 && newState != 0)
		{
			int onlineMode = App.getInstance().socialReader.isOnline();
			if (onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK)
				newState = 3;
		}
		else if (mHeaderState == 3 && newState != 0)
		{
			newState = 3;
		}

		View arrow = mListHeader.findViewById(R.id.arrow);
		View sadface = mListHeader.findViewById(R.id.sadface);

		if (mHeaderState != newState)
		{
			hideHeaderControls(newState == 0);

			switch (newState)
			{
			case 0:
				break;

			case 1:
			{
				TextView tv = (TextView) mListHeader.findViewById(R.id.tvHeader);
				if (mFeed != null && mFeed.getNetworkPullDate() != null)
				{
					Date synced = new Date();
					synced = mFeed.getNetworkPullDate();
					String lastSyncedAt = UIHelpers.dateDiffDisplayString(synced, getContext(), R.string.last_synced_never, R.string.last_synced_recently,
							R.string.last_synced_minutes, R.string.last_synced_minute, R.string.last_synced_hours, R.string.last_synced_hour,
							R.string.last_synced_days, R.string.last_synced_day);
					tv.setText(lastSyncedAt);
				}
				else
				{
					tv.setText(R.string.pulldown_to_sync);
				}
				break;
			}

			case 2:
			{
				TextView tv = (TextView) mListHeader.findViewById(R.id.tvHeader);
				tv.setText(R.string.release_to_sync);
				break;
			}

			case 3:
			{
				arrow.setVisibility(View.GONE);
				sadface.setVisibility(View.VISIBLE);
				TextView tv = (TextView) mListHeader.findViewById(R.id.tvHeader);
				tv.setText(R.string.pulldown_to_sync_no_net);
				break;
			}
			}
		}

		mHeaderState = newState;
		if (newState != 3)
		{
			arrow.setVisibility(View.VISIBLE);
			sadface.setVisibility(View.GONE);
		}
		//float degrees = 180.0f * (heightVisible / (float) mListHeader.getHeight());
		mListHeader.setDrawHeightLimit(heightVisible);
		// AnimationHelpers.rotate(arrow, degrees, degrees, 0);
	}

	private void hideHeaderControls(boolean hide)
	{
		if (hide)
		{
			mListHeader.findViewById(R.id.arrow).setVisibility(View.INVISIBLE);
			mListHeader.findViewById(R.id.sadface).setVisibility(View.INVISIBLE);
			mListHeader.findViewById(R.id.tvHeader).setVisibility(View.INVISIBLE);
			mListHeader.setBackgroundColor(Color.TRANSPARENT);
		}
		else
		{
			mListHeader.findViewById(R.id.arrow).setVisibility(View.VISIBLE);
			mListHeader.findViewById(R.id.sadface).setVisibility(View.VISIBLE);
			mListHeader.findViewById(R.id.tvHeader).setVisibility(View.VISIBLE);
			mListHeader.setBackgroundColor(0xffffffff);
		}
	}

	@Override
	public void onListDroppedWhilePulledDown()
	{
		if (mListener != null)
			mListener.onResync();
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		post(new Runnable()
		{
			@Override
			public void run() {
				// Remember old scroll position, so we can restore that after
				// orientation change!
				//
				saveListPosition(false);
				removeAllViews();
				init();
				if (mAdapter != null)
					mAdapter.notifyDataSetChanged();
					
				// Tell our listener that we recreated the list view!
				if (mListener != null)
					mListener.onListViewUpdated(mListStories);
			}
		});
	}

	private void createOrUpdateAdapter(Context context, ArrayList<Item> items, int headerView)
	{
		if (mAdapter == null)
		{
			mAdapter = new StoryListAdapter(context, null);
			mAdapter.setListener(mListener);
			mAdapter.setOnTagClickedListener(this);
			mAdapter.registerDataSetObserver(new DataSetObserver()
			{
				@Override
				public void onChanged()
				{
					super.onChanged();
					scrollToSavedPosition();
				}
			});
		}
		mAdapter.setOnHeaderCreatedListener(this);
		mAdapter.setHeaderView(headerView, false);
		mAdapter.updateItems(sortItemsOnPublicationTime(items));
	}

	public void updateItems(Context context, ArrayList<Item> items, int headerView, final boolean rememberPosition)
	{
		// Remember old position so we can restore it after update if we want.
		saveListPosition(!rememberPosition);
		createOrUpdateAdapter(context, items, headerView);
	}

	private void saveListPosition(boolean reset) 
	{
		long oldItemId = -1;
		int oldY = 0;
		if (mListStories != null && !reset && mListStories.getCount() > 0)
		{
			int oldIndex = mListStories.getFirstVisiblePosition();
			if (oldIndex != -1)
				oldItemId = mListStories.getAdapter().getItemId(oldIndex);
			View child = mListStories.getChildAt(0);
			if (child != null)
				oldY = child.getTop();
			Log.v(MainActivity.LOGTAG, "Remember list position " + oldItemId
					+ "," + oldY);
		}
		mOldItemId = oldItemId;
		mOldY = oldY;
	}
	
	private void scrollToSavedPosition()
	{
		if (mOldItemId != Integer.MIN_VALUE)
		{
			int index = 0;
			int offset = 0;
			
			Log.v(MainActivity.LOGTAG, "Scrolling list back to item " + mOldItemId + "," + mOldY);
			if (mOldItemId != -1)
			{
				ListAdapter adapter = mListStories.getAdapter();
				for (int iItem = 0; iItem < adapter.getCount(); iItem++)
				{
					if (adapter.getItemId(iItem) == mOldItemId)
					{
						index = iItem;
						offset = mOldY;
						break;
					}
				}
			}
			mListStories.setSelectionFromTop(index, offset);
			mOldItemId = Integer.MIN_VALUE;
		}
	}

	public void setIsLoading(boolean loading)
	{
		mIsLoading = loading;
		if (loading)
		{
			if (mListStories != null)
			{
				mListStories.setHeaderEnabled(false);
			}
			if (mListHeader != null)
			{
				mListHeader.setVisibility(View.INVISIBLE);
			}
			if (mFrameLoading != null)
				mFrameLoading.expand();
			if (mIvLoading != null)
				mIvLoading.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
		}
		else
		{
			if (mFrameLoading != null)
				mFrameLoading.collapse();
			if (mIvLoading != null)
				mIvLoading.clearAnimation();
			if (mListStories != null && mListHeader != null)
			{
				mListHeader.setVisibility(View.VISIBLE);
				mListStories.setHeaderEnabled(true);
			}
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		setIsLoading(mIsLoading);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (changed)
			setIsLoading(mIsLoading);
		if (mListStories != null)
			mListStories.setHeaderHeight(mListHeader.getHeight());
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
		if (mListener != null)
			mListener.onHeaderCreated(headerView, resIdHeader);
	}

	private ArrayList<Item> sortItemsOnPublicationTime(ArrayList<Item> unsortedItems)
	{
		if (unsortedItems == null)
			return null;

		ArrayList<Item> items = new ArrayList<Item>(unsortedItems);
		Collections.sort(items, new Comparator<Item>()
		{
			@Override
			public int compare(Item i1, Item i2)
			{
				if (i1.equals(i2))
					return 0;
				else if (i1.getPublicationTime() == null && i2.getPublicationTime() == null)
					return 0;
				else if (i1.getPublicationTime() == null)
					return 1;
				else if (i2.getPublicationTime() == null)
					return -1;
				return i2.getPublicationTime().compareTo(i1.getPublicationTime());
			}
		});
		return items;
	}

	public void showError(String error)
	{
		if (mFrameError != null)
		{
			TextView tv = (TextView) mFrameError.findViewById(R.id.tvError);
			if (tv != null)
				tv.setText(error);
			mFrameError.expand();
		}
	}

	public void hideError()
	{
		if (mFrameError != null)
			mFrameError.collapse();
	}

	public void setCurrentFeed(Feed feed) 
	{
		mFeed = feed;
	}
}
