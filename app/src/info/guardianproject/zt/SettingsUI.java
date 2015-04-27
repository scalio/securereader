package info.guardianproject.zt;

import info.guardianproject.securereader.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUI extends Settings
{
	public static final String KEY_ENABLE_SCREENSHOTS = "enable_screenshots";

	public SettingsUI(Context _context)
	{
		super(_context);
	}

	/**
	 * @return Gets whether screen captures are enabled
	 * 
	 */
	public boolean enableScreenshots()
	{
		return mPrefs.getBoolean(KEY_ENABLE_SCREENSHOTS, false);
	}

	/**
	 * @return Sets whether screen captures are enabled
	 * 
	 */
	public void setEnableScreenshots(boolean enable)
	{
		mPrefs.edit().putBoolean(KEY_ENABLE_SCREENSHOTS, enable).commit();
	}
	
}
