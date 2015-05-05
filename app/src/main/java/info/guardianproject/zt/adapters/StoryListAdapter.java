package info.guardianproject.zt.adapters;

import info.guardianproject.zt.App;
import info.guardianproject.zt.models.FeedFilterType;
import info.guardianproject.zt.ui.MediaViewCollection;
import info.guardianproject.zt.ui.UICallbacks;
import info.guardianproject.zt.ui.MediaViewCollection.OnMediaLoadedListener;
import info.guardianproject.zt.views.StoryItemPageView;
import info.guardianproject.zt.views.StoryItemPageView.StoryItemPageViewListener;
import info.guardianproject.zt.views.StoryItemPageView.ViewType;
import info.guardianproject.zt.views.StoryListView.StoryListListener;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class StoryListAdapter extends BaseAdapter implements OnMediaLoadedListener, Filterable, StoryItemPageViewListener
{
	public static final String LOGTAG = "StoryListAdapter";
	public static final boolean LOGGING = false;	
	
	public interface OnTagClickedListener
	{
		void onTagClicked(String tag);
	}

	public interface OnHeaderCreatedListener
	{
		void onHeaderCreated(View headerView, int resIdHeader);
	}

	protected final Context mContext;
	protected final LayoutInflater mInflater;
	private ArrayList<Item> mStories;
	private ArrayList<Item> mFilteredStories;
	private boolean mShowTags;
	protected OnTagClickedListener mOnTagClickedListener;
	protected OnHeaderCreatedListener mOnHeaderCreatedListener;
	private StoryListListener mListener;
	private int mResIdHeaderView;
	private boolean mHeaderOnlyIfNoItems;
	private String mTagFilter;
	private Feed mTagFilterFeed;
	private boolean mDeferMediaLoading;
	private Filter mFilter;

	public StoryListAdapter(Context context, ArrayList<Item> stories)
	{
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mStories = mFilteredStories = stories;
		mShowTags = false;
		mOnTagClickedListener = null;
		mHeaderOnlyIfNoItems = false;
	}

	public void setHeaderView(int resIdHeaderView, boolean onlyIfNoItems)
	{
		if (resIdHeaderView != mResIdHeaderView)
		{
			mResIdHeaderView = resIdHeaderView;
			mHeaderOnlyIfNoItems = onlyIfNoItems;
		}
	}

	public void updateItems(ArrayList<Item> items)
	{
		mStories = items;
		applyFilters(); // Will call NotifyDataSetChanged when complete
	}

	public void setTagFilter(Feed feed, String tagFilter)
	{
		if (!TextUtils.equals(mTagFilter, tagFilter))
		{
			mTagFilterFeed = feed;
			mTagFilter = tagFilter;
			applyFilters();
		}
	}

	private void applyFilters()
	{
		getFilter().filter(mTagFilter);
	}

	public void setListener(StoryListListener listener)
	{
		mListener = listener;
	}

	public boolean showTags()
	{
		return mShowTags;
	}

	public void setShowTags(boolean showTags)
	{
		mShowTags = showTags;
	}

	public void setOnTagClickedListener(OnTagClickedListener listener)
	{
		mOnTagClickedListener = listener;
	}

	public void setOnHeaderCreatedListener(OnHeaderCreatedListener listener)
	{
		mOnHeaderCreatedListener = listener;
	}

	
	@Override
	public boolean hasStableIds()
	{
		return true;
	}

	private boolean hasHeaderView()
	{
		return (mResIdHeaderView != 0) && (!mHeaderOnlyIfNoItems || mFilteredStories == null || mFilteredStories.size() == 0);
	}
	
	@Override
	public int getItemViewType(int position)
	{
		if (position == 0 && hasHeaderView())
			return 0; // This is a header type
		
		Item item = (Item) getItem(position);

		int type = 1; // No media
		
		ArrayList<MediaContent> media = item.getMediaContent();
		if (media != null && media.size() > 0)
		{
			if (Iterables.any(media, new Predicate<MediaContent>()
				{
					@Override
					public boolean apply(MediaContent mc)
					{
						return App.getInstance().socialReader.isMediaContentLoaded(mc);
					}
				}))
			{
				if (media.get(0).getHeight() > media.get(0).getWidth())
					type = 2;
				else
					type = 3;
			}
		}
		return type;
	}

	@Override
	public int getViewTypeCount()
	{
		// We have an (optional) header and three types of items
		return 4;
	}

	@Override
	public int getCount()
	{
		int count = 0;
		if (mFilteredStories != null)
			count = mFilteredStories.size();
		if (hasHeaderView())
			count += 1;
		return count;
	}

	@Override
	public Object getItem(int position)
	{
		if (position == 0 && hasHeaderView())
			return mResIdHeaderView;
		if (mFilteredStories == null || mFilteredStories.size() == 0)
			return null;
		return mFilteredStories.get(position - (hasHeaderView() ? 1 : 0));
	}

	@Override
	public long getItemId(int position)
	{
		if (position == 0 && hasHeaderView())
			return -1;
		if (mFilteredStories == null || mFilteredStories.size() == 0)
			return -1;
		return mFilteredStories.get(position - (hasHeaderView() ? 1 : 0)).getDatabaseId();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		if (position == 0 && hasHeaderView())
		{
			View headerView = convertView;
			if (headerView == null || headerView.getTag() == null || Integer.valueOf(mResIdHeaderView).compareTo((Integer) headerView.getTag()) != 0)
			{
				headerView = mInflater.inflate(mResIdHeaderView, parent, false);
				headerView.setTag(Integer.valueOf(mResIdHeaderView));
			}
			if (this.mOnHeaderCreatedListener != null)
				this.mOnHeaderCreatedListener.onHeaderCreated(headerView, mResIdHeaderView);
			return headerView;
		}
		View view = (convertView != null) ? convertView : createView(position, parent);
		bindView(view, position, (Item) getItem(position));
		return view;
	}
	
	protected View createView(int position, ViewGroup parent)
	{
		int type = getItemViewType(position);
		StoryItemPageView view = new StoryItemPageView(parent.getContext());
		if (type == 1)
			view.createViews(ViewType.NO_PHOTO);
		else if (type == 2)
			view.createViews(ViewType.PORTRAIT_PHOTO);
		else
			view.createViews(ViewType.LANDSCAPE_PHOTO);
		return view;
	}

	protected void bindView(View view, int position, Item item)
	{
		StoryItemPageView pv = (StoryItemPageView) view;
		pv.showTags(mShowTags);
		pv.setListener(this);
		pv.populateWithItem(item);
		if (!getDeferMediaLoading())
			pv.loadMedia(this);
		view.setOnClickListener(new ItemClickListener(position));
	}
	
//	public void recycleView(View view)
//	{
//		if (view != null && view instanceof StoryItemPageView)
//			((StoryItemPageView) view).recycle();
//	}

	public void setDeferMediaLoading(boolean deferMediaLoading)
	{
		if (mDeferMediaLoading != deferMediaLoading)
		{
			mDeferMediaLoading = deferMediaLoading;
		}
	}
	
	public boolean getDeferMediaLoading()
	{
		return mDeferMediaLoading;
	}

	protected class ItemClickListener implements View.OnClickListener
	{
		private final int mPosition;

		public ItemClickListener(int position)
		{
			mPosition = position;
		}

		@Override
		public void onClick(View v)
		{
			int positionInList = mPosition - (hasHeaderView() ? 1 : 0);
			if (mListener != null)
				mListener.onStoryClicked(mFilteredStories, positionInList, v);
		}
	}


	@Override
	public void onViewLoaded(MediaViewCollection collection, int index, boolean wasCached)
	{
		// If it was not cached it means we need to reload cause the layout has changed
		if (!wasCached)
			notifyDataSetChanged();
	}

	@Override
	public Filter getFilter()
	{
		if (mFilter == null)
		{
			mFilter = new Filter()
			{
				@Override
				protected FilterResults performFiltering(CharSequence constraint)
				{
					ArrayList<Item> filteredStories = null;

					if (mTagFilterFeed != null)
					{
						Feed result = App.getInstance().socialReader.getFeedItemsWithTag(mTagFilterFeed, constraint.toString());
						filteredStories = result.getItems();
					}
					else
					{
						if (mStories != null)
						{
							filteredStories = new ArrayList<Item>();
							for (Item item : mStories)
							{
								if (constraint != null)
								{
									Pattern pattern = Pattern.compile(Pattern.quote(constraint.toString()), Pattern.CASE_INSENSITIVE);
									if (item.getTags() != null)
									{
										boolean bFoundTag = false;
										for (String tag : item.getTags())
										{
											if (pattern.matcher(tag).find())
											{
												bFoundTag = true;
												break;
											}
										}
										if (!bFoundTag)
											continue; // Don't add this, i.e.
														// filter
														// it
									}
								}
								filteredStories.add(item);
							}
						}
					}
					FilterResults results = new FilterResults();
					results.count = (filteredStories != null) ? filteredStories.size() : 0;
					results.values = filteredStories;
					return results;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results)
				{
					if (TextUtils.isEmpty(constraint))
						mFilteredStories = mStories;
					else if (results.count > 0)
						mFilteredStories = (ArrayList<Item>) results.values;
					else
						mFilteredStories = null;
					notifyDataSetChanged();
				}
			};
		}

		return mFilter;
	}
	
	@Override
	public void onSourceClicked(long feedId)
	{
		UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, feedId, this);
	}

	@Override
	public void onTagClicked(String tag)
	{
		if (mOnTagClickedListener != null)
			mOnTagClickedListener.onTagClicked(tag);
	}
}
