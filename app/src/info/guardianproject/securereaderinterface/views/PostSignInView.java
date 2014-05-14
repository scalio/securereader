
package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;

import info.guardianproject.securereaderinterface.R;

public class PostSignInView extends FrameLayout implements View.OnClickListener, OnCheckedChangeListener
{
	public interface OnAgreeListener
	{
		void onAgreed();
	}

	private CheckBox mChkAgree;
	private Button mBtnNext;

	private OnAgreeListener mListener;

	public PostSignInView(Context context)
	{
		super(context);
		init();
	}

	public PostSignInView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	private void init()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.post_sign_in, this);

		if (!this.isInEditMode())
		{
			mChkAgree = (CheckBox) findViewById(R.id.chkAgree);
			mChkAgree.setOnCheckedChangeListener(this);
			mBtnNext = (Button) findViewById(R.id.btnNext);
			mBtnNext.setOnClickListener(this);
			enableDisableCreateButton();
		}
	}

	public void setActionListener(OnAgreeListener listener)
	{
		mListener = listener;
	}

	@Override
	public void onClick(View v)
	{
		if (v == mBtnNext)
		{
			if (mListener != null && mChkAgree.isChecked())
				mListener.onAgreed();
		}
	}

	private void enableDisableCreateButton()
	{
		mBtnNext.setEnabled(mChkAgree.isChecked());
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		enableDisableCreateButton();
	}
}
