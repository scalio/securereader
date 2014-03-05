package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.yakreader.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ListView;

public class SyncableListView extends ListView
{
	public interface OnPullDownListener
	{
		/**
		 * Called when the list has been pulled down to reveal the header (if
		 * header height is set with
		 * {@link SyncableListView#setHeaderHeight(int)}).
		 * 
		 * @param heightVisible
		 *            Number of visible pixels.
		 */
		void onListPulledDown(int heightVisible);

		/**
		 * Called when the list was "dropped" while it was expanded (if header
		 * height > 0)
		 */
		void onListDroppedWhilePulledDown();
	}

	private int mHeaderHeight;
	private boolean mHeaderEnabled = true;
	private int mCurrentPullDownHeight;
	private boolean mDragging;
	private float mDragStartY;
	private final Interpolator mDragInterpolator = new LinearInterpolator();
	private OnPullDownListener mListener;
	private int mScaledTouchSlop;
	private Paint mEdgePaint;

	public SyncableListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	public SyncableListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public SyncableListView(Context context)
	{
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		this.setAdapter(null);
		
		mEdgePaint = new Paint();
		mEdgePaint.setColor(getContext().getResources().getColor(R.color.grey_dark));
		mEdgePaint.setStyle(Style.STROKE);
		mEdgePaint.setStrokeWidth(0);
	}

	public void setPullDownListener(OnPullDownListener listener)
	{
		mListener = listener;
	}

	public void setHeaderEnabled(boolean enabled)
	{
		mHeaderEnabled = enabled;
	}
	
	public void setHeaderHeight(int height)
	{
		mHeaderHeight = height;
	}

	/**
	 * Examine a touch motion event to see if this is the start of a "pull down" operation.
	 * @param ev
	 * @return True if we should capture motion events for a pull down operation.
	 */
	private boolean checkPullDownStart(MotionEvent ev)
	{
		if (!mDragging && ev.getAction() == MotionEvent.ACTION_MOVE  && mDragStartY != -1 && mHeaderEnabled && mHeaderHeight > 0)
		{
			if (ev.getY() > (mDragStartY + mScaledTouchSlop))
			{
				mDragging = true;
				mDragStartY += mScaledTouchSlop;
				return true;
			}
			else if (ev.getY() < (mDragStartY - mScaledTouchSlop))
			{
				mDragStartY = -1; // reset, no scroll, we are moving up
			}
		}
		return false;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN)
		{
			View view = this.getChildAt(0);
			if (view == null || view.getTop() == 0)
			{
				mDragStartY = ev.getY();
			}
			else
			{
				mDragStartY = -1;
			}
		}
		else if (checkPullDownStart(ev))
		{
			return true;
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		checkPullDownStart(ev);
		
		if (mDragging)
		{
			if (ev.getAction() == MotionEvent.ACTION_MOVE)
			{
				int pullDown = (int) Math.max(0, ev.getY() - mDragStartY);
				if (pullDown > mHeaderHeight)
				{
					// Adjust dragStartY so that moving upwards will start to close the panel
					// immediately after touch slop!
					if (pullDown > (mHeaderHeight + mScaledTouchSlop))
						mDragStartY += (pullDown - (mHeaderHeight + mScaledTouchSlop));
					pullDown = mHeaderHeight;
				}
				
				float fractionVisible = (float)pullDown / (float)mHeaderHeight;
				float outputVisible = mDragInterpolator.getInterpolation(fractionVisible);
				int pullDownPixels = (int) (outputVisible * mHeaderHeight);
				if (pullDownPixels != mCurrentPullDownHeight)
				{
					mCurrentPullDownHeight = pullDownPixels;
					if (mListener != null)
						mListener.onListPulledDown(mCurrentPullDownHeight);
					invalidate();
				}
				return true;
			}
			else if (ev.getAction() == MotionEvent.ACTION_UP)
			{
				// Check if we "dropped" it when it was fully expanded
				if (mHeaderHeight > 0 && mCurrentPullDownHeight == mHeaderHeight && mListener != null)
				{
					mListener.onListDroppedWhilePulledDown();
				}
				mDragging = false;
				mCurrentPullDownHeight = 0;
				if (mListener != null)
					mListener.onListPulledDown(0);
				invalidate();
				return true;
			}
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public int pointToPosition(int x, int y)
	{
		int position = super.pointToPosition(x, y);
		if (position == INVALID_POSITION && this.getAdapter() != null && this.getAdapter().getCount() > 0)
			position = this.getAdapter().getCount() - 1;
		return position;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.translate(0, mCurrentPullDownHeight);
		super.draw(canvas);
		if (mCurrentPullDownHeight != 0)
		{
			// Draw a small edge so it does not blend with the background.
			canvas.drawLine(0, 0, getWidth(), 0, mEdgePaint);
		}
	}
}
