package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.R;

public class GroupView extends LinearLayout
{
	boolean mHasBeenMeasured = false;
	boolean mIsExpanded = false;
	View mHeaderView;
	View mCollapseView;

	public GroupView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public GroupView(Context context)
	{
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		if (attrs != null)
		{
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GroupView);
			int resIdHeader = a.getResourceId(R.styleable.GroupView_header_layout, 0);
			int resIdCollapse = a.getResourceId(R.styleable.GroupView_collapse_layout, 0);
			int resIdHeaderString = a.getResourceId(R.styleable.GroupView_header_text, 0);
			int resIdHeaderSubString = a.getResourceId(R.styleable.GroupView_header_sub_text, 0);

			LayoutInflater inflater = LayoutInflater.from(getContext());
			if (resIdHeader != 0)
			{
				mHeaderView = inflater.inflate(resIdHeader, this, false);
				addView(mHeaderView);

				// Set optional title and subtitle
				//
				if (resIdHeaderString != 0)
				{
					setTitle(getContext().getString(resIdHeaderString));
				}
				if (resIdHeaderSubString != 0)
				{
					setSubTitle(getContext().getString(resIdHeaderSubString));
				}

				mHeaderView.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						toggle();
					}
				});
			}

			if (resIdCollapse != 0)
			{
				mCollapseView = inflater.inflate(resIdCollapse, this, false);
				mCollapseView.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						setExpanded(false, true);
					}
				});
			}
			a.recycle();
		}
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();

		if (mCollapseView != null)
			addView(mCollapseView);
	}
	
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		if (params != null && !mHasBeenMeasured)
		{
			if (!mIsExpanded)
				params.height = getCollapsedHeight();
			mHasBeenMeasured = true;
		}
		super.setLayoutParams(params);
	}

	public boolean getExpanded()
	{
		return mIsExpanded;
	}

	public void setExpanded(boolean expanded, boolean animated)
	{
		if (expanded)
			expand(animated);
		else
			collapse(animated);
	}

	public void toggle()
	{
		setExpanded(!getExpanded(), true);
	}

	public void expand(boolean animated)
	{
		mIsExpanded = true;
		if (animated)
		{
			int width = getWidth();
			if (width > 0)
			{
				super.measure(View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
				final ExpandAnim anim = new ExpandAnim(this, getHeight(), getMeasuredHeight());
				anim.setDuration(500);
				this.startAnimation(anim);
			}
		}
		else
		{
			ViewGroup.LayoutParams layoutParams = getLayoutParams();
			if (layoutParams != null)
			{
				layoutParams.height = LayoutParams.WRAP_CONTENT;
				setLayoutParams(layoutParams);
			}
		}
	}

	public void collapse(boolean animated)
	{
		mIsExpanded = false;

		if (animated)
		{
			final ExpandAnim anim = new ExpandAnim(this, getHeight(), getCollapsedHeight());
			anim.setDuration(500);
			this.startAnimation(anim);
		}
		else
		{
			// Default collapsed
			ViewGroup.LayoutParams layoutParams = getLayoutParams();
			if (layoutParams != null)
			{
				layoutParams.height = getCollapsedHeight();
				setLayoutParams(layoutParams);
			}
		}
	}

	private int getCollapsedHeight()
	{
		int toHeight = 0;
		if (mHeaderView != null)
		{
			mHeaderView.measure(View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
			toHeight = mHeaderView.getMeasuredHeight();
		}
		return toHeight;
	}

	private class ExpandAnim extends Animation
	{
		View view;
		int fromHeight;
		int toHeight;

		public ExpandAnim(View view, int fromHeight, int toHeight)
		{
			this.view = view;
			this.fromHeight = fromHeight;
			this.toHeight = toHeight;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			int newHeight;
			newHeight = (int) (fromHeight + ((toHeight - fromHeight) * interpolatedTime));
			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams != null)
			{
				layoutParams.height = newHeight;
				view.setLayoutParams(layoutParams);
			}
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

	public void setTitle(String title)
	{
		if (mHeaderView != null)
		{
			TextView tv = (TextView) mHeaderView.findViewById(R.id.tvTitle);
			if (tv != null)
				tv.setText(title);
		}
	}

	public void setSubTitle(String subTitle)
	{
		if (mHeaderView != null)
		{
			TextView tv = (TextView) mHeaderView.findViewById(R.id.tvSubTitle);
			if (tv != null)
				tv.setText(subTitle);
		}
	}
}
