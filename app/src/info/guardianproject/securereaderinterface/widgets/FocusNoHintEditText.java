package info.guardianproject.securereaderinterface.widgets;

import ch.boye.httpclientandroidlib.util.TextUtils;
import android.content.Context;
import android.graphics.Rect;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class FocusNoHintEditText extends EditText
{

	private CharSequence mHint;

	public FocusNoHintEditText(Context context)
	{
		super(context);
		init();
	}

	public FocusNoHintEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public FocusNoHintEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	private void init()
	{
		mHint = getHint();
		applyHintTextSmall();
	}

	private void applyHintTextSmall()
	{
		if (!TextUtils.isEmpty(mHint))
		{
			SpannableString span = new SpannableString(mHint);
			span.setSpan(new RelativeSizeSpan(0.7f), 0, mHint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			super.setHint(span);
		}
	}
	
	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
	{
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused)
			super.setHint("");
		else
			applyHintTextSmall();
	}
	
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
        {
           this.clearFocus();
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
