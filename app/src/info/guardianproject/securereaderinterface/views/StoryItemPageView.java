package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.StoryItemMediaContentPagerAdapter;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.DottedProgressView;
import info.guardianproject.securereaderinterface.widgets.NestedViewPager;
import info.guardianproject.yakreader.R;

import com.tinymission.rss.Item;

public class StoryItemPageView extends RelativeLayout 
{
	public interface StoryItemPageViewListener
	{
		void onSourceClicked(long feedId);
		void onTagClicked(String tag);
	}
	
	public enum ViewType
	{
		NO_PHOTO, PORTRAIT_PHOTO, LANDSCAPE_PHOTO
	}
	
	protected Item mItem;
	protected ViewType mCurrentViewType;
	protected NestedViewPager mMediaPager;
	protected DottedProgressView mMediaPagerIndicator;
	protected TextView mTvTitle;
	protected TextView mTvContent;
	protected TextView mTvTime;
	protected TextView mTvSource;
	protected View mLayoutTags;
	protected LinearLayout mLlTags;
	
	protected StoryItemPageViewListener mListener;
	
	// Configuration
	//
	private boolean mShowTags;
	private MediaViewCollection mMediaViewCollection;

	public StoryItemPageView(Context context)
	{
		super(context);
		init(null);
	}

	public StoryItemPageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public StoryItemPageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	private void init(AttributeSet attrs)
	{
		mCurrentViewType = null;
		setBackgroundResource(R.drawable.story_item_background_selector);
//		if (attrs != null && !this.isInEditMode())
//		{
//			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StoryItemPageView);
//			mShowAuthor = a.getBoolean(R.styleable.StoryItemPageView_show_author, true);
//			mShowContent = a.getBoolean(R.styleable.StoryItemPageView_show_content, true);
//			mShowSource = a.getBoolean(R.styleable.StoryItemPageView_show_source, true);
//			a.recycle();
//		}
	}
	
	public static ViewType getViewTypeForMedia(MediaViewCollection mediaViewCollection)
	{
		// No media
		if (mediaViewCollection == null || mediaViewCollection.getCount() == 0)
			return ViewType.NO_PHOTO;
	
		if (mediaViewCollection.containsLoadedMedia())
		{
			if (mediaViewCollection.isFirstViewPortrait())
				return ViewType.PORTRAIT_PHOTO; // Portrait mode
			return ViewType.LANDSCAPE_PHOTO; // Landscape mode
		}
		return ViewType.NO_PHOTO; // Nothing loaded
	}
	
	protected int getViewResourceByType(ViewType type)
	{
		if (type == ViewType.NO_PHOTO)
			return R.layout.story_item_page_merge_no_photo;
		else if (type == ViewType.PORTRAIT_PHOTO)
			return R.layout.story_item_page_merge_portrait_photo;
		return R.layout.story_item_page_merge_landscape_photo;
	}

	private void createViews()
	{
		ViewType type = getViewTypeForMedia(mMediaViewCollection);
		createViews(type);
	}
	
	public void createViews(ViewType type)
	{
		if (mItem == null)
			return;

		if (type == mCurrentViewType)
			return;
		
		if (mCurrentViewType != null)
			removeAllViews();
		
		mCurrentViewType = type;

		LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(getViewResourceByType(mCurrentViewType), this, true);
		findViews(view);
	}
	
	protected void findViews(View view)
	{
		mMediaPager = (NestedViewPager) view.findViewById(R.id.mediaPager);
		mMediaPagerIndicator = (DottedProgressView) view.findViewById(R.id.mediaPagerIndicator);
		mTvTitle = (TextView) view.findViewById(R.id.tvTitle);
		mTvContent = (TextView) view.findViewById(R.id.tvContent);
		mTvTime = (TextView) view.findViewById(R.id.tvTime);
		mTvSource = (TextView) view.findViewById(R.id.tvSource);
		mLayoutTags = view.findViewById(R.id.layout_tags);
		mLlTags = (LinearLayout) view.findViewById(R.id.llTags);
		if (mTvContent != null)
			mTvContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTvContent.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
		
		// Additional one-time configuration
		if (mMediaPager != null)
		{
			mMediaPager.setViewPagerIndicator(mMediaPagerIndicator);
			mMediaPager.setPropagateClicks(true);
		}
	}

