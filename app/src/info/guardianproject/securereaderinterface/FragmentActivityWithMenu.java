package info.guardianproject.securereaderinterface;

import info.guardianproject.yakreader.R;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.ui.ActionProviderShare;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.uiutil.ActivitySwitcher;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.LeftSideMenu;
import info.guardianproject.securereaderinterface.views.LeftSideMenu.LeftSideMenuListener;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tinymission.rss.Feed;

public class FragmentActivityWithMenu extends LockableActivity implements LeftSideMenuListener
{
	private KillReceiver mKillReceiver;
	private SetUiLanguageReceiver mSetUiLanguageReceiver;
	private WipeReceiver mWipeReceiver;
	private int mIdMenu;
	private Menu mOptionsMenu;
	private boolean mDisplayHomeAsUp = false;

	/**
	 * The main menu that will host all content links.
	 */
	LeftSideMenu mLeftSideMenu;

	int mDeferedCommand;
	protected boolean mResumed;
	private boolean mNeedToRecreate;

	protected void setMenuIdentifier(int idMenu)
	{
		mIdMenu = idMenu;
	}

	protected boolean useLeftSideMenu()
	{
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getWindow().setBackgroundDrawable(null);

		mKillReceiver = new KillReceiver();
		registerReceiver(mKillReceiver, new IntentFilter(App.EXIT_BROADCAST_ACTION), App.EXIT_BROADCAST_PERMISSION, null);
		mSetUiLanguageReceiver = new SetUiLanguageReceiver();
		registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION), App.EXIT_BROADCAST_PERMISSION, null);
		mWipeReceiver = new WipeReceiver();
		registerReceiver(mWipeReceiver, new IntentFilter(App.WIPE_BROADCAST_ACTION), App.EXIT_BROADCAST_PERMISSION, null);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		if (useLeftSideMenu())
		{
			mLeftSideMenu = new LeftSideMenu(this, actionBar, R.layout.left_side_menu);
			mLeftSideMenu.setListener(this);
		}

		setDisplayHomeAsUp(false);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setHomeButtonEnabled(true);
	}

	public void setDisplayHomeAsUp(boolean displayHomeAsUp)
	{
		Drawable logo = null;
		if (displayHomeAsUp)
		{
			logo = this.getResources().getDrawable(R.drawable.actionbar_logo_up).mutate();

			TypedValue outValue = new TypedValue();
			getTheme().resolveAttribute(R.attr.actionBarThemeBackground, outValue, true);
			if (outValue.resourceId == R.drawable.actionbar_dark_background)
				logo = this.getResources().getDrawable(R.drawable.actionbar_logo_up_read).mutate();
		}
		else
			logo = this.getResources().getDrawable(R.drawable.actionbar_logo).mutate();
		// UIHelpers.colorizeDrawable(this, logo);
		getSupportActionBar().setLogo(logo);
		mDisplayHomeAsUp = displayHomeAsUp;
		
		if (mLeftSideMenu != null)
			mLeftSideMenu.setDisplayMenuIndicator(!displayHomeAsUp);
	}

	public void setActionBarTitle(String title)
	{
		if (title != null)
		{
			if (getSupportActionBar().getCustomView() == null)
			{
				View titleView = getLayoutInflater().inflate(R.layout.actionbar_custom_title, null);
				getSupportActionBar().setCustomView(titleView,
						new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			}
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			TextView tvTitle = (TextView) getSupportActionBar().getCustomView().findViewById(R.id.tvTitle);
			tvTitle.setText(title);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(mMenuCommandReceiver, new IntentFilter("MenuCommand"));
		if (mLeftSideMenu != null)
			mLeftSideMenu.checkMenuCreated();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMenuCommandReceiver);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mResumed = false;
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume()
	{
		super.onResume();
		mResumed = true;
		if (mNeedToRecreate)
		{
			onUiLanguageChanged();
			return;
		}
		
		if (Build.VERSION.SDK_INT >= 11)
			invalidateOptionsMenu();
	}

	private final class KillReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			finish();
		}
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

	private final class WipeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{
				@Override
				public void run()
				{
					onWipe();
				}
			});
		}
	}

	/**
	 * Override this to react to a wipe!
	 */
	protected void onWipe()
	{

	}

	@SuppressLint("NewApi")
	protected void onUiLanguageChanged()
	{
		if (!mResumed)
		{
			mNeedToRecreate = true;
		}
		else
		{
			mNeedToRecreate = false;
			Intent intentThis = getIntent();
			
			Bundle b = new Bundle();
			onSaveInstanceState(b);
			intentThis.putExtra("savedInstance", b);
			intentThis.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intentThis);
			overridePendingTransition(0, 0);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(mKillReceiver);
		unregisterReceiver(mSetUiLanguageReceiver);
		unregisterReceiver(mWipeReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (mIdMenu == 0)
			return false;
		mOptionsMenu = menu;
		super.onCreateOptionsMenu(menu);

		getSupportMenuInflater().inflate(mIdMenu, menu);
		colorizeMenuItems();
		return true;
	}

	private void colorizeMenuItems()
	{
		if (mOptionsMenu == null)
			return;
		for (int i = 0; i < mOptionsMenu.size(); i++)
		{
			MenuItem item = mOptionsMenu.getItem(i);
			Drawable d = item.getIcon();
			if (d != null)
			{
				d = d.getConstantState().newDrawable(getResources()).mutate();
				UIHelpers.colorizeDrawable(this, d);
				item.setIcon(d);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (mDisplayHomeAsUp)
			{
				Intent intent = new Intent(this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				this.startActivity(intent);
				this.overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
				return true;
			}
			if (mLeftSideMenu != null)
				mLeftSideMenu.toggle();
			return true;

		case R.id.menu_panic:
		{
			Intent intent = new Intent(this, PanicActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		}

		case R.id.menu_add_post:
		{
			Intent intent = new Intent(this, AddPostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			return true;
		}

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	@SuppressLint("NewApi")
	public void onMenuCreated(final View parent, final View menuRoot, final View menu)
	{
		// News
		//
		menu.findViewById(R.id.llNews).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_news_list);
			}
		});

		menu.findViewById(R.id.btnAddFeed).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_feed_add);
			}
		});

		// Reporter
		//
		if (App.UI_ENABLE_REPORTER)
		{
			menu.findViewById(R.id.llReporter).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuCommandReceiver.handleCommand(R.integer.command_posts_list);
				}
			});

			menu.findViewById(R.id.btnAddPost).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuCommandReceiver.handleCommand(R.integer.command_post_add);
				}
			});
		}
		else
		{
			// Disable reporter functionality
			menu.findViewById(R.id.llReporter).setVisibility(View.GONE);
			menu.findViewById(R.id.llReporterSeparator).setVisibility(View.GONE);
			menu.findViewById(R.id.btnAddPost).setVisibility(View.GONE);
			menu.findViewById(R.id.btnAddPostSeparator).setVisibility(View.GONE);
		}

		// Chat
		//
		if (App.UI_ENABLE_CHAT)
		{
			menu.findViewById(R.id.llChat).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuCommandReceiver.handleCommand(R.integer.command_chat);
				}
			});
		}
		else
		{
			// Disable chat functionality
			menu.findViewById(R.id.llChat).setVisibility(View.GONE);
			menu.findViewById(R.id.llChatSeparator).setVisibility(View.GONE);
		}

		// Help
		//
		menu.findViewById(R.id.llHelp).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_help);
			}
		});

		// Connect
		menu.findViewById(R.id.btnTorStatus).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_toggle_online, true);
				onBeforeShow(); // update menu
			}
		});

		// Receive share
		menu.findViewById(R.id.llReceiveShare).setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_receiveshare);
			}
		});

		menu.findViewById(R.id.llShareApp).setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_shareapp);
			}
		});

		// Preferences
		//
		View btnSettings = menu.findViewById(R.id.llSettings);
		btnSettings.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mMenuCommandReceiver.handleCommand(R.integer.command_settings);
			}
		});

		performRotateTransition(parent, menuRoot);
	}

	@SuppressLint("NewApi")
	protected void performRotateTransition(final View parent, final View content)
	{
		// Get bitmap from intent!!!
		Bitmap bmp = App.getInstance().getTransitionBitmap();
		if (bmp != null)
		{
			final ImageView snap = new ImageView(this);
			snap.setImageBitmap(bmp);
			((ViewGroup) parent).addView(snap);

			if (Build.VERSION.SDK_INT >= 11)
			{
				content.setLayerType(View.LAYER_TYPE_HARDWARE, null);
				snap.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			}
			else
			{
				content.setDrawingCacheEnabled(true);
				snap.setDrawingCacheEnabled(true);
			}

			content.setVisibility(View.INVISIBLE);

			// Animate!!!
			ActivitySwitcher.animationOut(snap, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener()
			{
				@Override
				public void onAnimationFinished()
				{
					ActivitySwitcher.animationIn(content, getWindowManager(), new ActivitySwitcher.AnimationFinishedListener()
					{
						@Override
						public void onAnimationFinished()
						{
							content.post(new Runnable()
							{
								@Override
								@SuppressLint("NewApi")
								public void run()
								{
									((ViewGroup) parent).removeView(snap);
									App.getInstance().putTransitionBitmap(null);

									if (Build.VERSION.SDK_INT >= 11)
									{
										content.setLayerType(View.LAYER_TYPE_NONE, null);
									}
									else
									{
										content.setDrawingCacheEnabled(false);
									}

									content.setVisibility(View.VISIBLE);
									content.clearAnimation();
									onAfterResumeAnimation();
								}
							});
						}
					});
				}
			});
		}
		else
		{
			onAfterResumeAnimation();
		}
	}

	protected void onAfterResumeAnimation()
	{
		// Override this to start doing stuff after the animation is complete
	}

	private class MenuViewHolder
	{
		public TextView tvTorStatus;
		public ImageView ivTorStatus;
		public TextView tvNumFeeds;
		public TextView tvNumStories;
		public TextView tvNumChats;
	}

	private MenuViewHolder mMenuViewHolder;

	@Override
	public void onBeforeShow()
	{
		View menuView = mLeftSideMenu.getMenuView();
		if (menuView != null)
		{
			UpdateMenuTask task = new UpdateMenuTask();
			task.execute((Void) null);
		}
	}

	class UpdateMenuTask extends ThreadedTask<Void, Void, Void>
	{
		private boolean isUsingTor;
		private boolean isOnline;
		// private boolean isSignedIn;
		private int numFeeds;
		private int numPosts;

		@Override
		protected Void doInBackground(Void... values)
		{
			if (mMenuViewHolder == null)
			{
				mMenuViewHolder = new MenuViewHolder();
				View menuView = mLeftSideMenu.getMenuView();
				mMenuViewHolder.tvTorStatus = (TextView) menuView.findViewById(R.id.tvTorStatus);
				mMenuViewHolder.ivTorStatus = (ImageView) menuView.findViewById(R.id.btnTorStatus);
				mMenuViewHolder.tvNumFeeds = (TextView) menuView.findViewById(R.id.tvNumFeeds);
				mMenuViewHolder.tvNumStories = (TextView) menuView.findViewById(R.id.tvNumStories);
				mMenuViewHolder.tvNumChats = (TextView) menuView.findViewById(R.id.tvNumChats);
			}

			isUsingTor = App.getInstance().socialReader.useTor();
			isOnline = App.getInstance().socialReader.isTorOnline();
			// isSignedIn = App.getInstance().socialReporter.isSignedIn();

			numFeeds = App.getInstance().socialReader.getSubscribedFeedsList().size();
			numPosts = 0;
			// if (isSignedIn)
			numPosts = App.getInstance().socialReporter.getPosts().size();
			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			// Update TOR connection status
			//
			if (isOnline)
			{
				mMenuViewHolder.tvTorStatus.setText(R.string.menu_tor_connected);
				mMenuViewHolder.ivTorStatus.setImageResource(R.drawable.ic_menu_tor_on);
			}
			else
			{
				mMenuViewHolder.tvTorStatus.setText(R.string.menu_tor_not_connected);
				mMenuViewHolder.ivTorStatus.setImageResource(R.drawable.ic_menu_tor_off);
			}
			if (isUsingTor)
			{
				mMenuViewHolder.tvTorStatus.setVisibility(View.VISIBLE);
				mMenuViewHolder.ivTorStatus.setVisibility(View.VISIBLE);
			}
			else
			{
				mMenuViewHolder.tvTorStatus.setVisibility(View.INVISIBLE);
				mMenuViewHolder.ivTorStatus.setVisibility(View.INVISIBLE);
			}
			mMenuViewHolder.tvNumFeeds.setText(getString(R.string.menu_num_feeds, numFeeds));
			mMenuViewHolder.tvNumStories.setText(getString(R.string.menu_num_stories, numPosts));
			mMenuViewHolder.tvNumChats.setText(getString(R.string.menu_num_chats, 0));
		}
	}

	private class MenuBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			int commandId = intent.getIntExtra("command", 0);
			handleCommand(commandId);
		}

		public void handleCommand(int commandId)
		{
			handleCommand(commandId, false);
		}

		public void handleCommand(int commandId, boolean forceNow)
		{
			mDeferedCommand = commandId;
			if (mLeftSideMenu != null && mLeftSideMenu.isOpen() && !forceNow)
			{
				mLeftSideMenu.hide();
			}
			else
			{
				doHandleCommand();
			}
		}
	};

	private final MenuBroadcastReceiver mMenuCommandReceiver = new MenuBroadcastReceiver();

	private void doHandleCommand()
	{
		if (mDeferedCommand != 0)
		{
			int command = mDeferedCommand;
			mDeferedCommand = 0;

			onCommand(command, null);
		}
	}

	@Override
	public void onHide()
	{
		doHandleCommand(); // Handle command, if any
	}

	protected boolean onCommand(int command, Bundle commandParameters)
	{
		UICallbacks.handleCommand(this, command, null);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if (this.mLeftSideMenu != null)
			mLeftSideMenu.onConfigurationChanged();
	}

	@Override
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, data);
		if (requestCode == UICallbacks.RequestCode.CREATE_CHAT_ACCOUNT.Value)
		{
			if (resultCode == RESULT_OK)
			{
				App.getSettings().setChatUsernamePasswordSet();
				// Then redirect somewhere?
			}
		}
	}
	
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			LayoutInflater mParent = (LayoutInflater) super.getSystemService(name);
			LayoutInflater inflater = mParent.cloneInContext(mParent.getContext());
			inflater.setFactory(new LayoutFactoryWrapper(this));
			return inflater;
		}
		return super.getSystemService(name);
	}
}
