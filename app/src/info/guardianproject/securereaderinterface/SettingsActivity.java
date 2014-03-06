package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.GroupView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import org.holoeverywhere.app.Dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import info.guardianproject.yakreader.R;
import info.guardianproject.cacheword.CacheWordHandler;

public class SettingsActivity extends FragmentActivityWithMenu
{
	private static final String TAG = "Settings";

	public static final String EXTRA_GO_TO_GROUP = "go_to_group";

	Settings mSettings;
	private ViewGroup rootView;

	private RadioButton mRbUseKillPassphraseOn;
	private RadioButton mRbUseKillPassphraseOff;

	private boolean mLanguageBeingUpdated;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setMenuIdentifier(R.menu.activity_settings);

		mSettings = App.getSettings();

		rootView = (ViewGroup) findViewById(R.id.root);

		TypedArray array = this.obtainStyledAttributes(R.style.SettingsRadioButtonSubStyle, new int[] { android.R.attr.textColor });
		if (array != null)
		{
			int color = array.getColor(0, 0x999999);
			CharacterStyle colored = new ForegroundColorSpan(color);
			CharacterStyle small = new RelativeSizeSpan(0.85f);

			applySpanToAllRadioButtons(rootView, small, colored);

			array.recycle();
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
		
		if (getIntent().hasExtra("savedInstance"))
		{
			this.onRestoreInstanceState(getIntent().getBundleExtra("savedInstance"));
			getIntent().removeExtra("savedInstance");
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

		this.hookupCheckbox(tabView, R.id.chkRequireTor, "requireTor");

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
		this.hookupRadioButtonWithArray(tabView, "passphraseTimeout", int.class, new ResourceValueMapping[] {
			new ResourceValueMapping(R.id.rbPassphraseTimeout1, 0), 
			new ResourceValueMapping(R.id.rbPassphraseTimeout2, 1),
			new ResourceValueMapping(R.id.rbPassphraseTimeout3, 2),
			new ResourceValueMapping(R.id.rbPassphraseTimeout4, 5),
			new ResourceValueMapping(R.id.rbPassphraseTimeout5, Integer.MAX_VALUE / 60000), }); //MAX_INT milliseconds given in minutes
		this.hookupRadioButton(tabView, "articleExpiration", Settings.ArticleExpiration.class, R.id.rbExpirationNever, R.id.rbExpiration1Day,
				R.id.rbExpiration1Week, R.id.rbExpiration1Month);
		this.hookupRadioButton(tabView, "syncFrequency", Settings.SyncFrequency.class, R.id.rbSyncManual, R.id.rbSyncWhenRunning, R.id.rbSyncInBackground);
		this.hookupRadioButton(tabView, "syncMode", Settings.SyncMode.class, R.id.rbSyncModeBitwise, R.id.rbSyncModeFlow);
		this.hookupRadioButton(tabView, "syncNetwork", Settings.SyncNetwork.class, R.id.rbSyncNetworkWifiAndMobile, R.id.rbSyncNetworkWifiOnly);
		this.hookupRadioButton(tabView, "readerSwipeDirection", Settings.ReaderSwipeDirection.class, R.id.rbSwipeDirectionRtl, R.id.rbSwipeDirectionLtr,
				R.id.rbSwipeDirectionAutomatic);

		this.hookupRadioButton(tabView, "uiLanguage", Settings.UiLanguage.class, R.id.rbUiLanguageEnglish, 0, R.id.rbUiLanguageTibetan, R.id.rbUiLanguageChinese);

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
		Log.v(TAG, methodNameOfGetter);

		Checkable cb = (Checkable) parentView.findViewById(resIdCheckbox);
		if (cb == null)
		{
			Log.v(TAG, "Failed to find checkbox: " + resIdCheckbox);
			return;
		}

		try
		{
			String methodNameOfSetter = "set" + String.valueOf(methodNameOfGetter.charAt(0)).toUpperCase() + methodNameOfGetter.substring(1);

			final Method getter = mSettings.getClass().getMethod(methodNameOfGetter, (Class[]) null);
			final Method setter = mSettings.getClass().getMethod(methodNameOfSetter, new Class<?>[] { boolean.class });
			if (getter == null || setter == null)
			{
				Log.v(TAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
				return;
			}

			// Set initial value
			cb.setChecked((Boolean) getter.invoke(mSettings, (Object[]) null));

			// Set listener
			((View) cb).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					boolean checked;
					try
					{
						checked = (Boolean) getter.invoke(mSettings, (Object[]) null);
						boolean newState = !checked;
						setter.invoke(mSettings, newState);
						((Checkable) v).setChecked(newState);
					}
					catch (Exception e)
					{
						Log.v(TAG, "Failed checked change listener: " + e.toString());
					}
				}
			});
		}
		catch (NoSuchMethodException e)
		{
			Log.v(TAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
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
			Log.w(TAG, "hookupRadioButton: mismatched classes!");
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
				Log.w(TAG, "Failed to find propety getter/setter for: " + methodNameOfGetter);
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
					Log.w(TAG, "Failed to find checkbox: " + resId);
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
			Log.v(TAG, "Failed to find propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalArgumentException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (IllegalAccessException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
		}
		catch (InvocationTargetException e)
		{
			Log.v(TAG, "Failed to invoke propety getter/setter for: " + methodNameOfGetter + " error: " + e.toString());
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
				Log.v(TAG, "Failed checked change listener: " + e.toString());
			}
		}
	}