	public void populateWithItem(Item item)
	{
		if (mItem != item)
		{
			mItem = item;
			if (mMediaViewCollection != null)
			{
				mMediaViewCollection.recycle();
			}
			mMediaViewCollection = null;
			if (mMediaViewCollection == null && item.getNumberOfMediaContent() > 0)
				mMediaViewCollection = new MediaViewCollection(getContext(), item);
			
			createViews();

			if (mMediaPager != null)
				mMediaPager.setAdapter(null);
			if (mTvTitle != null)
				mTvTitle.setText(item.getTitle());
			if (mTvContent != null)
				mTvContent.setText(item.getCleanMainContent());
			if (mTvSource != null)
			{
				mTvSource.setText(item.getSource());
				mTvSource.setTag(Long.valueOf(item.getFeedId()));
				mTvSource.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						long feedId = ((Long) v.getTag()).longValue();
						if (feedId != -1)
						{
							if (mListener != null)
								mListener.onSourceClicked(feedId);
						}
					}
				});
			}
			if (mLlTags != null && mShowTags && item.getNumberOfTags() > 0)
			{
				mLlTags.removeAllViews();
				for (final String tag : item.getTags())
				{
					View tagItem = LayoutInflater.from(getContext()).inflate(R.layout.story_item_short_tag_item, mLlTags, false);
					TextView tv = (TextView) tagItem.findViewById(R.id.tvTag);
					tv.setText(tag);
					tagItem.setOnClickListener(new OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							if (mListener != null)
								mListener.onTagClicked(tag);
						}
					});
					mLlTags.addView(tagItem);
				}
			}
		}

		populateTime();
		
		if (mLayoutTags != null)
		{
			if (mShowTags && item.getNumberOfTags() > 0)
				mLayoutTags.setVisibility(View.VISIBLE);
			else
				mLayoutTags.setVisibility(View.GONE);
		}
	}
	
	public void loadMedia(MediaViewCollection.OnMediaLoadedListener listener)
	{
		if (mMediaViewCollection != null)
		{
			if (listener != null)
				mMediaViewCollection.addListener(listener);
			mMediaViewCollection.load(false, false);
			if (mMediaPager != null && mMediaViewCollection.getCountLoaded() > 0)
				mMediaPager.setAdapter(new StoryItemMediaContentPagerAdapter(getContext(), mMediaViewCollection.getLoadedViews(), false));
		}
	}
	
	protected void populateTime()
	{
		if (mTvTime != null)
			mTvTime.setText(UIHelpers.dateDiffDisplayString(mItem.getPublicationTime(), getContext(), R.string.story_item_short_published_never,
						R.string.story_item_short_published_recently, R.string.story_item_short_published_minutes, R.string.story_item_short_published_minute,
						R.string.story_item_short_published_hours, R.string.story_item_short_published_hour, R.string.story_item_short_published_days,
						R.string.story_item_short_published_day));
	}

	private final Runnable mUpdateTimestamp = new Runnable()
	{
		@Override
		public void run()
		{
			// Every minute
			populateTime();

			Handler handler = getHandler();
			if (handler != null)
			{
				handler.postDelayed(mUpdateTimestamp, 1000 * 60); // Every
																	// minute
			}
		}
	};

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		Handler handler = getHandler();
		if (handler != null)
		{
			handler.removeCallbacks(mUpdateTimestamp);
			handler.postDelayed(mUpdateTimestamp, 1000 * 60); // Every minute
		}
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		Handler handler = getHandler();
		if (handler != null)
		{
			handler.removeCallbacks(mUpdateTimestamp);
		}
	}
	
	public boolean showTags()
	{
		return mShowTags;
	}
	
	public void showTags(boolean showTags)
	{
		mShowTags = showTags;
	}
	
	public void setListener(StoryItemPageViewListener listener)
	{
		mListener = listener;
	}
}
