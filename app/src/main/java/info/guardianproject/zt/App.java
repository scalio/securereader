package info.guardianproject.zt;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.Settings.UiLanguage;
import info.guardianproject.securereader.SocialReader.SocialReaderLockListener;
import info.guardianproject.zt.models.FeedFilterType;
import info.guardianproject.zt.ui.UICallbackListener;
import info.guardianproject.zt.ui.UICallbacks;
import info.guardianproject.zt.widgets.CustomFontButton;
import info.guardianproject.zt.widgets.CustomFontEditText;
import info.guardianproject.zt.widgets.CustomFontRadioButton;
import info.guardianproject.zt.widgets.CustomFontTextView;
import info.guardianproject.zt.z.rss.utils.DataStorage;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReporter;

import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.tinymission.rss.Feed;

public class App extends MultiDexApplication implements OnSharedPreferenceChangeListener, SocialReaderLockListener
{
	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;
	
	public static final boolean UI_ENABLE_POPULAR_ITEMS = false;
			
	public static final boolean UI_ENABLE_COMMENTS = false;
	public static final boolean UI_ENABLE_TAGS = true;
	public static final boolean UI_ENABLE_POST_LOGIN = false;
	public static final boolean UI_ENABLE_REPORTER = false;
	public static final boolean UI_ENABLE_CHAT = false;
	public static final boolean UI_ENABLE_LANGUAGE_CHOICE = true;
	
	public static final String EXIT_BROADCAST_ACTION = "info.guardianproject.zt.exit.action";
	public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "info.guardianproject.zt.setuilanguage.action";
	public static final String WIPE_BROADCAST_ACTION = "info.guardianproject.zt.wipe.action";
	public static final String LOCKED_BROADCAST_ACTION = "info.guardianproject.zt.lock.action";
	public static final String UNLOCKED_BROADCAST_ACTION = "info.guardianproject.zt.unlock.action";

	public static final String FRAGMENT_TAG_RECEIVE_SHARE = "FragmentReceiveShare";
	public static final String FRAGMENT_TAG_SEND_BT_SHARE = "FragmentSendBTShare";

	private static App m_singleton;

	public static Context m_context;
	public static SettingsUI m_settings;

	public SocialReader socialReader;
	public SocialReporter socialReporter;
	
	private String mCurrentLanguage;
	private FeedFilterType mCurrentFeedFilterType = FeedFilterType.ALL_FEEDS;
	private Feed mCurrentFeed = null;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		m_singleton = this;
		m_context = this;
		m_settings = new SettingsUI(m_context);
		applyUiLanguage();

		socialReader = SocialReader.getInstance(this.getApplicationContext());
		socialReader.setLockListener(this);
		socialReporter = new SocialReporter(socialReader);
		//applyPassphraseTimeout();
		
		m_settings.registerChangeListener(this);
		
		mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
		UICallbacks.getInstance().addListener(new UICallbackListener()
		{
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				Feed feed = null;
				if (type == FeedFilterType.SINGLE_FEED)
				{
					feed = getFeedById(feedId);
				}
				mCurrentFeedFilterType = type;
				mCurrentFeed = feed;
			}
		});
		
		//init RZ App
		initRadioZApp();
	}

	public static Context getContext()
	{
		return m_context;
	}

	public static App getInstance()
	{
		return m_singleton;
	}

	public static SettingsUI getSettings()
	{
		return m_settings;
	}

	private Bitmap mTransitionBitmap;

	private LockScreenActivity mLockScreen;

	public Bitmap getTransitionBitmap()
	{
		return mTransitionBitmap;
	}

	public void putTransitionBitmap(Bitmap bitmap)
	{
		mTransitionBitmap = bitmap;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(Settings.KEY_UI_LANGUAGE))
		{
			applyUiLanguage();
		}
		else if (key.equals(Settings.KEY_PASSPHRASE_TIMEOUT))
		{
			applyPassphraseTimeout();
		}
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		applyUiLanguage();
	}

	@SuppressLint("NewApi")
	private void applyUiLanguage()
	{
		UiLanguage lang = m_settings.uiLanguage();

		// Update language!
		//
		Configuration config = new Configuration();
		
		String language = "en";
		if (lang == UiLanguage.Farsi)
			language = "ar";
		else if (lang == UiLanguage.Tibetan)
			language = "bo";
		else if (lang == UiLanguage.Chinese)
			language = "zh";
		else if (lang == UiLanguage.Ukrainian)
			language = "uk";
		else if (lang == UiLanguage.Russian)
			language = "ru";
		else if (lang == UiLanguage.Spanish)
			language = "es";
		else if (lang == UiLanguage.Japanese)
			language = "ja";
		else if (lang == UiLanguage.Norwegian)
			language = "nb";
		else if (lang == UiLanguage.Turkish)
			language = "tr";
		
		if (language.equals(mCurrentLanguage))
			return;
		mCurrentLanguage = language;
		
		Locale loc = new Locale(language);
		if (Build.VERSION.SDK_INT >= 17)
			config.setLocale(loc);
		else
			config.locale = loc;
		Locale.setDefault(loc);
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
	
		// Notify activities (if any)
		LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
}

	private void applyPassphraseTimeout()
	{
		socialReader.setCacheWordTimeout(m_settings.passphraseTimeout());
	}

	public void wipe(int wipeMethod)
	{
		socialReader.doWipe(wipeMethod);

		// Notify activities (if any)
		LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(new Intent(App.WIPE_BROADCAST_ACTION));
	}
	
	public static View createView(String name, Context context, AttributeSet attrs)
	{
		int id = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "id", -1);
		if (Build.VERSION.SDK_INT < 11)
		{
			// Older devices don't support setting the "android:alertDialogTheme" in styles.xml
			int idParent = Resources.getSystem().getIdentifier("parentPanel", "id", "android");
			if (id == idParent)
				context.setTheme(R.style.ModalDialogTheme);
		}
		
		if (name.equals("TextView") || name.endsWith("DialogTitle"))
		{
			return new CustomFontTextView(context, attrs);
		}
		else if (name.equals("Button"))
		{
			View view = null;
			if (id == android.R.id.button1) // Positive button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonPositiveTheme), attrs);
			else if (id == android.R.id.button2) // Negative button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonNegativeTheme), attrs);
			else
				view = new CustomFontButton(context, attrs);		
			return view;
		}
		else if (name.equals("RadioButton"))
		{
			return new CustomFontRadioButton(context, attrs);
		}
		else if (name.equals("EditText"))
		{
			return new CustomFontEditText(context, attrs);
		}
		return null;
	}

	private int mnResumed = 0;
	private Activity mLastResumed;
	private boolean mIsLocked = true;
	
	public void onActivityPause(Activity activity)
	{
		mnResumed--;
		if (mnResumed == 0)
			socialReader.onPause();
		if (mLastResumed == activity)
			mLastResumed = null;
	}

	public void onActivityResume(Activity activity)
	{
		mLastResumed = activity;
		mnResumed++;
		if (mnResumed == 1)
			socialReader.onResume();
		showLockScreenIfLocked();
	}
	
	public boolean isActivityLocked()
	{
		return mIsLocked;
	}
	
	private void showLockScreenIfLocked()
	{
		if (mIsLocked && mLastResumed != null && mLockScreen == null)
		{
			Intent intent = new Intent(App.this, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra("originalIntent", mLastResumed.getIntent());
			mLastResumed.startActivity(intent);
			mLastResumed.overridePendingTransition(0, 0);
			mLastResumed = null;
		}
	}
	
	@Override
	public void onLocked()
	{
		mIsLocked = true;
		showLockScreenIfLocked();
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(LOCKED_BROADCAST_ACTION));
	}

	@Override
	public void onUnlocked()
	{
		mIsLocked = false;
		if (mLockScreen != null)
			mLockScreen.onUnlocked();
		mLockScreen = null;
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(UNLOCKED_BROADCAST_ACTION));
	}

	public void onLockScreenResumed(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = lockScreenActivity;
	}

	public void onLockScreenPaused(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = null;
	}
	
	private Feed getFeedById(long idFeed)
	{
		ArrayList<Feed> items = socialReader.getSubscribedFeedsList();
		for (Feed feed : items)
		{
			if (feed.getDatabaseId() == idFeed)
				return feed;
		}
		return null;
	}
	
	public FeedFilterType getCurrentFeedFilterType()
	{
		return mCurrentFeedFilterType;
	}
	
	public Feed getCurrentFeed()
	{
		return mCurrentFeed;
	}
	
	public long getCurrentFeedId()
	{
		if (getCurrentFeed() != null)
			return getCurrentFeed().getDatabaseId();
		return 0;
	}

	/**
	 * Update the current feed property. Why is this needed? Because if the feed was just
	 * updated from the network a new Feed object will have been created and we want to
	 * pick up changes to the network pull date (and possibly other changes) here.
	 * @param feed
	 */
	public void updateCurrentFeed(Feed feed)
	{
		if (mCurrentFeed != null && mCurrentFeed.getDatabaseId() == feed.getDatabaseId())
			mCurrentFeed = feed;
	}

	private DataStorage dataStorage;
	
	private void initRadioZApp() {
		dataStorage = new DataStorage();
        initImageLoader();
	}
	
    private void initImageLoader() {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        // ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext()).threadPoolSize(3).threadPriority(Thread.NORM_PRIORITY - 2).memoryCacheSize(2500000) // 2.5 Mb
                .denyCacheImageMultipleSizesInMemory().discCacheFileNameGenerator(new Md5FileNameGenerator()) // Not necessary in common
                .defaultDisplayImageOptions(new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisc(true).imageScaleType(ImageScaleType.IN_SAMPLE_INT).build())
                .build();
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }
	
}