	private void promptForNewPassphrase()
	{
		final Dialog alert = new Dialog(this);
		alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
		alert.setContentView(R.layout.settings_change_passphrase);

		final EditText editEnterPassphrase = (EditText) alert.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) alert.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) alert.findViewById(R.id.editConfirmNewPassphrase);

		alert.findViewById(R.id.btnOk).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0)
					return; // Both empty, ignore click

				// Check old
				if (!editEnterPassphrase.getText().toString().equals(App.getSettings().launchPassphrase())
						|| !editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()))
				{
					editEnterPassphrase.setText("");
					editNewPassphrase.setText("");
					editConfirmNewPassphrase.setText("");
					editEnterPassphrase.requestFocus();
					Toast.makeText(SettingsActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_LONG).show();
					alert.dismiss();
					promptForNewPassphrase();
					return; // Try again...
				}

				// Store
				App.getSettings().setLaunchPassphrase(editNewPassphrase.getText().toString());

				alert.dismiss();
			}
		});
		alert.findViewById(R.id.btnCancel).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				alert.cancel();
			}
		});
		alert.show();
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
		final Dialog alert = new Dialog(this);
		alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
		alert.setContentView(R.layout.settings_set_kill_passphrase);

		final EditText editNewPassphrase = (EditText) alert.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) alert.findViewById(R.id.editConfirmNewPassphrase);

		alert.findViewById(R.id.btnOk).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0)
					return; // Both empty, ignore click

				// Check old
				boolean matching = (editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()));
				boolean sameAsPassphrase = false;
				CacheWordHandler cwh = new CacheWordHandler((Context)SettingsActivity.this, null, null);
				try {
					cwh.setPassphrase(editNewPassphrase.getText().toString().toCharArray());
					sameAsPassphrase = true;
                } catch (GeneralSecurityException e) {
                    Log.e(TAG, "Cacheword initialization failed: " + e.getMessage());
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
					alert.dismiss();
					promptForKillPassphrase(setToOnIfSuccessful);
					return; // Try again...
				}

				// Store
				App.getSettings().setKillPassphrase(editNewPassphrase.getText().toString());
				if (setToOnIfSuccessful)
					updateUseKillPassphrase();
				alert.dismiss();
			}
		});
		alert.findViewById(R.id.btnCancel).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				alert.cancel();
			}
		});
		alert.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				if (setToOnIfSuccessful)
					updateUseKillPassphrase();
			}
		});
		alert.show();
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

	@Override
	protected void onUiLanguageChanged()
	{
		mLanguageBeingUpdated = true;
		super.onUiLanguageChanged();
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
		if (mLanguageBeingUpdated)
		{
			ArrayList<Integer> expandedViews = new ArrayList<Integer>();
			collectExpandedGroupViews(rootView, expandedViews);
			outState.putIntegerArrayList("expandedViews", expandedViews);
		}
	}

	private void expandSelectedGroupViews(View current, ArrayList<Integer> expandedViews)
	{
		if (current instanceof ViewGroup)
		{
			for (int child = 0; child < ((ViewGroup) current).getChildCount(); child++)
				expandSelectedGroupViews(((ViewGroup) current).getChildAt(child), expandedViews);
		}
		if (current instanceof GroupView)
		{
			if (expandedViews.contains(Integer.valueOf(current.getId())))
				((GroupView) current).setExpanded(true, false);
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		//super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState.containsKey("expandedViews"))
		{
			expandSelectedGroupViews(rootView, savedInstanceState.getIntegerArrayList("expandedViews"));
			handleGoToGroup(R.id.groupLanguage);
		}
	}
	
	
}
