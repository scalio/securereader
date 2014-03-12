package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereader.Settings.SyncFrequency;
import info.guardianproject.securereader.Settings.SyncMode;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.widgets.CheckableImageView;
import info.guardianproject.securereader.SocialReader;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import info.guardianproject.yakreader.R;
import com.tinymission.rss.Feed;

public class FeedFilterView extends ListView implements ListAdapter, OnItemClickListener
{
	private enum FeedFilterItemType
	{
		DISPLAY_PHOTOS(0), ALL_FEEDS(1), FAVORITES(2), POPULAR(3), SHARED(4), FEED(5);

		private final int value;

		private FeedFilterItemType(int value)
		{
			this.value = value;
		}
	}
	
	public interface FeedFilterViewCallbacks
	{
		void viewFavorites();

		void viewPopular();

		void viewDownloads();

		void viewShared();

		void viewFeed(Feed feedToView);

		void addNew();
	}

	private class ViewTag
	{
		public ViewTag()
		{
		}

		public View.OnClickListener clickListener;
		
		public CheckableImageView checkView;
		public TextView tvName;
		public TextView tvCount;
		public ImageView ivFeedImage;
		public View ivRefresh;
	}

	private FeedFilterViewCallbacks mCallbacks;
	private ArrayList<Feed> mListFeeds;
	private boolean mIsOnline; // Save this so we don't have to call it for
								// every view!
	private String mCountFavorites;
	private String mCountShared;
	private String mCountNumInProgress;

	public FeedFilterView(Context context)
	{
		super(context);
	}

