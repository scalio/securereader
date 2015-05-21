package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.PostListFragment.PostListType;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter.OnTagClickedListener;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.views.CreateAccountView;
import info.guardianproject.securereaderinterface.views.FullScreenStoryItemView;
import info.guardianproject.securereaderinterface.views.PostSignInView;
import info.guardianproject.securereaderinterface.views.CreateAccountView.OnActionListener;
import info.guardianproject.securereaderinterface.views.PostSignInView.OnAgreeListener;
import info.guardianproject.securereaderinterface.R;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class PostActivity extends ItemExpandActivity implements ActionBar.TabListener, OnActionListener, OnTagClickedListener, OnAgreeListener,
		FadeInFadeOutListener
{
	public static final String LOGTAG = "PostActivity";
	public static final boolean LOGGING = false;
	
	PostPagerAdapter mPostPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	CreateAccountView mViewCreateAccount;
	PostSignInView mViewSignIn;

	private String mCurrentSearchTag;

	private MenuItem mMenuItemTag;

	private MenuItem mMenuAddPost;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post);

		setMenuIdentifier(R.menu.activity_post);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mPostPagerAdapter = new PostPagerAdapter(getSupportFragmentManager());

		setActionBarTitle(getString(R.string.title_activity_post));

		// Set up the action bar.
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mViewSignIn = (PostSignInView) findViewById(R.id.signIn);
		mViewSignIn.setActionListener(this);

		mViewCreateAccount = (CreateAccountView) findViewById(R.id.createAccount);
		mViewCreateAccount.setActionListener(this);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setAdapter(mPostPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab.
		// We can also use ActionBar.Tab#select() to do this if we have a
		// reference to the
		// Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				if (getSupportActionBar().getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS)
					getSupportActionBar().setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mPostPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			getSupportActionBar().addTab(getSupportActionBar().newTab().setText(mPostPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);
		mMenuAddPost = menu.findItem(R.id.menu_add_post);
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_tag:
		{
			showTagSearchPopup(getSupportActionBar().getCustomView());
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class PostPagerAdapter extends FragmentPagerAdapter
	{
		PostListFragment mFragmentPublished;
		PostListFragment mFragmentOutgoing;
		PostListFragment mFragmentDrafts;

		public PostPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int i)
		{
			Bundle args = new Bundle();
			if (i == 0)
			{
				if (mFragmentPublished == null)
				{
					mFragmentPublished = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.PUBLISHED.toString());
				}
				mFragmentPublished.setStoryListListener(PostActivity.this);
				mFragmentPublished.setOnTagClickedListener(PostActivity.this);
				if (mCurrentSearchTag != null)
					mFragmentPublished.setTagFilter(mCurrentSearchTag);
				mFragmentPublished.setArguments(args);
				return mFragmentPublished;
			}
			else if (i == 1)
			{
				if (mFragmentOutgoing == null)
				{
					mFragmentOutgoing = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.OUTGOING.toString());
					mFragmentOutgoing.setStoryListListener(PostActivity.this);
					mFragmentOutgoing.setOnTagClickedListener(PostActivity.this);
					if (mCurrentSearchTag != null)
						mFragmentOutgoing.setTagFilter(mCurrentSearchTag);
					mFragmentOutgoing.setArguments(args);
				}
				return mFragmentOutgoing;
			}
			else if (i == 2)
			{
				if (mFragmentDrafts == null)
				{
					mFragmentDrafts = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.DRAFTS.toString());
					mFragmentDrafts.setStoryListListener(PostActivity.this);
					mFragmentDrafts.setOnTagClickedListener(PostActivity.this);
					if (mCurrentSearchTag != null)
						mFragmentDrafts.setTagFilter(mCurrentSearchTag);
					mFragmentDrafts.setArguments(args);
				}
				return mFragmentDrafts;
			}
			return null;
		}

		@Override
		public int getCount()
		{
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position)
			{
			case 0:
				return getString(R.string.post_title_published).toUpperCase();
			case 1:
				return getString(R.string.post_title_outgoing).toUpperCase();
			case 2:
				return getString(R.string.post_title_drafts).toUpperCase();
			}
			return null;
		}

		public void setTagFilter(String tag)
		{
			if (mFragmentPublished != null)
				mFragmentPublished.setTagFilter(tag);
			if (mFragmentOutgoing != null)
				mFragmentOutgoing.setTagFilter(tag);
			if (mFragmentDrafts != null)
				mFragmentDrafts.setTagFilter(tag);
		}

		public void updateAdapter()
		{
			if (mFragmentPublished != null)
				mFragmentPublished.updateListAdapter();
			if (mFragmentOutgoing != null)
				mFragmentOutgoing.updateListAdapter();
			if (mFragmentDrafts != null)
				mFragmentDrafts.updateListAdapter();
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft)
	{
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft)
	{
		mViewPager.setCurrentItem(tab.getPosition(), false);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft)
	{
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		showHideCreateAccount(false);

		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();

		// Anyone telling us where to go?
		if (getIntent().hasExtra("go_to_tab"))
		{
			int tab = getIntent().getIntExtra("go_to_tab", -1);
			getIntent().removeExtra("go_to_tab");
			if (tab >= 0 && tab < 3)
				mViewPager.setCurrentItem(tab, false);
		}
	}

	@Override
	public void onTagClicked(String tag)
	{
		mCurrentSearchTag = tag;
		mPostPagerAdapter.setTagFilter(tag);
		if (tag != null)
		{
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}
		else
		{
			// Clear the tag search. Show the tabs again!
			//
			int currentIndex = mViewPager.getCurrentItem();
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			getSupportActionBar().setSelectedNavigationItem(currentIndex);
		}
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = getLayoutInflater();
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.story_search_by_tag, null, false), this.mViewPager.getWidth(),
					this.mViewPager.getHeight(), true);

			ListView lvTags = (ListView) mMenuPopup.getContentView().findViewById(R.id.lvTags);

			String[] rgTags = new String[0];
			// rgTags[0] = "#one";
			// rgTags[1] = "#two";
			// rgTags[2] = "#three";
			// rgTags[3] = "#four";

			ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.story_search_by_tag_item, R.id.tvTag, rgTags);
			lvTags.setAdapter(adapter);
			lvTags.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
				{
					String tag = (String) arg0.getAdapter().getItem(position);
					onTagClicked(tag);
					mMenuPopup.dismiss();
				}
			});

			EditText editTag = (EditText) mMenuPopup.getContentView().findViewById(R.id.editTag);
			editTag.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
				{
					if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH)
					{
						onTagClicked(v.getText().toString());
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			mMenuPopup.showAsDropDown(anchorView);
			mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuPopup.dismiss();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void prepareFullScreenView(FullScreenStoryItemView fullView)
	{
		super.prepareFullScreenView(fullView);
		fullView.showFavoriteButton(false);
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		super.configureActionBarForFullscreen(fullscreen);
		if (mMenuAddPost != null)
			mMenuAddPost.setVisible(!fullscreen);
		if (mMenuItemTag != null)
			mMenuItemTag.setVisible(!fullscreen);

		if (!fullscreen)
		{
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			setDisplayHomeAsUp(false);
			toggleActionBarTabs(true);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			setDisplayHomeAsUp(true);
			toggleActionBarTabs(false);
		}
	}

	private void toggleActionBarTabs(boolean showTabs)
	{
		if (showTabs)
		{
			// Clear the tag search. Show the tabs again!
			//
			int currentIndex = mViewPager.getCurrentItem();
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			getSupportActionBar().setSelectedNavigationItem(currentIndex);
		}
		else
		{
			// Dont show tabs when we are searching for a tag
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (App.getSettings().acceptedPostPermission())
		{
			mViewCreateAccount.setVisibility(View.GONE);
			if (animate)
				AnimationHelpers.fadeOut(mViewSignIn, 500, 0, false, this);
			else
				mViewSignIn.setVisibility(View.GONE);
		}
		else if (App.getInstance().socialReporter.getAuthorName() != null)
		{
			if (animate)
			{
				AnimationHelpers.fadeOut(mViewCreateAccount, 500, 0, false, this);
				AnimationHelpers.fadeIn(mViewSignIn, 500, 0, false, this);
			}
			else
			{
				mViewSignIn.setVisibility(View.VISIBLE);
				mViewCreateAccount.setVisibility(View.GONE);
			}
		}
		else
		{
			mViewCreateAccount.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCreateIdentity(String authorName)
	{
		App.getInstance().socialReporter.createAuthorName(authorName);
		showHideCreateAccount(true);
	}

	@Override
	public void onAgreed()
	{
		App.getSettings().setAcceptedPostPermission(true);
		showHideCreateAccount(true);
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();

		// Reload the adapters after the the wipe!
		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();
	}

	@Override
	public void onFadeInStarted(View view)
	{
		if (view == mViewSignIn)
			view.setVisibility(View.VISIBLE);
	}

	@Override
	public void onFadeInEnded(View view)
	{
	}

	@Override
	public void onFadeOutStarted(View view)
	{
	}

	@Override
	public void onFadeOutEnded(View view)
	{
		view.setVisibility(View.GONE);
		// To avoid old device bug, see
		// http://stackoverflow.com/questions/4728908/android-view-with-view-gone-still-receives-ontouch-and-onclick
		view.clearAnimation();
		
		UIHelpers.hideSoftKeyboard(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();
	}
	
	
}
