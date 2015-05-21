package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
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

import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.Feed;

public class StoryListView extends RelativeLayout implements OnTagClickedListener, OnPullDownListener, OnHeaderCreatedListener 
{
	public static final String LOGTAG = "StoryListView";
	public static final boolean LOGGING = false;
	
	public interface StoryListListener
	{
		/**
		 * Called when list has been pulled down to resync.
		 */
		void onResync();

		void onStoryClicked(StoryListAdapter adapter, int indexOfStory, View storyView);

		void onHeaderCreated(View headerView, int resIdHeader);
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
		//if (mAdapter == null)
		//	createOrUpdateAdapter(getContext(), null, 0);
		//mListStories.setAdapter(mAdapter);
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
		
		searchByTag(null, null);
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
				searchByTag(mFeed, tag);
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

	private void searchByTag(Feed feed, String tag)
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
		if (mAdapter != null)
			mAdapter.setTagFilter(feed, tag);
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

	public void setAdapter(StoryListAdapter adapter)
	{
		if (mAdapter != adapter)
		{
			mAdapter = adapter;
			if (mAdapter != null)
			{
				mAdapter.setListener(mListener);
				mAdapter.setOnTagClickedListener(this);
				mAdapter.setOnHeaderCreatedListener(this);
			}
			mListStories.setAdapter(mAdapter);
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

	public Point saveScrollPosition()
	{
		int index = mListStories.getFirstVisiblePosition();
		View v = mListStories.getChildAt(0);
		int top = (v == null) ? 0 : (v.getTop() - mListStories.getPaddingTop());
		return new Point(index, top);
	}
	
	public void restoreScrollPosition(Point pt)
	{
		mListStories.setSelectionFromTop(pt.x, pt.y);
	}
}