	public FeedFilterView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void setFeedFilterViewCallbacks(FeedFilterViewCallbacks callbacks)
	{
		mCallbacks = callbacks;
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		if (!isInEditMode())
		{
			setOnItemClickListener(this);
			setItemsCanFocus(true);
			setAdapter(this);

			View btnAddFeeds = this.findViewById(R.id.btnAddFeeds);
			if (btnAddFeeds != null)
			{
				btnAddFeeds.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (mCallbacks != null)
							mCallbacks.addNew();
					}
				});
			}
		}
	}

	private boolean isOnline()
	{
		boolean isOnline = true;
		int onlineMode = App.getInstance().socialReader.isOnline();
		if (onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK)
			isOnline = false;
		return isOnline;
	}

	public void updateList(ArrayList<Feed> feeds)
	{
		mIsOnline = isOnline();
		mListFeeds = feeds;
		mCountFavorites = String.valueOf(App.getInstance().socialReader.getAllFavoritesCount());
		mCountShared = String.valueOf(App.getInstance().socialReader.getAllSharedCount());
		mCountNumInProgress = String.valueOf(DownloadsAdapter.getNumInProgress());
		invalidateViews();
	}
	
	private class RefreshFeed implements View.OnClickListener
	{
		private final Feed mFeed;

		public RefreshFeed(Feed feed)
		{
			mFeed = feed;
		}

		@Override
		public void onClick(View v)
		{
			UICallbacks.requestResync(mFeed);
			setAdapter(null);
			setAdapter(FeedFilterView.this);
		}
	}

	private class ViewFeed implements View.OnClickListener
	{
		private final Feed mFeed;

		public ViewFeed(Feed feed)
		{
			mFeed = feed;
		}

		@Override
		public void onClick(View v)
		{
			if (mCallbacks != null)
				mCallbacks.viewFeed(mFeed);
		}
	}

	private int getCountSpecials()
	{
		return 4 + (App.UI_ENABLE_POPULAR_ITEMS ? 1 : 0);
	}

	@Override
	public int getCount()
	{
		return getCountSpecials() + (mListFeeds != null ? mListFeeds.size() : 0);
	}

	@Override
	public Object getItem(int position)
	{
		if (position < getCountSpecials())
			return null;
		return mListFeeds.get(position - getCountSpecials());
	}

	@Override
	public long getItemId(int position)
	{
		if (position < getCountSpecials())
			return 0;
		return mListFeeds.get(position - getCountSpecials()).hashCode();
	}

	@Override
	public int getItemViewType(int position)
	{
		return getItemFeedFilterType(position).value;
	}
	
	private FeedFilterItemType getItemFeedFilterType(int position)
	{
		if (position == 0)
			return FeedFilterItemType.DISPLAY_PHOTOS;
		else if (position == 1)
			return FeedFilterItemType.ALL_FEEDS;
		else if (position == 2)
			return FeedFilterItemType.FAVORITES;
		else if (position == 3 && App.UI_ENABLE_POPULAR_ITEMS)
			return FeedFilterItemType.POPULAR;
		
		if (App.UI_ENABLE_POPULAR_ITEMS)
			position -= 1; // Offset 1 if popular is enabled

		if (position == 3)
			return FeedFilterItemType.SHARED;
		return FeedFilterItemType.FEED;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		FeedFilterItemType type = getItemFeedFilterType(position);

		View returnView = null;
		View.OnClickListener listener = null;

		switch (type)
		{
		case DISPLAY_PHOTOS:
		{
			if (convertView == null)
				convertView = createDisplayPhotosView();
			ViewTag holder = (ViewTag) convertView.getTag();
			
			holder.checkView.setChecked(App.getSettings().syncMode() == SyncMode.LetItFlow);
			listener = new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					ViewTag holder = (ViewTag) v.getTag();
					CheckableImageView view = holder.checkView;
					if (view != null)
						view.toggle();
					App.getSettings().setSyncMode(view.isChecked() ? SyncMode.LetItFlow : SyncMode.BitWise);
				}
			};
			returnView = convertView;
			break;
		}
		case FAVORITES:
		{
			if (convertView == null)
				convertView = createFavoritesView();
			ViewTag holder = (ViewTag) convertView.getTag();

			holder.tvCount.setText(mCountFavorites);
			listener = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mCallbacks != null)
						mCallbacks.viewFavorites();
				}
			};

			returnView = convertView;
			break;
		}
		case SHARED:
		{
			if (convertView == null)
				convertView = createSharedView();
			ViewTag holder = (ViewTag) convertView.getTag();
			
			holder.ivFeedImage.setImageResource(R.drawable.ic_share_receiver);
			holder.tvCount.setText(mCountShared);
			holder.tvName.setText(R.string.feed_filter_shared_stories);
			listener = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mCallbacks != null)
						mCallbacks.viewShared();
				}
			};

			returnView = convertView;
			break;
		}
		case ALL_FEEDS:
		{
			if (convertView == null)
				convertView = createAllFeedsView();
			ViewTag holder = (ViewTag) convertView.getTag();

			holder.ivFeedImage.setImageResource(R.drawable.ic_menu_news);
			// ivFeedImage.setVisibility(View.GONE);

			holder.ivRefresh.setVisibility(App.getSettings().syncFrequency() == SyncFrequency.Manual ? View.VISIBLE : View.GONE);
			holder.ivRefresh.setEnabled(mIsOnline);
			holder.ivRefresh.setOnClickListener(new RefreshFeed(null));
			if (holder.ivRefresh.getVisibility() == View.VISIBLE && App.getInstance().socialReader.manualSyncInProgress())
				holder.ivRefresh.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
			else
				holder.ivRefresh.clearAnimation();
			holder.tvName.setText(R.string.feed_filter_all_feeds);
			listener = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mCallbacks != null)
						mCallbacks.viewFeed(null);
				}
			};

			returnView = convertView;
			break;
		}
		case FEED:
		{
			Feed feed = mListFeeds.get(position - getCountSpecials());

			if (convertView == null)
				convertView = createAllFeedsView();
			ViewTag holder = (ViewTag) convertView.getTag();

			// Set image
			//
			holder.ivFeedImage.setVisibility(View.VISIBLE);

			// TODO - feed images

			holder.tvName.setText(feed.getTitle());
			if (TextUtils.isEmpty(feed.getTitle()))
				holder.tvName.setText(R.string.add_feed_not_loaded);

			holder.ivRefresh.setVisibility(App.getSettings().syncFrequency() == SyncFrequency.Manual ? View.VISIBLE : View.GONE);
			holder.ivRefresh.setEnabled(mIsOnline);
			holder.ivRefresh.setOnClickListener(new RefreshFeed(feed));
			if (holder.ivRefresh.getVisibility() == View.VISIBLE && feed.getStatus() == Feed.STATUS_SYNC_IN_PROGRESS)
				holder.ivRefresh.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
			else
				holder.ivRefresh.clearAnimation();
			listener = new ViewFeed(feed);

			returnView = convertView;
			break;
		}
		case POPULAR:
		{
			if (convertView == null)
				convertView = createFavoritesView();
			ViewTag holder = (ViewTag) convertView.getTag();

			holder.ivFeedImage.setImageResource(R.drawable.ic_filter_popular);
			holder.tvName.setText(R.string.feed_filter_popular);
			holder.tvCount.setText("0");
			listener = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mCallbacks != null)
						mCallbacks.viewPopular();
				}
			};

			returnView = convertView;
			break;
		}
		}

		if (returnView != null)
			((ViewTag)returnView.getTag()).clickListener = listener;
		return returnView;
	}

	@Override
	public int getViewTypeCount()
	{
		return 6;
	}

	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
	{
		ViewTag tag = (ViewTag) view.getTag();
		if (tag != null && tag.clickListener != null)
		{
			tag.clickListener.onClick(view);
		}
	}

	public View createDisplayPhotosView()
	{
		// Display photos
		View view = LayoutInflater.from(getContext()).inflate(R.layout.feed_list_display_photos, this, false);
		createViewHolder(view);
		return view;
	}

	public View createFavoritesView()
	{
		View view = LayoutInflater.from(getContext()).inflate(R.layout.feed_list_favorites, this, false);
		createViewHolder(view);
		return view;
	}

	public View createSharedView()
	{
		View view = LayoutInflater.from(getContext()).inflate(R.layout.feed_list_favorites, this, false);
		createViewHolder(view);
		return view;
	}

	public View createAllFeedsView()
	{
		View view = LayoutInflater.from(getContext()).inflate(R.layout.feed_list_item, this, false);
		createViewHolder(view);
		return view;
	}
	
	private void createViewHolder(View view)
	{
		ViewTag holder = new ViewTag();
		holder.ivFeedImage = (ImageView) view.findViewById(R.id.ivFeedImage);
		holder.tvName = (TextView) view.findViewById(R.id.tvFeedName);
		holder.tvCount = (TextView) view.findViewById(R.id.tvCount);
		holder.ivRefresh = view.findViewById(R.id.ivRefresh);
		holder.checkView = (CheckableImageView) view.findViewById(R.id.chkShowImages);
		view.setTag(holder);
	}
}
