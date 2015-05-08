package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.Settings.ReaderSwipeDirection;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ItemExpandActivity;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection.OnMediaLoadedListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.AnimatedRelativeLayout;
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
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tinymission.rss.Item;

public class StoryItemView implements OnUpdateListener, OnMediaLoadedListener
{
	public static final String LOGTAG = "StoryItemView";
	public static final boolean LOGGING = false;
	
	private final Item mItem;
	private MediaViewCollection mMediaViewCollection;
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
	
	public void recycle()
	{
		if (mMediaViewCollection != null)
		{
			mMediaViewCollection.removeListener(this);
			mMediaViewCollection.recycle();
		}
		mView = null;
	}
	
	public View getView(ViewGroup parentContainer)
	{
		if (mView == null)
		{
			if (mItem.getMediaContent() != null && mItem.getMediaContent().size()> 0)
			{
				mMediaViewCollection  = new MediaViewCollection(parentContainer.getContext(), mItem);
				mMediaViewCollection.load(false, true);
				mMediaViewCollection.addListener(this);
			}
			
			LayoutInflater inflater = LayoutInflater.from(parentContainer.getContext());

			ViewGroup blueprint = (ViewGroup) inflater.inflate(R.layout.story_item_page_blueprint, parentContainer, false);
			populateViewWithItem(blueprint, mItem);
			blueprint.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
			mView = blueprint;
			mView.setTag(this);
			updateTextSize();
		}
		return mView;
	}

	private boolean willCreateMediaView()
	{
		if (mMediaViewCollection != null && mMediaViewCollection.getCount() > 0)
			return true;
		return false;
	}

	private AnimatedRelativeLayout getAnimatedRoot()
	{
		if (mView != null)
		{
			return (AnimatedRelativeLayout) mView.findViewById(R.id.animatedRoot);
		}
		return null;
	}
	
	/**
	 * Use this method to set optional initial starting positions for the views.
	 * 
	 * @param storedPositions
	 */
	public void setStoredPositions(SparseArray<Rect> storedPositions)
	{
		mStoredPositions = storedPositions;
		
		// Animations?
		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.setStartPositions(mStoredPositions);
		}
	}

	public void resetToStoredPositions(SparseArray<Rect> storedPositions, int duration)
	{
		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.animateToStartPositions(storedPositions, duration);
		}
	}

	void updateTextSize()
	{
		if (mView != null)
		{
			TextView tv = (TextView)mView.findViewById(R.id.tvContent);
			if (tv != null)
			{
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.mDefaultTextSize + App.getSettings().getContentFontSizeAdjustment());
			}
			tv = (TextView)mView.findViewById(R.id.tvAuthor);
			if (tv != null)
			{
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.mDefaultAuthorTextSize + App.getSettings().getContentFontSizeAdjustment());
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
					if (feedId != -1 && mView.getContext() instanceof ItemExpandActivity)
					{
						((ItemExpandActivity)mView.getContext()).onBackPressed();
						UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, feedId, StoryItemView.this);
					}
				}
			});
		}

		// Time
		UpdatingTextView tvTime = (UpdatingTextView) blueprint.findViewById(R.id.tvTime);
		if (tvTime != null)
		{
			onUpdateNeeded(tvTime);
			tvTime.setOnUpdateListener(this);
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

	private SparseArray<Rect> getStoredPositions()
	{
		SparseArray<Rect> positions = null;
		if (mView != null)
		{
			AnimatedRelativeLayout animatedRoot = (AnimatedRelativeLayout) mView.findViewById(R.id.animatedRoot);
			if (animatedRoot != null)
			{
				positions = new SparseArray<Rect>();
				for (int iChild = 0; iChild < animatedRoot.getChildCount(); iChild++)
				{
					View child = animatedRoot.getChildAt(iChild);
					if (child.getId() != View.NO_ID)
					{
						Rect currentRect = new Rect(child.getLeft(), child.getTop(), child.getLeft() + child.getWidth(), child.getTop() + child.getHeight());
						positions.put(child.getId(), currentRect);
					}
				}
			}
		}
		return positions;
	}

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

	@Override
	public void onViewLoaded(MediaViewCollection collection, int index, boolean wasCached)
	{
		if (LOGGING)
			Log.v(LOGTAG, "Media content has requested relayout.");

		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.setStartPositions(getStoredPositions());
			animatedRoot.requestLayout();
		}
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

