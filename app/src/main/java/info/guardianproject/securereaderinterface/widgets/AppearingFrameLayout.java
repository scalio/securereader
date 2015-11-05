package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.uiutil.ViewExpander;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class AppearingFrameLayout extends FrameLayout
{
	private ViewExpander mExpander;

	public AppearingFrameLayout(Context context)
	{
		super(context);
		init();
	}

	public AppearingFrameLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	public AppearingFrameLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	private void init()
	{
		mExpander = new ViewExpander(this);
	}

	@Override
	public void draw(Canvas canvas)
	{
		mExpander.prepareDraw(canvas);
		super.draw(canvas);
	}

	public void expand()
	{
		mExpander.expand();
	}

	public void collapse()
	{
		mExpander.collapse();
	}
}
