package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.adapters.ShareSpinnerAdapter;
import info.guardianproject.securereaderinterface.adapters.TextSizeSpinnerAdapter;
import info.guardianproject.securereaderinterface.models.PagedViewContent;
import info.guardianproject.securereaderinterface.models.ViewPagerIndicator;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.widgets.CheckableImageView;
import info.guardianproject.securereaderinterface.widgets.PagedView;
import info.guardianproject.securereaderinterface.widgets.PagedView.PagedViewListener;
import info.guardianproject.courier.R;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.holoeverywhere.widget.AdapterView;
import org.holoeverywhere.widget.Spinner;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.tinymission.rss.Item;

public class FullScreenStoryItemView extends FrameLayout implements PagedViewListener
{
	protected static final String LOGTAG = "FullScreenStoryItemView";

	private View mBtnRead;
	private View mBtnComments;
	private CheckableImageView mBtnFavorite;
	private View mCurrentPageIndicator;
	private ShareSpinnerAdapter mShareAdapter;
	private TextSizeSpinnerAdapter mTextSizeAdapter;
	private PagedView mHorizontalPagerContent;

	private ArrayList<Item> mItems;
	private int mCurrentIndex;

	public FullScreenStoryItemView(Context context)
	{
		super(context);
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

		PagedView oldPagedViewIfAny = mHorizontalPagerContent;
		initialize();
		if (oldPagedViewIfAny != null)
		{
			this.mHorizontalPagerContent.setContentPrevious(oldPagedViewIfAny.getContentPrevious());
			this.mHorizontalPagerContent.setContentThis(oldPagedViewIfAny.getContentThis());
			this.mHorizontalPagerContent.setContentNext(oldPagedViewIfAny.getContentNext());
		}
		setCurrentStoryIndex(mCurrentIndex);
		refresh();
	}

	private void initialize()
	{
		mCurrentPageIndicator = findViewById(R.id.contentPageIndicator);
		mHorizontalPagerContent = (PagedView) findViewById(R.id.horizontalPagerContent);
		mHorizontalPagerContent.setViewPagerIndicator((ViewPagerIndicator)mCurrentPageIndicator);
		mHorizontalPagerContent.setListener(this);

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
				mHorizontalPagerContent.recreateAllViews();
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
		if (mItems == null || mCurrentIndex < 0 || mCurrentIndex >= mItems.size())
			return null;
		return mItems.get(mCurrentIndex);
	}

	public void setStory(ArrayList<Item> items, int currentIndex, SparseArray<Rect> initialViewPositions)
	{
		mItems = items;
		setCurrentStoryIndex(currentIndex);

		mHorizontalPagerContent.setContentPrevious(createStoryItemPageView(currentIndex - 1));

		StoryItemView contentThis = createStoryItemPageView(currentIndex);
		if (initialViewPositions != null)
			contentThis.setStoredPositions(initialViewPositions);

		mHorizontalPagerContent.setContentThis(contentThis);
		mHorizontalPagerContent.setContentNext(createStoryItemPageView(currentIndex + 1));
		refresh();
	}

	private void setCurrentStoryIndex(int index)
	{
		mCurrentIndex = index;
		updateNumberOfComments();
		mBtnFavorite.setChecked(getCurrentStory().getFavorite());
		mShareAdapter.clear();
		Intent shareIntent = App.getInstance().socialReader.getShareIntent(getCurrentStory());
		mShareAdapter.addSecureBTShareResolver(shareIntent);
		//mShareAdapter.addSecureChatShareResolver(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory(), true));
		// mShareAdapter.addIntentResolvers(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory()),
		// PackageHelper.URI_CHATSECURE,
		// R.string.share_via_secure_chat, R.drawable.ic_share_sharer);

		mShareAdapter.addIntentResolvers(shareIntent);

		Item current = getCurrentStory();
		if (current != null)
			DownloadsAdapter.viewed(current.getDatabaseId());
	}

	private StoryItemView createStoryItemPageView(int index)
	{
		if (index < 0 || index >= mItems.size())
			return null;

		Item item = mItems.get(index);
		StoryItemView content = new StoryItemView(item);
		return content;
	}

	public void refresh()
	{
		mHorizontalPagerContent.updateViews(0);
	}

	private void showContent()
	{
		mBtnRead.setSelected(true);
		mBtnComments.setSelected(false);
		mHorizontalPagerContent.setVisibility(View.VISIBLE);
		mCurrentPageIndicator.setVisibility(View.VISIBLE);
	}

	private void showComments()
	{
		Item currentStory = getCurrentStory();
		if (currentStory != null)
		{
			String roomName = "story_" + MD5_Hash(currentStory.getGuid());
			Bundle params = new Bundle();
			params.putString("room_name", roomName);
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

	@Override
	public PagedViewContent onMovedToPrevious()
	{
		if (mCurrentIndex > 0)
		{
			setCurrentStoryIndex(mCurrentIndex - 1);
			return createStoryItemPageView(mCurrentIndex - 1);
		}
		return null;
	}

	@Override
	public PagedViewContent onMovedToNext()
	{
		if (mCurrentIndex < (mItems.size() - 1))
		{
			setCurrentStoryIndex(mCurrentIndex + 1);
			return createStoryItemPageView(mCurrentIndex + 1);
		}
		return null;
	}
	
	public void onBeforeCollapse()
	{
		PagedViewContent content = mHorizontalPagerContent.getContentThis();
		if (content != null && content instanceof StoryItemView)
		{
			StoryItemView siv = (StoryItemView) content;
			siv.resetToStoredPositions(ExpandingFrameLayout.DEFAULT_COLLAPSE_DURATION);
		}
	}
}
