package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings.UiLanguage;
import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.DropdownSpinner;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.yakreader.R;
import info.guardianproject.cacheword.CacheWordActivityHandler;
import info.guardianproject.cacheword.CacheWordSettings;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import java.security.GeneralSecurityException;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class LockScreenActivity extends Activity implements LockScreenCallbacks, OnFocusChangeListener, ICacheWordSubscriber
{
    private static final String TAG = "LockScreenActivity";
	private EditText mEnterPassphrase;
	private EditText mNewPassphrase;
	private EditText mConfirmNewPassphrase;
	private Button mBtnOpen;

	private CacheWordActivityHandler mCacheWord;
	private info.guardianproject.securereaderinterface.LockScreenActivity.SetUiLanguageReceiver mSetUiLanguageReceiver;
	private DropdownSpinner mDropdownLanguage;
	private String[] mLanguageNames;
	private UiLanguage[] mLanguageCodes;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		CacheWordSettings settings = null;
		mCacheWord = new CacheWordActivityHandler(this, settings);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			 getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
			 WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSetUiLanguageReceiver = new SetUiLanguageReceiver();
		registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION), App.EXIT_BROADCAST_PERMISSION, null);
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    if (mSetUiLanguageReceiver != null)
	    {
	    	unregisterReceiver(mSetUiLanguageReceiver);
	    	mSetUiLanguageReceiver = null;
	    }
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		App.getInstance().onLockScreenResumed(this);
		mCacheWord.onResume();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		App.getInstance().onLockScreenPaused(this);
		    mCacheWord.onPause();
	    }

	@Override
	public boolean isInternalActivityOpened()
	{
		return false;
	}

	private Bitmap takeSnapshot(View view)
	{
		if (view.getWidth() == 0 || view.getHeight() == 0)
			return null;

		view.setDrawingCacheEnabled(true);
		Bitmap bmp = view.getDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(bmp, 0, 0, view.getWidth(), view.getHeight()).copy(bmp.getConfig(), false);
		view.setDrawingCacheEnabled(false);
		return bitmap;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			// Back from lock screen means quit app. So send a kill signal to
			// any open activity and finish!
			Intent intent = new Intent(App.EXIT_BROADCAST_ACTION);
			this.sendOrderedBroadcast(intent, App.EXIT_BROADCAST_PERMISSION, new BroadcastReceiver()
			{
				@Override
				public void onReceive(Context context, Intent intent)
				{
			finish();
				}
			}, null, Activity.RESULT_OK, null, null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void createFirstTimeView()
	{
		setContentView(R.layout.activity_lock_screen);

		// Passphrase is not set, so allow the user to create one!
		//
		Button btnCreate = (Button) findViewById(R.id.btnStartCreatePassphrase);
		btnCreate.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				createCreatePassphraseView();
			}
		});

		
		mDropdownLanguage = (DropdownSpinner) findViewById(R.id.languagePopup);
		
		if (App.UI_ENABLE_LANGUAGE_CHOICE)
		{
			mLanguageNames = new String[] { 
					getString(R.string.settings_language_english),
					getString(R.string.settings_language_tibetan),
					getString(R.string.settings_language_chinese),
					getString(R.string.settings_language_ukrainian)
			};
			mLanguageCodes = new UiLanguage[] { 
					UiLanguage.English,
					UiLanguage.Tibetan,
					UiLanguage.Chinese,
					UiLanguage.Ukrainian
			};
			
			ListAdapter adapter = new LanguageListAdapter(this, mLanguageNames);
			mDropdownLanguage.setAdapter(adapter);
			selectLanguageByUiLanguage(App.getSettings().uiLanguage(), false);
			mDropdownLanguage.setOnSelectionChangedListener(new DropdownSpinner.OnSelectionChangedListener() {
			
				@Override
				public void onSelectionChanged(int position) {
					App.getSettings().setUiLanguage(mLanguageCodes[position]);
				}
			});
		}
		else
		{
			// Hide language selection!
			mDropdownLanguage.setVisibility(View.GONE);
		}
	}
	
	private boolean selectLanguageByUiLanguage(UiLanguage language, boolean sendNotification)
	{
		for (int i = 0; i < mLanguageCodes.length; i++)
		{
			if (mLanguageCodes[i].equals(language))
			{
				mDropdownLanguage.setCurrentSelection(i, sendNotification);
				return true;
			}
		}		
		return false;
	}
	
	private void createCreatePassphraseView()
	{
		setContentView(R.layout.lock_screen_create_passphrase);

		mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
		mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);

		// Passphrase is not set, so allow the user to create one!
		//
		Button btnCreate = (Button) findViewById(R.id.btnCreate);
		btnCreate.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Disallow empty fields (user just pressing "create")
				if (mNewPassphrase.getText().length() == 0 && mConfirmNewPassphrase.getText().length() == 0)
					return;

				// Compare the two text fields!
				if (!mNewPassphrase.getText().toString().equals(mConfirmNewPassphrase.getText().toString()))
				{
					Toast.makeText(LockScreenActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_SHORT).show();
					mNewPassphrase.setText("");
					mConfirmNewPassphrase.setText("");
					mNewPassphrase.requestFocus();
					return; // Try again...
				}

				// Store
				try {
                    mCacheWord.setPassphrase(mNewPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                    Log.e(TAG, "Cacheword initialization failed: " + e.getMessage());
                }
			}
		});
	}

	private void createLockView()
	{
		setContentView(R.layout.lock_screen_return);

		View root = findViewById(R.id.llRoot);
		root.setOnFocusChangeListener(this);

		root.findViewById(R.id.tvError).setVisibility(View.GONE);
		
		mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);
		mEnterPassphrase.setTypeface(Typeface.DEFAULT);
		mEnterPassphrase.setTransformationMethod(new PasswordTransformationMethod());

		mBtnOpen = (Button) findViewById(R.id.btnOpen);
		mBtnOpen.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (TextUtils.isEmpty(mEnterPassphrase.getText()))
					return;
				
				if (App.getSettings().useKillPassphrase() && mEnterPassphrase.getText().toString().equals(App.getSettings().killPassphrase()))
				{
					// Kill password entered, wipe!
					App.getInstance().wipe(SocialReader.DATA_WIPE);
					mEnterPassphrase.setText("");
					findViewById(R.id.tvError).setVisibility(View.VISIBLE);
					return; // Try again...
				}

				// Check passphrase
			    try {
                    mCacheWord.setPassphrase(mEnterPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                    Log.e(TAG, "Cacheword pass verification failed: " + e.getMessage());
                    int failedAttempts = App.getSettings().currentNumberOfPasswordAttempts();
                    failedAttempts++;
                    App.getSettings().setCurrentNumberOfPasswordAttempts(failedAttempts);
                    if (failedAttempts == App.getSettings().numberOfPasswordAttempts())
                    {
                        // Ooops, to many attempts! Wipe the data...
                        App.getInstance().wipe(SocialReader.DATA_WIPE);
                    }

                    mEnterPassphrase.setText("");
                    findViewById(R.id.tvError).setVisibility(View.VISIBLE);
                    return; // Try again...
                }
                
				App.getSettings().setCurrentNumberOfPasswordAttempts(0);
				UIHelpers.hideSoftKeyboard(LockScreenActivity.this);

			}
		});

		mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO)
				{
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

					Handler threadHandler = new Handler();

					imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(threadHandler)
					{
						@Override
						protected void onReceiveResult(int resultCode, Bundle resultData)
						{
							super.onReceiveResult(resultCode, resultData);
							mBtnOpen.performClick();
						}
					});
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		if (hasFocus && !(v instanceof EditText))
		{
			LockScreenActivity.hideSoftKeyboard(this);
		}
	}

	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	@SuppressLint("NewApi")
	protected void onUiLanguageChanged()
	{
		Intent intentThis = getIntent();
		intentThis.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intentThis);
		overridePendingTransition(0, 0);
	}

	@Override
    public void onCacheWordUninitialized() {
	    createFirstTimeView();
    }

    @Override
    public void onCacheWordLocked() {
        createLockView();
    }

    @Override
    public void onCacheWordOpened() {
        App.getSettings().setCurrentNumberOfPasswordAttempts(0);

        Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
        if (intent == null)
        	intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Bitmap snap = takeSnapshot(((ViewGroup) (getWindow().getDecorView())).getChildAt(0));
        App.getInstance().putTransitionBitmap(snap);

        startActivity(intent);
        finish();
        LockScreenActivity.this.overridePendingTransition(0, 0);
    }
      
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			LayoutInflater mParent = (LayoutInflater) super.getSystemService(name);
			LayoutInflater inflater = mParent.cloneInContext(this);
			inflater.setFactory(new LayoutFactoryWrapper(this));
			return inflater;
		}
		return super.getSystemService(name);
	}
	
	private final class SetUiLanguageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{

				@Override
				public void run()
				{
					onUiLanguageChanged();
				}
			});
		}
	}
	
	private class LanguageListAdapter extends ArrayAdapter<String>
	{
		public LanguageListAdapter(Context context, String[] languages)
		{
			super(context, R.layout.dropdown_language_item, R.id.tvLanguageName, languages);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			// User super class to create the View
			View v = super.getView(position, convertView, parent);
			TextView tv = (TextView) v.findViewById(R.id.tvLanguageName);
			tv.setText(getItem(position));
			return v;
		}
	}
	
	public void onUnlocked()
	{
      App.getSettings().setCurrentNumberOfPasswordAttempts(0);

      Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
      if (intent == null)
      	intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

      Bitmap snap = takeSnapshot(((ViewGroup) (getWindow().getDecorView())).getChildAt(0));
      App.getInstance().putTransitionBitmap(snap);

      startActivity(intent);
      finish();
      LockScreenActivity.this.overridePendingTransition(0, 0);
	}
}
