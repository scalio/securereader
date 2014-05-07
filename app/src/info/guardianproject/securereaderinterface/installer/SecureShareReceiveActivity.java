package info.guardianproject.securereaderinterface.installer;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.FragmentActivityWithMenu;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.views.StoryItemPageView;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class SecureShareReceiveActivity extends FragmentActivityWithMenu
{
	public static final String LOGTAG = "SecureShareReceiveActivity";
	public static final boolean LOGGING = false;
	
	private View mLLReceive;
	private View mLLSharedStory;
	private ProgressBar mProgressReceive;
	private Item mItemReceived;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setMenuIdentifier(R.menu.activity_bluetooth_receiver);

		setContentView(R.layout.activity_secure_share_receiver);
		setActionBarTitle(getString(R.string.title_activity_secure_blue_tooth_receiver));

		mLLReceive = findViewById(R.id.llReceive);
		mLLSharedStory = findViewById(R.id.llSharedStory);
		mProgressReceive = (ProgressBar) findViewById(R.id.progressReceive);

		mLLSharedStory.findViewById(R.id.btnClose).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mItemReceived = null;
				finish();
			}
		});
		mLLSharedStory.findViewById(R.id.btnRead).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mItemReceived != null)
				{
					Intent intent = new Intent(v.getContext(), MainActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM, mItemReceived.getDatabaseId());
					intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED, mItemReceived.getFeedId());
					intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_TYPE, FeedFilterType.SHARED);
					startActivity(intent);
					finish();
				}
			}
		});

		mLLSharedStory.post(mLoadDataRunnable);
	}

	private final Runnable mLoadDataRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			java.io.File receivedContentBundleFile;
			BufferedOutputStream bos = null;
			InputStream instream = null;

			receivedContentBundleFile = ((App) SecureShareReceiveActivity.this.getApplication()).socialReader.nonVfsTempItemBundle();
			// receivedContentBundleFile = ((App)
			// this.getApplication()).socialReader.vfsTempItemBundle();

			Intent intent = getIntent();
			String action = intent.getAction();
			String type = intent.getType();

			if (LOGGING) {
				if (intent != null && type != null && action != null)
				{
					Log.v(LOGTAG, "intent: " + intent.toString());
					Log.v(LOGTAG, "action: " + action.toString());
					Log.v(LOGTAG, "type: " + type.toString());
				}
			}
			
			if (Intent.ACTION_VIEW.equals(action) && type != null && intent.getData() != null && type.equals(SocialReader.CONTENT_SHARING_MIME_TYPE))
			{
				try
				{
					// First, write to a java.io.File

					bos = new BufferedOutputStream(new java.io.FileOutputStream(receivedContentBundleFile));

					Uri streamUri = intent.getData();
					if (LOGGING) 
						Log.v(LOGTAG, "Received: " + streamUri.toString());
					instream = getContentResolver().openInputStream(streamUri);

					int count;
					byte[] buffer = new byte[256];
					while ((count = instream.read(buffer, 0, buffer.length)) != -1)
					{
						if (LOGGING) 
							Log.v(LOGTAG, "Read " + count + " bytes");
						bos.write(buffer, 0, count);
					}

					instream.close();
					bos.close();

					// Ok, done

					// / BRING IT IN

					/*
					 * ObjectInputStream objectInputStream = new
					 * ObjectInputStream(new BufferedInputStream(new
					 * FileInputStream(receivedContentBundleFile)));
					 * 
					 * try { // Deserialize it Item inItem = (Item)
					 * objectInputStream.readObject();
					 * objectInputStream.close(); Log.v(LOGTAG,
					 * "We have an Item!!!: " + inItem.getTitle());
					 * inItem.setShared(true); // Add it in.. ((App)
					 * this.getApplication()).socialReader.setItemData(inItem);
					 * } catch (ClassNotFoundException e) { // TODO
					 * Auto-generated catch block e.printStackTrace(); }
					 */

					ArrayList<File> mediaFiles = new ArrayList<File>();

					// Now unzip it
					ZipFile zipFile = new ZipFile(receivedContentBundleFile);

					int nEntries = zipFile.size();
					int iEntry = 0;

					for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();)
					{
						ZipEntry currentEntry = entries.nextElement();
						if (currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION))
						{
							iEntry++;
							InputStream inputStream = zipFile.getInputStream(currentEntry);
							ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

							try
							{
								// Deserialize it
								mItemReceived = (Item) objectInputStream.readObject();
								objectInputStream.close();
								if (LOGGING)
									Log.v(LOGTAG, "We have an Item!!!");
								mItemReceived.setShared(true);
								mItemReceived.setDatabaseId(Item.DEFAULT_DATABASE_ID);
								mItemReceived.setFeedId(Feed.DEFAULT_DATABASE_ID);
								for (MediaContent mc : mItemReceived.getMediaContent())
								{
									mc.setDatabaseId(MediaContent.DEFAULT_DATABASE_ID);
								}
								// Add it in..
								((App) SecureShareReceiveActivity.this.getApplication()).socialReader.setItemData(mItemReceived);
							}
							catch (ClassNotFoundException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							updateProgress(iEntry, nEntries);
						}
						else
						{ // Ignore for now, we'll loop through again in a
							// second
							if (LOGGING) 
								Log.v(LOGTAG, "Ignoring media element for now");
						}
					}

					if (mItemReceived != null)
					{
						int mediaContentCount = 0;
						for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();)
						{
							ZipEntry currentEntry = entries.nextElement();
							if (currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION))
							{
								// Ignore it, we should already have it

							}
							else
							{
								iEntry++;

								// It's Media Content
								// Save the files in the normal place

								InputStream inputStream = zipFile.getInputStream(currentEntry);
								BufferedOutputStream mbos = null;

								MediaContent mediaContent = mItemReceived.getMediaContent(mediaContentCount);
								mediaContentCount++;

								File savedFile = new File(((App) SecureShareReceiveActivity.this.getApplication()).socialReader.getFileSystemDir(),
										SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaContent.getDatabaseId());
								mbos = new BufferedOutputStream(new FileOutputStream(savedFile));

								byte mbuffer[] = new byte[1024];
								int mcount;
								while ((mcount = inputStream.read(mbuffer)) != -1)
								{
									mbos.write(mbuffer, 0, mcount);
								}
								inputStream.close();
								mbos.close();
								updateProgress(iEntry, nEntries);
							}
						}
					}
					else
					{
						if (LOGGING)
							Log.e(LOGTAG, "Didn't get an item");
					}

				}
				catch (FileNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally
				{
					if (instream != null)
					{
						try
						{
							instream.close();
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (bos != null)
					{
						try
						{
							bos.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}

			// Delete the incoming file
			receivedContentBundleFile.delete();

			mLLReceive.setVisibility(View.GONE);

			StoryItemPageView storyView = (StoryItemPageView) mLLSharedStory.findViewById(R.id.sharedItemView);
			storyView.populateWithItem(mItemReceived);
			storyView.loadMedia(null);
			mLLSharedStory.setVisibility(View.VISIBLE);

			// Intent mainIntent = new Intent(SecureShareReceiveActivity.this,
			// MainActivity.class);
			// if (mItemReceived != null)
			// {
			// mainIntent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM,
			// mItemReceived.getDatabaseId());
			// mainIntent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED,
			// mItemReceived.getFeedId());
			// }
			// mainIntent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_TYPE,
			// FeedFilterType.SHARED);
			// SecureShareReceiveActivity.this.startActivity(mainIntent);
		}

	};

	private void updateProgress(final long cb, final long max)
	{
		mProgressReceive.post(new Runnable()
		{
			@Override
			public void run()
			{
				mProgressReceive.setMax((int) max);
				mProgressReceive.setProgress((int) cb);
			}
		});
	}
}
