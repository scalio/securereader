package info.guardianproject.securereaderinterface.adapters;

import java.util.ArrayList;

import android.content.Context;
import info.guardianproject.securereaderinterface.R;
import com.tinymission.rss.Item;

public class PostOutgoingListAdapter extends PostPublishedListAdapter
{
	public static final String LOGTAG = "PostOutgoingListAdapter";
	public static final boolean LOGGING = false;
	
	public PostOutgoingListAdapter(Context context, ArrayList<Item> posts)
	{
		super(context, posts);
		setHeaderView(R.layout.post_list_no_outgoing, true);
	}
}
