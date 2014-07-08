package info.guardianproject.securereaderinterface.uiutil;

import info.guardianproject.securereaderinterface.widgets.CustomFontSpan;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

public class FontManager
{
	public static final String LOGTAG = "FontManager";
	public static final boolean LOGGING = false;
	
	private static HashMap<String, Typeface> gFonts = new HashMap<String, Typeface>();
	
	public static Typeface getFontByName(Context context, String name)
	{
		if (gFonts.containsKey(name))
			return gFonts.get(name);

		try
		{
			Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + ".ttf");
			if (font != null)
			{
				gFonts.put(name, font);
			}
			return font;
		}
		catch (Exception ex)
		{
			if (LOGGING)
				Log.e(LOGTAG, "Failed to get font: " + name);
		}
		return null;
	}
	
	private static Pattern gTibetanPattern = null;
	private static Pattern gTibetanTransformedPattern = null;
	private static Pattern gCyrillicPattern = null;
	
	public static boolean isTibetan(CharSequence text)
	{
		if (!TextUtils.isEmpty(text))
		{
			if (gTibetanPattern == null)
				gTibetanPattern = Pattern.compile("[\u0F00-\u0FFF]+", 0);
	        Matcher unicodeTibetanMatcher = gTibetanPattern.matcher(text);
	        if (unicodeTibetanMatcher.find())
	        {
	        	return true;
	        }
		}
		return false;
	}

	public static boolean isCyrillic(CharSequence text)
	{
		if (!TextUtils.isEmpty(text))
		{
			if (gCyrillicPattern == null)
				gCyrillicPattern = Pattern.compile("[\u0400-\u04FF]+", 0);
	        Matcher unicodeCyrillicMatcher = gCyrillicPattern.matcher(text);
	        if (unicodeCyrillicMatcher.find())
	        {
	        	return true;
	        }
		}
		return false;
	}
	
	public static void getTibetanSpans(Context context, Spannable text)
	{
		if (!TextUtils.isEmpty(text))
		{
        	Typeface font = FontManager.getFontByName(context, "Jomolhari");

			if (gTibetanTransformedPattern == null)
				gTibetanTransformedPattern = Pattern.compile("[[\u0F00-\u0FFF][\uE000-\uF8FF]]+", 0);
	        Matcher unicodeTibetanMatcher = gTibetanTransformedPattern.matcher(text);
	        while (unicodeTibetanMatcher.find())
	        {
				CustomFontSpan tibetanFontSpan = new CustomFontSpan(font);
	        	text.setSpan(tibetanFontSpan, unicodeTibetanMatcher.start(0), unicodeTibetanMatcher.end(0), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				//For debugging, make spanned text red
	        	//ForegroundColorSpan cs = new ForegroundColorSpan(Color.RED);
	        	//text.setSpan(cs, unicodeTibetanMatcher.start(0), unicodeTibetanMatcher.end(0), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
	        }
		}
	}
	
	public static CharSequence transformText(TextView view, CharSequence text)
	{
		if (isCyrillic(text))
		{
			if (view.getTypeface() == FontManager.getFontByName(view.getContext(), "Lato-Light"))
			{
				view.setTypeface(Typeface.DEFAULT);
			}
		}
		return text;
	}
	

}
