package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.iocipher.File;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.tinymission.rss.MediaContent;

public class ImageMediaContentPreviewView extends ImageView implements MediaContentPreviewView
{
	public static final String LOGTAG = "ImageMediaContentPreviewView";
	public static final boolean LOGGING = false;
	
	private MediaContent mMediaContent;
	private File mMediaFile;
	private Bitmap mRealBitmap;
	private Thread mSetImageThread;
	private Handler mHandler;
	private boolean mUseThisThread;
	private boolean mIsUpdate; // true if this view has already shown this content previously

	public ImageMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public ImageMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public ImageMediaContentPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		this.setScaleType(ScaleType.CENTER_CROP);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		setBitmapIfDownloaded();
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	public void setImageBitmap(Bitmap bm)
	{
		// If we are setting the image from a different thread, make sure to
		// fade it in.
		// If we, however, set it from this thread (as we do when closing the
		// full screen mode
		// view) we want it to show immediately!
		if (bm != null && !mUseThisThread && !mIsUpdate)
			AnimationHelpers.fadeOut(this, 0, 0, false);
		super.setImageBitmap(bm);
		if (bm != null && !mUseThisThread && !mIsUpdate)
			AnimationHelpers.fadeIn(this, 500, 0, false);
		else if (bm != null)
			AnimationHelpers.fadeIn(this, 0, 0, false);
	}

	public void recycle()
	{
		setImageBitmap(null);
		if (mRealBitmap != null)
		{
			mRealBitmap.recycle();
			mRealBitmap = null;
		}
	}

	private synchronized void setBitmapIfDownloaded()
	{
		if (mMediaFile != null && mRealBitmap == null && mSetImageThread == null)
		{
			if (mHandler == null && !mUseThisThread)
				mHandler = new Handler();

			Runnable setImageRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					int w = getWidth();
					int h = getHeight();
					Bitmap bmp = UIHelpers.scaleToMaxGLSize(getContext(), mMediaFile, w, h);

					Runnable doSetImageRunnable = new Runnable()
					{
						private Bitmap mBitmap;

						@Override
						public void run()
						{
							mRealBitmap = mBitmap;
							setImageBitmap(mRealBitmap);
						}

						private Runnable init(Bitmap bitmap)
						{
							mBitmap = bitmap;
							return this;
						}

					}.init(bmp);

					if (mUseThisThread)
						doSetImageRunnable.run();
					else
						mHandler.post(doSetImageRunnable);
				}
			};

			if (mUseThisThread)
			{
				setImageRunnable.run();
			}
			else
			{
				mSetImageThread = new Thread(setImageRunnable);
				mSetImageThread.start();
			}
		}
	}

	@Override
	protected int getSuggestedMinimumHeight()
	{
		if (mMediaContent != null && mRealBitmap == null)
			return mMediaContent.getHeight();
		return super.getSuggestedMinimumHeight();
	}

	@Override
	protected int getSuggestedMinimumWidth()
	{
		if (mMediaContent != null && mRealBitmap == null)
			return mMediaContent.getWidth();
		return super.getSuggestedMinimumWidth();
	}

	@Override
	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mIsUpdate = (mediaContent == mMediaContent && mMediaFile != null && mMediaFile.equals(mediaFile));
		mMediaContent = mediaContent;
		mMediaFile = mediaFile;
		mUseThisThread = useThisThread;
		if (mMediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}
		
		int w = getWidth();
		int h = getHeight();
		if (w > 0 && h > 0)
		{
			setBitmapIfDownloaded();
		}
	}

	@Override
	public MediaContent getMediaContent()
	{
		return mMediaContent;
	}

}