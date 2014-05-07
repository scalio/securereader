package info.guardianproject.securereaderinterface.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import info.guardianproject.securereaderinterface.views.StoryItemDraftPageView;
import info.guardianproject.securereaderinterface.R;
import com.tinymission.rss.Item;

public class PostDraftsListAdapter extends PostPublishedListAdapter
{
	public static final String LOGTAG = "PostDraftsListAdapter";
	public static final boolean LOGGING = false;
	
	public interface PostDraftsListAdapterListener
	{
		void onEditDraft(Item item);

		void onDeleteDraft(Item item);
	}

	private PostDraftsListAdapterListener mPostDraftsListAdapterListener;

	public PostDraftsListAdapter(Context context, ArrayList<Item> posts)
	{
		super(context, posts);
		mPostDraftsListAdapterListener = null;
		setHeaderView(R.layout.post_list_no_drafts, true);
	}

	public void setPostDraftsListAdapterListener(PostDraftsListAdapterListener listener)
	{
		mPostDraftsListAdapterListener = listener;
	}

	@Override
	protected View createView(int position, ViewGroup parent)
	{
		StoryItemDraftPageView view = new StoryItemDraftPageView(parent.getContext());
		return view;
	}
	
	@Override
	protected void bindView(View view, int position, Item item)
	{
		super.bindView(view, position, item);
		
		StoryItemDraftPageView pv = (StoryItemDraftPageView) view;
		pv.setButtonClickListeners(new DeleteButtonClickListener(item), new EditButtonClickListener(item));
	}


	private class EditButtonClickListener implements View.OnClickListener
	{
		private final Item mItem;

		public EditButtonClickListener(Item item)
		{
			mItem = item;
		}

		@Override
		public void onClick(View v)
		{
			if (mPostDraftsListAdapterListener != null)
				mPostDraftsListAdapterListener.onEditDraft(mItem);
		}
	}

	private class DeleteButtonClickListener implements View.OnClickListener
	{
		private final Item mItem;

		public DeleteButtonClickListener(Item item)
		{
			mItem = item;
		}

		@Override
		public void onClick(View v)
		{
			if (mPostDraftsListAdapterListener != null)
				mPostDraftsListAdapterListener.onDeleteDraft(mItem);
		}
	}
}
