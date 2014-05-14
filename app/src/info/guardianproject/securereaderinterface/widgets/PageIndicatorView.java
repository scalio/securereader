package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.models.ViewPagerIndicator;
import info.guardianproject.securereaderinterface.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

public class PageIndicatorView extends View implements ViewPagerIndicator 
{
	private int mGravity;
	private int mNumDots; // Total number of dots
	private int mCurrentDot; // Current dot
	private boolean mHideIfOnlyOne = true;
	private Drawable mDrawable;
	private int mWselected;
	private int mHselected;
	private int mWunselected;
	private int mHunselected;
	
	public PageIndicatorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public PageIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public PageIndicatorView(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		mGravity = Gravity.CENTER;

		if (attrs != null)
		{
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PageIndicatorView);
			mGravity = a.getInt(R.styleable.PageIndicatorView_android_gravity, Gravity.CENTER);
			mNumDots = a.getInt(R.styleable.PageIndicatorView_dot_total, 3);
			mCurrentDot = a.getInt(R.styleable.PageIndicatorView_dot_current, 1);
			mHideIfOnlyOne = a.getBoolean(R.styleable.PageIndicatorView_hide_if_only_one, true);
			mDrawable = a.getDrawable(R.styleable.PageIndicatorView_android_drawable);
			a.recycle();
		}
		
		if (mDrawable != null)
		{
			mDrawable.setState(new int[] { android.R.attr.state_selected });
			mWselected = mDrawable.getCurrent().getIntrinsicWidth();
			mHselected = mDrawable.getCurrent().getIntrinsicHeight();
			mDrawable.setState(new int[] { 0 });
			mWunselected = mDrawable.getCurrent().getIntrinsicWidth();
			mHunselected = mDrawable.getCurrent().getIntrinsicHeight();
		}
	}
	
	/*
	 * Calculate the number of dots we can fit in the screen space we are given.
	 */
	private int getMaxNumberOfDots()
	{
		int w = this.getWidth();
		if (mWunselected == -1 || mWselected == -1)
			return 0;
		
		// Start with current "dot"
		int n = 0;
		if (w >= mWselected)
		{
			n++;
			w -= mWselected;
		}
		if (w >= mWunselected)
		{
			n += w / mWunselected;
		}
		return n;
	}

	private int getNumDotsWidth(int numDots)
	{
		if (mWunselected == -1 || mWselected == -1)
			return 0;
		
		// Start with current "dot"
		int n = 0;
		if (numDots >= 1)
		{
			numDots -= 1;
			n += mWselected;
		}
		n += numDots * mWunselected;
		return n;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mNumDots <= 1 && mHideIfOnlyOne)
		{
			return;
		}

		int sc = canvas.save();

		int numDotsToDraw = mNumDots;
		int currentDot = mCurrentDot;

		// If all dots don't fit, show only the remaining pages (see design
		// document)
		//
		int dotsPerPage = getMaxNumberOfDots();
		if (dotsPerPage < mNumDots)
		{
			// Get the current "page" for the current dot
			int page = (int) ((float) mCurrentDot / (dotsPerPage - 1));
			currentDot = (int) ((float) mCurrentDot % (dotsPerPage - 1));
			numDotsToDraw = Math.min(dotsPerPage, 1 + mNumDots - (page * dotsPerPage));
		}

		int xPos = 0;
		int widthOfDots = this.getNumDotsWidth(numDotsToDraw);
		if (mGravity == Gravity.RIGHT)
		{
			xPos = getWidth() - widthOfDots;
		}
		else if (mGravity == Gravity.CENTER_HORIZONTAL)
		{
			xPos = (getWidth() - widthOfDots) / 2;
		}
		
		// Do the draw
		for (int i = 0; i < numDotsToDraw; i++)
		{
			if (i == currentDot)
			{
				mDrawable.setState(new int[] { android.R.attr.state_selected });
				mDrawable.getCurrent().setBounds(xPos, getHeight() / 2 - mHselected / 2, xPos + mWselected, getHeight() / 2 + mHselected / 2);
				mDrawable.getCurrent().draw(canvas);
				xPos += mWselected;
			}
			else
			{
				mDrawable.setState(new int[] { 0 });
				mDrawable.getCurrent().setBounds(xPos, getHeight() / 2 - mHunselected / 2, xPos + mWunselected, getHeight() / 2 + mHunselected / 2);
				mDrawable.getCurrent().draw(canvas);
				xPos += mWunselected;
			}
		}

		canvas.restoreToCount(sc);
	}

	@Override
	public void onTotalChanged(int total)
	{
		if (total != mNumDots)
		{
			mNumDots = total;
			invalidate();
		}
	}

	@Override
	public void onCurrentChanged(int current)
	{
		if (current != mCurrentDot)
		{
			mCurrentDot = current;
			invalidate();
		}
	}
	
}
