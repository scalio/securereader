package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.Settings.ReaderSwipeDirection;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ItemExpandActivity;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.models.PagedViewContent;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection.OnMediaLoadedListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.AnimatedRelativeLayout;
import info.guardianproject.securereaderinterface.widgets.CustomFontTextView;
import info.guardianproject.securereaderinterface.widgets.HeightLimitedRelativeLayout;
import info.guardianproject.securereaderinterface.widgets.PagedView;
import info.guardianproject.securereaderinterface.widgets.UpdatingTextView;
import info.guardianproject.securereaderinterface.widgets.UpdatingTextView.OnUpdateListener;
import info.guardianproject.securereader.SocialReader;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tinymission.rss.Item;

public class StoryItemView implements PagedViewContent, OnUpdateListener, OnMediaLoadedListener
{
	public static final String LOGTAG = "StoryItemView";
	public static final boolean LOGGING = false;
	
	private ViewGroup mPagedView;
	private final Item mItem;
	private MediaViewCollection mMediaViewCollection;
	private ArrayList<View> mBlueprintViews;
	private ArrayList<View> mPages;
	private SparseArray<Rect> mStoredPositions;
	private float mDefaultTextSize;
	private float mDefaultAuthorTextSize;
	private View mView;
	
	public StoryItemView(Item item)
	{
		mItem = item;
	}

	public Item getItem()
	{
		return mItem;
	}
	
	public View getView(ViewGroup parentContainer)
	{
		if (mView == null)
		{
			LayoutInflater inflater = LayoutInflater.from(parentContainer.getContext());

			ViewGroup blueprint = (ViewGroup) inflater.inflate(R.layout.story_item_page_blueprint, parentContainer, false);
			populateViewWithItem(blueprint, mItem);
			blueprint.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
			mView = blueprint;
			//
//			
//			
//			mPagedView = parent;
//		if (mMediaViewCollection != null)
//		{
//			mMediaViewCollection.removeListener(this);
//			mMediaViewCollection.recycle();
//		}
//		mMediaViewCollection = null;
//		if (mItem.getMediaContent() != null && mItem.getMediaContent().size()> 0)
//		{
//			mMediaViewCollection  = new MediaViewCollection(parent.getContext(), mItem);
//			mMediaViewCollection.load(false, true);
//			mMediaViewCollection.addListener(this);
//		}
//		createBlueprintViews(parent);
//		relayout();
//		mView = mPages.get(0);
		}
		return mView;
	}
	
//	private void createBlueprintViews(ViewGroup parentContainer)
//	{
//		mBlueprintViews = new ArrayList<View>();
//
//		LayoutInflater inflater = LayoutInflater.from(parentContainer.getContext());
//
//		ViewGroup blueprint = (ViewGroup) inflater.inflate(R.layout.story_item_page_blueprint, parentContainer, false);
//		populateViewWithItem(blueprint, mItem);
//		blueprint.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
//
//		while (blueprint.getChildCount() > 0)
//		{
//			View child = blueprint.getChildAt(0);
//			mBlueprintViews.add(child);
//			blueprint.removeViewAt(0);
//		}
//	}

