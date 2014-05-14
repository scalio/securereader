package info.guardianproject.securereaderinterface;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import info.guardianproject.securereaderinterface.models.OnMediaOrientationListener;
import info.guardianproject.securereaderinterface.views.ImageMediaContentView;
import info.guardianproject.securereaderinterface.views.VideoMediaContentView;
import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.MediaContent;

public class ViewMediaFragment extends Fragment implements OnMediaOrientationListener
{
	public static final String LOGTAG = "ViewMediaFragment";
	public static final boolean LOGGING = false;
	
	private View mRootView;
	private View mMediaContentView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mRootView = inflater.inflate(R.layout.view_media_fragment, container, false);
		return mRootView;
	}

	public void setMediaContent(MediaContent mediaContent)
	{
		((ViewGroup) mRootView).removeAllViews();
		boolean isVideo = mediaContent.getType().startsWith("video/");
		boolean isAudio = mediaContent.getType().startsWith("audio/");
		if (isVideo || isAudio)
		{
			VideoMediaContentView vmc = new VideoMediaContentView(mRootView.getContext());
			vmc.setOnMediaOrientationListener(this);
			vmc.setContentUri(Uri.parse(mediaContent.getUrl()));
			mMediaContentView = vmc;
		}
		else
		{
			ImageMediaContentView imc = new ImageMediaContentView(mRootView.getContext());
			imc.setOnMediaOrientationListener(this);
			imc.setMediaContent(mediaContent, true);
			mMediaContentView = imc;
		}
		((ViewGroup) mRootView).addView(mMediaContentView);
	}

	@Override
	public void onMediaOrientation(View view, int orientation)
	{
		// int accelerometerRotation =
		// Settings.System.getInt(getActivity().getContentResolver(),
		// Settings.System.ACCELEROMETER_ROTATION, 1);
		// if (accelerometerRotation == 0) //

		if (getActivity() != null)
		{
			getActivity().setRequestedOrientation(orientation);
		}
	}

}