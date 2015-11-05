package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.App;
import android.content.Context;
import android.support.v4.view.LayoutInflaterFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;


public class LayoutFactoryWrapper implements LayoutInflaterFactory
{
	private android.view.LayoutInflater.Factory mParent;
	
	public LayoutFactoryWrapper(LayoutInflater.Factory parent)
	{
		mParent = parent;
	}

	@Override
	public View onCreateView(View view, String name, Context context, AttributeSet attrs)
	{
		View ret = App.createView(name, context, attrs);
		if (ret == null && mParent != null)
		{
			ret = mParent.onCreateView(name, context, attrs);
		}
		return ret;
	}	
}
