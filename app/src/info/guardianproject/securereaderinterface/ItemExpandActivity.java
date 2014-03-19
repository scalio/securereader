package info.guardianproject.securereaderinterface;


import java.util.ArrayList;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout;
import info.guardianproject.securereaderinterface.views.FullScreenStoryItemView;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout.ExpansionListener;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout.SwipeListener;
import info.guardianproject.securereaderinterface.views.StoryListView.StoryListListener;
import info.guardianproject.yakreader.R;

import com.tinymission.rss.Item;

public class ItemExpandActivity extends FragmentActivityWithMenu implements StoryListListener
{
	public static String LOGTAG = "Big Buffalo";

	private ExpandingFrameLayout mFullStoryView;
	private FullScreenStoryItemView mFullView;
	private ListView mFullListStories;
	private int mFullOpeningOffset;

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
	public void onStoryClicked(ArrayList<Item> stories, int index, View storyView)
	{
		if (storyView != null)
			openStoryFullscreen(stories, index, (ListView) storyView.getParent(), storyView);
	}

	public void openStoryFullscreen(ArrayList<Item> stories, int index, ListView listStories, View storyView)
	{
		FrameLayout screenFrame = getTopFrame();

		if (stories != null && screenFrame != null)
		{
			// Remove old view (if set) from view tree
			//
			removeFullStoryView();

			// Disable drag of the left side menu
			//
			mLeftSideMenu.setDragEnabled(false);

			mFullView = new FullScreenStoryItemView(this);
			mFullStoryView = new ExpandingFrameLayout(this, mFullView);

			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.LEFT | Gravity.TOP);
			// params.topMargin = UIHelpers.getStatusBarHeight(this);
			mFullStoryView.setLayoutParams(params);

			screenFrame.addView(mFullStoryView);

			mFullView.setStory(stories, index, getStoredPositions((ViewGroup) storyView));
			this.prepareFullScreenView(mFullView);

			mFullListStories = listStories;

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
			screenFrame.getLocationOnScreen(locationTopFrame);

			int fromClip = Math.max(0, locationLv[1] - location[1]);
			int fromTop = location[1] - locationTopFrame[1] - params.topMargin;
			int fromHeight = (storyView != null) ? storyView.getHeight() : listStories.getHeight();

			mFullOpeningOffset = location[1] - locationLv[1];

			mFullStoryView.setSwipeListener(new SwipeListener()
			{
				@Override
				public void onSwipeUp()
				{
					// Only once
					mFullStoryView.setSwipeListener(null);
					exitFullScreenMode();
				}

				@Override
				public void onSwipeDown()
				{
					mFullStoryView.post(new Runnable()
					{
						@Override
						public void run()
						{
							mFullStoryView.showActionBar(getSupportActionBar().getHeight());
						}
					});
				}
			});

			mFullStoryView.setExpansionListener(new ExpansionListener()
			{
				@Override
				public void onExpanded()
				{
					configureActionBarForFullscreen(true);

					// Minimize overdraw by hiding list
					mFullListStories.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onCollapsed()
				{
					removeFullStoryView();
					mLeftSideMenu.setDragEnabled(true);
					refreshMenu();
				}
			});

			mFullStoryView.setCollapsedSize(fromClip, fromTop, fromHeight);
		}
	}

	private void getStoredPositionForViewWithId(ViewGroup parent, int viewId, SparseArray<Rect> positions)
	{
		View view = parent.findViewById(viewId);
		if (view != null)
		{
			Rect rect = UIHelpers.getRectRelativeToView(parent, view);
			rect.offset(0, view.getPaddingTop());
			rect.bottom -= view.getPaddingBottom();
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

	private FrameLayout getTopFrame()
	{
		try
		{
			FrameLayout parent = (FrameLayout) (((FrameLayout) getWindow().getDecorView()).getChildAt(0));
			return parent;
		}
		catch (Exception ex)
		{
			Log.e(LOGTAG, "Failed to get top level frame: " + ex.toString());
		}
		return null;
	}

	private void removeFullStoryView()
	{
		if (mFullStoryView != null)
		{
			try
			{
				AnimationHelpers.fadeOut(mFullStoryView, 500, 0, true);
				mFullStoryView = null;
			}
			catch (Exception ex)
			{
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

	protected void configureActionBarForFullscreen(boolean b)
	{
	}

	private boolean isInFullScreenMode()
	{
		return (mFullStoryView != null);
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
		configureActionBarForFullscreen(false);
		// getSupportActionBar().hide();
		// getSupportActionBar().show();
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
				updateList();
				mFullListStories.post(new Runnable()
				{
					@Override
					public void run()
					{
						mFullListStories.setVisibility(View.VISIBLE);
						if (mFullView != null)
							mFullView.onBeforeCollapse();
						if (mFullStoryView != null)
							mFullStoryView.collapse();
					}
				});
			}
		});
	}

	private void scrollToCurrentItem()
	{
		// Try to find index of current item, so that we can
		// scroll the
		// list to the actual story the user was reading
		// while in full screen mode.
		Item currentItem = mFullView.getCurrentStory();
		if (currentItem != null && mFullListStories.getAdapter() != null)
		{
			ListAdapter adapter = mFullListStories.getAdapter();
			for (int iItem = 0; iItem < adapter.getCount(); iItem++)
			{
				if (adapter.getItemId(iItem) == currentItem.getDatabaseId())
				{
					mFullListStories.setSelectionFromTop(iItem, mFullOpeningOffset);
					break;
				}
			}
		}
	}

	/**
	 * Called before we collapse the full screen view. This is to update all
	 * list view items that are visible. We might have loaded media etc while in
	 * full screen mode, so we need to pick that up in the list.
	 */
	private void updateList()
	{
		if (mFullListStories == null || mFullListStories.getCount() == 0)
			return;

		if (mFullListStories.getAdapter() != null && mFullListStories.getAdapter() instanceof BaseAdapter)
		{
			((BaseAdapter) mFullListStories.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	public void onListViewUpdated(ListView newList)
	{
		// List view has been recreated (probably due to orientation change).
		// Remember the new one!
		mFullListStories = newList;
		if (isInFullScreenMode() && mFullListStories != null)
			mFullListStories.setVisibility(View.INVISIBLE);
	}

}
