package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import info.guardianproject.paik.R;

public class StoryListHintTorView extends FrameLayout implements View.OnClickListener
{
	public interface OnButtonClickedListener
	{
		void onNoNetClicked();

		void onGoOnlineClicked();

		void onNoNetPsiphonClicked();

		void onGoOnlinePsiphonClicked();
	}

	private View mViewExpand;
	private View mViewCollapse;
	private View mViewExpansionArea;
	private View mBtnGoOnline;
	private View mBtnNoNet;
	private View mBtnGoOnlinePsiphon;
	private View mBtnNoNetPsiphon;
	private OnButtonClickedListener mOnButtonClickedListener;
	private View mLlConnected;
	private View mLlNotConnected;
	private TextView mTvSuccessfulConnection;
	
	public StoryListHintTorView(Context context)
	{
		super(context);
	}

	public StoryListHintTorView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public StoryListHintTorView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setOnButtonClickedListener(OnButtonClickedListener listener)
	{
		mOnButtonClickedListener = listener;
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		mViewExpand = findViewById(R.id.llExpander);
		mViewCollapse = findViewById(R.id.llCollapser);
		mViewExpansionArea = findViewById(R.id.llExpansionArea);
		mBtnGoOnline = findViewById(R.id.btnGoOnline);
		mBtnNoNet = findViewById(R.id.btnNoNet);
		mBtnGoOnlinePsiphon = findViewById(R.id.btnGoOnlinePsiphon);
		mBtnNoNetPsiphon = findViewById(R.id.btnNoNetPsiphon);
		mLlConnected = findViewById(R.id.llConnected);
		mLlNotConnected = findViewById(R.id.llNotConnected);
		mTvSuccessfulConnection = (TextView) findViewById(R.id.tvSuccessfulConnection);
		if (!isInEditMode())
		{
			mViewExpand.setOnClickListener(this);
			mViewCollapse.setOnClickListener(this);
			mBtnGoOnline.setOnClickListener(this);
			mBtnNoNet.setOnClickListener(this);
			mBtnGoOnlinePsiphon.setOnClickListener(this);
			mBtnNoNetPsiphon.setOnClickListener(this);
			onClick(mViewCollapse);
			mBtnGoOnline.setVisibility(View.GONE);
			mBtnGoOnlinePsiphon.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View v)
	{
		if (v == mViewExpand)
		{
			mViewExpand.setVisibility(View.GONE);
			mViewCollapse.setVisibility(View.VISIBLE);
			mViewExpansionArea.setVisibility(View.VISIBLE);
		}
		else if (v == mViewCollapse)
		{
			mViewExpand.setVisibility(View.VISIBLE);
			mViewCollapse.setVisibility(View.GONE);
			mViewExpansionArea.setVisibility(View.GONE);
		}
		else if (v == mBtnGoOnline)
		{
			if (this.mOnButtonClickedListener != null)
				mOnButtonClickedListener.onGoOnlineClicked();
		}
		else if (v == mBtnNoNet)
		{
			if (this.mOnButtonClickedListener != null)
				mOnButtonClickedListener.onNoNetClicked();
		}
		else if (v == mBtnGoOnlinePsiphon)
		{
			if (this.mOnButtonClickedListener != null)
				mOnButtonClickedListener.onGoOnlinePsiphonClicked();
		}
		else if (v == mBtnNoNetPsiphon)
		{
			if (this.mOnButtonClickedListener != null)
				mOnButtonClickedListener.onNoNetPsiphonClicked();
		}
	}

	public void setIsOnline(boolean hasNetwork, boolean isConnectedToTor)
	{
		if (isConnectedToTor)
		{
			mLlConnected.setVisibility(View.VISIBLE);
			mLlNotConnected.setVisibility(View.GONE);
		}
		else
		{
			if (hasNetwork)
			{
				mBtnGoOnline.setVisibility(View.VISIBLE);
				mBtnNoNet.setVisibility(View.GONE);
				mBtnGoOnlinePsiphon.setVisibility(View.VISIBLE);
				mBtnNoNetPsiphon.setVisibility(View.GONE);
			}
			else
			{
				mBtnGoOnline.setVisibility(View.GONE);
				mBtnNoNet.setVisibility(View.VISIBLE);
				mBtnGoOnlinePsiphon.setVisibility(View.GONE);
				mBtnNoNetPsiphon.setVisibility(View.VISIBLE);
			}
			mLlConnected.setVisibility(View.GONE);
			mLlNotConnected.setVisibility(View.VISIBLE);
		}
	}
	
	public void updateProxyText(int resIdSuccessfulConnection)
	{
		mTvSuccessfulConnection.setText(resIdSuccessfulConnection);
	}

}
