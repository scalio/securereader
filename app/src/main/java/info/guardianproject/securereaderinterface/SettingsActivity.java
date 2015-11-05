package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.GroupView;
import info.guardianproject.securereaderinterface.widgets.InitialScrollScrollView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;

public class SettingsActivity extends FragmentActivityWithMenu implements ICacheWordSubscriber
{
	private static final boolean LOGGING = false;
	private static final String LOGTAG = "Settings";

	public static final String EXTRA_GO_TO_GROUP = "go_to_group";

	Settings mSettings;
	private InitialScrollScrollView rootView;

	private RadioButton mRbUseKillPassphraseOn;
	private RadioButton mRbUseKillPassphraseOff;

	private boolean mIsBeingRecreated;
	private String mLastChangedSetting;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setMenuIdentifier(R.menu.activity_settings);
		setDisplayHomeAsUp(true);

		mSettings = App.getSettings();

		rootView = (InitialScrollScrollView) findViewById(R.id.root);

		TypedArray array = this.obtainStyledAttributes(R.style.SettingsRadioButtonSubStyle, new int[] { android.R.attr.textColor });
		if (array != null)
		{
			int color = array.getColor(0, 0x999999);
			CharacterStyle colored = new ForegroundColorSpan(color);
			CharacterStyle small = new RelativeSizeSpan(0.85f);

			applySpanToAllRadioButtons(rootView, small, colored);

			array.recycle();
		}

