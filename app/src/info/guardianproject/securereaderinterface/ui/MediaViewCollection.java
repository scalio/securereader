package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.MediaDownloader.MediaDownloaderCallback;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.views.ApplicationMediaContentPreviewView;
import info.guardianproject.securereaderinterface.views.EPubMediaContentPreviewView;
import info.guardianproject.securereaderinterface.views.ImageMediaContentPreviewView;
import info.guardianproject.securereaderinterface.views.MediaContentPreviewView;
import info.guardianproject.securereaderinterface.views.VideoMediaContentPreviewView;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView.ScaleType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class MediaViewCollection 
{
	public interface OnMediaLoadedListener
	{
		void onViewLoaded(MediaViewCollection collection, int index, boolean wasCached);
	}

	public static final String LOGTAG = "MediaContentLoader";
	public static final boolean LOGGING = false;
	
	private Context mContext;
	private Item mStory;
	private ArrayList<MediaContentLoadInfo> mLoadInfos;
	private ArrayList<MediaContentPreviewView> mViews;
	private ArrayList<OnMediaLoadedListener> mListeners;
	private boolean mHasBeenRecycled;
	private ScaleType mDefaultScaleType;
	private boolean mForceBitwiseDownloads;
	private boolean mUseThisThread;
	
	public MediaViewCollection(Context context, Item story)
	{
		mContext = context;
		mStory = story;
		mForceBitwiseDownloads = false;
		mUseThisThread = false;
		mDefaultScaleType = ScaleType.CENTER_CROP;
		mLoadInfos = new ArrayList<MediaContentLoadInfo>();
		mViews = new ArrayList<MediaContentPreviewView>();
		mListeners = new ArrayList<OnMediaLoadedListener>();
		createLoadInfos();
	}	
	
	public void load(boolean forceBitwiseDownloads, boolean useThisThread)
	{
		mHasBeenRecycled = false;
		mForceBitwiseDownloads = forceBitwiseDownloads;
		mUseThisThread = useThisThread;
		createMediaViews();
		for (MediaContentLoadInfo info : mLoadInfos)
		{
			info.load(mForceBitwiseDownloads);
		}
	}
	
	public Item getItem()
	{
		return mStory;
	}
	
	public void addListener(OnMediaLoadedListener listener)
	{
		synchronized(this)
		{
			if (!mListeners.contains(listener))
				mListeners.add(listener);
		}
	}
	
	public void removeListener(OnMediaLoadedListener listener)
	{
		synchronized(this)
		{
			if (mListeners.contains(listener))
				mListeners.remove(listener);
		}
	}
	
	private void createLoadInfos()
	{
		if (mStory != null)
		{
			int idxView = 0;
			for (int i = 0; i < mStory.getNumberOfMediaContent(); i++)
			{
				MediaContent mediaContent = mStory.getMediaContent(i);
				if (mediaContent == null || mediaContent.getUrl() == null || mediaContent.getType() == null)
					continue;

				MediaContentLoadInfo info = new MediaContentLoadInfo(mediaContent, idxView++);
				mLoadInfos.add(info);
			}
		}
	}
	
	private void createMediaViews()
	{
		if (mViews == null || mViews.size() == 0)
		{
			for (MediaContentLoadInfo info : mLoadInfos)
			{
				createMediaView(info);
			}
		}
	}
	
	private void createMediaView(MediaContentLoadInfo content)
	{
		MediaContentPreviewView mediaView = null;
		
		// Create a view for it
		if (content.isVideo())
		{
			VideoMediaContentPreviewView vmc = new VideoMediaContentPreviewView(mContext);
			vmc.setScaleType(mDefaultScaleType);
			mediaView = vmc;
		}
		else if (content.isApplication())
		{
			ApplicationMediaContentPreviewView amc = new ApplicationMediaContentPreviewView(mContext);
			mediaView = amc;
		}
		else if (content.isEpub())
		{
			EPubMediaContentPreviewView amc = new EPubMediaContentPreviewView(mContext);
			mediaView = amc;
		}
		else if (content.isAudio())
		{
			VideoMediaContentPreviewView vmc = new VideoMediaContentPreviewView(mContext);
			mediaView = vmc;
		}
		else
		{
			ImageMediaContentPreviewView imc = new ImageMediaContentPreviewView(mContext);
			imc.setScaleType(mDefaultScaleType);
			mediaView = imc;
		}
		mViews.add(mediaView);
	}
	
	public ArrayList<MediaContentPreviewView> getViews()
	{
		return mViews;
	}
	
	public ArrayList<MediaContentPreviewView> getLoadedViews()
	{
		ArrayList<MediaContentPreviewView> views = null;
		if (mViews != null && mViews.size() > 0)
		{
			views = new ArrayList<MediaContentPreviewView>();
			for (MediaContentPreviewView view : mViews)
			{
				if (view.getMediaContent() != null)
					views.add(view);
			}
		}
		return views;
	}
	
	public int getCount()
	{
		return mLoadInfos.size();
	}
	
	public int getCountLoaded()
	{
		int n = 0;
		if (mViews != null && mViews.size() > 0)
		{
			for (MediaContentPreviewView view : mViews)
			{
				if (view.getMediaContent() != null)
					n++;
			}
		}
		return n;
	}
	
	/**
	 * Stop loading
	 */
	public void recycle()
	{
		mHasBeenRecycled = true;
		for (MediaContentPreviewView view : getViews())
		{
			view.recycle();
		}
		mViews.clear();
	}
	
	public class MediaContentLoadInfo implements MediaDownloaderCallback
	{
		public info.guardianproject.iocipher.File mFile;
		public java.io.File mFileNonVFS;
		
		private MediaContent mContent;
		private int mIndex;
		private boolean mIsLoading;
		private boolean mIsLoaded;
		private boolean mWasCached;
		private boolean mInConstructor;
		private boolean mNotifyDownloadsAdapter;
		
		public MediaContentLoadInfo(MediaContent content, int index)
		{
			mContent = content;
			mIndex = index;
			mInConstructor = true;
			mIsLoaded = mWasCached = App.getInstance().socialReader.loadMediaContent(content, this, false, false);
			mInConstructor = false;
			mNotifyDownloadsAdapter = false;
		}
		
		public void load(boolean forceBitwiseDownloads)
		{
			synchronized (this)
			{
				if (forceBitwiseDownloads)
					mNotifyDownloadsAdapter = true;

				if (mFile != null || mFileNonVFS != null)
				{
					onMediaAvailable(mContent, mIndex, mWasCached, mFileNonVFS, mFile);
					if (mNotifyDownloadsAdapter)
						DownloadsAdapter.downloaded(MediaViewCollection.this);
				}
				else
				{
					if (!isLoading())
					{
						mIsLoading = true;
						if (!App.getInstance().socialReader.loadMediaContent(mContent, this, forceBitwiseDownloads))
							mIsLoading = false; // Already loaded
						else if (mNotifyDownloadsAdapter)
							DownloadsAdapter.downloading(MediaViewCollection.this);
					}
				}
			}
		}

		public boolean isLoaded()
		{
			return mIsLoaded;
		}
		
		public boolean isLoading()
		{
			return mIsLoading;
		}

		public boolean isVideo()
		{
			return mContent.getMediaContentType() == MediaContent.MediaContentType.VIDEO;
		}
		
		public boolean isAudio()
		{
			return mContent.getMediaContentType() == MediaContent.MediaContentType.AUDIO;
		}
		
		public boolean isApplication()
		{
			return mContent.getMediaContentType() == MediaContent.MediaContentType.APPLICATION;
		}
		
		public boolean isEpub()
		{
			return mContent.getMediaContentType() == MediaContent.MediaContentType.EPUB;
		}
		
		@Override
		public void mediaDownloaded(File mediaFile)
		{
			synchronized (this)
			{
				mFile = mediaFile;
				mIsLoading = false;
				mIsLoaded = true;
				if (!mInConstructor)
					onMediaAvailable(mContent, mIndex, mWasCached, mFileNonVFS, mFile);
				if (mNotifyDownloadsAdapter)
					DownloadsAdapter.downloaded(MediaViewCollection.this);
			}
		}

		@Override
		public void mediaDownloadedNonVFS(java.io.File mediaFile)
		{
			synchronized (this)
			{
				mContent.setDownloadedNonVFSFile(mediaFile);
				
				mFileNonVFS = mediaFile;
				mIsLoading = false;
				mIsLoaded = true;
				if (!mInConstructor)
					onMediaAvailable(mContent, mIndex, mWasCached, mFileNonVFS, mFile);
				if (mNotifyDownloadsAdapter)
					DownloadsAdapter.downloaded(MediaViewCollection.this);
			};
		}

		
		public MediaContent getMediaContent()
		{
			return mContent;
		}
	}

	public boolean containsLoadedMedia()
	{
		return Iterables.any(mLoadInfos, new Predicate<MediaContentLoadInfo>()
			{
				@Override
				public boolean apply(MediaContentLoadInfo info)
				{
					return info.isLoaded();
				}
			});
	}

	public void onMediaAvailable(MediaContent content, int index, boolean wasCached, java.io.File mediaFileNonVFS, info.guardianproject.iocipher.File mediaFile)
	{
		if (mHasBeenRecycled)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Media downloaded, but already recycled. Ignoring.");
			return;
		}
		
		MediaContentPreviewView mediaView = mViews.get(index);
		mediaView.setMediaContent(content, mediaFile, mediaFileNonVFS, mUseThisThread);		

		ArrayList<OnMediaLoadedListener> listeners;
		synchronized(this)
		{
			listeners = new ArrayList<OnMediaLoadedListener>(mListeners);
		}
		for (OnMediaLoadedListener listener : listeners)
			listener.onViewLoaded(this, index, wasCached);
	}

	public boolean isLoadingMedia()
	{
		return Iterables.any(mLoadInfos, new Predicate<MediaContentLoadInfo>()
				{
					@Override
					public boolean apply(MediaContentLoadInfo info)
					{
						return info.isLoading();
					}
				});
	}
	
	public MediaContentPreviewView getFirstView()
	{
		for (MediaContentPreviewView v : mViews)
		{
			if (v != null)
				return v;
		}
		return null;
	}
	
	public MediaContentLoadInfo getFirstLoadInfo()
	{
		if (mLoadInfos != null && mLoadInfos.size() > 0)
			return mLoadInfos.get(0);
		return null;
	}
	
	public boolean isFirstViewPortrait()
	{
		MediaContentPreviewView first = getFirstView();
		if (first != null && first.getMediaContent() != null)
		{
			return (first.getMediaContent().getHeight() > first.getMediaContent().getWidth());
		}
		return false;
	}

	public void setScaleType(ScaleType scaleType)
	{
		if (scaleType != mDefaultScaleType)
		{
			mDefaultScaleType = scaleType;
			
			for (MediaContentPreviewView view : mViews)
			{
				if (view != null)
				{
					if (view instanceof ImageMediaContentPreviewView)
					{
						((ImageMediaContentPreviewView) view).setScaleType(mDefaultScaleType);
					}
					else if (view instanceof VideoMediaContentPreviewView)
					{
						((VideoMediaContentPreviewView) view).setScaleType(mDefaultScaleType);
					}
				}
			}
		}
	}
}
