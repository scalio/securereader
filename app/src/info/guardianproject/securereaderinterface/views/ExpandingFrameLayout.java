package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.R;

public class ExpandingFrameLayout extends FrameLayout
{
	public static final String LOGTAG = "ExpandingFrameLayout";
	public static final boolean LOGGING = false;		 
	
	public static final int DEFAULT_EXPAND_DURATION = 500;
	public static final int DEFAULT_COLLAPSE_DURATION = 500;

	public interface ExpansionListener
	{
		void onExpanded();

		void onCollapsed();
	}

	public interface SwipeListener
	{
		void onSwipeUp();

		void onSwipeDown();
	}

	View mContentView;

	int mCollapsedClip;
	int mCollapsedTop;
	int mCollapsedHeight;

	int mCurrentClip = 0;
	int mCurrentTop = 0;
	int mCurrentHeight = 0;

	private boolean mHasExpanded;
	private ExpansionListener mExpansionListener;

	// Swipe listener
	//
	private GestureDetector mGestureDetector;
	private SwipeListener mSwipeListener;

	//
	private boolean isTakingSnap;
	private boolean mUseBitmap = false;
	private Bitmap mBitmap;

	public ExpandingFrameLayout(Context context, View content)
	{
		super(context);

		mContentView = content;

		FrameLayout.LayoutParams lays = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT
				| Gravity.TOP);
		lays.setMargins(0, 0, 0, 0);
		mContentView.setLayoutParams(lays);
		setBackgroundColor(0xffffffff);

		addView(mContentView);

		mSwipeListener = null;
	}

	private final Runnable mExpandRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			expand();
		}
	};

	private final Runnable mExpandNowRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			expand(0);
		}
	};

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (changed)
		{
			if (!mHasExpanded)
			{
				mHasExpanded = true;
				post(mExpandRunnable);
			}
			else
			{
				post(mExpandNowRunnable);
			}
		}
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.post(new Runnable()
		{
			@Override
			public void run()
			{
				// On orientation change we stop the clipping and go full screen
				mCurrentTop = 0;
				mCurrentHeight = getHeight();
				invalidate();
			}
		});
	}

	@Override
	public void draw(Canvas canvas)
	{
		if (isTakingSnap)
			return;

		int bottomMargin = getHeight() - mCurrentTop - mCurrentHeight;

		if (bottomMargin != 0 || mCurrentTop != 0 || mCurrentClip != 0)
		{
			if (mBitmap != null && mUseBitmap)
			{
				Rect rectSource;
				Rect rectDest;

				if ((mCurrentTop + mCurrentClip) > 0)
				{
					// Top part
					int visibleHeight = Math.max(0, mCurrentTop + mCurrentClip);
					rectSource = new Rect(0, Math.max(0, Math.max(0, mCollapsedTop + mCollapsedClip) - visibleHeight), getWidth(), Math.max(0, mCollapsedTop
							+ mCollapsedClip));
					rectDest = new Rect(0, 0, getWidth(), visibleHeight);
					canvas.drawBitmap(mBitmap, rectSource, rectDest, null);
				}
				if (bottomMargin > 0)
				{
					// Bottom part
					rectSource = new Rect(0, mCollapsedTop + mCollapsedHeight, getWidth(), mCollapsedTop + mCollapsedHeight + bottomMargin);
					rectDest = new Rect(0, getHeight() - bottomMargin, getWidth(), getHeight());
					canvas.drawBitmap(mBitmap, rectSource, rectDest, null);
				}
			}

			canvas.translate(0, mCurrentTop);
			canvas.clipRect(new Rect(0, mCurrentClip, getWidth(), mCurrentHeight), Op.REPLACE);
		}
		super.draw(canvas);
	}

	/**
	 * Set the starting (collapsed) size of the view.
	 * 
	 * @param clip
	 *            Top clip margin of the view (if it is hidden by other
	 *            components).
	 * @param top
	 *            Top coordinate of view (in parent coordinates).
	 * @param height
	 *            Height of view.
	 */
	public void setCollapsedSize(int clip, int top, int height)
	{
		mCollapsedClip = clip;
		mCollapsedTop = top;
		mCollapsedHeight = height;
	}

	public void setExpansionListener(ExpansionListener expansionListener)
	{
		mExpansionListener = expansionListener;
	}

	private void setSize(int clip, int top, int height)
	{
		mCurrentClip = clip;
		mCurrentTop = top;
		mCurrentHeight = height;
		invalidate();
	}

	private void takeSnapshot()
	{
		View parent = (View) getParent();
		if (parent != null)
		{
		ViewGroup.MarginLayoutParams params = (MarginLayoutParams) this.getLayoutParams();

		isTakingSnap = true;
		parent.setDrawingCacheEnabled(true);
		Bitmap bmp = parent.getDrawingCache();
		isTakingSnap = false;

			try
			{
		mBitmap = Bitmap.createBitmap(bmp, params.leftMargin, params.topMargin, bmp.getWidth() - params.rightMargin - params.leftMargin,
				bmp.getHeight() - params.bottomMargin - params.topMargin).copy(bmp.getConfig(), false);
			}
			catch(Exception e)
			{
				if (LOGGING)
					Log.e("ExpandingFrameLayout", e.toString());
			}
		parent.setDrawingCacheEnabled(false);
		}
	}

	/**
	 * Expand the view from the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the default
	 * duration.
	 */
	public void expand()
	{
		expand(DEFAULT_EXPAND_DURATION);
	}

	/**
	 * Expand the view from the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the given duration.
	 * 
	 * @param duration
	 *            Duration in milliseconds.
	 */
	public void expand(int duration)
	{
		takeSnapshot();
		mUseBitmap = true;

		ViewGroup.MarginLayoutParams params = (MarginLayoutParams) this.getLayoutParams();

		int toHeight = ((View) getParent()).getHeight() - params.topMargin - params.bottomMargin;

		final ExpandAnim anim = new ExpandAnim(mCollapsedClip, 0, mCollapsedTop, 0, mCollapsedHeight, toHeight);
		anim.setDuration(duration);
		anim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				mUseBitmap = false;
				mBitmap = null;
				if (mExpansionListener != null)
					mExpansionListener.onExpanded();

				// Show hint screen?
				if (!App.getSettings().hasShownSwipeUpHint())
				{
					postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							showSwipeHint();
							App.getSettings().setHasShownSwipeUpHint(true);
						}
					}, 3000);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
		this.startAnimation(anim);
	}

	/**
	 * Collapse the view to the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the default
	 * duration.
	 */
	public void collapse()
	{
		collapse(DEFAULT_COLLAPSE_DURATION);
	}

	/**
	 * Collapse the view to the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the given duration.
	 * 
	 * @param duration
	 *            Duration in milliseconds.
	 */
	public void collapse(int duration)
	{
		ViewGroup.MarginLayoutParams params = (MarginLayoutParams) this.getLayoutParams();
		int toHeight = ((View) getParent()).getHeight() - params.topMargin - params.bottomMargin;

		// TODO - remove old snapshot and take new one here to save memory?
		takeSnapshot();
		mUseBitmap = true;

		final ExpandAnim anim = new ExpandAnim(0, mCollapsedClip, 0, mCollapsedTop, toHeight, mCollapsedHeight);
		anim.setDuration(duration);
		anim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				mUseBitmap = false;
				// mBitmap = null;
				if (mExpansionListener != null)
					mExpansionListener.onCollapsed();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
		this.startAnimation(anim);
	}

	public class ExpandAnim extends Animation
	{
		int fromClip;
		int toClip;
		int fromTop;
		int toTop;
		int fromHeight;
		int toHeight;

		public ExpandAnim(int fromClip, int toClip, int fromTop, int toTop, int fromHeight, int toHeight)
		{
			this.fromClip = fromClip;
			this.toClip = toClip;
			this.fromTop = fromTop;
			this.toTop = toTop;
			this.fromHeight = fromHeight;
			this.toHeight = toHeight;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			int newClip;
			int newTop;
			int newHeight;

			newClip = (int) (fromClip + ((toClip - fromClip) * interpolatedTime));
			newTop = (int) (fromTop + ((toTop - fromTop) * interpolatedTime));
			newHeight = (int) (fromHeight + ((toHeight - fromHeight) * interpolatedTime));

			setSize(newClip, newTop, newHeight);
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
		}

		@Override
		public boolean willChangeBounds()
		{
			return true;
		}
	}

	/*
	 * Add a listener for swipe events (up and down)
	 */
	public void setSwipeListener(SwipeListener listener)
	{
		mSwipeListener = listener;
		if (mSwipeListener != null)
			createGestureDetector();
		else
			mGestureDetector = null;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		// If the event is within our clipped area we should not react to it!
		//
		int[] location = new int[2];
		this.getLocationOnScreen(location);
		if ((location[1] + mCurrentTop) > ev.getY())
			return false;
		if (mGestureDetector != null)
			mGestureDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}

