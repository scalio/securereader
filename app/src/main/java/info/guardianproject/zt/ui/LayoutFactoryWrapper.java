package info.guardianproject.zt.ui;

import info.guardianproject.zt.App;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

public class LayoutFactoryWrapper implements LayoutInflater.Factory
{
	private LayoutInflater.Factory mParent;
	
	public LayoutFactoryWrapper(LayoutInflater.Factory parent)
	{
		mParent = parent;
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		View ret = App.createView(name, context, attrs);
		if (ret == null && mParent != null)
		{
			ret = mParent.onCreateView(name, context, attrs);
		}
		return ret;
	}	
}
