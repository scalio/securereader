package info.guardianproject.securereaderinterface.views;

import com.tinymission.rss.Item;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.R;

public class StoryItemDraftPageView extends StoryItemPageView
{
	private View mBtnEdit;
	private View mBtnDelete;
	
	public StoryItemDraftPageView(Context context)
	{
		super(context);
	}

	public StoryItemDraftPageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	@Override
	protected int getViewResourceByType(ViewType type)
	{
		if (type == ViewType.NO_PHOTO)
			return R.layout.post_item_draft_no_photo;
		else if (type == ViewType.PORTRAIT_PHOTO)
			return R.layout.post_item_draft_portrait_photo;
		return R.layout.post_item_draft_landscape_photo;
	}

	@Override
	protected void findViews(View view)
	{
		super.findViews(view);
		mBtnEdit = view.findViewById(R.id.btnEdit);
		mBtnDelete = view.findViewById(R.id.btnDelete);
	}

	@Override
	public void populateWithItem(Item item)
	{
		mItem = null; // Drafts are dynamic, so null mItem so that super class will always regenerate views
		
		super.populateWithItem(item);
		if (mTvTitle != null && TextUtils.isEmpty(mTvTitle.getText()))
		{
			mTvTitle.setText(R.string.post_item_no_title);
		}
	}

	@Override
	protected void populateTime()
	{
		if (mTvTime != null)
			mTvTime.setText(UIHelpers.dateDiffDisplayString(mItem.getPublicationTime(), getContext(), R.string.post_draft_saved_never,
					R.string.post_draft_saved_recently, R.string.post_draft_saved_minutes, R.string.post_draft_saved_minute, R.string.post_draft_saved_hours,
					R.string.post_draft_saved_hour, R.string.post_draft_saved_days, R.string.post_draft_saved_day));
	}
	
	public void setButtonClickListeners(View.OnClickListener deleteListener, View.OnClickListener editListener)
	{
		if (mBtnDelete != null)
			mBtnDelete.setOnClickListener(deleteListener);
		if (mBtnEdit != null)
			mBtnEdit.setOnClickListener(editListener);
	}
}
