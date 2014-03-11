package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.models.PagedViewContent;
import info.guardianproject.securereaderinterface.models.ViewPagerIndicator;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class PagedView extends NestedViewPager
{
	public interface PagedViewListener
	{
		PagedViewContent onMovedToPrevious();

		PagedViewContent onMovedToNext();
	}

	private PagedViewPagerAdapter mPageAdapter;

	private PagedViewContent mOriginalContentPrevious;
	private PagedViewContent mOriginalContentThis;
	private PagedViewContent mOriginalContentNext;
	private boolean mContentPreviousReversed;
	private boolean mContentThisReversed;
	private boolean mContentNextReversed;
	private ArrayList<View> mPageViewsThis;
	private ArrayList<View> mPageViewsPrevious;
	private ArrayList<View> mPageViewsNext;
	private ArrayList<View> mPageViewsAll;
	private PagedViewListener mListener;
	private ViewPagerIndicator mViewPagerIndicator;

	public PagedView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	private void init()
	{
		mPageViewsAll = new ArrayList<View>();

		mPageAdapter = new PagedViewPagerAdapter();
		setAdapter(mPageAdapter);

		super.setOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageScrollStateChanged(int state)
			{
				if (state == ViewPager.SCROLL_STATE_IDLE)
				{
					post(new Runnable()
					{
						@Override
						public void run()
						{
							adjustCurrentIndex(getCurrentItem());
							if (mViewPagerIndicator != null)
							{
								if (mContentThisReversed)
									mViewPagerIndicator.onCurrentChanged(getCurrentItem() - ((mPageViewsNext != null) ? mPageViewsNext.size() : 0));
								else
									mViewPagerIndicator.onCurrentChanged(getCurrentItem() - ((mPageViewsPrevious != null) ? mPageViewsPrevious.size() : 0));
							}
						}
					});
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
			}

			@Override
			public void onPageSelected(int position)
			{
			}
		});
	}

	public void setListener(PagedViewListener listener)
	{
		mListener = listener;
	}

	@Override
	public void setViewPagerIndicator(ViewPagerIndicator viewPagerIndicator)
	{
		mViewPagerIndicator = viewPagerIndicator;
		updateViewPagerIndicator();
	}

	private void updateViewPagerIndicator()
	{
		if (mViewPagerIndicator != null)
		{
			if (mPageViewsThis != null)
				mViewPagerIndicator.onTotalChanged(mPageViewsThis.size());
			else
				mViewPagerIndicator.onTotalChanged(0);
			if (mContentThisReversed)
				mViewPagerIndicator.onCurrentChanged(getCurrentItem() - ((mPageViewsNext != null) ? mPageViewsNext.size() : 0));
			else
				mViewPagerIndicator.onCurrentChanged(getCurrentItem() - ((mPageViewsPrevious != null) ? mPageViewsPrevious.size() : 0));
		}
	}

	public PagedViewContent getContentThis()
	{
		return mOriginalContentThis;
	}

	public void setContentThis(PagedViewContent content)
	{
		mOriginalContentThis = content;
		update();
	}

	public PagedViewContent getContentPrevious()
	{
		return mOriginalContentPrevious;
	}

	public void setContentPrevious(PagedViewContent content)
	{
		mOriginalContentPrevious = content;
		update();
	}

	public PagedViewContent getContentNext()
	{
		return mOriginalContentNext;
	}

	public void setContentNext(PagedViewContent content)
	{
		mOriginalContentNext = content;
		update();
	}

	public void recreateAllViews()
	{
		int currentPageNumberInThisContent = -1;
		int pos = mPageAdapter.getItemPosition(mPageViewsThis.get(0));
		if (pos != PagerAdapter.POSITION_NONE)
		{
			if (getCurrentItem() >= pos)
				currentPageNumberInThisContent = getCurrentItem() - pos;
		}

		// Null them so they will be recreated
		mPageViewsThis = null;
		mPageViewsPrevious = null;
		mPageViewsNext = null;
		updateViews(currentPageNumberInThisContent);
	}

	public void recreateViewsForContent(PagedViewContent content)
	{
		int currentPageNumberInThisContent = -1;
		if (this.mOriginalContentPrevious == content)
			mPageViewsPrevious = null;
		else if (this.mOriginalContentNext == content)
			mPageViewsNext = null;
		else if (this.mOriginalContentThis == content)
		{
			int pos = PagerAdapter.POSITION_NONE;
			if (mPageViewsThis != null && mPageViewsThis.size() > 0)
				pos = mPageAdapter.getItemPosition(mPageViewsThis.get(0));
			if (pos != PagerAdapter.POSITION_NONE)
			{
				if (getCurrentItem() >= pos)
					currentPageNumberInThisContent = getCurrentItem() - pos;
			}
			mPageViewsThis = null;
		}
		updateViews(currentPageNumberInThisContent);
	}

	private ArrayList<View> createViewsForContent(PagedViewContent content)
	{
		if (content == null)
			return null;

		int height = this.getHeight();
		if (height == 0)
			return null;

		ArrayList<View> ret = content.createPages(this);
		for (View view : ret)
		{
			view.setTag(content);
		}
		return ret;
	}

	private void update()
	{
		if (getWidth() == 0 || getHeight() == 0)
			return;

		mContentThisReversed = (mOriginalContentThis == null) ? false : mOriginalContentThis.usesReverseSwipe();
		mContentPreviousReversed = (mOriginalContentPrevious == null) ? false : mOriginalContentPrevious.usesReverseSwipe();
		mContentNextReversed = (mOriginalContentNext == null) ? false : mOriginalContentNext.usesReverseSwipe();

		if (mPageViewsPrevious == null)
			mPageViewsPrevious = createViewsForContent(mOriginalContentPrevious);
		if (mPageViewsThis == null)
			mPageViewsThis = createViewsForContent(mOriginalContentThis);
		if (mPageViewsNext == null)
			mPageViewsNext = createViewsForContent(mOriginalContentNext);

		mPageViewsAll.clear();
		if (mContentThisReversed)
		{
			ArrayList<View> temp;
			if (mPageViewsNext != null)
			{
				temp = new ArrayList<View>(mPageViewsNext);
				Collections.reverse(temp);
				mPageViewsAll.addAll(temp);
			}
			if (mPageViewsThis != null)
			{
				temp = new ArrayList<View>(mPageViewsThis);
				Collections.reverse(temp);
				mPageViewsAll.addAll(temp);
			}
			if (mPageViewsPrevious != null)
			{
				temp = new ArrayList<View>(mPageViewsPrevious);
				Collections.reverse(temp);
				mPageViewsAll.addAll(temp);
			}
		}
		else
		{
			if (mPageViewsPrevious != null)
				mPageViewsAll.addAll(mPageViewsPrevious);
			if (mPageViewsThis != null)
				mPageViewsAll.addAll(mPageViewsThis);
			if (mPageViewsNext != null)
				mPageViewsAll.addAll(mPageViewsNext);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		post(new Runnable()
		{
			@Override
			public void run()
			{
				// Null them so they will be recreated
				mPageViewsThis = null;
				mPageViewsPrevious = null;
				mPageViewsNext = null;
				updateViews(0);
			}
		});
	}

	public void updateViews(int scrollToThisIndexInThis)
	{
		update();

		if (this.mPageAdapter != null)
			mPageAdapter.notifyDataSetChanged();

		if (scrollToThisIndexInThis != -1 && mPageViewsThis != null && mPageViewsThis.size() > 0 && mPageAdapter != null)
		{
			int pos = mPageAdapter.getItemPosition(mPageViewsThis.get(0));
			if (pos != PagerAdapter.POSITION_NONE)
			{
				if (scrollToThisIndexInThis >= mPageViewsThis.size())
					scrollToThisIndexInThis = mPageViewsThis.size() - 1;
				if (mContentThisReversed)
					pos -= scrollToThisIndexInThis;
				else
					pos += scrollToThisIndexInThis;
				setCurrentItem(pos, false);
			}
		}
		updateViewPagerIndicator();
	}

	private int getNumberOfPages()
	{
		int count = mPageViewsAll.size();
		return count;
	}

	private void adjustCurrentIndex(int position)
	{
		if (mOriginalContentPrevious != null && mPageViewsAll.get(position).getTag() == mOriginalContentPrevious)
		{
			// Moved to previous!
			//
			mContentNextReversed = mContentThisReversed;
			mOriginalContentNext = mOriginalContentThis;
			mPageViewsNext = mPageViewsThis;
			mContentThisReversed = mContentPreviousReversed;
			mOriginalContentThis = mOriginalContentPrevious;
			mPageViewsThis = mPageViewsPrevious;
			if (mListener != null)
			{
				mOriginalContentPrevious = mListener.onMovedToPrevious();
			}
			else
			{
				mOriginalContentPrevious = null;
			}
			mPageViewsPrevious = null;
			updateViews(-1);
		}
		else if (mOriginalContentNext != null && mPageViewsAll.get(position).getTag() == mOriginalContentNext)
		{
			// Moved to next!
			//
			mContentPreviousReversed = mContentThisReversed;
			mOriginalContentPrevious = mOriginalContentThis;
			mPageViewsPrevious = mPageViewsThis;
			mContentThisReversed = mContentNextReversed;
			mOriginalContentThis = mOriginalContentNext;
			mPageViewsThis = mPageViewsNext;
			if (mListener != null)
			{
				mOriginalContentNext = mListener.onMovedToNext();
			}
			else
			{
				mOriginalContentNext = null;
			}
			mPageViewsNext = null;
			updateViews(-1);
		}
	}

	private class PagedViewPagerAdapter extends PagerAdapter
	{
		public PagedViewPagerAdapter()
		{
			super();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			return arg0 == (View) arg1;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			View view = PagedView.this.mPageViewsAll.get(position);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
			((ViewPager) container).addView(view);
			return view;
		}

		@Override
		public int getItemPosition(Object object)
		{
			for (int i = 0; i < mPageViewsAll.size(); i++)
			{
				if (mPageViewsAll.get(i).equals(object))
				{
					return i;
				}
			}
			return POSITION_NONE;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public int getCount()
		{
			int count = PagedView.this.getNumberOfPages();
			return count;
		}
	}
}