		if (getIntent().hasExtra("savedInstance"))
		{
			Bundle savedInstance = getIntent().getBundleExtra("savedInstance");
			getIntent().removeExtra("savedInstance");
			if (savedInstance != null)
			{
				if (savedInstance.containsKey("expandedViews"))
					expandSelectedGroupViews(rootView, savedInstance.getIntegerArrayList("expandedViews"));

				int scrollToViewId = savedInstance.getInt("scrollToViewId", View.NO_ID);
				int scrollToViewOffset = savedInstance.getInt("scrollToViewOffset", 0);
				rootView.setInitialPosition(scrollToViewId, scrollToViewOffset);
			}
		}
	}

	private void applySpanToAllRadioButtons(ViewGroup parent, CharacterStyle... cs)
	{
		for (int i = 0; i < parent.getChildCount(); i++)
		{
			View view = parent.getChildAt(i);
			if (view instanceof RadioButton)
			{
				RadioButton rb = (RadioButton) view;
				rb.setText(setSpanOnMultilineText(rb.getText(), cs));
			}
			else if (view instanceof ViewGroup)
			{
				applySpanToAllRadioButtons((ViewGroup) view, cs);
			}
		}
	}

	private CharSequence setSpanOnMultilineText(CharSequence text, CharacterStyle... cs)
	{
		int idxBreak = text.toString().indexOf('\n');
		if (idxBreak > -1)
		{
			StringBuilder sb = new StringBuilder(text);
			sb.insert(idxBreak, "##");
			sb.append("##");
			return UIHelpers.setSpanBetweenTokens(sb.toString(), "##", cs);
		}
		return text;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		populateProfileTab();
		if (getIntent().hasExtra(EXTRA_GO_TO_GROUP))
		{
			handleGoToGroup(getIntent().getIntExtra(EXTRA_GO_TO_GROUP, 0));
			getIntent().removeExtra(EXTRA_GO_TO_GROUP);
		}
	}

	private void handleGoToGroup(int goToSection)
	{
		if (goToSection != 0)
		{
			final View view = rootView.findViewById(goToSection);
			if (view != null)
			{
				if (view instanceof GroupView)
				{
					((GroupView) view).setExpanded(true, false);
				}

				rootView.post(new Runnable()
				{
					@Override
					public void run()
					{
						int top = view.getTop();
						rootView.scrollTo(0, top - 5);
					}
				});
			}
		}
	}

	private void populateProfileTab()
	{
		View tabView = rootView;

		this.hookupRadioButton(tabView, "proxyType", Settings.ProxyType.class, R.id.rbProxyNone, R.id.rbProxyTor, R.id.rbProxyPsiphon);

		mRbUseKillPassphraseOn = (RadioButton) tabView.findViewById(R.id.rbKillPassphraseOn);
		mRbUseKillPassphraseOff = (RadioButton) tabView.findViewById(R.id.rbKillPassphraseOff);
		if (mSettings.useKillPassphrase())
			mRbUseKillPassphraseOn.setChecked(true);
		else
			mRbUseKillPassphraseOff.setChecked(true);
		mRbUseKillPassphraseOn.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked != mSettings.useKillPassphrase())
				{
					if (isChecked && TextUtils.isEmpty(mSettings.killPassphrase()))
					{
						promptForKillPassphrase(true);
					}
					else
					{
						mSettings.setUseKillPassphrase(isChecked);
					}
				}
			}
		});

		this.hookupBinaryRadioButton(tabView, R.id.rbWipeApp, R.id.rbWipeContent, "wipeApp");
		// Immediate, 1 minute, 1 hour, 1 day, 1 week
		this.hookupRadioButtonWithArray(tabView, "passphraseTimeout", int.class, new ResourceValueMapping[] {
				new ResourceValueMapping(R.id.rbPassphraseTimeout1, 0),
				new ResourceValueMapping(R.id.rbPassphraseTimeout2, 1),
				new ResourceValueMapping(R.id.rbPassphraseTimeout3, 60),
				new ResourceValueMapping(R.id.rbPassphraseTimeout4, 1440),
				new ResourceValueMapping(R.id.rbPassphraseTimeout5, 10080),});//Integer.MAX_VALUE / 60000), }); //MAX_INT milliseconds given in minutes
		this.hookupRadioButton(tabView, "articleExpiration", Settings.ArticleExpiration.class, 0, R.id.rbExpiration1Day,
				R.id.rbExpiration1Week, R.id.rbExpiration1Month);
		this.hookupRadioButton(tabView, "syncFrequency", Settings.SyncFrequency.class, R.id.rbSyncManual, R.id.rbSyncWhenRunning, R.id.rbSyncInBackground);
		this.hookupRadioButton(tabView, "syncMode", Settings.SyncMode.class, R.id.rbSyncModeBitwise, R.id.rbSyncModeFlow);
		this.hookupRadioButton(tabView, "syncNetwork", Settings.SyncNetwork.class, R.id.rbSyncNetworkWifiAndMobile, R.id.rbSyncNetworkWifiOnly);
		this.hookupRadioButton(tabView, "readerSwipeDirection", Settings.ReaderSwipeDirection.class, R.id.rbSwipeDirectionRtl, R.id.rbSwipeDirectionLtr,
				R.id.rbSwipeDirectionAutomatic);

		this.hookupRadioButtonWithArray(tabView, "uiLanguage", Settings.UiLanguage.class, new ResourceValueMapping[] {
				new ResourceValueMapping(R.id.rbUiLanguageEnglish, Settings.UiLanguage.English),
				new ResourceValueMapping(R.id.rbUiLanguageTibetan, Settings.UiLanguage.Tibetan),
				new ResourceValueMapping(R.id.rbUiLanguageChinese, Settings.UiLanguage.Chinese),
				new ResourceValueMapping(R.id.rbUiLanguageUkrainian, Settings.UiLanguage.Ukrainian),
				new ResourceValueMapping(R.id.rbUiLanguageRussian, Settings.UiLanguage.Russian),
				new ResourceValueMapping(R.id.rbUiLanguageJapanese, Settings.UiLanguage.Japanese),
				new ResourceValueMapping(R.id.rbUiLanguageNorwegian, Settings.UiLanguage.Norwegian),
				new ResourceValueMapping(R.id.rbUiLanguageSpanish, Settings.UiLanguage.Spanish),
				new ResourceValueMapping(R.id.rbUiLanguageTurkish, Settings.UiLanguage.Turkish),
				new ResourceValueMapping(R.id.rbUiLanguageFarsi, Settings.UiLanguage.Farsi)
		});

		this.hookupRadioButtonWithArray(tabView, "numberOfPasswordAttempts", int.class, new ResourceValueMapping[] {
				new ResourceValueMapping(R.id.rbNumberOfPasswordAttempts1, 2), new ResourceValueMapping(R.id.rbNumberOfPasswordAttempts2, 3),
				new ResourceValueMapping(R.id.rbNumberOfPasswordAttempts3, 0), });
		// this.hookupBinaryRadioButton(tabView, R.id.rbKillPassphraseOn,
		// R.id.rbKillPassphraseOff, "useKillPassphrase");

		tabView.findViewById(R.id.btnSetLaunchPassphrase).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				promptForNewPassphrase();
			}
		});

		tabView.findViewById(R.id.btnSetKillPassphrase).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				promptForKillPassphrase(false);
			}
		});

		this.hookupCheckbox(tabView, R.id.chkEnableScreenshots, "enableScreenshots");

