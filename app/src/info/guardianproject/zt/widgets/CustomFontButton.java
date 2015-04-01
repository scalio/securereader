package info.guardianproject.zt.widgets;

import info.guardianproject.zt.uiutil.FontManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class CustomFontButton extends Button {

	@SuppressWarnings("unused")
	private CustomFontTextViewHelper mHelper;

	public CustomFontButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	public CustomFontButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public CustomFontButton(Context context)
	{
		super(context);
		init(null);
	}
	
	private void init(AttributeSet attrs)
	{
		mHelper = new CustomFontTextViewHelper(this, attrs);
	}
	
	@Override
	public void setText(CharSequence text, BufferType type)
	{
		super.setText(FontManager.transformText(this, text), type);
	}
	
	@Override
	public void setTextAppearance(Context context, int resid)
	{
		super.setTextAppearance(context, resid);
		mHelper.setTextAppearance(resid);
	}
}
