package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.AddFeedActivity;
import info.guardianproject.securereaderinterface.AddPostActivity;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.CreateAccountActivity;
import info.guardianproject.securereaderinterface.DownloadEpubReaderActivity;
import info.guardianproject.securereaderinterface.DownloadsActivity;
import info.guardianproject.securereaderinterface.HelpActivity;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.PostActivity;
import info.guardianproject.securereaderinterface.SettingsActivity;
import info.guardianproject.securereaderinterface.ViewMediaActivity;
import info.guardianproject.securereaderinterface.installer.HTTPDAppSender;
import info.guardianproject.securereaderinterface.installer.SecureBluetooth;
import info.guardianproject.securereaderinterface.installer.SecureBluetoothReceiverFragment;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.widgets.compat.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

public class UICallbacks
{
	public static final String LOGTAG = "UICallbacks";
	public static final boolean LOGGING = false;	
	
	public enum RequestCode
	{
	    BT_ENABLE(SecureBluetooth.REQUEST_ENABLE_BT),
	    BT_DISCOVERABLE(SecureBluetooth.REQUEST_ENABLE_BT_DISCOVERY),
	    CREATE_CHAT_ACCOUNT(20);
	 
	    /**
	     * Value for this RequestCode
	     */
	    public final int Value;
	 
	    private RequestCode(int value)
	    {
	        Value = value;
	    }
	 
	    static void checkUniqueness()
	    {
	    	ArrayList<Integer> intvals = new ArrayList<Integer>();
	        for (RequestCode code : RequestCode.values())
	        {
	        	if (intvals.contains(code.Value))
	    	        throw new RuntimeException("RequestCode array is invalid (not usnique numbers), check the values!");
	            intvals.add(code.Value);
	        }
	    }
	};
	
	public interface OnCallbackListener
	{
		/*
		 * Called when the feed filter should change to only show the selected
		 * feed (or all feeds if the flag is set)
		 * 
		 * This may occur either when a feed is selected in the feed drop down
		 * or when the source feed of a story item in the stream is clicked.
		 */
		void onFeedSelect(FeedFilterType type, long feedId, Object source);

		/* Called to filter on a special tag */
		void onTagSelect(String tag);

		void onRequestResync(Feed feed);

		/**
		 * Called when an item has been marked/unmarked as a favorite. Affects
		 * our list of favorites.
		 */
		void onItemFavoriteStatusChanged(Item item);

		/**
		 * Called to handle a command.
		 */
		void onCommand(int command, Bundle commandParameters);
	}

	private static UICallbacks gInstance;

	public static UICallbacks getInstance()
	{
		if (gInstance == null)
			gInstance = new UICallbacks();
		return gInstance;
	}

	private final ArrayList<OnCallbackListener> mListeners;

	private UICallbacks()
	{
		mListeners = new ArrayList<OnCallbackListener>();
		RequestCode.checkUniqueness();
	}

	public void addListener(OnCallbackListener listener)
	{
		synchronized (mListeners)
		{
			if (!mListeners.contains(listener))
				mListeners.add(listener);
		}
	}

	public void removeListener(OnCallbackListener listener)
	{
		synchronized (mListeners)
		{
			if (mListeners.contains(listener))
				mListeners.remove(listener);
		}
	}

	public static void setFeedFilter(FeedFilterType type, long feedId, Object source)
	{
		getInstance().fireCallback("onFeedSelect", type, feedId, source);
	}

	public static void requestResync(Feed feed)
	{
		getInstance().fireCallback("onRequestResync", feed);
	}

	public static void setTagFilter(String tag, Object source)
	{
		getInstance().fireCallback("onTagSelect", tag);
	}

	public static void itemFavoriteStatusChanged(Item item)
	{
		getInstance().fireCallback("onItemFavoriteStatusChanged", item);
	}

	private void fireCallback(String methodName, Object... commandParameters)
	{
		Class<?>[] paramTypes = null;

		try
		{
			// Figure out what types we need for the call
			Method[] interfaceMethods = OnCallbackListener.class.getMethods();
			for (Method interfaceMethod : interfaceMethods)
			{
				if (interfaceMethod.getName().equals(methodName))
				{
					paramTypes = interfaceMethod.getParameterTypes();
					break;
				}
			}

			synchronized (mListeners)
			{
				for (int i = 0; i < mListeners.size(); i++)
				{
					OnCallbackListener listener = mListeners.get(i);
					try
					{
						Method m = listener.getClass().getMethod(methodName, paramTypes);
						m.invoke(listener, commandParameters);
					}
					catch (Exception ex)
					{
						// TODO - remove listener?
					}
				}
			}
		}
		catch (Exception ex)
		{
			if (LOGGING)
				Log.d(LOGTAG, "Failed to get callback method info: " + ex.toString());
		}
	}

