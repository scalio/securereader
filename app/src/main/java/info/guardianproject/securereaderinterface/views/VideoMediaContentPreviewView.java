package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.iocipher.File;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.provider.MediaStore.Video;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.tinymission.rss.MediaContent;

public class VideoMediaContentPreviewView extends FrameLayout implements MediaContentPreviewView
{
	public static final String LOGTAG = "VideoMediaContentPreviewView";
	public static final boolean LOGGING = false;
	
	private ImageView mImageView;
	private View mPlayView;
	private File mMediaFile;
	private Handler mHandler;
	private boolean mUseThisThread;
	private MediaContent mMediaContent;

	public VideoMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public VideoMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public VideoMediaContentPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	public void setScaleType(ScaleType scaleType)
	{
		if (mImageView != null)
			mImageView.setScaleType(scaleType);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.video_preview_view, this);

		mImageView = (ImageView) findViewById(R.id.image);
		mPlayView = findViewById(R.id.btnPlay);
		mPlayView.setVisibility(View.GONE);
	}

	public void setMediaContent(MediaContent mediaContent, info.guardianproject.iocipher.File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mMediaContent = mediaContent;
		mUseThisThread = useThisThread;

		mMediaFile = mediaFile;
		if (mMediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}

		if (mHandler == null && !mUseThisThread)
			mHandler = new Handler();

		Runnable getOrientationRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				if (LOGGING)
					Log.v(LOGTAG, "getOrientationThread");
				
				Bitmap preview = ThumbnailUtils.createVideoThumbnail(mMediaFile.getPath(), Video.Thumbnails.MINI_KIND);

				Runnable reportRunnable = new Runnable()
				{
					private Bitmap mBitmap;

					@Override
					public void run()
					{
						if (!mUseThisThread)
							AnimationHelpers.fadeOut(mImageView, 0, 0, false);
						if (mBitmap != null)
						{
							mImageView.setScaleType(ScaleType.CENTER_CROP);
							mImageView.setImageBitmap(mBitmap);
						}
						else
						{
							mImageView.setScaleType(ScaleType.CENTER_CROP);
							mImageView.setImageResource(R.drawable.img_placeholder);
						}
						if (!mUseThisThread)
							AnimationHelpers.fadeIn(mImageView, 500, 0, false);
						else
							AnimationHelpers.fadeIn(mImageView, 0, 0, false);
					}

					private Runnable init(Bitmap bitmap)
					{
						mBitmap = bitmap;
						return this;
					}

				}.init(preview);

				if (mUseThisThread)
					reportRunnable.run();
				else
					mHandler.post(reportRunnable);
			}
		};

		if (mUseThisThread)
		{
			getOrientationRunnable.run();
		}
		else
		{
			Thread getOrientationThread = new Thread(getOrientationRunnable);
			getOrientationThread.start();
		}
	}

	public MediaContent getMediaContent()
	{
		return mMediaContent;
	}
	
	public void recycle()
	{
		mImageView.setImageBitmap(null);
	}
}