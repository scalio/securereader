package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.yakreader.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.ActionBar;

public class LeftSideMenu
{
	public static final String LOGTAG = "LeftSideMenu";

	public interface LeftSideMenuListener
	{
		void onMenuCreated(View parent, View menuRoot, View menu);

		void onBeforeShow();

		void onHide();
	}

	private static final int ANIMATION_SLIDE_DURATION = 600; // Duration of the
																// slide
																// animation in
																// ms
	private static final float ANIMATION_DECELERATION = 2f;

	private static final float RUBBERBAND_LIMIT = 30.0f;
	private static final boolean USE_SHADOW = false;

	private final Activity mActivity;
	private final ActionBar mActionBar;
	private final int mResIdMenuLayout;
	private final FrameLayout mParent;
	private final View mContent;
	private boolean mMenuShown = false;
	private int mMenuWidth;
	private int mStatusBarHeight = 0;
	private LeftMenuRootLayout mRoot;
	private View mMenuView;
	private boolean mIsAnimating;
	private boolean mIsDragEnabled;
	private float mShowPercentage;

	private LeftSideMenuListener mListener;

	private int positionMenu;
	private int positionContent;
	private boolean mIsInteractive;
	private boolean mDisplayMenuIndicator;

	public LeftSideMenu(Activity activity, ActionBar actionBar, int resIdMenuLayout)
	{
		this.mActivity = activity;
		this.mActionBar = actionBar;
		this.mResIdMenuLayout = resIdMenuLayout;
		mIsInteractive = false;
		mIsDragEnabled = true;
		mIsAnimating = false;
		mShowPercentage = 0;
		mDisplayMenuIndicator = true;
		
		mParent = (FrameLayout) mActivity.getWindow().getDecorView();
		mContent = mParent.getChildAt(0);

		final ViewTreeObserver vto = mParent.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				int actionBarHeight = mActionBar.getHeight();
				if (actionBarHeight == 0)
					return; // No action bar height yet
				
				try
				{
					// remove the listener... or we'll be doing this a lot.
					ViewTreeObserver observer = vto;
					if (!observer.isAlive())
						observer = mContent.getViewTreeObserver();
					observer.removeGlobalOnLayoutListener(this);
				}
				catch (Exception ex)
				{
				}

				createMenuView();
				if (mListener != null)
					mListener.onMenuCreated(mParent, mRoot, mMenuView);
			}
		});
	}

	public void checkMenuCreated()
	{
		if (mParent != null && mRoot != null && mMenuView != null && mListener != null)
			mListener.onMenuCreated(mParent, mRoot, mMenuView);
	}

	public View getMenuView()
	{
		return mMenuView;
	}

	public boolean isDragEnabled()
	{
		return mIsDragEnabled;
	}

	public void setDragEnabled(boolean enabled)
	{
		mIsDragEnabled = enabled;
	}

	public void setListener(LeftSideMenuListener listener)
	{
		this.mListener = listener;
	}

	public void show()
	{
		this.show(true, true);
	}

	public void toggle()
	{
		if (mMenuShown)
			hide();
		else
			show();
	}

	public int getMenuWidth()
	{
		createMenuView();
		return mMenuWidth;
	}

	private View createMenuView()
	{
		if (mMenuView == null)
		{
			// get the height of the status bar
			if (mStatusBarHeight == 0)
			{
				mStatusBarHeight = UIHelpers.getStatusBarHeight(mActivity);
			}

			// Create the new root container that will hold both our menu and
			// the actual content
			// (as well as the hidden touch intercept view and the "drag bar")
			//
			mRoot = new LeftMenuRootLayout(mActivity);
			FrameLayout.LayoutParams laysRoot = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.LEFT | Gravity.TOP);
			laysRoot.setMargins(0, mStatusBarHeight, 0, 0);
			mRoot.setLayoutParams(laysRoot);

			LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mMenuView = inflater.inflate(mResIdMenuLayout, mRoot, false);
			mMenuView.setTag(this);

			int actionBarHeight = mActionBar.getHeight();

			FrameLayout.LayoutParams lays = (LayoutParams) mMenuView.getLayoutParams();
			lays.setMargins(0, actionBarHeight, 0, 0);
			lays.gravity = Gravity.LEFT | Gravity.TOP;
			mMenuView.setLayoutParams(lays);
			mMenuView.setVisibility(View.GONE);
			mMenuWidth = lays.width;
			if (USE_SHADOW)
				mMenuWidth += 30;

			// Then move the content over to our new root
			//
			mParent.removeView(mContent);

			// Set content layout params
			//
			FrameLayout.LayoutParams lpContent = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.LEFT | Gravity.TOP);
			lpContent.setMargins(0, -mStatusBarHeight, 0, 0);
			mContent.setLayoutParams(lpContent);

			mRoot.addView(mContent);
			mRoot.addView(mMenuView);
			mParent.addView(mRoot);
		}
		return mMenuView;
	}

	public boolean showMenuHintIfNotShown()
	{
		if (mMenuView != null)
		{
			if (!App.getSettings().hasShownMenuHint())
			{
				mRoot.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						showBounce(0f);
						App.getSettings().setHasShownMenuHint(true);
					}
				}, 3000);
				return true;
			}
		}
		return false;
	}

	public void show(boolean animate, boolean callOnBeforeShow)
	{
		if (mIsAnimating)
			return;
		mIsAnimating = true;

		if (mListener != null && callOnBeforeShow)
			mListener.onBeforeShow();

		final MenuShowAnimation anim = new MenuShowAnimation(100.0f);
		anim.setInterpolator(new DecelerateInterpolator(ANIMATION_DECELERATION));
		anim.setDuration(ANIMATION_SLIDE_DURATION);
		anim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				mRoot.post(new Runnable()
				{
					@Override
					public void run()
					{
						enableHardwareLayering(false);
						setInteractive(true);
					}
				});
				mIsAnimating = false;
				mMenuShown = true;
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
		mMenuView.startAnimation(anim);
	}

	public void hide()
	{
		if (mIsAnimating)
			return;
		mIsAnimating = true;

		if (mMenuView != null)
		{
			setInteractive(false);
			enableHardwareLayering(true);

			final MenuShowAnimation anim = new MenuShowAnimation(0.0f);
			anim.setInterpolator(new DecelerateInterpolator(ANIMATION_DECELERATION));
			anim.setDuration(ANIMATION_SLIDE_DURATION);
			anim.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationEnd(Animation animation)
				{
					mIsAnimating = false;
					mMenuShown = false;
					if (mListener != null)
						mListener.onHide();
					mRoot.post(new Runnable()
					{
						@Override
						public void run()
						{
							enableHardwareLayering(false);
						}
					});
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
			mMenuView.startAnimation(anim);
		}
	}

	public void showBounce(final float fractionVisible)
	{
		if (mIsAnimating)
			return;
		mIsAnimating = true;

		if (mListener != null)
			mListener.onBeforeShow();

		setInteractive(false);
		enableHardwareLayering(true);

		final MenuShowAnimation anim = new MenuShowAnimation(40.0f);
		MenuBounceInterpolator ip = new MenuBounceInterpolator(fractionVisible);
		anim.setInterpolator(ip);
		anim.setDuration(4 * ANIMATION_SLIDE_DURATION);
		anim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				mIsAnimating = false;
				mMenuShown = false;
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
		mMenuView.startAnimation(anim);
	}

	public class MenuBounceInterpolator implements Interpolator
	{
		private final Interpolator mDecelerateInterpolator = new DecelerateInterpolator();
		private final Interpolator mBounceInterpolator = new BounceInterpolator();
		private final float mFractionVisible;

		public MenuBounceInterpolator(float fractionVisible)
		{
			mFractionVisible = fractionVisible;
		}

		@Override
		public float getInterpolation(float t)
		{
			// The purpose of this is pretty simple :
			// - mDecelerateInterpolator for t < 0.33
			// - mBounceInterpolator for t >= 0.33
			if (t < 0.33)
			{
				return mDecelerateInterpolator.getInterpolation(t / 0.33f);
			}
			else
			{
				return 1f - (1.0f - mFractionVisible) * mBounceInterpolator.getInterpolation((t - 0.33f) / 0.67f);
			}
		}

	}

	private boolean getInteractive()
	{
		return mIsInteractive;
	}

	private void setInteractive(boolean interactive)
	{
		if (mIsInteractive != interactive)
		{
			mIsInteractive = interactive;
			if (mIsInteractive)
			{
				AnimationHelpers.setTranslationX(mContent, positionContent);
				AnimationHelpers.setTranslationX(mMenuView, positionMenu);
			}
			else
			{
				AnimationHelpers.setTranslationX(mContent, 0);
				AnimationHelpers.setTranslationX(mMenuView, 0);
			}
			mRoot.invalidate();
		}
	}

	@SuppressLint("NewApi")
	private void enableHardwareLayering(boolean enable)
	{
		if (Build.VERSION.SDK_INT >= 11)
		{
			int layerType = enable ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;
			if (layerType != mMenuView.getLayerType())
			{
				mMenuView.setLayerType(layerType, null);
				mContent.setLayerType(layerType, null);
			}
		}
		else if (mMenuView.isDrawingCacheEnabled() != enable)
		{
			mMenuView.setDrawingCacheEnabled(enable);
			mContent.setDrawingCacheEnabled(enable);
		}
	}

	public boolean isOpen()
	{
		return mMenuShown;
	}

	public float getShowPercentage()
	{
		return mShowPercentage;
	}

	@SuppressLint("NewApi")
	public void setShowPercentage(float percent)
	{
		if (percent > 100.0f)
			percent = 100.0f;
		else if (percent < 0.0f)
			percent = 0.0f;

		mShowPercentage = percent;

		createMenuView();

		int contentPercentage = (int) (0 * ((mMenuWidth * percent) / 100.0f));
		int menuPercentage = -(int) ((mMenuWidth * (100.0f - percent)) / 100.0f);

		if (mIsInteractive)
		{
			AnimationHelpers.setTranslationX(mContent, contentPercentage);
			AnimationHelpers.setTranslationX(mMenuView, menuPercentage);
		}
		else
		{
			positionContent = contentPercentage;
			positionMenu = menuPercentage;
		}
		ViewCompat.postInvalidateOnAnimation(mRoot);

		if (percent == 0)
		{
			mMenuView.setVisibility(View.GONE);
		}
		else
		{
			mMenuView.setVisibility(View.VISIBLE);
		}
	}

	public class MenuShowAnimation extends Animation
	{
		float fromShowPercentage;
		float toShowPercentage;

		public MenuShowAnimation(float toShowPercentage)
		{
			this.fromShowPercentage = LeftSideMenu.this.getShowPercentage();
			this.toShowPercentage = toShowPercentage;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			float newPercentage = fromShowPercentage + ((toShowPercentage - fromShowPercentage) * interpolatedTime);
			LeftSideMenu.this.setShowPercentage(newPercentage);
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

	private class LeftMenuRootLayout extends FrameLayout
	{
		private final Paint mShadowPaint;
		private int mMenuIndicatorY;
		private int mMenuIndicatorInset;
		private BitmapDrawable mMenuIndicatorDrawable;
		private boolean mClickedOutside;
		private int mTranslationAtStartOfDrag;
		private final int mTouchSlop;
		private boolean mIsBeingDragged;
		private boolean mIsUnableToDrag;
		private int mActivePointerId;
		private float mInitialMotionX;
		private float mInitialMotionY;
		private float mLastMotionX;
		private float mLastMotionY;

		public LeftMenuRootLayout(Context context)
		{
			super(context);

			mShadowPaint = new Paint();
			mShadowPaint.setAntiAlias(true);
			mShadowPaint.setDither(true);
			mShadowPaint.setStyle(Paint.Style.FILL);
			mShadowPaint.setShader(new LinearGradient(0f, 0f, 30f, 0f, new int[] { 0xff000000, 0x60000000, 0x00000000 }, new float[] { 0f, 0.2f, 1f },
					Shader.TileMode.CLAMP));

			updateMenuIcon();

			mIsBeingDragged = false;

			final ViewConfiguration configuration = ViewConfiguration.get(context);
			mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
		}

		public void updateMenuIcon()
		{
			// Get information about menu icon to display (if any)
			//
			TypedArray styled = getContext().obtainStyledAttributes(new int[] { 
					com.actionbarsherlock.R.attr.actionBarSize,
					R.attr.actionBarMenuIcon,
					R.attr.actionBarMenuIconInset
			} );
			int actionBarHeight = styled.getDimensionPixelOffset(0, UIHelpers.dpToPx(54, getContext()));
			Drawable actionBarMenuIcon = styled.getDrawable(1);
			mMenuIndicatorInset = styled.getDimensionPixelOffset(2, UIHelpers.dpToPx(6, getContext()));
			styled.recycle();
			if (actionBarMenuIcon != null && actionBarMenuIcon instanceof BitmapDrawable)
				mMenuIndicatorDrawable = (BitmapDrawable) actionBarMenuIcon;
			else
				mMenuIndicatorDrawable = null;
			mMenuIndicatorY = actionBarHeight / 2;			
		}
		
		@SuppressLint("NewApi")
		@Override
		protected boolean drawChild(Canvas canvas, View child, long drawingTime)
		{
			boolean ret;
			canvas.save();

			if (!getInteractive())
			{
				if (child == mContent)
				{
					canvas.translate(positionContent, 0);
				}
				else if (child == mMenuView)
				{
					canvas.translate(positionMenu, 0);
					canvas.clipRect(0, mMenuView.getTop(), mMenuWidth, mMenuView.getBottom(), Op.REPLACE);
				}
			}

			ret = super.drawChild(canvas, child, drawingTime);

			// Draw shadow?
			if (child == mMenuView && USE_SHADOW)
			{
				int offset = -positionMenu;
				if (offset > -mMenuWidth)
				{
					canvas.translate(mMenuWidth - 30, 0);
					canvas.drawRect(0, mMenuView.getTop(), 30f, mMenuView.getBottom(), mShadowPaint);
				}
			}
			
			canvas.restore();
			
			// Draw menu indicator icon?
			if (child == mContent && mMenuIndicatorDrawable != null && mDisplayMenuIndicator && mIsDragEnabled)
			{
				canvas.save();
				canvas.translate(positionContent, 0);
				int xTo = - (int)((mMenuIndicatorInset) * getShowPercentage() / 100f);
				Bitmap bmp = mMenuIndicatorDrawable.getBitmap();
				canvas.clipRect(0, 0, xTo + bmp.getWidth(), mContent.getBottom(), Op.REPLACE);
				canvas.drawBitmap(bmp, xTo, mMenuIndicatorY - (bmp.getHeight() / 2), null);
				canvas.restore();
			}
			
			return ret;
		}

		private boolean isWithinDragHandle(float x, float y)
		{
			Rect draggerRect = new Rect();
			if (mContent.getGlobalVisibleRect(draggerRect))
			{
				draggerRect.left -= UIHelpers.dpToPx(30, getContext());
				draggerRect.right = draggerRect.left + UIHelpers.dpToPx(40, getContext());
				draggerRect.top += mActionBar.getHeight();
				if (draggerRect.contains((int) x, (int) y))
				{
					return true;
				}
			}
			return false;
		}

		private boolean isWithinMenu(float x, float y)
		{
			Rect menuRect = new Rect();
			if (mMenuView.getGlobalVisibleRect(menuRect))
			{
				// Adjust rect to menu width!
				menuRect.right = menuRect.left + mMenuWidth;
				if (menuRect.contains((int) x, (int) y))
				{
					return true;
				}
			}
			return false;
		}

		protected boolean canScroll(View v, boolean checkV, int dx, int x, int y)
		{
			if (v.getVisibility() != View.VISIBLE)
				return false;

			if (v instanceof ViewGroup)
			{
				final ViewGroup group = (ViewGroup) v;
				final int scrollX = v.getScrollX();
				final int scrollY = v.getScrollY();
				final int count = group.getChildCount();
				// Count backwards - let topmost views consume scroll distance
				// first.
				for (int i = count - 1; i >= 0; i--)
				{
					final View child = group.getChildAt(i);
					if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
							&& canScroll(child, true, dx, x + scrollX - child.getLeft(), y + scrollY - child.getTop()))
					{
						return true;
					}
				}
			}
			return checkV && UIHelpers.canScrollHorizontally(v, -dx);
		}

		private void onSecondaryPointerUp(MotionEvent ev)
		{
			final int pointerIndex = MotionEventCompat.getActionIndex(ev);
			final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
			if (pointerId == mActivePointerId)
			{
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
				mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
			}
		}

		private void startDrag()
		{
			if (!mIsDragEnabled || mIsBeingDragged)
				return;

			mIsBeingDragged = true;
			if (mListener != null)
				mListener.onBeforeShow();
			mTranslationAtStartOfDrag = AnimationHelpers.getTranslationX(mContent);

			setInteractive(false);
			enableHardwareLayering(true);
		}

		private void endDrag()
		{
			mIsBeingDragged = false;
			mIsUnableToDrag = false;

			// Open or close, based on how far we pulled it out
			float rubberbandPercentage = LeftSideMenu.this.isOpen() ? (100.0f - RUBBERBAND_LIMIT) : RUBBERBAND_LIMIT;
			if (LeftSideMenu.this.getShowPercentage() > rubberbandPercentage)
				LeftSideMenu.this.show(true, false);
			else
				LeftSideMenu.this.hide();
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev)
		{
			/*
			 * This method JUST determines whether we want to intercept the
			 * motion. If we return true, onMotionEvent will be called and we do
			 * the actual scrolling there.
			 */
			final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
			// Always take care of the touch gesture being complete.
			if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
			{
				// Release the drag.
				mIsBeingDragged = false;
				mIsUnableToDrag = false;
				mActivePointerId = -1;
				return false;
			}
			// Nothing more to do here if we have decided whether or not we
			// are dragging.
			if (action != MotionEvent.ACTION_DOWN)
			{
				if (mIsBeingDragged)
				{
					return true;
				}
				if (mIsUnableToDrag)
				{
					return false;
				}
			}
			switch (action)
			{
			case MotionEvent.ACTION_MOVE:
			{
				/*
				 * mIsBeingDragged == false, otherwise the shortcut would have
				 * caught it. Check whether the user has moved far enough from
				 * his original down touch.
				 */
				/*
				 * Locally do absolute value. mLastMotionY is set to the y value
				 * of the down event.
				 */
				final int activePointerId = mActivePointerId;
				if (activePointerId == -1)
				{
					// If we don't have a valid id, the touch down wasn't on
					// content.
					break;
				}
				final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
				final float x = MotionEventCompat.getX(ev, pointerIndex);
				final float dx = x - mLastMotionX;
				final float xDiff = Math.abs(dx);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				final float yDiff = Math.abs(y - mInitialMotionY);
				if (dx != 0 && canScroll(this, false, (int) dx, (int) x, (int) y))
				{
					// Nested view has scrollable area under this point. Let it
					// be handled there.
					mLastMotionX = x;
					mLastMotionY = y;
					mIsUnableToDrag = true;
					return false;
				}
				if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff)
				{
					mLastMotionX = dx > 0 ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
					mLastMotionY = y;
					startDrag();
				}
				else if (yDiff > mTouchSlop)
				{
					// The finger has moved enough in the vertical
					// direction to be counted as a drag... abort
					// any attempt to drag horizontally, to work correctly
					// with children that have scrolling containers.
					mIsUnableToDrag = true;
				}
				if (mIsBeingDragged)
				{
					// Scroll to follow the motion event
					performDrag(x);
				}
				break;
			}
			case MotionEvent.ACTION_DOWN:
			{
				/*
				 * Remember location of down touch. ACTION_DOWN always refers to
				 * pointer index 0.
				 */
				mLastMotionX = mInitialMotionX = ev.getX();
				mLastMotionY = mInitialMotionY = ev.getY();
				mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
				mIsUnableToDrag = false;
				mIsBeingDragged = false;

				// If we clicked in the drag handle (and drag enabled),
				// intercept the event!
				if (isWithinDragHandle(mLastMotionX, mLastMotionY))
					startDrag();

				else if ((LeftSideMenu.this.isOpen() || LeftSideMenu.this.mIsAnimating) && !isWithinMenu(mLastMotionX, mLastMotionY))
				{
					mClickedOutside = true;
				}

				break;
			}
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				break;
			}
			/*
			 * The only time we want to intercept motion events is if we are in
			 * the drag mode or if the menu is open and we clicked outside the
			 * menu content.
			 */
			return mIsBeingDragged || mClickedOutside;
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev)
		{
			if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0)
			{
				// Don't handle edge touches immediately -- they may actually
				// belong to one of our
				// descendants.
				return false;
			}

			final int action = ev.getAction();

			// Of this is a click outside the menu area, consume the event
			if (mClickedOutside)
			{
				if ((action & MotionEventCompat.ACTION_MASK) == MotionEvent.ACTION_UP)
				{
					mClickedOutside = false;
					LeftSideMenu.this.hide();
				}
				return true;
			}

			switch (action & MotionEventCompat.ACTION_MASK)
			{
			case MotionEvent.ACTION_DOWN:
			{
				// Remember where the motion event started
				mLastMotionX = mInitialMotionX = ev.getX();
				mLastMotionY = mInitialMotionY = ev.getY();
				mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
				startDrag();
				break;
			}
			case MotionEvent.ACTION_MOVE:
				if (!mIsBeingDragged)
				{
					final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
					final float x = MotionEventCompat.getX(ev, pointerIndex);
					final float xDiff = Math.abs(x - mLastMotionX);
					final float y = MotionEventCompat.getY(ev, pointerIndex);
					final float yDiff = Math.abs(y - mLastMotionY);
					if (xDiff > mTouchSlop && xDiff > yDiff)
					{
						mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
						mLastMotionY = y;
						startDrag();
					}
				}
				// Not else! Note that mIsBeingDragged can be set above.
				if (mIsBeingDragged)
				{
					// Scroll to follow the motion event
					final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
					final float x = MotionEventCompat.getX(ev, activePointerIndex);
					performDrag(x);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mIsBeingDragged)
				{
					mActivePointerId = -1;
					endDrag();
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				if (mIsBeingDragged)
				{
					mActivePointerId = -1;
					endDrag();
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
			{
				final int index = MotionEventCompat.getActionIndex(ev);
				final float x = MotionEventCompat.getX(ev, index);
				mLastMotionX = x;
				mActivePointerId = MotionEventCompat.getPointerId(ev, index);
				break;
			}
			case MotionEventCompat.ACTION_POINTER_UP:
				onSecondaryPointerUp(ev);
				mLastMotionX = MotionEventCompat.getX(ev, MotionEventCompat.findPointerIndex(ev, mActivePointerId));
				break;
			}
			return true;
		}

		private boolean performDrag(float x)
		{
			mLastMotionX = x;
			setShowPercentage(100.0f * ((x - this.mInitialMotionX + mTranslationAtStartOfDrag) / getMenuWidth()));
			return true;
		}

	}

	public void onConfigurationChanged()
	{
		if (mRoot != null)
			mRoot.updateMenuIcon();
	}
	
	public void setDisplayMenuIndicator(boolean displayMenuIndicator)
	{
		mDisplayMenuIndicator = displayMenuIndicator;
	}
}