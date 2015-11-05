package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import info.guardianproject.securereaderinterface.R;

public class CreateAccountView extends FrameLayout implements View.OnClickListener, TextWatcher
{
	public interface OnActionListener
	{
		void onCreateIdentity(String authorName);
	}

	private EditText mEditAuthorName;
	private Button mBtnCreateIdentity;
	private OnActionListener mListener;

	public CreateAccountView(Context context)
	{
		super(context);
		init();
	}

	public CreateAccountView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	private void init()
	{
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.create_account, this);

		if (!this.isInEditMode())
		{
			mEditAuthorName = (EditText) findViewById(R.id.editUsername);
			mEditAuthorName.addTextChangedListener(this);
			mBtnCreateIdentity = (Button) findViewById(R.id.btnCreateIdentity);
			mBtnCreateIdentity.setOnClickListener(this);
			enableDisableCreateButton();
		}
	}

	public void setActionListener(OnActionListener listener)
	{
		mListener = listener;
	}

	@Override
	public void onClick(View v)
	{
		if (v == mBtnCreateIdentity)
		{
			if (mListener != null && mEditAuthorName.getText().length() > 0)
				mListener.onCreateIdentity(mEditAuthorName.getText().toString());
		}
	}

	private void enableDisableCreateButton()
	{
		mBtnCreateIdentity.setEnabled(mEditAuthorName.getText().length() > 0);
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		enableDisableCreateButton();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
}