	// Used for layout!
	//
//	private ViewGroup _CurrentColumn;
//	private ViewGroup _NextColumn;
//
//	private View createPage(boolean isFirstPage)
//	{
//		View newPage = null;
//		if (isFirstPage)
//		{
//			newPage = LayoutInflater.from(mPagedView.getContext()).inflate(R.layout.story_item_fullscreen_view_page_1, mPagedView, false);
//		}
//		else
//		{
//			newPage = LayoutInflater.from(mPagedView.getContext()).inflate(R.layout.story_item_fullscreen_view_page_n, mPagedView, false);
//		}
//		populateViewWithItem((ViewGroup) newPage, mItem);
//		newPage.measure(View.MeasureSpec.makeMeasureSpec(mPagedView.getWidth(), View.MeasureSpec.EXACTLY),
//				View.MeasureSpec.makeMeasureSpec(mPagedView.getHeight(), View.MeasureSpec.EXACTLY));
//		newPage.layout(0, 0, newPage.getMeasuredWidth(), newPage.getMeasuredHeight());
//		return newPage;
//	}

//	private ViewGroup getNextColumn(ArrayList<View> pages)
//	{
//		if (_NextColumn == null || _CurrentColumn == _NextColumn)
//		{
//			View newPage = createPage(_CurrentColumn == null);
//			_CurrentColumn = (ViewGroup) newPage.findViewById(R.id.column1);
//			_NextColumn = (ViewGroup) newPage.findViewById(R.id.column2);
//			pages.add(newPage);
//		}
//		else
//		{
//			_CurrentColumn = _NextColumn;
//		}
//		return _CurrentColumn;
//	}
//
//	private boolean isTwoColumnMode()
//	{
//		return _NextColumn != null;
//	}
//
//	private boolean isInFirstColumn()
//	{
//		return _CurrentColumn != _NextColumn;
//	}

