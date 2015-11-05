package info.guardianproject.securereaderinterface.ui;

import android.os.Bundle;
import android.view.View;

import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.MediaContent;

public class OnMediaItemClickedListener implements View.OnClickListener
{
	private final MediaContent mMediaContent;

	public OnMediaItemClickedListener(MediaContent mediaContent)
	{
		mMediaContent = mediaContent;
	}

	@Override
	public void onClick(View v)
	{
		final Bundle mediaData = new Bundle();
		mediaData.putSerializable("media", mMediaContent);
		UICallbacks.handleCommand(v.getContext(), R.integer.command_view_media, mediaData);
	}
}