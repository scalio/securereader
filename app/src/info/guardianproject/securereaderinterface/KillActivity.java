package info.guardianproject.securereaderinterface;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class KillActivity extends Activity
{
	public static final String LOGTAG = "KillActivity";
	public static final boolean LOGGING = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(App.EXIT_BROADCAST_ACTION);
		this.sendOrderedBroadcast(intent, App.EXIT_BROADCAST_PERMISSION, new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				// android.os.Process.killProcess(android.os.Process.myPid());
			}
		}, null, Activity.RESULT_OK, null, null);
		finish();
	}
}