	private boolean willCreateMediaView()
	{
		if (mMediaViewCollection != null && mMediaViewCollection.getCount() > 0)
			return true;
		return false;
	}
	
//	private ArrayList<View> relayout()
//	{
//		updateTextSize();
//
//		ArrayList<View> ret = new ArrayList<View>();
//
//		ArrayList<View> blueprints = new ArrayList<View>(mBlueprintViews);
//
//		// Null old layout helpers
//		_CurrentColumn = null;
//		_NextColumn = null;
//
//		ViewGroup column = getNextColumn(ret);
//		int columnHeightMax = column.getHeight();
//		int currentColumnHeight = 0;
//		
//		int fullMarginTop = column.getResources().getDimensionPixelOffset(R.dimen.full_top_margin);
//		
//		int marginLeft = column.getResources().getDimensionPixelOffset(R.dimen.card_left_margin);
//		int marginRight = column.getResources().getDimensionPixelOffset(R.dimen.card_right_margin);
//		int marginCenter = column.getResources().getDimensionPixelOffset(R.dimen.card_center_margin);		
//
//		for (int idxView = 0; idxView < blueprints.size();)
//		{
//			ViewGroup currentPageView = ((ViewGroup) ret.get(ret.size() - 1));
//
//			View child = blueprints.get(idxView);
//			if (child.getParent() != null)
//				((ViewGroup) child.getParent()).removeView(child);
//	
//			if (child.getId() == R.id.layout_media)
//			{
//				HeightLimitedRelativeLayout hlrl = (HeightLimitedRelativeLayout) child;
//				StoryMediaContentView mcv = (StoryMediaContentView) child.findViewById(R.id.ivPhotos);
//				if (isTwoColumnMode())
//				{
//					hlrl.setHeightLimit(0); // Full bleed
//					mcv.setUseFinalSizeForDownloadView(true);
//					if (currentColumnHeight == 0)
//					{
//						// Allow image to bleed to end of margin!
//						columnHeightMax += ((MarginLayoutParams)_CurrentColumn.getLayoutParams()).bottomMargin;
//					}
//				}
//				else
//				{
//					hlrl.setHeightLimit(1.75f);
//				}
//				mcv.setScaleType(!isTwoColumnMode());
//			}
//			else if (child instanceof CustomFontTextView)
//			{
//				CustomFontTextView tv = (CustomFontTextView) child;
//				tv.setMaxLines(Integer.MAX_VALUE);
//			}
//
//			if (child.getVisibility() == View.GONE)
//			{
//				// Dont add this view
//				idxView++;
//				continue;
//			}
//
//			MarginLayoutParams lpChild = (MarginLayoutParams) child.getLayoutParams();
//
//			// Adjust for the center margin between columns
//			if (isTwoColumnMode())
//			{
//				if (isInFirstColumn())
//				{
//					if (lpChild.rightMargin == marginRight)
//						lpChild.rightMargin = marginCenter;
//				}
//				else
//				{
//					if (lpChild.leftMargin == marginLeft)
//						lpChild.leftMargin = marginCenter;
//				}
//			}
//			
//			// If at top of column and child is not photo, add margin
//			if (currentColumnHeight == 0 && child.getId() != R.id.layout_media)
//				currentColumnHeight += fullMarginTop;
//			else if (currentColumnHeight != 0)
//				currentColumnHeight += lpChild.topMargin;
//			
//			if (child.getId() == R.id.layout_media && isTwoColumnMode() && currentColumnHeight != 0 && willCreateMediaView())
//			{
//				// Do nothing. This will pull up a new column for us, in which we will be topmost!
//			}
//			else
//			{
//				boolean spillToNextColumn = false;
//
//				// Adjust to column
//				int widthChild = column.getWidth() - lpChild.leftMargin - lpChild.rightMargin;
//				if (child.getId() == R.id.layout_media && isTwoColumnMode() && willCreateMediaView())
//					child.measure(View.MeasureSpec.makeMeasureSpec(widthChild, View.MeasureSpec.EXACTLY),
//							View.MeasureSpec.makeMeasureSpec(columnHeightMax - currentColumnHeight - lpChild.bottomMargin, View.MeasureSpec.EXACTLY));
//				else
//					child.measure(View.MeasureSpec.makeMeasureSpec(widthChild, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
//				child.layout(lpChild.leftMargin, 0, lpChild.leftMargin + child.getMeasuredWidth(), child.getMeasuredHeight());
//
//				int currentPlusHeight = child.getHeight() + lpChild.bottomMargin + currentColumnHeight;	
////				if (child instanceof CustomFontTextView
////						&& currentPlusHeight > columnHeightMax) 
////				{
////					CustomFontTextView tv = (CustomFontTextView) child;
////					int numVisibleLines = tv.getVisibleLines();
////
////					// Special case, we can't line break the "read more"
////					// control
////					if (child.getId() == R.id.tvReadMore)
////						numVisibleLines = 0;
////
////					if (numVisibleLines == 0) {
////						column = getNextColumn(ret);
////						columnHeightMax = column.getHeight();
////						currentColumnHeight = 0;
////						continue;
////					}
////
////					tv.setHeight(columnHeightMax - currentColumnHeight - lpChild.bottomMargin);
////					tv.measure(View.MeasureSpec.makeMeasureSpec(
////							widthChild, View.MeasureSpec.EXACTLY),
////							View.MeasureSpec.UNSPECIFIED);
////					tv.layout(lpChild.leftMargin, 0, lpChild.leftMargin + tv.getMeasuredWidth(),
////							tv.getMeasuredHeight());
////
////					if (tv.getId() == R.id.tvContent || tv.getId() == R.id.tvTitle
////							|| (tv.getTag() != null
////									&& tv.getTag() instanceof Integer && ((Integer) tv
////									.getTag()).intValue() == R.id.tvContent)) {
////						// Split it in two!
////						CustomFontTextView newClone = ((CustomFontTextView) child)
////								.createClone();
////						newClone.setText(tv.getOverflowingText());
////						newClone.setTag(Integer.valueOf(R.id.tvContent));
////						RelativeLayout.LayoutParams lpNewClone = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
////						lpNewClone.leftMargin = marginLeft;
////						lpNewClone.rightMargin = marginRight;
////						newClone.setLayoutParams(lpNewClone);
////						blueprints.add(idxView + 1, newClone);
////					}
////					spillToNextColumn = true;
////				}
//
//				idxView++;
//
//				// Fits
//				RelativeLayout.LayoutParams relLayout = new RelativeLayout.LayoutParams(child.getWidth(), child.getHeight());
//				relLayout.leftMargin = UIHelpers.getRelativeLeft(column) + lpChild.leftMargin;
//				relLayout.topMargin = UIHelpers.getRelativeTop(column) + currentColumnHeight;
//
//				currentPageView.addView(child);
//				child.setLayoutParams(relLayout);
//				currentColumnHeight += child.getHeight();
//				if (!spillToNextColumn)
//					continue;
//			}
//
//			column = getNextColumn(ret);
//			columnHeightMax = column.getHeight();
//			currentColumnHeight = 0;
//		}
//
//		// Remove the column views themselves
//		for (View page : ret)
//		{
//			View col1 = page.findViewById(R.id.column1);
//			View col2 = page.findViewById(R.id.column2);
//			if (col1 != null)
//				((ViewGroup) page).removeView(col1);
//			if (col2 != null)
//				((ViewGroup) page).removeView(col2);
//		}
//
//		// Animations?
//		View page1 = ret.get(0);
//		if (page1 != null && page1 instanceof AnimatedRelativeLayout)
//		{
//			((AnimatedRelativeLayout) page1).setStartPositions(mStoredPositions);
//		}
//
//		mPages = ret;
//		mStoredPositions = getStoredPositions();
//		this.updateTime();
//		return ret;
//	}

