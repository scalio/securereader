package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

public class UpdatingTextView extends CustomFontTextView
{
	public interface OnUpdateListener
	{
		void onUpdateNeeded(UpdatingTextView view);
	}

	private OnUpdateListener mOnUpdateListener;

	public UpdatingTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public UpdatingTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public UpdatingTextView(Context context)
	{
		super(context);
	}

	public void setOnUpdateListener(OnUpdateListener listener)
	{
		mOnUpdateListener = listener;
		startUpdateTimer();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		startUpdateTimer();
		if (mOnUpdateListener != null)
			mOnUpdateListener.onUpdateNeeded(this);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		stopUpdateTimer();
		super.onDetachedFromWindow();
	}

	private void startUpdateTimer()
	{
		stopUpdateTimer();

		Handler handler = getHandler();
		if (mOnUpdateListener != null && handler != null)
		{
			handler.postDelayed(mRunnableUpdate, 1000 * 60); // Every minute
		}
	}

	private void stopUpdateTimer()
	{
		Handler handler = getHandler();
		if (handler != null)
		{
			handler.removeCallbacks(mRunnableUpdate);
		}
	}

	private final Runnable mRunnableUpdate = new Runnable()
	{
		@Override
		public void run()
		{
			// Every minute
			if (mOnUpdateListener != null)
			{
				mOnUpdateListener.onUpdateNeeded(UpdatingTextView.this);
				Handler handler = getHandler();
				if (handler != null)
				{
					handler.postDelayed(mRunnableUpdate, 1000 * 60); // Every
																		// minute
				}
			}
		}
	};

}
