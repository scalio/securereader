package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

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
				mBitmap = Bitmap.createBitmap(bmp.getWidth() - params.rightMargin - params.leftMargin, bmp.getHeight() - params.bottomMargin - params.topMargin, bmp.getConfig());
				mBitmap.eraseColor(Color.WHITE);
				Canvas canvas = new Canvas(mBitmap);  // create a canvas to draw on the new image
				Rect source = new Rect(params.leftMargin, params.topMargin, params.leftMargin + bmp.getWidth() - params.rightMargin, params.topMargin + bmp.getHeight() - params.bottomMargin);
				Rect dest = new Rect(0, 0, source.width(), source.height());
				canvas.drawBitmap(bmp, source, dest, null);
				bmp.recycle();  // clear out old image 
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
		// TODO - remove old snapshot and take new one here to save memory?
		takeSnapshot();
		mUseBitmap = true;

		final ExpandAnim anim = new ExpandAnim(mCurrentClip, mCollapsedClip, mCurrentTop, mCollapsedTop, mCurrentHeight, mCollapsedHeight);
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
}
