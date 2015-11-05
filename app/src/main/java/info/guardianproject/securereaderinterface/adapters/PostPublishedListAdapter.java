package info.guardianproject.securereaderinterface.adapters;

import java.util.ArrayList;

import android.content.Context;
import com.tinymission.rss.Item;

public class PostPublishedListAdapter extends StoryListAdapter
{
	public static final String LOGTAG = "PostPublishedListAdapter";
	public static final boolean LOGGING = false;
	
	public PostPublishedListAdapter(Context context, ArrayList<Item> posts)
	{
		super(context, posts);
		setShowTags(true);
	}
}
