package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereader.MediaDownloader.MediaDownloaderCallback;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.models.OnMediaOrientationListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

//import java.io.File;
import info.guardianproject.iocipher.File;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import info.guardianproject.securereaderinterface.R;
import com.tinymission.rss.MediaContent;

public class ImageMediaContentView extends FrameLayout implements MediaDownloaderCallback, OnTouchListener
{
	private static final String LOGTAG = "IMAGEVIEWERMATRIX";
	public static final boolean LOGGING = false;

	// Image Matrix
	Matrix matrix = new Matrix();

	// Saved Matrix
	Matrix savedMatrix = new Matrix();

	// Initial Matrix
	Matrix baseMatrix = new Matrix();

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	static final float MAX_SCALE = 10f;

	// For Zooming
	float startFingerSpacing = 0f;
	float endFingerSpacing = 0f;
	PointF startFingerSpacingMidPoint = new PointF();

	// For Dragging
	PointF startPoint = new PointF();
	Bitmap realBmp;
	private float mMinScaleX;
	private float mMinScaleY;

	private ImageView mImageView;
	private MediaContent mMediaContent;
	private OnMediaOrientationListener mOrientationListener;

	private boolean mEnableInteraction;

	public ImageMediaContentView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public ImageMediaContentView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public ImageMediaContentView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.image_view, this);

		mImageView = (ImageView) findViewById(R.id.content);
	}

	public void setMediaContent(MediaContent mediaContent, boolean enableInteraction)
	{
		mMediaContent = mediaContent;
		mEnableInteraction = enableInteraction;
		App.getInstance().socialReader.loadMediaContent(mMediaContent, this);
	}

	/**
	 * Sets a listener that will be notified when media has been downloaded and
	 * it is known whether this media is in landscape or portrait mode.
	 * 
	 * @param listener
	 */
	public void setOnMediaOrientationListener(OnMediaOrientationListener listener)
	{
		this.mOrientationListener = listener;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		ImageView view = (ImageView) v;

		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_DOWN:

			savedMatrix.set(matrix);

			// Save the Start point. We have a single finger so it is drag
			startPoint.set(event.getX(), event.getY());
			mode = DRAG;
			if (LOGGING)
				Log.d(LOGTAG, "mode=DRAG");

			break;

		case MotionEvent.ACTION_POINTER_DOWN:

			// Get the spacing of the fingers, 2 fingers
			float sx = event.getX(0) - event.getX(1);
			float sy = event.getY(0) - event.getY(1);
			startFingerSpacing = (float) Math.sqrt(sx * sx + sy * sy);

			if (LOGGING)
				Log.d(LOGTAG, "Start Finger Spacing=" + startFingerSpacing);

			if (startFingerSpacing > 10f)
			{

				float xsum = event.getX(0) + event.getX(1);
				float ysum = event.getY(0) + event.getY(1);
				startFingerSpacingMidPoint.set(xsum / 2, ysum / 2);

				mode = ZOOM;
				if (LOGGING)
					Log.d(LOGTAG, "mode=ZOOM");
			}

			break;

		case MotionEvent.ACTION_UP:
			// Nothing

		case MotionEvent.ACTION_POINTER_UP:

			mode = NONE;
			if (LOGGING) 
				Log.d(LOGTAG, "mode=NONE");
			break;

		case MotionEvent.ACTION_MOVE:

			if (mode == DRAG)
			{

				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - startPoint.x, event.getY() - startPoint.y);
				view.setImageMatrix(matrix);
				putOnScreen();

			}
			else if (mode == ZOOM)
			{

				// Get the spacing of the fingers, 2 fingers
				float ex = event.getX(0) - event.getX(1);
				float ey = event.getY(0) - event.getY(1);
				endFingerSpacing = (float) Math.sqrt(ex * ex + ey * ey);

				if (LOGGING)
					Log.d(LOGTAG, "End Finger Spacing=" + endFingerSpacing);

				if (endFingerSpacing > 10f)
				{
					matrix.set(savedMatrix);

					// Ratio of spacing.. If it was 5 and goes to 10 the image
					// is 2x as big
					float scale = endFingerSpacing / startFingerSpacing;
					// Scale from the midpoint
					matrix.postScale(scale, scale, startFingerSpacingMidPoint.x, startFingerSpacingMidPoint.y);

					float[] matrixValues = new float[9];
					matrix.getValues(matrixValues);
					
					if (LOGGING) {
						Log.v(LOGTAG, "Total Scale: " + matrixValues[0]);
						Log.v(LOGTAG, "" + matrixValues[0] + " " + matrixValues[1] + " " + matrixValues[2] + " " + matrixValues[3] + " " + matrixValues[4] + " "
							+ matrixValues[5] + " " + matrixValues[6] + " " + matrixValues[7] + " " + matrixValues[8]);
					}
					
					if (matrixValues[0] > MAX_SCALE)
					{
						matrix.set(savedMatrix);
					}
					else if (matrixValues[Matrix.MSCALE_X] < mMinScaleX || matrixValues[Matrix.MSCALE_Y] < mMinScaleY)
					{
						matrix.set(null);
					}
					view.setImageMatrix(matrix);
					putOnScreen();
				}
			}
			break;
		}

		return true; // indicate event was handled
	}

	public void putOnScreen()
	{
		// Get Rectangle of Tranformed Image
		Matrix currentDisplayMatrix = new Matrix();
		currentDisplayMatrix.set(baseMatrix);
		currentDisplayMatrix.postConcat(matrix);

		RectF theRect = new RectF(0, 0, realBmp.getWidth(), realBmp.getHeight());
		currentDisplayMatrix.mapRect(theRect);

		if (LOGGING) 
			Log.v(LOGTAG, theRect.width() + " " + theRect.height());

		float deltaX = 0, deltaY = 0;
		if (theRect.width() < mImageView.getWidth())
		{
			deltaX = (mImageView.getWidth() - theRect.width()) / 2 - theRect.left;
		}
		else if (theRect.left > 0)
		{
			deltaX = -theRect.left;
			deltaX = Math.min(deltaX, 0);
		}
		else if (theRect.right < mImageView.getWidth())
		{
			deltaX = mImageView.getWidth() - theRect.right;
		}

		if (theRect.height() < mImageView.getHeight())
		{
			deltaY = (mImageView.getHeight() - theRect.height()) / 2 - theRect.top;
		}
		else if (theRect.top > 0)
		{
			deltaY = -theRect.top;
			deltaY = Math.min(deltaY, 0);
		}
		else if (theRect.bottom < mImageView.getHeight())
		{
			deltaY = mImageView.getHeight() - theRect.bottom;
		}

		if (LOGGING) 
			Log.v(LOGTAG, "Deltas:" + deltaX + " " + deltaY);

		currentDisplayMatrix.postTranslate(deltaX, deltaY);
		mImageView.setImageMatrix(currentDisplayMatrix);
		mImageView.invalidate();
	}

	@Override
	public void mediaDownloaded(final File mediaFile)
	{
		realBmp = UIHelpers.scaleToMaxGLSize(getContext(), mediaFile, 0, 0);
		rescaleImage();
		if (mOrientationListener != null)
		{
			boolean isPortrait = (realBmp != null) && (realBmp.getHeight() > realBmp.getWidth());
			mOrientationListener.onMediaOrientation(ImageMediaContentView.this, isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
					: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (changed)
			rescaleImage();
	}

	private void rescaleImage()
	{
		int width = mImageView.getWidth();
		int height = mImageView.getHeight();
		if (width == 0 || height == 0 || realBmp == null)
			return;

		matrix = new Matrix();

		// Get the image's rect
		RectF drawableRect = new RectF(0, 0, realBmp.getWidth(), realBmp.getHeight());

		// Get the image view's rect
		RectF viewRect = new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight());

		// draw the image in the view
		matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);

		float[] values = new float[9];
		matrix.getValues(values);
		float scaleX = values[Matrix.MSCALE_X];
		float scaleY = values[Matrix.MSCALE_Y];
		mMinScaleX = scaleX;
		mMinScaleY = scaleY;

		matrix.set(null);
		matrix.setScale(scaleX, scaleY);

		if (LOGGING) {
			Log.v(LOGTAG, "Display Width: " + width);
			Log.v(LOGTAG, "Display Height: " + height);

			Log.v(LOGTAG, "BMP Width: " + realBmp.getWidth());
			Log.v(LOGTAG, "BMP Height: " + realBmp.getHeight());
		}
		
		mImageView.setImageBitmap(realBmp);
		mImageView.setImageMatrix(matrix);
		baseMatrix.set(matrix);
		savedMatrix.set(matrix);
		matrix.set(null);
		if (mEnableInteraction)
			mImageView.setOnTouchListener(this);
		else
			mImageView.setOnTouchListener(null);
		putOnScreen();
	}

	@Override
	public void mediaDownloadedNonVFS(java.io.File mediaFile) {
		// TODO Auto-generated method stub
		// Not being used for image content at the moment
	}

	// public void onClick(View v)
	// {
	// if (v == zoomIn)
	// {
	// float scale = 1.5f;
	//
	// PointF midpoint = new PointF(view.getWidth() / 2, view.getHeight() / 2);
	// matrix.postScale(scale, scale, midpoint.x, midpoint.y);
	// view.setImageMatrix(matrix);
	// putOnScreen();
	//
	// }
	// else if (v == zoomOut)
	// {
	// float scale = 0.75f;
	//
	// PointF midpoint = new PointF(view.getWidth() / 2, view.getHeight() / 2);
	// matrix.postScale(scale, scale, midpoint.x, midpoint.y);
	// view.setImageMatrix(matrix);
	// putOnScreen();
	//
	// }
	// }
}