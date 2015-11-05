package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.uiutil.FontManager;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.R;

public class CustomFontTextView extends TextView
{
	private Rect mBounds;
	private Shader mShader;
	private boolean mFadeLastLine = false;
	private float mLineSpacingExtra;
	private float mLineSpacingMulti;
	private int mMaxLines;
	@SuppressWarnings("unused")
	private CustomFontTextViewHelper mHelper;
	private boolean needsTruncate;
	private boolean inSetText;
	private CharSequence mTruncatedText;
	private TruncateAt mEllipsize;

	public CustomFontTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public CustomFontTextView(Context context)
	{
		this(context, null, 0);
	}

	public CustomFontTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		mBounds = new Rect();
		mLineSpacingExtra = 0;
		mLineSpacingMulti = 1.0f;
		mMaxLines = Integer.MAX_VALUE;
		mEllipsize = null;
		mHelper = new CustomFontTextViewHelper(this, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView, defStyle, 0);
		mFadeLastLine = a.getBoolean(R.styleable.CustomFontTextView_fade_last_line, false);
		mMaxLines = a.getInteger(R.styleable.CustomFontTextView_android_maxLines, Integer.MAX_VALUE);
		if (a.hasValue(R.styleable.CustomFontTextView_android_ellipsize))
		{
			TypedValue tv = new TypedValue();
			a.getValue(R.styleable.CustomFontTextView_android_ellipsize, tv);
			if (tv.data == 3)
				mEllipsize = TruncateAt.END;
		}
		mLineSpacingExtra = a.getFloat(R.styleable.CustomFontTextView_android_lineSpacingExtra, mLineSpacingExtra);
		mLineSpacingMulti = a.getFloat(R.styleable.CustomFontTextView_android_lineSpacingMultiplier, mLineSpacingMulti);
		a.recycle();
		mShader = getPaint().getShader();
		setMaxLines(mMaxLines);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (heightMeasureSpec == MeasureSpec.UNSPECIFIED || heightMeasureSpec == MeasureSpec.AT_MOST)
		{
			if (getLineCount() > 0)
			{
				int bottom = 0;
				int n = getVisibleLines(this.getMeasuredHeight());
				if (n > 0)
				{
					this.getLineBounds(n - 1, mBounds);
					bottom = mBounds.bottom;
				}
				super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(bottom + this.getPaddingBottom(), MeasureSpec.EXACTLY));
			}
		}
	}

	public void setFadeLastLine(boolean fadeLastLine)
	{
		if (mFadeLastLine != fadeLastLine)
		{
			mFadeLastLine = fadeLastLine;
			fadeLastLine();
		}
	}

	public CharSequence getOverflowingText()
	{
		int nVisible = getVisibleLines();
		if (getLayout() != null)
		{
			Layout layout = getLayout();
			int offset = layout.getLineStart(nVisible);
			CharSequence overflow = "";
			if (offset >= 0 && offset < getText().length())
				overflow = getText().subSequence(offset, getText().length());
			return overflow;
		}
		return null;
	}

	public int getVisibleLines()
	{
		return getVisibleLines(getHeight());
	}

	private int getVisibleLines(int height)
	{
		if (getLineCount() > 0 && height > 0 && getLayout() != null)
		{
			Rect bounds = new Rect();
			for (int i = 0; i < getLineCount(); i++)
			{
				getLineBounds(i, bounds);
				if ((bounds.bottom) > (height - this.getPaddingBottom()))
				{
					return i;
				}
			}
			return getLineCount(); // All fit
		}
		return 0;
	}

	public CustomFontTextView createClone()
	{
		CustomFontTextView newClone = new CustomFontTextView(getContext());
		newClone.setTypeface(getTypeface());
		newClone.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.getTextSize());
		newClone.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
		newClone.setLineSpacing(mLineSpacingExtra, mLineSpacingMulti);
		LayoutParams params = getLayoutParams();
		if (params instanceof ViewGroup.MarginLayoutParams)
		{
			ViewGroup.MarginLayoutParams newParams = new ViewGroup.MarginLayoutParams(params);
			newClone.setLayoutParams(newParams);
		}
		return newClone;
	}

	@Override
	public void setLineSpacing(float add, float mult) {
		super.setLineSpacing(add, mult);
		mLineSpacingExtra = add;
		mLineSpacingMulti = mult;
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
	{
		super.onTextChanged(text, start, lengthBefore, lengthAfter);
		fadeLastLine();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		mTruncatedText = null;
		fadeLastLine();
	}

	private void fadeLastLine()
	{
		int nVisible = getVisibleLines();
		if (getLayout() != null && getLineCount() > nVisible)
		{
			if (mFadeLastLine)
			{		
				int bottomStartFade = 0;
				int bottomEndFade = 0;
				bottomEndFade = getHeight() - getPaddingBottom();
				bottomStartFade = bottomEndFade - UIHelpers.dpToPx(5, getContext());

				int defaultColor = getTextColors().getDefaultColor();
				int alphaColor = Color.argb(0, Color.red(defaultColor), Color.green(defaultColor), Color.blue(defaultColor));

				LinearGradient gradient = new LinearGradient(0f, bottomStartFade, 0f, bottomEndFade, new int[] { defaultColor, alphaColor }, new float[] { 0,
						1f }, Shader.TileMode.CLAMP);
				getPaint().setShader(gradient);
				return;
			}
		}
		
		if (!inSetText && !mFadeLastLine && mEllipsize == TruncateAt.END && mTruncatedText == null)
		{		
			needsTruncate = true;
		}
		getPaint().setShader(mShader); // original
	}
	
	
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		if (needsTruncate)
		{
			if (getLayout() != null)
			{
				int nVisible = getVisibleLines();
				int lineCount = getLineCount();
				int maxLines = Math.max(1, Math.min(nVisible, mMaxLines));
				if (lineCount > maxLines)
				{
					needsTruncate = false;
					
					int offsetLastWhole = 0;
					if (maxLines > 1)
						offsetLastWhole = getLayout().getLineEnd(maxLines - 2);
					int lineEnd = getLayout().getLineEnd(lineCount - 1);
					CharSequence seq = getText().subSequence(offsetLastWhole, lineEnd);
					if (this.getTransformationMethod() != null)
						seq = this.getTransformationMethod().getTransformation(seq, this);
					CharSequence ellipsized = TextUtils.ellipsize(seq, this.getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(), TruncateAt.END);
					if (ellipsized != seq)
					{
						inSetText = true;
						mTruncatedText = TextUtils.concat(getText().subSequence(0, offsetLastWhole), ellipsized);
						super.setText(mTruncatedText);
						inSetText = false;
					}
				}
			}
		}
		super.onDraw(canvas);
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		mTruncatedText = null;
		super.setText(FontManager.transformText(this, text), type);
	}

	@Override
	public void setMaxLines(int maxlines)
	{
		mMaxLines = maxlines;
		if (!inSetText && !mFadeLastLine && mEllipsize == TruncateAt.END)
		{		
			needsTruncate = true;
		}
	}

	@Override
	public void setEllipsize(TruncateAt where)
	{
		mEllipsize = where;
		if (mEllipsize == TruncateAt.MARQUEE)
			super.setEllipsize(where);
	}

	@Override
	public void setTextAppearance(Context context, int resid)
	{
		super.setTextAppearance(context, resid);
		mHelper.setTextAppearance(resid);
	}
	
	
}
