package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.R;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tinymission.rss.Feed;

public class FeedListAdapter extends BaseAdapter
{
	public final static String LOGTAG = "FeedList";
	public static final boolean LOGGING = false;

	public interface FeedListAdapterListener
	{
		void addFeed(Feed feed);

		void removeFeed(Feed feed);

		void deleteFeed(Feed feed);
	}

	public enum FeedListItemType
	{
		CATEGORY(0), FEED(1), EXPAND(2), CLOSE(3);

		private final int value;

		private FeedListItemType(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}
	}

	/*
	 * The number of items to show in the drop down before the "Show all" item
	 * appears.
	 */
	private final static int DEFAULT_NUMBER_OF_ITEMS_SHOWN = 3;

	private final Context mContext;
	private final FeedListAdapterListener mListener;
	private final LayoutInflater mInflater;

	private final ArrayList<Object> mItems;
	private ArrayList<Object> mVisibleItems;

	private View mCurrentOperationsView; // Only allow operations for one view
											// at a time!

	public FeedListAdapter(Context context, FeedListAdapterListener listener, ArrayList<Feed> feeds)
	{
		super();
		mContext = context;
		mListener = listener;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// TODO - order the feed on topic and name

		mItems = new ArrayList<Object>();

		Topic currentTopic = null;
		for (Feed feed : feeds)
		{
			if (feed.getStatus() != Feed.STATUS_LAST_SYNC_GOOD)
			{
				if (currentTopic == null)
				{
					currentTopic = new Topic(mContext.getString(R.string.add_feed_processing));
					currentTopic.isExpanded = true;
					mItems.add(currentTopic);
				}
				mItems.add(new FeedViewModel(feed, currentTopic));
			}
		}
		currentTopic = null;
		for (Feed feed : feeds)
		{
			if (feed.getStatus() != Feed.STATUS_LAST_SYNC_GOOD)
				continue; // Already added above

			/*
			 * if (feed.is_cat) { mItems.add(new Topic(feed)); } else {
			 */
			if (currentTopic == null)
			{
				// Safeguard to make sure that the list starts with a TOPIC
				// item
				// (need this for Collapse/Expand to work as expected)
				currentTopic = new Topic(mContext.getString(R.string.add_feed_default_topic));
				currentTopic.isExpanded = true;
				mItems.add(currentTopic);
			}
			mItems.add(new FeedViewModel(feed, currentTopic));
			/*
			 * }
			 */
		}
		updateVisibleItems();
	}

	private void updateVisibleItems()
	{
		if (mVisibleItems == null)
			mVisibleItems = new ArrayList<Object>();

		mVisibleItems.clear();

		int nInThisTopic = 0;
		Topic currentTopic = null;
		for (Object o : mItems)
		{
			if (o instanceof Topic)
			{
				if (nInThisTopic > DEFAULT_NUMBER_OF_ITEMS_SHOWN)
					mVisibleItems.add(new Closer(currentTopic));
				mVisibleItems.add(o);
				nInThisTopic = 0;
				currentTopic = (Topic) o;
			}
			else if (o instanceof FeedViewModel)
			{
				FeedViewModel model = (FeedViewModel) o;
				if (!model.topic.isExpanded)
				{
					if (nInThisTopic == DEFAULT_NUMBER_OF_ITEMS_SHOWN)
					{
						mVisibleItems.add(new Expander(currentTopic));
					}
					else if (nInThisTopic < DEFAULT_NUMBER_OF_ITEMS_SHOWN)
					{
						mVisibleItems.add(o);
					}
				}
				else
				{
					mVisibleItems.add(o);
				}
				nInThisTopic++;
			}
		}
		if (nInThisTopic > DEFAULT_NUMBER_OF_ITEMS_SHOWN && currentTopic.isExpanded)
			mVisibleItems.add(new Closer(currentTopic));
	}

