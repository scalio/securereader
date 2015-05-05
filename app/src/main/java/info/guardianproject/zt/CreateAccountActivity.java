package info.guardianproject.zt;

import android.os.Bundle;

import info.guardianproject.zt.ui.UICallbacks;
import info.guardianproject.zt.views.CreateAccountView;
import info.guardianproject.zt.views.CreateAccountView.OnActionListener;
import info.guardianproject.zt.R;

public class CreateAccountActivity extends FragmentActivityWithMenu implements OnActionListener
{
	public static final String LOGTAG = "CreateAccountActivity";
	public static final boolean LOGGING = false;
	
	private CreateAccountView mViewCreateAccount;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Display home as up
		setDisplayHomeAsUp(true);

		setContentView(R.layout.activity_create_account);
		setMenuIdentifier(R.menu.activity_create_account);

		mViewCreateAccount = (CreateAccountView) findViewById(R.id.createAccount);
		mViewCreateAccount.setActionListener(this);
	}

	@Override
	public void onCreateIdentity(String authorName)
	{
		info.guardianproject.zt.App.getInstance().socialReporter.createAuthorName(authorName);
		UICallbacks.handleCommand(this, R.integer.command_chat, null);
		finish();
	}
}
