package info.guardianproject.securereaderinterface;

import info.guardianproject.paik.R;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class ChatInfoActivity extends FragmentActivityWithMenu implements OnClickListener
{
	private View mBtnJoin;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Display home as up
		setDisplayHomeAsUp(true);

		setContentView(R.layout.activity_chat_info);
		setMenuIdentifier(R.menu.activity_chat_info);

		mBtnJoin = findViewById(R.id.btnJoin);
		mBtnJoin.setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		Bundle commandParameters = getIntent().getBundleExtra("parameters");
		if (commandParameters == null)
			commandParameters = new Bundle();
		commandParameters.putBoolean("dont_show_info", true);
		UICallbacks.handleCommand(this, R.integer.command_chat, commandParameters);
		finish();
	}
}
