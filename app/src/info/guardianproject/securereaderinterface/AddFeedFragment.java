package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapter;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapter.FeedListAdapterListener;
import info.guardianproject.securereaderinterface.uiutil.HttpTextWatcher;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import info.guardianproject.yakreader.R;
import com.tinymission.rss.Feed;

public class AddFeedFragment extends Fragment implements FeedListAdapterListener, FeedFetchedCallback
{
	private ListView mListFeeds;
	private EditText mEditManualUrl;
	private Button mBtnAddManualUrl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.add_feed_fragment, container, false);
		mListFeeds = (ListView) rootView.findViewById(R.id.listFeeds);
		mBtnAddManualUrl = (Button) rootView.findViewById(R.id.btnAddManualUrl);
		mBtnAddManualUrl.setEnabled(false);
		mBtnAddManualUrl.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				Handler threadHandler = new Handler();
				if (!imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS, new ResultReceiver(threadHandler)
				{
					@Override
					protected void onReceiveResult(int resultCode, Bundle resultData)
					{
						super.onReceiveResult(resultCode, resultData);
						doAddFeed();
					}
				}))
				{
					doAddFeed(); // Keyboard not open
				}
			}
		});
		mEditManualUrl = (EditText) rootView.findViewById(R.id.editManualUrl);
		mEditManualUrl.addTextChangedListener(new HttpTextWatcher(rootView.getContext(), mBtnAddManualUrl));
		
		Intent intent = getActivity().getIntent();
		String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null)
		{
			mEditManualUrl.setText(intent.getData().toString());
		}

		
		return rootView;
	}

	private void doAddFeed()
	{
		App.getInstance().socialReader.addFeedByURL(mEditManualUrl.getText().toString(), AddFeedFragment.this);
		updateList();
		mEditManualUrl.setText("");
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateList();
	}

	private void updateList()
	{
		ArrayList<Feed> feeds = App.getInstance().socialReader.getFeedsList();
		mListFeeds.setAdapter(new FeedListAdapter(mListFeeds.getContext(), this, feeds));
	}

	@Override
	public void addFeed(Feed feed)
	{
		App.getInstance().socialReader.subscribeFeed(feed);
		((FeedListAdapter) mListFeeds.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void removeFeed(Feed feed)
	{
		App.getInstance().socialReader.unsubscribeFeed(feed);
		((FeedListAdapter) mListFeeds.getAdapter()).notifyDataSetChanged();
	}

	@Override
	public void deleteFeed(Feed feed)
	{
		App.getInstance().socialReader.removeFeed(feed);
		updateList();
	}

	@Override
	public void feedFetched(Feed _feed)
	{
		// We have now downloaded information about manually added feed, so
		// update list!
		Log.v("AddFeedFragment", "Feed " + _feed.getFeedURL() + " loaded, update list");
		App.getInstance().socialReader.subscribeFeed(_feed);
		updateList();
	}
}