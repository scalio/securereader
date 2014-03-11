package info.guardianproject.securereaderinterface.adapters;

import java.util.ArrayList;

import info.guardianproject.securereaderinterface.ui.OnMediaItemClickedListener;
import info.guardianproject.securereaderinterface.views.MediaContentPreviewView;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;


public class StoryItemMediaContentPagerAdapter extends PagerAdapter
{
	private final Context mContext;
	private ArrayList<MediaContentPreviewView> mMediaViewCollection;

	public StoryItemMediaContentPagerAdapter(Context context, ArrayList<MediaContentPreviewView> mediaViewCollection, boolean allowFullScreenViewing)
	{
		super();
		mContext = context;
		mMediaViewCollection = mediaViewCollection;

		// Hookup events?
		for (int i = 0; i < mMediaViewCollection.size(); i++)
		{
			View mediaView = (View) mMediaViewCollection.get(i);
			if (allowFullScreenViewing)
			{
				mediaView.setOnClickListener(new OnMediaItemClickedListener(((MediaContentPreviewView)mediaView).getMediaContent()));
			}
			else
			{
				mediaView.setOnClickListener(null);
			}
		}
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1)
	{
		return arg0 == (View) arg1;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		View mediaView = (View) mMediaViewCollection.get(position);
		if (mediaView != null)
		{
			if (mediaView.getParent() != null)
				((ViewGroup) mediaView.getParent()).removeView(mediaView);
			container.addView(mediaView, 0);
		}
		return mediaView;
	}

	@Override
	public int getItemPosition(Object object)
	{
		return POSITION_NONE;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object)
	{
		container.removeView((View) object);
	}

	@Override
	public int getCount()
	{
		return mMediaViewCollection.size();
	}

	public View getView(int position)
	{
		return (View) mMediaViewCollection.get(position);
	}

	public long getItemId(int position)
	{
		View view = getView(position);
		if (view != null)
			return view.hashCode();
		return 0;
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		return "Page " + position;
	}
}
