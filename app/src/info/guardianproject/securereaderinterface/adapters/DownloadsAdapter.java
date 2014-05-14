package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.views.StoryMediaContentView;
import info.guardianproject.securereaderinterface.R;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tinymission.rss.Item;

public class DownloadsAdapter extends BaseAdapter
{
	public static final String LOGTAG = "DownloadsAdapter";
	public static final boolean LOGGING = false;
	
	private static final int VIEW_TYPE_HEADER_COMPLETE = 0;
	private static final int VIEW_TYPE_HEADER_IN_PROGRESS = 1;
	private static final int VIEW_TYPE_ITEM_COMPLETE = 2;
	private static final int VIEW_TYPE_ITEM_IN_PROGRESS = 3;

	private static final ArrayList<Long> gComplete = new ArrayList<Long>();
	private static final HashMap<Long, MediaViewCollection> gInProgress = new HashMap<Long, MediaViewCollection>();
	private static DownloadsAdapter gInstance;

	private final Context mContext;

	public static DownloadsAdapter getInstance(Context context)
	{
		if (gInstance == null)
		{
			gInstance = new DownloadsAdapter(context);
		}
		return gInstance;
	}

	private DownloadsAdapter(Context context)
	{
		mContext = context;
	}

	@Override
	public int getCount()
	{
		int num = 0;
		if (getNumComplete() > 0)
		{
			num += getNumComplete() + 1;
		}
		num += 1; // For "In progress" header
		num += getNumInProgress();
		return num;
	}

	@Override
	public int getItemViewType(int position)
	{
		int numComplete = getNumComplete();
		int numInProgress = getNumInProgress();
		if (numComplete > 0)
		{
			if (position == 0)
				return VIEW_TYPE_HEADER_COMPLETE;
			position -= 1;
			if (position < numComplete)
				return VIEW_TYPE_ITEM_COMPLETE;
			position -= numComplete;
		}
		if (position == 0)
			return VIEW_TYPE_HEADER_IN_PROGRESS;
		position -= 1;
		if (position < numInProgress)
			return VIEW_TYPE_ITEM_IN_PROGRESS;
		return -1;
	}

	@Override
	public int getViewTypeCount()
	{
		return 5;
	}

	@Override
	public Object getItem(int position)
	{
		int numComplete = getNumComplete();
		int numInProgress = getNumInProgress();

		if (numComplete > 0)
		{
			if (position == 0)
				return null; // "Complete" header
			position -= 1;
			if (position < numComplete)
				return this.getCompleteAtIndex(position);
			position -= numComplete;
		}
		if (position == 0)
			return null; // "In progress" header
		position -= 1;
		if (position < numInProgress)
			return this.getInProgressAtIndex(position);
		return null;
	}

	@Override
	public long getItemId(int position)
	{
		Item item = (Item) getItem(position);
		if (item != null)
			return item.getDatabaseId();
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		int type = getItemViewType(position);
		View returnView = convertView;

		switch (type)
		{
		case VIEW_TYPE_HEADER_COMPLETE:
		{
			if (returnView == null)
				returnView = createHeaderView();
			TextView tvTitle = (TextView) returnView.findViewById(R.id.tvTitle);
			tvTitle.setText(R.string.downloads_complete);
			break;
		}
		case VIEW_TYPE_HEADER_IN_PROGRESS:
		{
			if (returnView == null)
				returnView = createHeaderView();
			TextView tvTitle = (TextView) returnView.findViewById(R.id.tvTitle);
			tvTitle.setText(R.string.downloads_in_progress);
			break;
		}
		case VIEW_TYPE_ITEM_COMPLETE:
		{
			if (returnView == null)
				returnView = createItemCompleteView();
			populateItemCompleteView(returnView, (Item) getItem(position));
			break;
		}
		case VIEW_TYPE_ITEM_IN_PROGRESS:
		{
			if (returnView == null)
				returnView = createItemInProgressView();
			populateItemInProgressView(returnView, (Item) getItem(position));
			break;
		}

		}
		return returnView;
	}

