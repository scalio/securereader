package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.models.ViewPagerIndicator;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;


public class NestedViewPager extends ViewPager
{
	private final static int CLICK_SENSITIVITY = 5;

	private boolean mPropagateClicks;
	private ViewPagerIndicator mViewPagerIndicator;
	private OnPageChangeListener mOnPageChangeListener;
	private float mPositionOffset;

	public NestedViewPager(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		super.setOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				if (mOnPageChangeListener != null)
					mOnPageChangeListener.onPageScrollStateChanged(arg0);
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
			{
				mPositionOffset = positionOffset;
				if (mOnPageChangeListener != null)
					mOnPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}

			@Override
			public void onPageSelected(int arg0)
			{
				if (mViewPagerIndicator != null)
				{
					mViewPagerIndicator.onCurrentChanged(arg0);
				}
				if (mOnPageChangeListener != null)
					mOnPageChangeListener.onPageSelected(arg0);
			}
		});
	}

	/**
	 * If true this NestedViewPager will try to find its nearest parent with
	 * clickable=true and call setPressed/onClicked on that View.
	 * 
	 * @return true if click events are propagated.
	 */
	public boolean propagateClicks()
	{
		return mPropagateClicks;
	}

	/**
	 * Sets whether this NestedViewPager will try to find its nearest parent
	 * with clickable=true and call setPressed/onClicked on that View.
	 * 
	 * @param propagate
	 *            True to propagate events. Default is false.
	 */
	public void setPropagateClicks(boolean propagate)
	{
		mPropagateClicks = propagate;
	}

	private float downX = 0;
	private float downY = 0;

	@Override
	public boolean canScrollHorizontally(int direction)
	{
		if (direction < 0 && getCurrentItem() == 0 && mPositionOffset == 0)
		{
			return false;
		}
		else if (direction > 0 && mPositionOffset == 0)
		{
			if (getAdapter() != null)
			{
				int current = getCurrentItem();
				int count = getAdapter().getCount();
				if (current >= (count - 1))
				{
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event)
	{
		int action = event.getAction();
		switch (action)
		{
		case MotionEvent.ACTION_DOWN:
			downX = event.getX();
			downY = event.getY();
			if (mPropagateClicks)
			{
				propagatePressed(true);
				return true;
			}
			break;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int action = event.getAction();
		switch (action)
		{
		case MotionEvent.ACTION_DOWN:

			downX = event.getX();
			downY = event.getY();
			if (mPropagateClicks)
				propagatePressed(true);
			break;

		case MotionEvent.ACTION_MOVE:
			try
			{
				if (mPropagateClicks)
				{
					float xDeltaTotal = event.getX() - downX;
					float yDeltaTotal = event.getY() - downY;
					if (Math.abs(xDeltaTotal) >= CLICK_SENSITIVITY || Math.abs(yDeltaTotal) >= CLICK_SENSITIVITY)
					{
						propagatePressed(false);
					}
				}

				float xDelta = event.getX() - downX;
				float yDelta = event.getY() - downY;
				if (event.getHistorySize() > 0)
				{
					xDelta = event.getX() - event.getHistoricalX(0);
					yDelta = event.getY() - event.getHistoricalY(0);
				}

				if (Math.abs(xDelta) > Math.abs(yDelta))
				{
					if (xDelta > 0 && getCurrentItem() == 0 && mPositionOffset == 0)
					{
						return false;
					}
					else if (xDelta < 0 && mPositionOffset == 0)
					{
						if (getAdapter() != null)
						{
							int current = getCurrentItem();
							int count = getAdapter().getCount();
							if (current >= (count - 1))
							{
								return false;
							}
						}
					}
					NestedViewPager.this.getParent().requestDisallowInterceptTouchEvent(true);
				}
			}
			catch (Exception e)
			{
				// nothing
			}
			break;

		case MotionEvent.ACTION_UP:
			if (mPropagateClicks)
			{
				propagatePressed(false);

				float xDelta = event.getX() - downX;
				float yDelta = event.getY() - downY;
				if (Math.abs(xDelta) < CLICK_SENSITIVITY && Math.abs(yDelta) < CLICK_SENSITIVITY)
				{
					propagateClicked();
				}
			}
			break;
		}
		return super.onTouchEvent(event);
	}

	public void setViewPagerIndicator(ViewPagerIndicator viewPagerIndicator)
	{
		mViewPagerIndicator = viewPagerIndicator;
		updateViewPagerIndicator();
	}

	/**
	 * If a ViewPagerIndicator is set (using
	 * {@link #setViewPagerIndicator(ViewPagerIndicator)}) this will update it
	 * with total number of items and the current item.
	 */
	private void updateViewPagerIndicator()
	{
		if (mViewPagerIndicator != null)
		{
			if (getAdapter() != null)
				mViewPagerIndicator.onTotalChanged(getAdapter().getCount());
			else
				mViewPagerIndicator.onTotalChanged(0);
			mViewPagerIndicator.onCurrentChanged(this.getCurrentItem());
		}
	}

	private final DataSetObserver mDataSetObserver = new DataSetObserver()
	{
		@Override
		public void onChanged()
		{
			if (mViewPagerIndicator != null)
			{
				if (getAdapter() != null)
					mViewPagerIndicator.onTotalChanged(getAdapter().getCount());
			}
		}
	};

	@Override
	public void setAdapter(PagerAdapter adapter)
	{
		// Unregister old, if any
		if (getAdapter() != null)
			getAdapter().unregisterDataSetObserver(mDataSetObserver);
		if (adapter != null)
			adapter.registerDataSetObserver(mDataSetObserver);
		super.setAdapter(adapter);

		updateViewPagerIndicator();
	}

	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener)
	{
		mOnPageChangeListener = listener;
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (getAdapter() != null)
			getAdapter().notifyDataSetChanged();
	}

	private Runnable propagatePressed;

	private void propagatePressed(boolean pressed)
	{
		if (pressed)
		{
			if (propagatePressed == null)
			{
				propagatePressed = new Runnable()
				{
					@Override
					public void run()
					{
						_propagatePressed(true);
					}
				};
			}
			this.removeCallbacks(propagatePressed);
			this.postDelayed(propagatePressed, 200);
		}
		else
		{
			if (propagatePressed != null)
				this.removeCallbacks(propagatePressed);
			_propagatePressed(false);
		}
	}

	private void _propagatePressed(boolean pressed)
	{
		ViewParent parent = getParent();
		while (parent != null && parent instanceof View)
		{
			if (((View) parent).isClickable())
			{
				((View) parent).setPressed(pressed);
				break;
			}
			parent = parent.getParent();
		}
	}

	private void propagateClicked()
	{
		ViewParent parent = getParent();
		while (parent != null && parent instanceof View)
		{
			if (((View) parent).isClickable())
			{
				((View) parent).performClick();
				break;
			}
			parent = parent.getParent();
		}
	}
}
