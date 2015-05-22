package info.guardianproject.securereaderinterface;


import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout;
import info.guardianproject.securereaderinterface.views.FullScreenStoryItemView;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout.ExpansionListener;
import info.guardianproject.securereaderinterface.views.StoryListView.StoryListListener;
import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.Item;

public class ItemExpandActivity extends FragmentActivityWithMenu implements StoryListListener
{
	public static final String LOGTAG = "ItemExpandActivity";
	public static final boolean LOGGING = false;

	private ExpandingFrameLayout mFullStoryView;
	private FullScreenStoryItemView mFullView;
	private ListView mFullListStories;
	private int mFullOpeningOffset;
	private boolean mInFullScreenMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		configureActionBarForFullscreen(isInFullScreenMode());
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean ret = super.onPrepareOptionsMenu(menu);
		configureActionBarForFullscreen(isInFullScreenMode());
		return ret;
	}

	@Override
	public void onStoryClicked(StoryListAdapter adapter, int index, View storyView)
	{
		if (storyView != null)
		{
			openStoryFullscreen(adapter, index, (ListView) storyView.getParent(), storyView);
		}
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		updateItemView();
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);
		updateItemView();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		super.setContentView(view, params);
		updateItemView();
	}

	private void updateItemView() 
	{
		if (mFullView != null)
		{
			View container = findViewById(R.id.storyContainer);
			if (container != null && isInFullScreenMode()) {
				if (mFullView.getParent() != null)
					((ViewGroup)mFullView.getParent()).removeView(mFullView);
				
				// Close old full screen view
				//
				configureActionBarForFullscreen(false);
				this.removeFullStoryView(false);
				this.mInFullScreenMode = false;
				scrollToCurrentItem();
				
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.MATCH_PARENT,
						FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT
						| Gravity.TOP);
				mFullView.setLayoutParams(params);
				((FrameLayout)container).addView(mFullView);
			} else if (container == null && !isInFullScreenMode()) {
				if (mFullView.getParent() != null)
					((ViewGroup)mFullView.getParent()).removeView(mFullView);
//				createFullScreenContainer(mFullView);
//				mFullStoryView.setCollapsedSize(0, 0, getTopFrame().getHeight());
			}
		}
	}
	
	private void createFullScreenContainer(FullScreenStoryItemView content) {
		RelativeLayout screenFrame = getTopFrame();
		if (screenFrame != null) {
			// Disable drag of the left side menu
			//
			mDrawerLayout.closeDrawers();

			// Remove old view (if set) from view tree
			//
			removeFullStoryView(false);

			mFullStoryView = new ExpandingFrameLayout(this, content);
			mInFullScreenMode = true;

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT,
					RelativeLayout.LayoutParams.MATCH_PARENT);
			mFullStoryView.setLayoutParams(params);
			screenFrame.addView(mFullStoryView);
			if (mToolbarShadow != null)
				mToolbarShadow.bringToFront();
			mToolbar.bringToFront();

			mFullStoryView.setExpansionListener(new ExpansionListener() {
				@Override
				public void onExpanded() {
					configureActionBarForFullscreen(true);

					// Minimize overdraw by hiding list
					mFullListStories.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onCollapsed() {
					removeFullStoryView(true);
				}
			});
		}
	}
	
	public void openStoryFullscreen(StoryListAdapter adapter, int index,
			ListView listStories, View storyView) {
		if (adapter != null) {
			
			mFullListStories = listStories;
			
			View container = findViewById(R.id.storyContainer);
			if (container != null) {
				mFullView = new FullScreenStoryItemView(this);
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT
							| Gravity.TOP);
				mFullView.setLayoutParams(params);
				((FrameLayout)container).addView(mFullView);
			} else if (getTopFrame() != null) {

				mFullView = new FullScreenStoryItemView(this);
				createFullScreenContainer(mFullView);
				setCollapsedSizeToStoryViewSize(storyView);
			}
			this.prepareFullScreenView(mFullView);
			mFullView.setStory(adapter, index,
					getStoredPositions((ViewGroup) storyView));
		}
	}

	private void setCollapsedSizeToStoryViewSize(View storyView)
	{
		// // Get screen position of the story view
		int[] locationLv = new int[2];
		mFullListStories.getLocationOnScreen(locationLv);

		int[] location = new int[2];
		if (storyView != null)
			storyView.getLocationOnScreen(location);
		else
			location = locationLv;

		// Get from top and bottom
		int[] locationTopFrame = new int[2];
		getTopFrame().getLocationOnScreen(locationTopFrame);

		int fromClip = Math.max(0, locationLv[1] - location[1]);
		int fromTop = location[1] - locationTopFrame[1];
		int fromHeight = (storyView != null) ? storyView.getHeight() : mFullListStories.getHeight();

		mFullOpeningOffset = location[1] - locationLv[1];
		mFullStoryView.setCollapsedSize(fromClip, fromTop, fromHeight);
	}
	
	private void getStoredPositionForViewWithId(ViewGroup parent, int viewId, SparseArray<Rect> positions)
	{
		View view = parent.findViewById(viewId);
		if (view != null)
		{
			Rect rect = UIHelpers.getRectRelativeToView(parent, view);
			rect.offset(view.getPaddingLeft(), view.getPaddingTop());
			rect.right -= (view.getPaddingRight() + view.getPaddingLeft());
			rect.bottom -= (view.getPaddingBottom() + view.getPaddingTop());
			positions.put(view.getId(), rect);
		}
	}
	
	private SparseArray<Rect> getStoredPositions(ViewGroup viewGroup)
	{
		if (viewGroup == null || viewGroup.getChildCount() == 0)
			return null;

		SparseArray<Rect> positions = new SparseArray<Rect>();

		getStoredPositionForViewWithId(viewGroup, R.id.layout_media, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvTitle, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvContent, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_source, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_author, positions);
		return positions;
	}

	protected void prepareFullScreenView(FullScreenStoryItemView fullView)
	{
	}

	private RelativeLayout getTopFrame()
	{
		return (RelativeLayout) findViewById(R.id.layout_root);
	}

	private void removeFullStoryView(boolean animated)
	{
		if (mFullStoryView != null)
		{
			try
			{
				if (animated)
					AnimationHelpers.fadeOut(mFullStoryView, 500, 0, true);
				else
					((ViewGroup)mFullStoryView.getParent()).removeView(mFullStoryView);
				mFullStoryView.setExpansionListener(null);
				mFullStoryView = null;
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}
			catch (Exception ex)
			{
				if (LOGGING)
					Log.e(LOGTAG, "Failed to remove full story view from view tree: " + ex.toString());
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if (isInFullScreenMode())
		{
			exitFullScreenMode();
		}
		else
		{
			// If the user is not currently in full screen story mode, allow the
			// system to handle the
			// Back button. This calls finish() on this activity and pops the
			// back stack.
			super.onBackPressed();
		}
	}

	protected void configureActionBarForFullscreen(boolean isFullscreen)
	{
		setUsePullDownActionBar(isFullscreen);
	}

	private boolean isInFullScreenMode()
	{
		return mInFullScreenMode;
	}

	@Override
	public void onResync()
	{
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (isInFullScreenMode())
			{
				exitFullScreenMode();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void exitFullScreenMode()
	{
		mInFullScreenMode = false;
		configureActionBarForFullscreen(false);
		mFullListStories.setVisibility(View.VISIBLE);
		mFullStoryView.post(new Runnable()
		{
			// Reason for post? We need to give the call above
			// (configureActionBar...) a chance
			// to do all layout changes it needs to do. This is
			// because in collapse() below we
			// take a snapshot of the screen and need to have valid
			// data.
			@Override
			public void run()
			{
				scrollToCurrentItem();
				mFullListStories.post(new Runnable()
				{
					@Override
					public void run()
					{
						if (mFullView != null)
							mFullView.onBeforeCollapse();
						if (mFullStoryView != null)
							mFullStoryView.collapse();
					}
				});
			}
		});
	}

	boolean bWaitingForCallToScroll = false;
	
	private void scrollToCurrentItem() {
		bWaitingForCallToScroll = true;
		if (mFullStoryView != null) {
			mFullListStories.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					if (!bWaitingForCallToScroll)
						mFullListStories.setOnScrollListener(null);

					Item currentItem = mFullView.getCurrentStory();
					for (int i = firstVisibleItem; i < totalItemCount
							&& i < (firstVisibleItem + visibleItemCount); i++) {
						Item item = (Item) mFullListStories
								.getItemAtPosition(i);
						if (item.getDatabaseId() == currentItem.getDatabaseId()) {
							View storyView = mFullListStories.getChildAt(i
									- mFullListStories
											.getFirstVisiblePosition());
							if (storyView != null)
								setCollapsedSizeToStoryViewSize(storyView);
							break;
						}
					}
				}
			});
		}
		mFullListStories.setSelectionFromTop(mFullView.getCurrentStoryIndex(),
				mFullOpeningOffset);
		bWaitingForCallToScroll = false;
	}
}
