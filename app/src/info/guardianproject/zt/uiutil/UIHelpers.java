package info.guardianproject.zt.uiutil;

import info.guardianproject.zt.widgets.NestedViewPager;
import info.guardianproject.zt.R;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;

import javax.microedition.khronos.egl.EGLContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

public class UIHelpers
{
	public static final String LOGTAG = "UIHelpers";
	public static final boolean LOGGING = false;	
	
	public static int dpToPx(int dp, Context ctx)
	{
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static String dateDateDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getDateInstance().format(date);
	}

	public static String dateTimeDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getTimeInstance().format(date);
	}
	
	public static String dateDiffDisplayString(Date date, Context context, int idStringNever, int idStringRecently, int idStringMinutes, int idStringMinute,
			int idStringHours, int idStringHour, int idStringDays, int idStringDay)
	{
		if (date == null)
			return "";

		Date todayDate = new Date();
		double ti = todayDate.getTime() - date.getTime();
		if (ti < 0)
			ti = -ti;
		ti = ti / 1000; // Convert to seconds
		if (ti < 1)
		{
			return context.getString(idStringNever);
		}
		else if (ti < 60)
		{
			return context.getString(idStringRecently);
		}
		else if (ti < 3600 && (int) Math.round(ti / 60) < 60)
		{
			int diff = (int) Math.round(ti / 60);
			if (diff == 1)
				return context.getString(idStringMinute, diff);
			return context.getString(idStringMinutes, diff);
		}
		else if (ti < 86400 && (int) Math.round(ti / 60 / 60) < 24)
		{
			int diff = (int) Math.round(ti / 60 / 60);
			if (diff == 1)
				return context.getString(idStringHour, diff);
			return context.getString(idStringHours, diff);
		}
		else
		// if (ti < 2629743)
		{
			int diff = (int) Math.round(ti / 60 / 60 / 24);
			if (diff == 1)
				return context.getString(idStringDay, diff);
			return context.getString(idStringDays, diff);
		}
		// else
		// {
		// return context.getString(idStringNever);
		// }
	}

	public static int getRelativeLeft(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft() + UIHelpers.getRelativeLeft((View) myView.getParent());
	}

	public static int getRelativeTop(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + UIHelpers.getRelativeTop((View) myView.getParent());
	}

	/**
	 * Get the coordinates of a view relative to another anchor view. The anchor
	 * view is assumed to be in the same view tree as this view.
	 * 
	 * @param anchorView
	 *            View relative to which we are getting the coordinates
	 * @param view
	 *            The view to get the coordinates for
	 * @return A Rect containing the view bounds
	 */
	public static Rect getRectRelativeToView(View anchorView, View view)
	{
		Rect ret = new Rect(getRelativeLeft(view) - getRelativeLeft(anchorView), getRelativeTop(view) - getRelativeTop(anchorView), 0, 0);
		ret.right = ret.left + view.getWidth();
		ret.bottom = ret.top + view.getHeight();
		return ret;
	}

	public static int getStatusBarHeight(Activity activity)
	{
		Rect rectContent = new Rect();
		Window window = activity.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectContent);
		return rectContent.top;
	}

	// From.
	// http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also remove
	 * tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 * 
	 * @param text
	 *            The text, with the tokens, to adjust.
	 * @param token
	 *            The token string; there should be at least two instances of
	 *            token in text.
	 * @param cs
	 *            The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise the
	 *            second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 * 
	 * @see http 
	 *      ://developer.android.com/reference/android/text/style/CharacterStyle
	 *      .html
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs)
	{
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1)
		{
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

	public static void colorizeDrawable(Context context, Drawable drawable)
	{
		if (drawable == null)
			return;

		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(R.attr.actionBarThemeColorIconTint, outValue, true);
		if ((outValue.data & 0xff000000) != 0)
			drawable.setColorFilter(outValue.data, Mode.SRC_ATOP);
		else
			drawable.setColorFilter(null);
	}

	private static Point gMaxGLSize;
	
	@SuppressLint("NewApi")
	private static Point getMaxGLSize(Context context)
	{
		if (gMaxGLSize == null)
		{
// TODO - getMaximumBitmapWidth returns width for a software rendered canvas below, so no use to us.
//			if (Build.VERSION.SDK_INT >= 14)
//			{
//				Bitmap bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
//				Canvas c = new Canvas(bmp);
//				gMaxGLSize = new Point(c.getMaximumBitmapWidth(), c.getMaximumBitmapHeight());
//			}
//			else
			{
				// Try the GL approach
				//
				int[] buf = new int[1];
				EGLContext.getEGL();
				GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, buf, 0);
				int glSize = buf[0];
				if (glSize < 1)
				{
					if (LOGGING)
						Log.v(LOGTAG, "GL Max texture size returned 0!");
					
					int rounded = (int)(2048f / context.getResources().getDisplayMetrics().density);
					gMaxGLSize = new Point(rounded, rounded);
				}
				else
				{
					gMaxGLSize = new Point(glSize, glSize);
				}
			}
		}
		return gMaxGLSize;
	}
	
	public static Bitmap scaleToMaxGLSize(Context context, File mediaFile, int width, int height)
	{
		try {

			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
		
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(mediaFile));
			BitmapFactory.decodeStream(bis, null, o);
			bis.close();
			
			Point maxSize = getMaxGLSize(context);

			int scale = 1;
			if (o.outWidth > maxSize.x || o.outHeight > maxSize.y)
			{
				// Find the correct scale value. It should be the power of 2.
				int width_tmp = o.outWidth, height_tmp = o.outHeight;
				while (true)
				{
					if (width_tmp < maxSize.x && height_tmp < maxSize.y)
					{
						break;
					}
					width_tmp /= 2;
					height_tmp /= 2;
					scale *= 2;
				}
			}

			int scaleIV = 1;
			if (width > 0 && height > 0)
			{
				int heightRatio = (int) Math.ceil(o.outHeight / (float) height);
				int widthRatio = (int) Math.ceil(o.outWidth / (float) width);
				if (heightRatio > 1 && widthRatio > 1)
				{
					if (heightRatio > widthRatio)
					{
						scaleIV = heightRatio;
					}
					else
					{
						scaleIV = widthRatio;
					}
				}
			}

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = Math.max(scale, scaleIV);
			//return BitmapFactory.decodeFile(mediaFile.getAbsolutePath(), o2);
			
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(mediaFile));
			Bitmap returnBitmap = BitmapFactory.decodeStream(bis2, null, o2);
			bis2.close();
			
			return returnBitmap;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressLint("NewApi")
	public static boolean canScrollHorizontally(View v, int direction)
	{
		if (Build.VERSION.SDK_INT >= 14)
		{
			return v.canScrollHorizontally(direction);
		}
		else
		{
			// Check for method!
			try
			{
				if (v instanceof NestedViewPager)
					return ((NestedViewPager) v).canScrollHorizontally(direction);

				Method m = v.getClass().getMethod("canScrollHorizontally", int.class);
				if (m != null)
					return (Boolean) m.invoke(v, direction);
			}
			catch (NoSuchMethodException e)
			{
			}
			catch (IllegalArgumentException e)
			{
			}
			catch (IllegalAccessException e)
			{
			}
			catch (InvocationTargetException e)
			{
			}
		}
		return ViewCompat.canScrollHorizontally(v, direction);
	}
	
	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = activity.getCurrentFocus();
		if (view != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
}