//	@Override
//	public boolean onInterceptTouchEvent(MotionEvent ev)
//	{
//		if (mGestureDetector != null)
//		{
//			if (mGestureDetector.onTouchEvent(ev))
//				return true;
//		}
//		return super.onInterceptTouchEvent(ev);
//	}
//
//	@Override
//	public boolean onTouchEvent(MotionEvent event)
//	{
//		if (mGestureDetector != null)
//		{
//			if (mGestureDetector.onTouchEvent(event))
//				return true;
//		}
//		return super.onTouchEvent(event);
//	}

	public void createGestureDetector()
	{
		if (mGestureDetector == null)
		{
			mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener()
			{	
				private int mTouchSlop;

				public GestureDetector.SimpleOnGestureListener init()
				{
					final ViewConfiguration configuration = ViewConfiguration.get(getContext());
					mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
					return this;
				}
				
				@Override
				public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
				{
					try
					{			
						double xDelta = e1.getX() - e2.getX();
						double yDelta = e2.getY() - e1.getY();
						if (yDelta > mTouchSlop)
						{
							if (Math.abs(xDelta) > (Math.abs(yDelta) / 2))
								return false;
		
							if (mSwipeListener != null)
								mSwipeListener.onSwipeDown();
							return false;
						}
					}
					catch (Exception e)
					{
						// nothing
					}
					return false;
				}
			}.init());
		}
	}

	private final Runnable hideActionBarRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			hideActionBar();
		}
	};

	public void showActionBar(int actionBarHeight)
	{
		removeCallbacks(hideActionBarRunnable);

		final ExpandAnim anim = new ExpandAnim(mCurrentClip, actionBarHeight, mCurrentTop, mCurrentTop, getHeight(), getHeight());
		anim.setDuration(300);
		this.startAnimation(anim);
		this.postDelayed(hideActionBarRunnable, 5000);
	}

	public void hideActionBar()
	{
		final ExpandAnim anim = new ExpandAnim(mCurrentClip, 0, mCurrentTop, mCurrentTop, getHeight(), getHeight());
		anim.setDuration(300);
		this.startAnimation(anim);
	}

	private void showSwipeHint()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		final View view = inflater.inflate(R.layout.story_item_swipe_hint, this, false);
		AnimationHelpers.fadeOut(view, 0, 0, false);
		addView(view);
		AnimationHelpers.fadeIn(view, 500, 3000, true);
	}
}
