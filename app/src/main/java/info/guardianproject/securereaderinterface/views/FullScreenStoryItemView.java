package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.adapters.ShareSpinnerAdapter;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.adapters.TextSizeSpinnerAdapter;
import info.guardianproject.securereaderinterface.installer.SecureBluetoothSenderFragment;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.widgets.CheckableImageView;
import info.guardianproject.securereaderinterface.widgets.NestedViewPager;
import info.guardianproject.securereaderinterface.widgets.compat.Spinner;
import info.guardianproject.securereaderinterface.R;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import com.tinymission.rss.Item;

public class FullScreenStoryItemView extends FrameLayout
{
	protected static final String LOGTAG = "FullScreenStoryItemView";
	public static final boolean LOGGING = false;
	
	private View mBtnRead;
	private View mBtnComments;
	private CheckableImageView mBtnFavorite;
	private ShareSpinnerAdapter mShareAdapter;
	private TextSizeSpinnerAdapter mTextSizeAdapter;
	private NestedViewPager mContentPager;
	private ContentPagerAdapter mContentPagerAdapter;

	private StoryListAdapter mItemAdapter;
	private int mCurrentIndex;
	private SparseArray<Rect> mInitialViewPositions;
	private SparseArray<Rect> mFinalViewPositions;
	

	public FullScreenStoryItemView(Context context)
	{
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.story_item, this);
		initialize();
	}

	public FullScreenStoryItemView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.story_item, this);
		initialize();
	}



	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		super.onTouchEvent(event);
		return true;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig)
	{
		this.removeAllViews();
		super.onConfigurationChanged(newConfig);
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.story_item, this);
		initialize();
		setCurrentStoryIndex(mCurrentIndex);
		refresh();
	}

	private void initialize()
	{
		mContentPager = (NestedViewPager) findViewById(R.id.horizontalPagerContent);
		mContentPagerAdapter = new ContentPagerAdapter();
		mContentPager.setAdapter(mContentPagerAdapter);
		
		View toolbar = findViewById(R.id.storyToolbar);

		// Read story
		//
		mBtnRead = toolbar.findViewById(R.id.btnRead);
		mBtnRead.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showContent();
			}
		});

		// Read comments
		//
		mBtnComments = toolbar.findViewById(R.id.btnComments);
		mBtnComments.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showComments();
			}
		});

		// Disable comments?
		if (!App.UI_ENABLE_COMMENTS)
		{
			mBtnRead.setVisibility(View.GONE);
			mBtnComments.setVisibility(View.GONE);
			// toolbar.findViewById(R.id.separatorComments).setVisibility(View.GONE);
		}

		// Text Size
		//
		final Spinner spinnerTextSize = (Spinner) toolbar.findViewById(R.id.spinnerTextSize);
		mTextSizeAdapter = new TextSizeSpinnerAdapter(spinnerTextSize, getContext(), R.layout.text_size_story_item_button);
		spinnerTextSize.setAdapter(mTextSizeAdapter);
		spinnerTextSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				int adjustment = App.getSettings().getContentFontSizeAdjustment();
				if (position == 0 && adjustment < 8)
					adjustment += 2;
				else if (position == 1 && adjustment > -8)
					adjustment -= 2;
				App.getSettings().setContentFontSizeAdjustment(adjustment);
				
				for (int iChild = 0; iChild < mContentPager.getChildCount(); iChild++)
				{
					StoryItemView storyItemView = (StoryItemView) mContentPager.getChildAt(iChild).getTag();
					if (storyItemView != null)
						storyItemView.updateTextSize();
				}
				//mContentPager.recreateAllViews();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});

		// Favorite
		//
		mBtnFavorite = (CheckableImageView) toolbar.findViewById(R.id.chkFavorite);
		mBtnFavorite.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (getCurrentStory() != null)
				{
					CheckableImageView view = (CheckableImageView) v;
					view.toggle();
					App.getInstance().socialReader.markItemAsFavorite(getCurrentStory(), view.isChecked());
					UICallbacks.itemFavoriteStatusChanged(getCurrentStory());
				}
			}
		});

		// Share
		//
		Spinner spinnerShare = (Spinner) toolbar.findViewById(R.id.spinnerShare);
		mShareAdapter = new ShareSpinnerAdapter(spinnerShare, getContext(), R.string.story_item_share_popup_title, R.layout.share_story_item_button);
		spinnerShare.setAdapter(mShareAdapter);
		spinnerShare.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				ShareSpinnerAdapter adapter = (ShareSpinnerAdapter) parent.getAdapter();
				Intent shareIntent = adapter.getIntentAtPosition(position);
				if (adapter.isSecureBTShareIntent(shareIntent))
				{
					// BT Share is a dialog popup, so handle that here.
			        FragmentManager fm = ((FragmentActivity)getContext()).getSupportFragmentManager();
			        SecureBluetoothSenderFragment dialogSendShare = new SecureBluetoothSenderFragment(); 
			        
			        // Get the share intent and make sure to forward it on to our fragment
			        Bundle args = new Bundle();
			        args.putParcelable("intent", shareIntent.getParcelableExtra("intent"));
			        dialogSendShare.setArguments(args);
			        
			        dialogSendShare.show(fm, App.FRAGMENT_TAG_SEND_BT_SHARE);
					return;
				}
				if (adapter.isSecureChatIntent(shareIntent))
					shareIntent = App.getInstance().socialReader.getSecureShareIntent(getCurrentStory(), false);
				if (shareIntent != null)
				{
					/*
					if (!App.getSettings().chatUsernamePasswordSet() 
							&& App.getInstance().socialReader.ssettings.getChatSecureUsername() != null
							&& App.getInstance().socialReader.ssettings.getChatSecurePassword() != null) {
					*/	
						/*
						ima://foo:pass@bar.com/
						action = android.intent.action.INSERT 
						 */
						/*
						Intent usernamePasswordIntent = new Intent(Intent.ACTION_INSERT, 
								Uri.parse("ima://"+App.getInstance().socialReader.ssettings.getChatSecureUsername()+":"
										+App.getInstance().socialReader.ssettings.getChatSecurePassword()+"@dukgo.com/"));
						*/
/*
 * 						Possible Example:
 * 						if (context instanceof FragmentActivityWithMenu)
 *							((FragmentActivityWithMenu) context).startActivityForResultAsInternal(intent, -1);
 *						else
 *							context.startActivity(intent);						
 */
						//((Activity)getContext()).startActivityForResult(usernamePasswordIntent, UICallbacks.RequestCode.CREATE_CHAT_ACCOUNT); 
						//getContext().startActivity(usernamePasswordIntent);
						
						// How to tell if it worked?
						//((Activity)context).startActivityForResult(usernamePasswordIntent,REGISTER_CHAT_USERNAME_PASSWORD);
						// if it is OK then App.getSettings().setChatUsernamePasswordSet();
					/*
					} else if (App.getInstance().socialReader.ssettings.getChatSecureUsername() == null ||
							App.getInstance().socialReader.ssettings.getChatSecurePassword() == null) {
						// Register Social Reporter username/password
						
					} else {
					*/
						getContext().startActivity(shareIntent);
					/*}*/
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});

		// Default to show story content
		showContent();
	}

	public Item getCurrentStory()
	{
		if (mItemAdapter == null)
			return null;
		return (Item) mItemAdapter.getItem(mCurrentIndex);
	}

	public int getCurrentStoryIndex()
	{
		return mCurrentIndex;
	}
	
	public void setStory(StoryListAdapter itemAdapter, int currentIndex, SparseArray<Rect> initialViewPositions)
	{
		mItemAdapter = itemAdapter;
		setCurrentStoryIndex(currentIndex);
		mInitialViewPositions = initialViewPositions;
		mFinalViewPositions = initialViewPositions;
		//mContentPager.setCurrentItem(currentIndex);
		refresh();
	}

	private void setCurrentStoryIndex(int index)
	{
		mCurrentIndex = index;
		updateNumberOfComments();
		Item current = getCurrentStory();
		if (current != null)
		{
			mBtnFavorite.setChecked(current.getFavorite());
			mShareAdapter.clear();
			Intent shareIntent = App.getInstance().socialReader.getShareIntent(current);
			mShareAdapter.addSecureBTShareResolver(shareIntent);
			//mShareAdapter.addSecureChatShareResolver(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory(), true));
			// mShareAdapter.addIntentResolvers(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory()),
			// PackageHelper.URI_CHATSECURE,
			// R.string.share_via_secure_chat, R.drawable.ic_share_sharer);
			mShareAdapter.addIntentResolvers(shareIntent);
			DownloadsAdapter.viewed(current.getDatabaseId());
		}
	}

	public void refresh()
	{
		mContentPagerAdapter.notifyDataSetChanged();
	}

	private void showContent()
	{
		mBtnRead.setSelected(true);
		mBtnComments.setSelected(false);
		mContentPager.setVisibility(View.VISIBLE);
	}

	private void showComments()
	{
		Item currentStory = getCurrentStory();
		if (currentStory != null)
		{
			String roomName = "story_" + MD5_Hash(currentStory.getGuid());
			Bundle params = new Bundle();
			params.putString("room_name", roomName);
			if (LOGGING)
				Log.v(LOGTAG, "Show comments, so start the chat application now with room: " + roomName);
			UICallbacks.handleCommand(getContext(), R.integer.command_chat, params);
		}
		// mBtnRead.setSelected(false);
		// mBtnComments.setSelected(true);
		// mHorizontalPagerContent.setVisibility(View.GONE);
		// mCurrentPageIndicator.setVisibility(View.GONE);
	}

	public static String MD5_Hash(String s)
	{
		MessageDigest m = null;

		try
		{
			m = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}

		m.update(s.getBytes(), 0, s.length());
		String hash = new BigInteger(1, m.digest()).toString(16);
		return hash;
	}

	public void showFavoriteButton(boolean bShow)
	{
		if (!bShow)
			this.mBtnFavorite.setVisibility(View.GONE);
		else
			this.mBtnFavorite.setVisibility(View.VISIBLE);
	}

	private void updateNumberOfComments()
	{
		// if (mBtnComments != null)
		// {
		// int numberOfComments = 0;
		// if (getCurrentStory() != null)
		// numberOfComments = getCurrentStory().getNumberOfComments();
		// ((TextView)
		// mBtnComments.findViewById(R.id.tvNumComments)).setText(String.valueOf(numberOfComments));
		// }
	}
	
	public void onBeforeCollapse()
	{
		StoryItemView storyItemView = mContentPagerAdapter.getCurrentView();
		if (storyItemView != null)
			storyItemView.resetToStoredPositions(mFinalViewPositions, ExpandingFrameLayout.DEFAULT_COLLAPSE_DURATION);
	}
	
	private class ContentPagerAdapter extends PagerAdapter
	{
		private StoryItemView mLeftView;
		private StoryItemView mCurrentView;
		private StoryItemView mRightView;
		private ArrayList<StoryItemView> mViews;
		
		public ContentPagerAdapter()
		{
			super();
			mViews = new ArrayList<StoryItemView>();
			updateViews();
		}

		public StoryItemView getCurrentView()
		{
			return mCurrentView;
		}
		
		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			int newIndex = mCurrentIndex;
			if (mCurrentView != null)
			{
				if (mCurrentView.usesReverseSwipe())
					newIndex = newIndex + getItemPosition(mCurrentView) - position;
				else
					newIndex = newIndex + position - getItemPosition(mCurrentView);
			}
			super.setPrimaryItem(container, position, object);
			if (newIndex != mCurrentIndex)
			{
				mCurrentIndex = newIndex;
				notifyDataSetChanged();
			}
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			return arg0 == ((StoryItemView)arg1).getView(null);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			StoryItemView storyItemView = mViews.get(position);
			View view = storyItemView.getView(container);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
			
			if (mInitialViewPositions != null && position == mContentPager.getCurrentItem())
			{
				storyItemView.setStoredPositions(mInitialViewPositions);
				mInitialViewPositions = null;
			}
			((ViewPager) container).addView(view);
			return storyItemView;
		}

		@Override
		public int getItemPosition(Object object)
		{
			StoryItemView storyItemView = (StoryItemView)object;
			int index = mViews.indexOf(storyItemView);
			if (index == -1)
				index = POSITION_NONE;
			return index;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			container.removeView(((StoryItemView)object).getView(container));
		}

		@Override
		public int getCount()
		{
			return mViews.size();
		}

		private StoryItemView getViewForItem(Item item, ArrayList<StoryItemView> reuseViews)
		{
			StoryItemView ret = null;
			for (StoryItemView storyItemView : reuseViews)
			{
				if (storyItemView.getItem().getDatabaseId() == item.getDatabaseId())
				{
					ret = storyItemView;
					reuseViews.remove(storyItemView);
					break;
				}
			}
			if (ret == null)
				ret = new StoryItemView(item);
			return ret;
		}
		
		private void updateViews()
		{
			mLeftView = null;
			mCurrentView = null;
			mRightView = null;
			
			if (mItemAdapter != null && mCurrentIndex >= 0 && mCurrentIndex < mItemAdapter.getCount())
				mCurrentView = getViewForItem((Item)mItemAdapter.getItem(mCurrentIndex), mViews);
			
			if (mCurrentView != null)
			{
				if (mCurrentView.usesReverseSwipe())
				{
					if (mCurrentIndex > 0)
						mRightView = getViewForItem((Item)mItemAdapter.getItem(mCurrentIndex - 1), mViews);
					if (mCurrentIndex < (mItemAdapter.getCount() - 1))
						mLeftView = getViewForItem((Item)mItemAdapter.getItem(mCurrentIndex + 1), mViews);
				}
				else
				{
					if (mCurrentIndex > 0)
						mLeftView = getViewForItem((Item)mItemAdapter.getItem(mCurrentIndex - 1), mViews);
					if (mCurrentIndex < (mItemAdapter.getCount() - 1))
						mRightView = getViewForItem((Item)mItemAdapter.getItem(mCurrentIndex + 1), mViews);
				}
			}
						
			// Clean up views not used anymore
			//
			for (StoryItemView storyItemView : mViews)
			{
				storyItemView.recycle();
			}
			mViews.clear();
			
			if (mLeftView != null)
				mViews.add(mLeftView);
			if (mCurrentView != null)
				mViews.add(mCurrentView);
			if (mRightView != null)
				mViews.add(mRightView);
		}

		@Override
		public void notifyDataSetChanged()
		{
			updateViews();
			super.notifyDataSetChanged();
			if (mCurrentView != null)
				mContentPager.setCurrentItem(this.getItemPosition(mCurrentView));
		}
	}
}
