package info.guardianproject.zt.installer;

import info.guardianproject.zt.R;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.zt.App;
import info.guardianproject.zt.FragmentActivityWithMenu;
import info.guardianproject.zt.LockableFragment;
import info.guardianproject.zt.installer.SecureBluetooth.SecureBluetoothEventListener;
import info.guardianproject.zt.views.StoryItemPageView;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tinymission.rss.Item;

public class SecureBluetoothSenderFragment extends DialogFragment implements LockableFragment, OnClickListener, SecureBluetoothEventListener, OnItemClickListener
{
	public static final String LOGTAG = "SecureBluetoothSenderFragment";
	public static final boolean LOGGING = false;

	private enum UIState
	{
		Scanning, Sending, SentOk
	};

	SecureBluetooth sb;

	TextView sendTextView;

	View mLLScan;
	View mLLSend;
	View mLLSharedStory;

	View scanButton;

	View ivScanning;
	TextView tvScanning;

	ArrayAdapter<DeviceInfo> mNewDevicesArrayAdapter;

	ListView newDevicesListView;

	Intent shareIntent;
	File fileToSend;
	long itemIdToSend;
	Item mItemSent;

	UIState mCurrentState = UIState.Scanning;
	ProgressBar mProgressSend;

	private Dialog mDialog;