	public static void handleCommand(Context context, int command, Bundle commandParameters)
	{
		getInstance().fireCallback("onCommand", command, commandParameters);

		switch (command)
		{
		case R.integer.command_news_list:
		{
			Intent intent = new Intent(context, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			break;
		}

		case R.integer.command_posts_list:
		{
			Intent intent = new Intent(context, PostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			if (commandParameters != null && commandParameters.containsKey("go_to_tab"))
			{
				intent.putExtra("go_to_tab", commandParameters.getInt("go_to_tab", -1));
			}
			context.startActivity(intent);
			break;
		}

		case R.integer.command_post_add:
		{
			Intent intent = new Intent(context, AddPostActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			break;
		}

		case R.integer.command_feed_add:
		{
			Intent intent = new Intent(context, AddFeedActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			break;
		}

		case R.integer.command_settings:
		{
			Intent intent = new Intent(context, SettingsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			break;
		}

		case R.integer.command_toggle_online:
		{
			if (App.getInstance().socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY)
				App.getInstance().socialReader.connectProxy((Activity) context);
			// else
			// Not sure this makes sense
			// App.getInstance().socialReader.goOffline();

			break;
		}

		case R.integer.command_view_media:
		{	
			if (LOGGING)
				Log.v(LOGTAG, "command_view_media");
			if (commandParameters != null && commandParameters.containsKey("media"))
			{
				MediaContent mediaContent = (MediaContent) commandParameters.getSerializable("media");
				if (LOGGING)
					Log.v(LOGTAG, "MediaContent " + mediaContent.getType());

				if (mediaContent != null && mediaContent.getType().startsWith("application/vnd.android.package-archive"))
				{
					// This is an application package. View means
					// "ask for installation"...
					if (mediaContent.getDownloadedNonVFSFile() != null) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.setDataAndType(Uri.fromFile(mediaContent.getDownloadedNonVFSFile()),mediaContent.getType());
						context.startActivity(intent);
					}
				} 
				else if (mediaContent != null && mediaContent.getType().startsWith("application/epub+zip"))
				{
					if (LOGGING)
						Log.v(LOGTAG, "MediaContent is epub");

					// This is an epub
					if (mediaContent.getDownloadedNonVFSFile() != null) {
						if (LOGGING)
							Log.v(LOGTAG, "Not null");
						
						try {
							File properlyNamed = new File(mediaContent.getDownloadedNonVFSFile().toString() + ".epub"); 
							InputStream in = new FileInputStream(mediaContent.getDownloadedNonVFSFile());
							OutputStream out = new FileOutputStream(properlyNamed);

						    // Transfer bytes from in to out
						    byte[] buf = new byte[1024];
						    int len;
						    while ((len = in.read(buf)) > 0) {
						        out.write(buf, 0, len);
						    }
						    in.close();
						    out.close();
						    
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(properlyNamed),mediaContent.getType());

							PackageManager packageManager = context.getPackageManager();
						    List list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
						    if (list.size() > 0) {
						    	if (LOGGING)
									Log.v(LOGTAG, "Launching epub reader" + Uri.fromFile(properlyNamed).toString());
						    	context.startActivity(intent);
						    }
						    else {
						    	if (LOGGING)
									Log.v("UICallbacks", "No application found" + Uri.fromFile(properlyNamed).toString());
						    	
						    	// Download epub reader?
								int numShown = App.getSettings().downloadEpubReaderDialogShown();
								if (numShown < 1)
								{
									App.getSettings().setDownloadEpubReaderDialogShown(numShown + 1);
									intent = new Intent(context, DownloadEpubReaderActivity.class);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
									context.startActivity(intent);
									((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
								}
						    }
						} catch (FileNotFoundException e) {
						
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}					    
					    
					}
					else {
						if (LOGGING)
							Log.v(LOGTAG, "NULL");
					}
				}
				else
				{
					Intent intent = new Intent(context, ViewMediaActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtra("parameters", commandParameters);
					context.startActivity(intent);
				}
			}
			else
			{
				if (LOGGING)
					Log.e(LOGTAG, "Invalid parameters to command command_view_media.");
			}
			break;
		}

		case R.integer.command_chat:
		{
			// If we don't have an account yet, we need to create that!
			if (!PackageHelper.isChatSecureInstalled(context))
			{
				int numShown = App.getSettings().chatSecureDialogShown();
				if (numShown < 2)
				{
					AlertDialog dialog = PackageHelper.showDownloadDialog(context, R.string.install_chat_secure_title, R.string.install_chat_secure_prompt,
							android.R.string.ok, android.R.string.cancel, PackageHelper.URI_CHATSECURE_PLAY);
					App.getSettings().setChatSecureDialogShown(numShown + 1);
				}
				else
				{
					// Stop prompting, just show a toast with info
					Toast.makeText(context, R.string.install_chat_secure_toast, Toast.LENGTH_SHORT).show();
				}
			}
			else if (App.getInstance().socialReporter.getAuthorName() == null)
			{
				Intent intent = new Intent(context, CreateAccountActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			}
			else
			{
				/*
				if (!App.getSettings().chatUsernamePasswordSet() 
						&& App.getInstance().socialReader.ssettings.getChatSecureUsername() != null
						&& App.getInstance().socialReader.ssettings.getChatSecurePassword() != null) {
				*/
					/*
					ima://foo:pass@bar.com/
					action = android.intent.action.INSERT 
					 */
					/*
					Intent usernamePasswordIntent = new Intent(Intent.ACTION_INSERT, 
							Uri.parse("ima://"+App.getInstance().socialReader.ssettings.getXMLRPCUsername()+":"
									+App.getInstance().socialReader.ssettings.getXMLRPCPassword()+"@dukgo.com/"));
					*/
					//context.startActivity(usernamePasswordIntent);
					//((FragmentActivityWithMenu) context).startActivityForResultAsInternal(usernamePasswordIntent, RequestCode.CREATE_CHAT_ACCOUNT.Value);
					// How to tell if it worked?
					//((Activity)context).startActivityForResult(usernamePasswordIntent,REGISTER_CHAT_USERNAME_PASSWORD);
					// if it is OK then App.getSettings().setChatUsernamePasswordSet();
				/*
				}  else if (App.getInstance().socialReader.ssettings.getChatSecurePassword() == null ||
						App.getInstance().socialReader.ssettings.getChatSecureUsername() == null) {
				*/
					// Register Social Reporter username/password
				/*} else {*/
				
					if (LOGGING)
						Log.v(LOGTAG, "Start the chat application now!");
					
					String roomName = context.getString(R.string.chatroom_name);
					
					if (commandParameters != null && commandParameters.containsKey("room_name"))
						roomName = commandParameters.getString("room_name");
					if (roomName == null)
						roomName = "";
					Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("immu://" + App.getInstance().socialReporter.getAuthorName()
							+ "@conference.dukgo.com/" + roomName));
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					if (!PackageHelper.canIntentBeHandled(context, intent))
					{
						// Old version of ChatSecure that don't support intent API.
						// Need to upgrade!
						AlertDialog dialog = PackageHelper.showDownloadDialog(context, R.string.install_chat_secure_title,
								R.string.install_chat_secure_prompt_upgrade, android.R.string.ok, android.R.string.cancel, PackageHelper.URI_CHATSECURE_PLAY);
					}
					else
					{
							context.startActivity(intent);
					}
				/*}*/
			}
			break;
		}

		case R.integer.command_downloads:
		{
			Intent intent = new Intent(context, DownloadsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			break;
		}

		case R.integer.command_help:
		{
			Intent intent = new Intent(context, HelpActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			break;
		}

		case R.integer.command_receiveshare:
		{
			if (context instanceof FragmentActivity)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Calling receive share fragment dialog");
				FragmentManager fm = ((FragmentActivity)context).getSupportFragmentManager();
				SecureBluetoothReceiverFragment dialogReceiveShare = new SecureBluetoothReceiverFragment(); 
				dialogReceiveShare.show(fm, App.FRAGMENT_TAG_RECEIVE_SHARE);
			}
			break;
		}

		case R.integer.command_shareapp:
		{
			if (LOGGING)
				Log.v(LOGTAG, "Calling HTTPDAppSender");
			Intent intent = new Intent(context, HTTPDAppSender.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
			((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			break;
		}

		}
	}
}
