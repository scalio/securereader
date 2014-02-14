package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.views.FeedFilterView;
import info.guardianproject.securereaderinterface.views.FeedFilterView.FeedFilterViewCallbacks;

import org.holoeverywhere.drawable.ColorDrawable;
import org.holoeverywhere.widget.PopupWindow;
import org.holoeverywhere.widget.PopupWindow.OnDismissListener;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionProvider;
import info.guardianproject.paik.R;

import com.tinymission.rss.Feed;

public class ActionProviderFeedFilter extends ActionProvider implements OnClickListener, OnDismissListener, FeedFilterViewCallbacks
{
	private final Context mContext;
	private PopupWindow mWindow;
	private String mTitle;
	private TextView mActionView;

	public ActionProviderFeedFilter(Context context)
	{
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView()
	{
		LayoutInflater inflater = LayoutInflater.from(mContext);

		mActionView = (TextView) inflater.inflate(R.layout.actionbar_spinner_feeds_item, null, false);
		mActionView.setText(mTitle);
		mActionView.setOnClickListener(this);
		return mActionView;
	}

	@Override
	public boolean hasSubMenu()
	{
		return false;
	}

	public void setCurrentTitle(String title)
	{
		mTitle = title;
		if (mActionView != null)
			mActionView.setText(mTitle);
	}

	@Override
	public void onClick(View v)
	{
		if (mWindow == null)
		{
			LayoutInflater inflater = LayoutInflater.from(mContext);
			FeedFilterView content = (FeedFilterView) inflater.inflate(R.layout.feed_list, null, false);

			content.setFeedFilterViewCallbacks(this);

			mWindow = new PopupWindow(content);
			mWindow.setOnDismissListener(this);

			TypedArray a = mContext.obtainStyledAttributes(null, R.styleable.SherlockSpinner, 0, R.style.Widget_Sherlock_Light_Spinner_DropDown_ActionBar);
			if (a != null)
			{
				Drawable back = a.getDrawable(R.styleable.SherlockSpinner_android_popupBackground);
				mWindow.setBackgroundDrawable(back);
				a.recycle();
			}
			if (mWindow.getBackground() == null)
				mWindow.setBackgroundDrawable(new ColorDrawable());
			mWindow.setTouchModal(true);
			mWindow.setOutsideTouchable(true);
			mWindow.setFocusable(true);
			mWindow.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

			// The drawable probably has a padding that we don't want, so offset
			// the popup with a negative padding
			int offsetY = 0;
			if (mWindow.getBackground() instanceof NinePatchDrawable)
			{
				Rect padding = new Rect();
				if (((NinePatchDrawable) mWindow.getBackground()).getPadding(padding))
					offsetY = -padding.top;
			}
			mWindow.showAsDropDown(v, 0, offsetY);
		}
	}

	public boolean isOpen()
	{
		return mWindow != null;
	}

	public void close()
	{
		if (mWindow != null)
		{
			mWindow.dismiss();
		}
	}

	@Override
	public void onDismiss()
	{
		mWindow = null;
	}

	@Override
	public void viewFavorites()
	{
		close();
		UICallbacks.setFeedFilter(FeedFilterType.FAVORITES, 0, this);
	}

	@Override
	public void viewPopular()
	{
		close();
		UICallbacks.setFeedFilter(FeedFilterType.POPULAR, 0, this);
	}

	@Override
	public void viewDownloads() 
	{
		close();
		UICallbacks.handleCommand(mContext, R.integer.command_downloads, null);
	}

	@Override
	public void viewShared()
	{
		close();
		UICallbacks.setFeedFilter(FeedFilterType.SHARED, 0, this);
	}

	@Override
	public void viewFeed(Feed feedToView)
	{
		close();
		if (feedToView == null)
			UICallbacks.setFeedFilter(FeedFilterType.ALL_FEEDS, 0, this);
		else
			UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, feedToView.getDatabaseId(), this);
	}

	@Override
	public void addNew()
	{
		close();
		UICallbacks.handleCommand(mContext, R.integer.command_feed_add, null);
	}
}