	public SecureBluetoothSenderFragment()
	{
		super();
		sb = new SecureBluetooth();
		sb.setSecureBluetoothEventListener(this);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE;
        int theme = R.style.AppTheme_Dialog;
        setStyle(style, theme);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View root = inflater.inflate(R.layout.activity_secure_blue_tooth_sender, container, false);

		// Pull out the item id
		shareIntent = this.getArguments().getParcelable("intent");
		if (shareIntent.hasExtra(SocialReader.SHARE_ITEM_ID))
		{
			SocialReader socialReader = App.getInstance().socialReader;
			itemIdToSend = shareIntent.getLongExtra(SocialReader.SHARE_ITEM_ID, Item.DEFAULT_DATABASE_ID);
			fileToSend = socialReader.packageItem(itemIdToSend);
			mItemSent = socialReader.getItemFromId(itemIdToSend);
		}
		else
		{
			if (LOGGING)
				Log.e(LOGTAG, "No Item Id to Share");
			dismiss();
		}

		mLLScan = root.findViewById(R.id.llScan);
		mLLSend = root.findViewById(R.id.llSend);
		mLLSharedStory = root.findViewById(R.id.llSharedStory);

		// Initialize array adapters. One for already paired devices and one for
		// newly discovered devices
		mNewDevicesArrayAdapter = new ArrayAdapter<DeviceInfo>(getActivity(), R.layout.activity_secure_blue_tooth_sender_device_name);

		// Find and set up the ListView for newly discovered devices
		newDevicesListView = (ListView) root.findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(this);

		sendTextView = (TextView) root.findViewById(R.id.btSendText);
		sendTextView.setText("");

		mProgressSend = (ProgressBar) root.findViewById(R.id.progressSend);

		scanButton = root.findViewById(R.id.btScanButton);
		scanButton.setEnabled(true);
		scanButton.setOnClickListener(this);
		ivScanning = root.findViewById(R.id.ivScanning);
		tvScanning = (TextView) root.findViewById(R.id.tvScanning);

		mLLSharedStory.findViewById(R.id.btnClose).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Go back to what we were doing...
				dismiss();
			}
		});

		showScanningSpinner(false);
		
		// Start by trying to receive
		if (!sb.isEnabled())
			sb.enableBluetooth(getActivity());
		
		return root;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		mDialog = super.onCreateDialog(savedInstanceState);
		mDialog.setOnShowListener(new OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				// If BT not enabled, hide us for now. We will prompt the user to enable
				// BT and handle the result in onUnlockedActivityResult. Based on the
				// user�s choice the dialog will either be dismissed or shown there.
				if (!sb.isEnabled())
					mDialog.hide();
			}
		});
		return mDialog;
	}
	
	private final BroadcastReceiver receiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already
				// if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
				mNewDevicesArrayAdapter.add(new DeviceInfo(device.getName(), device.getAddress()));
				// }
				// When discovery is finished, change the Activity title
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				//setProgressBarIndeterminateVisibility(false);
				// setTitle(R.string.select_device);
				if (mNewDevicesArrayAdapter.getCount() == 0)
				{
					// String noDevices =
					// getResources().getText(R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(new DeviceInfo(getString(R.string.bluetooth_no_devices_found), null));
				}
				showScanningSpinner(false);
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
			{
				//setProgressBarIndeterminateVisibility(true);
				showScanningSpinner(true);
			}
		}
	};

	@Override
	public void onResume()
	{
		super.onResume();
		updateUi();

		// Start a scan automatically
		if (mCurrentState == UIState.Scanning)
			scanButton.performClick();
	}

	private void updateUi()
	{
		if (mCurrentState == UIState.Scanning)
		{
			mLLScan.setVisibility(View.VISIBLE);
			mLLSend.setVisibility(View.GONE);
			mLLSharedStory.setVisibility(View.GONE);
			newDevicesListView.setOnItemClickListener(this);
		}
		else if (mCurrentState == UIState.Sending)
		{
			mLLScan.setVisibility(View.GONE);
			mLLSend.setVisibility(View.VISIBLE);
			mLLSharedStory.setVisibility(View.GONE);
		}
		else if (mCurrentState == UIState.SentOk)
		{
			mLLScan.setVisibility(View.GONE);
			mLLSend.setVisibility(View.GONE);

			StoryItemPageView storyView = (StoryItemPageView) mLLSharedStory.findViewById(R.id.sharedItemView);
			storyView.populateWithItem(mItemSent);
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
	public void onPause()
	{
		if (LOGGING) 
			Log.v(LOGTAG,"onPause");
		super.onPause();
	}

	
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		getActivity().registerReceiver(receiver, filter);
	}

	@Override
	public void onStop()
	{
		if (LOGGING)
			Log.v(LOGTAG,"onStop");
		if (sb.btAdapter.isDiscovering()) { sb.btAdapter.cancelDiscovery(); }
		sb.disconnect();
		showScanningSpinner(false);
		getActivity().unregisterReceiver(receiver);
		super.onStop();
	}

	private void setStatusText(final int idText)
	{
		sendTextView.post(new Runnable()
		{

			@Override
			public void run()
			{
				sendTextView.setText(idText);
			}
		});
	}

	/*
	public void onClick(View clickedView) {
		if (clickedView == sendButton) {
			//sb.write("Hello Receiver, this is sender sending");
			//sb.disconnect();
			
			if (fileToSend != null) {
				try {
			        // For debuggging
			        FileInputStream fis = new FileInputStream(fileToSend);
				    BufferedInputStream bis = new BufferedInputStream( fis );
				    ObjectInputStream input = new ObjectInputStream ( bis );
				    
				    //Deserialize it
					try {
						
						Item inItem = (Item) input.readObject();
						if (inItem != null) {
							if (LOGGING)
								Log.v(LOGTAG,"We packaged an Item!!!: " + inItem.toString());
						} 
						
						sb.writeObject(inItem);

						if (LOGGING)
							Log.v(LOGTAG,"We sent an Item!!!: " + inItem.toString());

						
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
	*/
	
	
	private void sendFile() 
	{
		if (fileToSend != null)
		{
			BufferedInputStream bin = null;
			try
			{
				long lengthTotal = fileToSend.length();
				long lengthSent = 0;
				updateSendProgress(0, lengthTotal);

				// First send size
				sb.writeLength(lengthTotal);
				
				bin = new BufferedInputStream(new FileInputStream(fileToSend));
				byte[] buffer = new byte[256];
				int bytesRead;
				while ((bytesRead = bin.read(buffer)) != -1)
				{
					sb.write(buffer);
					lengthSent += bytesRead;
					updateSendProgress(lengthSent, lengthTotal);
					
					if (LOGGING)
						Log.v(LOGTAG,"bytes sent now: " + bytesRead + " " + lengthSent + " total: " + lengthTotal);
				}
									
				setStatusText(R.string.bluetooth_send_sent_item);
				setUiState(UIState.SentOk);
			}
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				setStatusText(R.string.bluetooth_send_error);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				setStatusText(R.string.bluetooth_send_error);
 
			} 
			finally 
			{
				if (bin != null) {
					try {
						bin.close();
					} catch (IOException e) {
					
					}
				}
				//sb.disconnect();
			}
		}
		else
		{
			setStatusText(R.string.bluetooth_send_error);
		}		
	}
	
	
	@Override
	public void onClick(View clickedView)
	{
		if (clickedView == scanButton)
		{
			mNewDevicesArrayAdapter.clear();
			showScanningSpinner(true);
			sb.startDiscovery();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long arg3)
	{
		// Cancel discovery because it's costly and we're about to connect
		sb.btAdapter.cancelDiscovery();
		showScanningSpinner(false);

		// Get the device MAC address
		DeviceInfo info = (DeviceInfo) av.getAdapter().getItem(position);
		if (info != null)
		{
			setUiState(UIState.Sending);

			String address = info.getAddress();
			setStatusText(R.string.bluetooth_send_connecting);

			// Remove onitemclicklistener
			newDevicesListView.setOnItemClickListener(null);

			// Connect
			v.post(new Runnable()
			{
				private String mAddress;

				@Override
				public void run()
				{
					if (!sb.connect(mAddress))
					{
						setUiState(UIState.Scanning);
						Toast.makeText(getActivity(), R.string.bluetooth_send_connect_error, Toast.LENGTH_LONG).show();
					}
				}

				public Runnable init(String address)
				{
					mAddress = address;
					return this;
				}
			}.init(address));

		}
	}

	@Override
	public void secureBluetoothEvent(int eventType, int dataLength, Object data)
	{
		if (LOGGING)
			Log.v(LOGTAG, "secureBluetoothEvent " + eventType);
		if (eventType == SecureBluetooth.EVENT_CONNECTED)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Connected");
			setStatusText(R.string.bluetooth_send_connected);
			sendFile();
		}
		else if (eventType == SecureBluetooth.EVENT_DISCONNECTED)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Disconnected");
			setStatusText(R.string.bluetooth_send_disconnected);
		}
	}

	private void showScanningSpinner(boolean show)
	{
		if (show)
		{
			tvScanning.setText(R.string.bluetooth_send_scanning);
			ivScanning.setVisibility(View.VISIBLE);
			ivScanning.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.rotate));
			scanButton.setEnabled(false);
		}
		else
		{
			tvScanning.setText(R.string.bluetooth_send_scan);
			ivScanning.clearAnimation();
			ivScanning.setVisibility(View.GONE);
			scanButton.setEnabled(true);
		}
	}

	private void updateSendProgress(long lengthSent, long max)
	{
		mProgressSend.setMax((int) max);
		mProgressSend.setProgress((int) lengthSent);
		mProgressSend.invalidate();
	}

	private class DeviceInfo
	{
		private final String name;
		private final String address;

		public DeviceInfo(String name, String address)
		{
			this.name = name;
			this.address = address;
		}

		public String getName()
		{
			return name;
		}

		public String getAddress()
		{
			return address;
		}

		@Override
		public String toString()
		{
			return getName();
		}

	}

	@Override
	public void onUnlockedActivityResult(int requestCode, int resultCode, Intent data)
	{
		// If we don�t allow BT to be turned on, just quit out of this activity!
		if (requestCode == SecureBluetooth.REQUEST_ENABLE_BT)
		{
			if (resultCode == Activity.RESULT_CANCELED)	
			{
				this.dismiss();
			}
			else if (resultCode == Activity.RESULT_OK)
			{
				mDialog.show();
			}
		}
	}
	
	
}