	@Override
	public int getCount()
	{
		return mVisibleItems.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mVisibleItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Object o = getItem(position);
		if (o instanceof Topic)
		{
			Topic topic = (Topic) o;

			View view;
			if (convertView == null)
			{
				view = mInflater.inflate(R.layout.add_feed_header_item, parent, false);
			}
			else
			{
				view = convertView;
			}

			TextView tv = (TextView) view.findViewById(R.id.tvTopic);
			tv.setText(topic.title);

			return view;
		}
		if (o instanceof Expander)
		{
			Expander expander = (Expander) o;

			View view;
			if (convertView == null)
			{
				view = mInflater.inflate(R.layout.add_feed_expander, parent, false);
			}
			else
			{
				view = convertView;
			}
			view.setOnClickListener(new ExpandClickListener(expander.topic, true));
			return view;
		}
		if (o instanceof Closer)
		{
			Closer closer = (Closer) o;

			View view;
			if (convertView == null)
			{
				view = mInflater.inflate(R.layout.add_feed_closer, parent, false);
			}
			else
			{
				view = convertView;
			}
			view.setOnClickListener(new ExpandClickListener(closer.topic, false));
			return view;
		}
		if (o instanceof FeedViewModel)
		{
			FeedViewModel feedModel = (FeedViewModel) o;

			View view;
			if (convertView == null)
			{
				view = mInflater.inflate(R.layout.add_feed_list_item, parent, false);
			}
			else
			{
				view = convertView;
			}

			// ImageView iv = (ImageView) view.findViewById(R.id.ivFeedIcon);
			// if (feedModel.feed.getImageManager() != null)
			// feedModel.feed.getImageManager().download(feedModel.feed.,
			// imageView)
			// App.getInstance().socialReader.loadDisplayImageMediaContent(feedModel.feed.getImageManager(),
			// iv);

			// Name
			TextView tv = (TextView) view.findViewById(R.id.tvFeedName);
			tv.setText(feedModel.feed.getTitle());
			tv.setTextColor(mContext.getResources().getColor(R.color.feed_list_title_normal));

			int feedStatus = feedModel.feed.getStatus();
			if (feedStatus == Feed.STATUS_LAST_SYNC_FAILED_404 || feedStatus == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL
					|| feedStatus == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
			{
				tv.setText(R.string.add_feed_error_loading);
				tv.setTextColor(mContext.getResources().getColor(R.color.feed_list_title_error));
			}
			else if (TextUtils.isEmpty(feedModel.feed.getTitle()))
				tv.setText(R.string.add_feed_not_loaded);

			// Description
			tv = (TextView) view.findViewById(R.id.tvFeedDescription);
			tv.setText(feedModel.feed.getDescription());
			if (TextUtils.isEmpty(feedModel.feed.getTitle()) || tv.getText().length() == 0)
				tv.setText(feedModel.feed.getFeedURL());

			// Operation?
			View btnAdd = view.findViewById(R.id.btnAdd);
			View btnRemove = view.findViewById(R.id.btnRemove);
			if (feedStatus == Feed.STATUS_NOT_SYNCED || feedStatus == Feed.STATUS_LAST_SYNC_FAILED_404 || feedStatus == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL
					|| feedStatus == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
			{
				// Error, hide add and remove
				btnAdd.setVisibility(View.GONE);
				btnRemove.setVisibility(View.GONE);
				view.setOnClickListener(null);
				view.setOnLongClickListener(new FeedLongClickedListener(feedModel.feed, view.findViewById(R.id.llOperationButtons), true));
			}
			else if (feedModel.feed.isSubscribed())
			{
				btnAdd.setVisibility(View.GONE);
				btnRemove.setVisibility(View.VISIBLE);
				btnAdd.setOnClickListener(null);
				btnRemove.setTag(feedModel);
				btnRemove.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						FeedViewModel model = (FeedViewModel) v.getTag();
						mListener.removeFeed(model.feed);
					}
				});
				view.setOnClickListener(new FeedClickedListener(feedModel));
				view.setOnLongClickListener(new FeedLongClickedListener(feedModel.feed, view.findViewById(R.id.llOperationButtons), false));
			}
			else
			{
				btnAdd.setVisibility(View.VISIBLE);
				btnRemove.setVisibility(View.GONE);
				btnAdd.setTag(feedModel);
				btnAdd.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						FeedViewModel model = (FeedViewModel) v.getTag();
						mListener.addFeed(model.feed);
					}
				});
				btnRemove.setOnClickListener(null);
				view.setOnClickListener(new FeedClickedListener(feedModel));
				view.setOnLongClickListener(new FeedLongClickedListener(feedModel.feed, view.findViewById(R.id.llOperationButtons), false));
			}
			return view;
		}
		return null;
	}

	@Override
	public void notifyDataSetChanged()
	{
		mCurrentOperationsView = null;
		super.notifyDataSetChanged();
	}

	private class FeedClickedListener implements View.OnClickListener
	{
		private final FeedViewModel mFeedViewModel;

		public FeedClickedListener(FeedViewModel feedViewModel)
		{
			mFeedViewModel = feedViewModel;
		}

		@Override
		public void onClick(View v)
		{
			UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, mFeedViewModel.feed.getDatabaseId(), this);
			if (mContext instanceof Activity)
			{
				((Activity) mContext).finish();
			}
		}
	}

	private class FeedLongClickedListener implements View.OnLongClickListener, OnClickListener, FadeInFadeOutListener
	{
		private final View mOperationsView;
		private final View mBtnRemove;
		private final View mBtnCopyURL;
		private final View mBtnCancel;
		private final Feed mFeed;

		public FeedLongClickedListener(Feed feed, View operationsView, boolean showCopy)
		{
			mFeed = feed;
			mOperationsView = operationsView;
			mBtnRemove = mOperationsView.findViewById(R.id.btnRemove);
			mBtnCopyURL = mOperationsView.findViewById(R.id.btnCopyURL);
			mBtnCancel = mOperationsView.findViewById(R.id.btnCancel);
			mBtnRemove.setOnClickListener(this);
			mBtnCopyURL.setOnClickListener(this);
			mBtnCancel.setOnClickListener(this);
			if (showCopy)
			{
				mBtnCopyURL.setVisibility(View.VISIBLE);
				mBtnCancel.setVisibility(View.GONE);
			}
			else
			{
				mBtnCopyURL.setVisibility(View.GONE);
				mBtnCancel.setVisibility(View.VISIBLE);
			}
			mOperationsView.setVisibility(View.GONE);
			AnimationHelpers.fadeOut(mOperationsView, 0, 0, false);
		}

		@Override
		public boolean onLongClick(View v)
		{
			if (mCurrentOperationsView == null && mOperationsView.getVisibility() == View.GONE)
			{
				mCurrentOperationsView = mOperationsView;
				mOperationsView.setVisibility(View.VISIBLE);
				AnimationHelpers.fadeIn(mOperationsView, 200, 5000, false, this);
			}
			return true;
		}

		@Override
		public void onClick(View v)
		{
			AnimationHelpers.fadeOut(mOperationsView, 200, 0, false);
			if (v == mBtnRemove)
			{
				v.post(new Runnable()
				{
					@Override
					public void run()
					{
						mCurrentOperationsView = null;
						mListener.deleteFeed(mFeed);
					}
				});
			}
			else if (v == mBtnCopyURL)
			{
				v.post(new Runnable()
				{
					@SuppressLint("NewApi")
					@Override
					public void run()
					{
						mCurrentOperationsView = null;

						int sdk = android.os.Build.VERSION.SDK_INT;
						if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB)
						{
							android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
							clipboard.setText(mFeed.getFeedURL());
						}
						else
						{
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext
									.getSystemService(Context.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText(
									mContext.getString(R.string.add_feed_operation_copy_url_clipboard_label), mFeed.getFeedURL());
							clipboard.setPrimaryClip(clip);
						}
					}
				});
			}
			else
			{
				mCurrentOperationsView = null;
			}
		}

		@Override
		public void onFadeInStarted(View view)
		{
		}

		@Override
		public void onFadeInEnded(View view)
		{
		}

		@Override
		public void onFadeOutStarted(View view)
		{
			mCurrentOperationsView = null;
		}

		@Override
		public void onFadeOutEnded(View view)
		{
		}
	}

	@Override
	public int getItemViewType(int position)
	{
		Object o = getItem(position);
		if (o instanceof Topic)
			return FeedListItemType.CATEGORY.getValue();
		else if (o instanceof Expander)
			return FeedListItemType.EXPAND.getValue();
		else if (o instanceof Closer)
			return FeedListItemType.CLOSE.getValue();
		return FeedListItemType.FEED.getValue();
	}

	@Override
	public int getViewTypeCount()
	{
		return 4;
	}

	private class ExpandClickListener implements OnClickListener
	{
		private final Topic mTopic;
		private final boolean mExpand;

		public ExpandClickListener(Topic topic, boolean expand)
		{
			mTopic = topic;
			mExpand = expand;
		}

		@Override
		public void onClick(View v)
		{
			expandTopic(mTopic, mExpand);
		}
	}

	private void expandTopic(Topic topic, boolean expand)
	{
		topic.isExpanded = expand;
		this.updateVisibleItems();
		this.notifyDataSetChanged();
	}

	private class Topic
	{
		public Feed feed;
		public String title;
		public boolean isExpanded;

		public Topic(Feed feed)
		{
			this.feed = feed;
			this.title = feed.getTitle();
			isExpanded = false;
		}

		public Topic(String title)
		{
			this.title = title;
		}
	}

	private class Expander
	{
		public Topic topic;

		public Expander(Topic topic)
		{
			this.topic = topic;
		}
	}

	private class Closer
	{
		public Topic topic;

		public Closer(Topic topic)
		{
			this.topic = topic;
		}
	}

	private class FeedViewModel
	{
		public Feed feed;
		public Topic topic;

		public FeedViewModel(Feed feed, Topic topic)
		{
			this.feed = feed;
			this.topic = topic;
		}

	}
}