	/**
	 * Use this method to set optional initial starting positions for the views.
	 * 
	 * @param storedPositions
	 */
	public void setStoredPositions(SparseArray<Rect> storedPositions)
	{
		mStoredPositions = storedPositions;
		
		// Animations?
		if (mView != null)
		{
			AnimatedRelativeLayout animatedRoot = (AnimatedRelativeLayout) mView.findViewById(R.id.animatedRoot);
			if (animatedRoot != null)
			{
				animatedRoot.setStartPositions(mStoredPositions);
			}
		}
//		mStoredPositions = getStoredPositions();
//		View page1 = ret.get(0);
//		if (page1 != null && page1 instanceof AnimatedRelativeLayout)
//		{
//			((AnimatedRelativeLayout) page1).setStartPositions(mStoredPositions);
//		}
//
//		mPages = ret;
//		mStoredPositions = getStoredPositions();
//		this.updateTime();
//		return ret;
	}

	public void resetToStoredPositions(int duration)
	{
		View page1 = mPages.get(0);
		if (page1 != null && page1 instanceof AnimatedRelativeLayout)
		{
			((AnimatedRelativeLayout) page1).animateToStartPositions(duration);
		}
	}
	
	private SparseArray<Rect> getStoredPositions()
	{
		if (mPages == null || mPages.size() == 0)
			return null;

		SparseArray<Rect> positions = new SparseArray<Rect>();
		for (View child : mBlueprintViews)
		{
			if (child.getId() != View.NO_ID)
			{
				if (child.getLayoutParams() != null && child.getLayoutParams() instanceof RelativeLayout.LayoutParams)
				{
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) child.getLayoutParams();
					Rect currentRect = new Rect(lp.leftMargin, lp.topMargin, lp.leftMargin + child.getWidth(), lp.topMargin + child.getHeight());
					positions.put(child.getId(), currentRect);
				}
			}
		}
		return positions;
	}

	private void updateTextSize()
	{
		for (View view : this.mBlueprintViews)
		{
			if (view.getId() == R.id.tvContent)
			{
				((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, this.mDefaultTextSize + App.getSettings().getContentFontSizeAdjustment());
			}
			else if (view.getId() == R.id.tvAuthor)
			{
				((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, this.mDefaultAuthorTextSize + App.getSettings().getContentFontSizeAdjustment());
			}
		}
	}

	private void populateViewWithItem(ViewGroup blueprint, Item story)
	{
		// Set title
		//
		TextView tv = (TextView) blueprint.findViewById(R.id.tvTitle);
		if (tv != null)
			tv.setText(story.getTitle());

		// Set image(s)
		//
		StoryMediaContentView mediaContent = (StoryMediaContentView) blueprint.findViewById(R.id.ivPhotos);
		if (mediaContent != null && mMediaViewCollection != null)
		{
			mediaContent.setMediaCollection(mMediaViewCollection, true, true);
		}
		View mediaContainer = blueprint.findViewById(R.id.layout_media);
		if (mediaContainer != null)
		{
			if (!willCreateMediaView())
				mediaContainer.setVisibility(View.GONE);
			else
				mediaContainer.setVisibility(View.VISIBLE);
		}

		// Author
		tv = (TextView) blueprint.findViewById(R.id.tvAuthor);
		if (tv != null)
		{
			mDefaultAuthorTextSize = tv.getTextSize();
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
			if (story.getAuthor() != null)
				tv.setText(blueprint.getContext().getString(R.string.story_item_short_author, story.getAuthor()));
			else
				tv.setText(null);
			if (TextUtils.isEmpty(tv.getText()))
			{
				tv.setVisibility(View.GONE);
			}
		}

		// Author date
		tv = (TextView) blueprint.findViewById(R.id.tvAuthorDate);
		if (tv != null)
			tv.setText(UIHelpers.dateDateDisplayString(story.getPublicationTime(), tv.getContext()));

		// Author time
		tv = (TextView) blueprint.findViewById(R.id.tvAuthorTime);
		if (tv != null)
			tv.setText(UIHelpers.dateTimeDisplayString(story.getPublicationTime(), tv.getContext()));
		
		// Content
		tv = (TextView) blueprint.findViewById(R.id.tvContent);
		if (tv != null)
		{
			mDefaultTextSize = tv.getTextSize();
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tv.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
			tv.setText(story.getCleanMainContent());
			tv.setPaintFlags(tv.getPaintFlags() | Paint.SUBPIXEL_TEXT_FLAG);
			if (TextUtils.isEmpty(tv.getText()))
				tv.setVisibility(View.GONE);
		}

		// Set source
		//
		tv = (TextView) blueprint.findViewById(R.id.tvSource);
		if (tv != null)
		{
			tv.setText(story.getSource());
			tv.setTag(Long.valueOf(story.getFeedId()));
			tv.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					long feedId = ((Long) v.getTag()).longValue();
					if (feedId != -1 && mPagedView.getContext() instanceof ItemExpandActivity)
					{
						((ItemExpandActivity)mPagedView.getContext()).onBackPressed();
						UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, feedId, StoryItemView.this);
					}
				}
			});
		}

		// Time
		tv = (TextView) blueprint.findViewById(R.id.tvTime);
		if (tv != null)
		{
			onUpdateNeeded((UpdatingTextView) tv);
		}

		// Read more
		tv = (TextView) blueprint.findViewById(R.id.tvReadMore);
		if (tv != null)
		{
			if (story.getLink() != null)
			{
				boolean isReadMoreEnabled = !TextUtils.isEmpty(story.getLink()) && App.getInstance().socialReader.isOnline() == SocialReader.ONLINE;

				tv.setEnabled(isReadMoreEnabled);
				if (!isReadMoreEnabled)
				{
					//tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_read_orweb_gray, 0, 0, 0);
					tv.setOnClickListener(null);
				}
				else
				{
					//tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_read_orweb, 0, 0, 0);
					//if (PackageHelper.isOrwebInstalled(blueprint.getContext()))
						tv.setOnClickListener(new ReadMoreClickListener(story));
					//else
					//	tv.setOnClickListener(new PromptOrwebClickListener(blueprint.getContext()));
				}
				tv.setVisibility(View.VISIBLE);
			}
			else
			{
				tv.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public boolean usesReverseSwipe()
	{
		boolean bReverse = false;
		if (mItem != null)
		{
			// Use the bidi class to figure out the swipe direction!
			if (App.getSettings().readerSwipeDirection() == ReaderSwipeDirection.Automatic)
			{
				try
				{
					Bidi bidi = new Bidi(mItem.getCleanMainContent(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
					if (!bidi.baseIsLeftToRight())
						bReverse = true;
				}
				catch (IllegalArgumentException e)
				{
					// Content probably null for some reason.
				}
			}
			else if (App.getSettings().readerSwipeDirection() == ReaderSwipeDirection.Ltr)
			{
				bReverse = true;
			}
		}
		return bReverse;
	}

//	@Override
//	public ArrayList<View> createPages(PagedView parent)
//	{
//		mPagedView = parent;
//		if (mMediaViewCollection != null)
//		{
//			mMediaViewCollection.removeListener(this);
//			mMediaViewCollection.recycle();
//		}
//		mMediaViewCollection = null;
//		if (mItem.getMediaContent() != null && mItem.getMediaContent().size()> 0)
//		{
//			mMediaViewCollection  = new MediaViewCollection(parent.getContext(), mItem);
//			mMediaViewCollection.load(false, true);
//			mMediaViewCollection.addListener(this);
//		}
//		createBlueprintViews(parent);
//		relayout();
//		return mPages;
//	}

	@Override
	public void onUpdateNeeded(UpdatingTextView view)
	{
		if (view != null)
		{
			if (mItem != null)
			{
				view.setText(UIHelpers.dateDiffDisplayString(mItem.getPublicationTime(), view.getContext(), R.string.story_item_short_published_never,
						R.string.story_item_short_published_recently, R.string.story_item_short_published_minutes, R.string.story_item_short_published_minute,
						R.string.story_item_short_published_hours, R.string.story_item_short_published_hour, R.string.story_item_short_published_days,
						R.string.story_item_short_published_day));
			}
			else
			{
				view.setText(R.string.story_item_short_published_never);
			}
		}
	}

	protected void updateTime()
	{
		if (mPages == null)
			return;

		for (View view : mPages)
		{
			UpdatingTextView tvTime = (UpdatingTextView) view.findViewById(R.id.tvTime);
			if (tvTime != null)
			{
				this.onUpdateNeeded(tvTime);
				tvTime.setOnUpdateListener(this);
			}
		}
	}

	@Override
	public void onViewLoaded(MediaViewCollection collection, int index, boolean wasCached)
	{
		if (LOGGING)
			Log.v(LOGTAG, "Media content has requested relayout.");
		//mPagedView.recreateViewsForContent(this);
	}

	private class ReadMoreClickListener implements View.OnClickListener
	{
		private final Item mItem;

		public ReadMoreClickListener(Item item)
		{
			mItem = item;
		}

		@Override
		public void onClick(View v)
		{
			try
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mItem.getLink()));

				String thisPackageName = App.getInstance().getPackageName();

				// Instead of using built in functionality, we create our own chooser so that we
				// can remove ourselves from the list (opening the story in this app would actually
				// take us to the AddFeed page, so it does not make sense to have it as an option)
				List<Intent> targetedShareIntents = new ArrayList<Intent>();
				List<ResolveInfo> resInfo = v.getContext().getPackageManager().queryIntentActivities(intent, 0);
				if (resInfo != null && resInfo.size() > 0)
				{
					for (ResolveInfo resolveInfo : resInfo)
					{
						String packageName = resolveInfo.activityInfo.packageName;

						Intent targetedShareIntent = (Intent) intent.clone();
						targetedShareIntent.setPackage(packageName);
						if (!packageName.equals(thisPackageName)) // Remove
																	// ourselves
						{
							targetedShareIntents.add(targetedShareIntent);
						}
					}

					if (targetedShareIntents.size() > 0)
					{
						Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), null);
						chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[] {}));

						v.getContext().startActivity(chooserIntent);
					}
				}
			}
			catch (Exception e)
			{
				if (LOGGING)
					Log.d(LOGTAG, "Error trying to open read more link: " + mItem.getLink());
			}
		}
	}

//	private class PromptOrwebClickListener implements View.OnClickListener
//	{
//		private final Context mContext;
//
//		public PromptOrwebClickListener(Context context)
//		{
//			mContext = context;
//		}
//
//		@Override
//		public void onClick(View v)
//		{
//			AlertDialog dialog = PackageHelper.showDownloadDialog(mContext, R.string.install_orweb_title, R.string.install_orweb_prompt, android.R.string.ok,
//					android.R.string.cancel, PackageHelper.URI_ORWEB_PLAY);
//		}
//	}
}

