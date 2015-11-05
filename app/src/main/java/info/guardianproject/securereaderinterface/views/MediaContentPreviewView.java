package info.guardianproject.securereaderinterface.views;

import com.tinymission.rss.MediaContent;

public interface MediaContentPreviewView
{
	public void setMediaContent(MediaContent mediaContent, info.guardianproject.iocipher.File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread);
	public MediaContent getMediaContent();
	public void recycle();
}