	private View createHeaderView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_header, null);
		return view;
	}

	private View createItemCompleteView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_item_complete, null);
		return view;
	}

	private View createItemInProgressView()
	{
		View view = LayoutInflater.from(mContext).inflate(R.layout.downloads_item_in_progress, null);
		return view;
	}

	private void populateItemCompleteView(View view, Item item)
	{
		StoryMediaContentView mediaView = (StoryMediaContentView) view.findViewById(R.id.mediaContentView);
		MediaViewCollection collection = new MediaViewCollection(mContext, item);
		collection.load(false, false);
		mediaView.setMediaCollection(collection, false, true);

		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		tvTitle.setText(item.getTitle());

		view.setOnClickListener(new View.OnClickListener()
		{
			private Item mItem;

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(mContext, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM, mItem.getDatabaseId());
				intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED, mItem.getFeedId());
				mContext.startActivity(intent);
			}

			public View.OnClickListener init(Item item)
			{
				mItem = item;
				return this;
			}
		}.init(item));
	}

	private void populateItemInProgressView(View view, Item item)
	{
		TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
		tvTitle.setText(item.getTitle());

		View operationButtons = view.findViewById(R.id.llOperationButtons);
		operationButtons.setVisibility(View.GONE);
		AnimationHelpers.fadeOut(operationButtons, 0, 0, false);

		View btnCancel = operationButtons.findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new ItemCancelListener(item));
		View btnRetry = operationButtons.findViewById(R.id.btnRefresh);
		btnRetry.setOnClickListener(new ItemRetryListener(item, operationButtons));
		
		
		View menuView = view.findViewById(R.id.ivMenu);
		menuView.setOnClickListener(new View.OnClickListener()
		{
			private View mOperationView;

			@Override
			public void onClick(View v)
			{
				if (mOperationView.getVisibility() == View.GONE)
				{
					mOperationView.setVisibility(View.VISIBLE);
					AnimationHelpers.fadeIn(mOperationView, 500, 5000, false);
				}
			}

			public View.OnClickListener init(View operationView)
			{
				mOperationView = operationView;
				return this;
			}
		}.init(operationButtons));
	}

	public static int getNumComplete()
	{
		if (LOGGING)
			Log.v(LOGTAG, "getNumComplete");
		return gComplete.size();
	}

	private Item getCompleteAtIndex(int index)
	{
		if (LOGGING)
			Log.v(LOGTAG, "getCompleteAtIndex " + index);
		Long l = gComplete.get(index);
		return App.getInstance().socialReader.getItemFromId(l.longValue());
	}

	public static int getNumInProgress()
	{
		if (LOGGING)
			Log.v(LOGTAG, "getNumInProgress");
		return gInProgress.size();
	}

	private Item getInProgressAtIndex(int index)
	{
		if (LOGGING)
			Log.v(LOGTAG, "getInProgressAtIndex " + index);
		Long l = (Long) gInProgress.keySet().toArray()[index];
		return App.getInstance().socialReader.getItemFromId(l.longValue());
	}

	public static void downloading(MediaViewCollection mvc)
	{
		Long itemLong = Long.valueOf(mvc.getItem().getDatabaseId());
		if (gComplete.contains(itemLong))
			gComplete.remove(itemLong);
		gInProgress.put(itemLong, mvc);
		if (gInstance != null)
			gInstance.notifyDataSetChanged();
	}

	public static void downloaded(MediaViewCollection mvc)
	{
		Long itemLong = Long.valueOf(mvc.getItem().getDatabaseId());
		if (mvc.getCountLoaded() == mvc.getCount())
		{
			// Done
			if (gInProgress.containsKey(itemLong))
			{
				gInProgress.remove(itemLong);
				gComplete.add(itemLong);
				if (gInstance != null)
					gInstance.notifyDataSetChanged();
			}
		}
	}

	public static void cancel(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		if (gInProgress.containsKey(itemLong))
		{
			if (LOGGING)
				Log.v(LOGTAG, "Cancel media load for item id " + itemId);
			MediaViewCollection mvc = gInProgress.get(itemLong);
			mvc.recycle();
			gInProgress.remove(itemLong);
			if (gInstance != null)
				gInstance.notifyDataSetChanged();
		}
	}
	
	public static void retry(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		if (gInProgress.containsKey(itemLong))
		{
			if (LOGGING) 
				Log.v(LOGTAG, "Retry media load for item id " + itemId);
			MediaViewCollection mvc = gInProgress.get(itemLong);
			mvc.load(true, false);
		}
	}
	
	public static void viewed(long itemId)
	{
		Long itemLong = Long.valueOf(itemId);
		if (gComplete.contains(itemLong))
		{
			gComplete.remove(itemLong);
			if (gInstance != null)
				gInstance.notifyDataSetChanged();
		}
	}
	
	private class ItemCancelListener implements View.OnClickListener
	{
		private Item mItem;

		public ItemCancelListener(Item item)
		{
			mItem = item;
		}
		
		@Override
		public void onClick(View v)
		{
			DownloadsAdapter.cancel(mItem.getDatabaseId());
		}
	}
	
	private class ItemRetryListener implements View.OnClickListener
	{
		private Item mItem;
		private View mOperationView;

		public ItemRetryListener(Item item, View operationView)
		{
			mItem = item;
			mOperationView = operationView;
		}
		
		@Override
		public void onClick(View v)
		{
			AnimationHelpers.fadeOut(mOperationView, 500, 0, false);
			DownloadsAdapter.retry(mItem.getDatabaseId());
		}
	}

}
