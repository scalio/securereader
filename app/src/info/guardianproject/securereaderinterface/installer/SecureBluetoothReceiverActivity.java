package info.guardianproject.securereaderinterface.installer;

import info.guardianproject.courier.R;
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class SecureBluetoothReceiverActivity extends FragmentActivityWithMenu implements OnClickListener, SecureBluetooth.SecureBluetoothEventListener
{
	public static final String LOGTAG = "SecureBluetoothReceiverActivity";

	private enum UIState
	{
		Listening, Receiving, ReceivedOk
	};

	Button receiveButton;
	TextView receiveText;

	SecureBluetooth sb;
	java.io.File receivedContentBundleFile;
	BufferedOutputStream bos;

	boolean readyToReceive = false;

	private View mLLWait;
	private View mLLReceive;
	private View mLLSharedStory;

	UIState mCurrentState = UIState.Listening;
	private ProgressBar mProgressReceive;
	private Item mItemReceived;

	private long bytesReceived = 0;
	private boolean mReceiverRegistered;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setMenuIdentifier(R.menu.activity_bluetooth_receiver);

		setContentView(R.layout.activity_secure_blue_tooth_receiver);
		setActionBarTitle(getString(R.string.title_activity_secure_blue_tooth_receiver));

		sb = new SecureBluetooth();
		sb.setSecureBluetoothEventListener(this);
		sb.enableBluetooth(this);

		mLLWait = findViewById(R.id.llWait);
		mLLReceive = findViewById(R.id.llReceive);
		mLLSharedStory = findViewById(R.id.llSharedStory);

		receiveText = (TextView) this.findViewById(R.id.btReceiveText);

		receiveButton = (Button) this.findViewById(R.id.btReceiveButton);
		receiveButton.setOnClickListener(this);

		mProgressReceive = (ProgressBar) findViewById(R.id.progressReceive);

		mLLSharedStory.findViewById(R.id.btnClose).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Receive more?
				mItemReceived = null;
				setUiState(UIState.Listening);
				
				// Update according to current scan mode
				if (sb != null && sb.btAdapter != null)
					updateBasedOnScanMode(sb.btAdapter.getScanMode());
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
				}
			}
		});

		registerReceiver();

		// Start by trying to receive
		if (sb.isEnabled())
		receiveButton.performClick();
	}

	@Override
	public void onPause()
	{
		Log.v(LOGTAG,"onPause");
		super.onPause();
	}
	
	@Override
	public void onStop()
	{
		Log.v(LOGTAG,"onStop");
		sb.disconnect();
		unregisterReceiver();
		super.onStop();
	}

	@Override
	protected void onResume()
	{
		registerReceiver();
		super.onResume();
		updateUi();
	}

	private void updateUi()
	{
		if (mCurrentState == UIState.Receiving)
		{
			mLLReceive.setVisibility(View.VISIBLE);
			mLLWait.setVisibility(View.GONE);
			mLLSharedStory.setVisibility(View.GONE);
		}
		else if (mCurrentState == UIState.Listening)
		{
			receiveText.setText(R.string.bluetooth_receive_info);
			receiveButton.setEnabled(true);
			mLLReceive.setVisibility(View.GONE);
			mLLWait.setVisibility(View.VISIBLE);
			mLLSharedStory.setVisibility(View.GONE);
		}
		else if (mCurrentState == UIState.ReceivedOk)
		{
			mLLReceive.setVisibility(View.GONE);
			mLLWait.setVisibility(View.GONE);

			StoryItemPageView storyView = (StoryItemPageView) mLLSharedStory.findViewById(R.id.sharedItemView);
			storyView.populateWithItem(mItemReceived);
			storyView.loadMedia(null);
			mLLSharedStory.setVisibility(View.VISIBLE);
		}
	}

	private void setUiState(UIState state)
	{
		mCurrentState = state;
		updateUi();
	}

	@Override
	public void onClick(View clickedView)
	{
		if (clickedView == receiveButton)
		{
			sb.enableDiscovery(this);
			sb.listen();
			this.updateBasedOnScanMode(sb.btAdapter.getScanMode());
			Log.v(LOGTAG, "listen called, ready to receive");
			receiveButton.setEnabled(false);
		}
	}

	private void getReadyToReceive()
	{
		//receivedContentBundleFile = ((App) this.getApplication()).socialReader.vfsTempItemBundle();
		receivedContentBundleFile = ((App) this.getApplication()).socialReader.nonVfsTempItemBundle();

		receiveText.setText(getString(R.string.bluetooth_receive_connected));

		try
		{
			bos = new BufferedOutputStream(new java.io.FileOutputStream(receivedContentBundleFile));
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		readyToReceive = true;
	}

	@Override
	public void secureBluetoothEvent(int eventType, int dataLength, Object data)
	{
		Log.v(LOGTAG, "secureBluetoothEvent " + eventType);

		if (eventType == SecureBluetooth.EVENT_CONNECTED)
		{

			Log.v(LOGTAG, "We have a connection");
			setUiState(UIState.Receiving);
			getReadyToReceive();

		}
		else if (eventType == SecureBluetooth.EVENT_DISCONNECTED)
		{

			Log.v(LOGTAG, "Got a disconnect, " + bytesReceived + " bytes received");

			try
			{
				bos.close();
				
				Item receivedItem = null;
				ArrayList<File> mediaFiles = new ArrayList<File>();
				
				// Now unzip it
				ZipFile zipFile = new ZipFile(receivedContentBundleFile);
				
				for(Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) 
				{
					ZipEntry currentEntry = entries.nextElement();
					if (currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION)) 
					{
						InputStream inputStream = zipFile.getInputStream(currentEntry);
						ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

						try
						{
							// Deserialize it
							receivedItem = (Item) objectInputStream.readObject();
							objectInputStream.close();
							Log.v(LOGTAG, "We have an Item!!!: " + receivedItem.getTitle());
							mItemReceived = receivedItem;
							receivedItem.setShared(true);
							receivedItem.setDatabaseId(Item.DEFAULT_DATABASE_ID);
							receivedItem.setFeedId(Feed.DEFAULT_DATABASE_ID);
							for (MediaContent mc : receivedItem.getMediaContent()) {
								mc.setDatabaseId(MediaContent.DEFAULT_DATABASE_ID);
							}
							// Add it in..
							((App) this.getApplication()).socialReader.setItemData(receivedItem);							
						}
						catch (ClassNotFoundException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}						
					} else { // Ignore for now, we'll loop through again in a second 
						Log.v(LOGTAG,"Ignoring media element for now");
					}
				}
				
				if (receivedItem != null) {
					int mediaContentCount = 0;
					for(Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) 
					{
						ZipEntry currentEntry = entries.nextElement();
						if (currentEntry.getName().contains(SocialReader.CONTENT_ITEM_EXTENSION)) 
						{
							// Ignore it, we should already have it
							
						} else {  
							
							// It's Media Content
							// Save the files in the normal place
							
							InputStream inputStream = zipFile.getInputStream(currentEntry);
							BufferedOutputStream bos = null;
							
							MediaContent mediaContent = receivedItem.getMediaContent(mediaContentCount);
							mediaContentCount++;
							
							File savedFile = new File(((App) this.getApplication()).socialReader.getFileSystemDir(), SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaContent.getDatabaseId());
							bos = new BufferedOutputStream(new FileOutputStream(savedFile));
							
							byte buffer[] = new byte[1024];
							int count;
							while ((count = inputStream.read(buffer)) != -1)
							{
								bos.write(buffer, 0, count);
							}
							inputStream.close();
							bos.close();						
						}
					}
				}
				else {
					Log.e(LOGTAG,"Didn't get an item");
				}
				

			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (mItemReceived != null)
				setUiState(UIState.ReceivedOk);
			else
				setUiState(UIState.Listening);
		}
		else if (eventType == SecureBluetooth.EVENT_DATA_RECEIVED)
		{
			if (!readyToReceive)
			{
				getReadyToReceive();
			}

			// Log.v(LOGTAG,"Reading data: " + sb.available());
			Log.v(LOGTAG, "Reading data: " + dataLength);
			bytesReceived += dataLength;

			try
			{
				bos.write((byte[]) data, 0, dataLength);
				String textReceived = new String((byte[]) data);
				Log.v(LOGTAG, textReceived);
				updateProgress(dataLength, 2 * dataLength);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

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

	private void registerReceiver()
	{
		if (!mReceiverRegistered)
		{
			mReceiverRegistered = true;
			Log.d(LOGTAG, "Register receiver");

			IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
			registerReceiver(receiver, filter);
		}

		// Update according to current scan mode
		if (sb != null && sb.btAdapter != null)
			updateBasedOnScanMode(sb.btAdapter.getScanMode());
	}

	private void unregisterReceiver()
	{
		if (mReceiverRegistered)
		{
			mReceiverRegistered = false;
			Log.d(LOGTAG, "Unregister receiver");
			unregisterReceiver(receiver);
		}
	}

	private final BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent != null)
			{
				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
				{
					int newScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
					updateBasedOnScanMode(newScanMode);
					if (sb.isEnabled())
						sb.listen();
				}
			}
		}
	};

	private void updateBasedOnScanMode(int scanMode)
	{
		if (scanMode == BluetoothAdapter.SCAN_MODE_NONE || scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE)
		{
			receiveText.setVisibility(View.GONE);
			receiveButton.setVisibility(View.VISIBLE);
		}
		else
		{
			receiveText.setVisibility(View.VISIBLE);
			receiveButton.setVisibility(View.GONE);
		}
	}
	
	@Override
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, data);
		
		// If we don´t allow BT to be turned on, just quit out of this activity!
		if (requestCode == SecureBluetooth.REQUEST_ENABLE_BT)
		{
			if (resultCode == RESULT_CANCELED)	
			{
				finish();
			}
			else if (resultCode == RESULT_OK)
			{
				receiveButton.performClick();
			}
		}
	}

}