//		// On older devices there is no reliable way to turn off screen captures!
//		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
//		{
//			View chkEnableScreenshots = tabView.findViewById(R.id.chkEnableScreenshots);
//			if (chkEnableScreenshots != null)
//			{
//				((Checkable)chkEnableScreenshots).setChecked(true);
//				chkEnableScreenshots.setEnabled(false);
//			}
//		}
	}

	private class ResourceValueMapping
	{
		private final int mResId;
		private final Object mValue;

		public ResourceValueMapping(int resId, Object value)
		{
			mResId = resId;
			mValue = value;
		}

		public int getResId()
		{
			return mResId;
		}

		public Object getValue()
		{
			return mValue;
		}
	}

	private void hookupCheckbox(View parentView, int resIdCheckbox, String methodNameOfGetter)
	{
		if (LOGGING)
			Log.v(LOGTAG, methodNameOfGetter);

		CheckBox cb = (CheckBox) parentView.findViewById(resIdCheckbox);
		if (cb == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find checkbox: " + resIdCheckbox);
			return;
		}

		try
		{
			String methodNameOfSetter = "set" + String.valueOf(methodNameOfGetter.charAt(0)).toUpperCase() + methodNameOfGetter.substring(1);

			final Method getter = mSettings.getClass().getMethod(methodNameOfGetter, (Class[]) null);
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[] { boolean.class });
			if (getter == null || setter == null)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
				return;
			}

			// Set initial value
			cb.setChecked((Boolean) getter.invoke(mSettings, (Object[]) null));

			// Set listener
			cb.setOnCheckedChangeListener(new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
											 boolean isChecked) {
					try
					{
						setter.invoke(mSettings, isChecked);
					}
					catch (Exception e)
					{
						if (LOGGING)
							Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
					}

				}
			});
		}
		catch (NoSuchMethodException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
	}

	private void hookupBinaryRadioButton(View parentView, int resIdRadioTrue, int resIdRadioFalse, String methodNameOfGetter)
	{
		hookupRadioButtonWithArray(parentView, methodNameOfGetter, boolean.class, new ResourceValueMapping[] { new ResourceValueMapping(resIdRadioTrue, true),
				new ResourceValueMapping(resIdRadioFalse, false) });
	}

	private void hookupRadioButton(View parentView, String methodNameOfGetter, Class<?> enumClass, int... resIds)
	{
		Object[] constants = enumClass.getEnumConstants();
		if (constants.length != resIds.length)
		{
			if (LOGGING)
				Log.w(LOGTAG, "hookupRadioButton: mismatched classes!");
			return;
		}

		ArrayList<ResourceValueMapping> mappings = new ArrayList<ResourceValueMapping>();

		int idx = 0;
		for (int resId : resIds)
		{
			mappings.add(new ResourceValueMapping(Integer.valueOf(resId), constants[idx++]));
		}

		hookupRadioButtonWithArray(parentView, methodNameOfGetter, enumClass, mappings.toArray(new ResourceValueMapping[] {}));
	}

	private void hookupRadioButtonWithArray(View parentView, String methodNameOfGetter, Class<?> valueType, ResourceValueMapping[] values)
	{
		try
		{
			String methodNameOfSetter = "set" + String.valueOf(methodNameOfGetter.charAt(0)).toUpperCase() + methodNameOfGetter.substring(1);

			final Method getter = mSettings.getClass().getMethod(methodNameOfGetter, (Class[]) null);
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[] { valueType });
			if (getter == null || setter == null)
			{
				if (LOGGING)
					Log.w(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
				return;
			}

			RadioButtonChangeListener listener = new RadioButtonChangeListener(mSettings, getter, setter);

			Object currentValueInSettings = getter.invoke(mSettings, (Object[]) null);

			for (ResourceValueMapping value : values)
			{
				int resId = value.getResId();
				if (resId == 0)
					continue; // Ignore this value, cant be set in the ui

				RadioButton rb = (RadioButton) parentView.findViewById(resId);
				if (rb == null)
				{
					if (LOGGING)
						Log.w(LOGTAG, "Failed to find checkbox: " + resId);
					return;
				}
				if (currentValueInSettings.equals(value.getValue()))
					rb.setChecked(true);
				rb.setTag(value.getValue());
				rb.setOnCheckedChangeListener(listener);
			}
		}
		catch (NoSuchMethodException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
	}

	private class RadioButtonChangeListener implements RadioButton.OnCheckedChangeListener
	{
		private final Settings mSettings;
		private final Method mGetter;
		private final Method mSetter;

		public RadioButtonChangeListener(Settings settings, Method getter, Method setter)
		{
			mSettings = settings;
			mGetter = getter;
			mSetter = setter;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			try
			{
				if (isChecked)
				{
					Object currentValueInSettings = mGetter.invoke(mSettings, (Object[]) null);
					Object valueOfThisRB = ((RadioButton) buttonView).getTag();
					if (!currentValueInSettings.equals(valueOfThisRB))
						mSetter.invoke(mSettings, valueOfThisRB);
				}
			}
			catch (Exception e)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Failed checked change listener: " + e.toString());
			}
		}
	}

	private void promptForNewPassphrase()
	{
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_change_passphrase, null, false);

		final EditText editEnterPassphrase = (EditText) contentView.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_change_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0)
						{
							dialog.dismiss();
							promptForNewPassphrase();
							return; // Try again...
						}

						if (!(editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()))) {
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_not_matching), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase();
							return; // Try again...
						}

						CacheWordHandler cwh = new CacheWordHandler((Context)SettingsActivity.this);

						char[] passwd = editEnterPassphrase.getText().toString().toCharArray();
						PassphraseSecrets secrets;
						try {
							secrets = PassphraseSecrets.fetchSecrets(SettingsActivity.this, passwd);
							cwh.changePassphrase(secrets, editNewPassphrase.getText().toString().toCharArray());
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_changed), Toast.LENGTH_LONG).show();

						} catch (Exception e) {
							// Invalid password or the secret key has been
							if (LOGGING)
								Log.e(LOGTAG, e.getMessage());

							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_incorrect), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase();
							return; // Try again...
						}

						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}

	/**
	 * Lets the user input a kill passphrase
	 *
	 * @param setToOnIfSuccessful
	 *            If true, update the settings if we manage to set the
	 *            passphrase.
	 */
	private void promptForKillPassphrase(final boolean setToOnIfSuccessful)
	{
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_set_kill_passphrase, null, false);

		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_set_kill_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0)
						{
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Check old
						boolean matching = (editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()));
						boolean sameAsPassphrase = false;
						CacheWordHandler cwh = new CacheWordHandler((Context)SettingsActivity.this);
						try {
							cwh.setPassphrase(editNewPassphrase.getText().toString().toCharArray());
							sameAsPassphrase = true;
						} catch (GeneralSecurityException e) {
							if (LOGGING)
								Log.e(LOGTAG, "Cacheword initialization failed: " + e.getMessage());
						}
						if (!matching || sameAsPassphrase)
						{
							editNewPassphrase.setText("");
							editConfirmNewPassphrase.setText("");
							editNewPassphrase.requestFocus();
							if (!matching)
								Toast.makeText(SettingsActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_LONG).show();
							else
								Toast.makeText(SettingsActivity.this, getString(R.string.settings_security_kill_passphrase_same_as_login), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Store
						App.getSettings().setKillPassphrase(editNewPassphrase.getText().toString());
						if (setToOnIfSuccessful)
							updateUseKillPassphrase();
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (setToOnIfSuccessful)
					updateUseKillPassphrase();
			}
		});
		dialog.show();
	}

	private void updateUseKillPassphrase()
	{
		if (!TextUtils.isEmpty(mSettings.killPassphrase()))
		{
			mRbUseKillPassphraseOn.setChecked(true);
			mSettings.setUseKillPassphrase(true);
		}
		else
		{
			mRbUseKillPassphraseOff.setChecked(true);
			mSettings.setUseKillPassphrase(false);
		}
	}

	private void collectExpandedGroupViews(View current, ArrayList<Integer> expandedViews)
	{
		if (current instanceof ViewGroup)
		{
			for (int child = 0; child < ((ViewGroup) current).getChildCount(); child++)
				collectExpandedGroupViews(((ViewGroup) current).getChildAt(child), expandedViews);
		}
		if (current instanceof GroupView)
		{
			if (((GroupView) current).getExpanded())
				expandedViews.add(Integer.valueOf(current.getId()));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Dont call base, see http://stackoverflow.com/questions/4504024/android-localization-problem-not-all-items-in-the-layout-update-properly-when-s
		//super.onSaveInstanceState(outState);
		if (mIsBeingRecreated)
		{
			ArrayList<Integer> expandedViews = new ArrayList<Integer>();
			collectExpandedGroupViews(rootView, expandedViews);
			outState.putIntegerArrayList("expandedViews", expandedViews);

			if (mLastChangedSetting != null)
			{
				if (SettingsUI.KEY_ENABLE_SCREENSHOTS.equals(mLastChangedSetting))
				{
					outState.putInt("scrollToViewId", R.id.chkEnableScreenshots);
					outState.putInt("scrollToViewOffset", UIHelpers.getRelativeTop(findViewById(R.id.chkEnableScreenshots)) - UIHelpers.getRelativeTop(rootView) - rootView.getScrollY());
				}
				else if (SettingsUI.KEY_UI_LANGUAGE.equals(mLastChangedSetting))
				{
					outState.putInt("scrollToViewId", R.id.groupLanguage);
					outState.putInt("scrollToViewOffset", UIHelpers.getRelativeTop(findViewById(R.id.groupLanguage)) - UIHelpers.getRelativeTop(rootView) - rootView.getScrollY());
				}
			}
		}
	}

	private void expandSelectedGroupViews(View current, ArrayList<Integer> expandedViews)
	{
		for (int id : expandedViews)
		{
			View view = current.findViewById(id);
			if (view != null && view instanceof GroupView)
			{
				((GroupView) view).setExpanded(true, false);
			}
		}
	}

	@Override
	public void recreateNowOrOnResume()
	{
		mIsBeingRecreated = true;
		super.recreateNowOrOnResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		mLastChangedSetting = key;
		super.onSharedPreferenceChanged(sharedPreferences, key);
		if (key == Settings.KEY_PROXY_TYPE)
		{
			mSettings.setRequireProxy(mSettings.proxyType() != Settings.ProxyType.None);
			if (mSettings.requireProxy())
				App.getInstance().socialReader.connectProxy(this);
		}
		if (key.equals(Settings.KEY_PROXY_TYPE) || key.equals(Settings.KEY_SYNC_MODE))
		{
			updateLeftSideMenu();
		}
	}

	public void screenshotsSectionClicked(View v) {
		CheckBox check = (CheckBox) v.findViewById(R.id.chkEnableScreenshots);
		check.toggle();
	}

	@Override
	public void onCacheWordLocked() {
	}

	@Override
	public void onCacheWordOpened() {
	}

	@Override
	public void onCacheWordUninitialized() {
	}
}
