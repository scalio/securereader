package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.R;

import android.os.Bundle;

public class AddFeedActivity extends FragmentActivityWithMenu
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_add_feed);
		setMenuIdentifier(R.menu.activity_add_feed);
		
		// Display home as up
		setDisplayHomeAsUp(true);
	}
}
