package info.guardianproject.securereaderinterface.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.widgets.compat.Spinner;

public class TextSizeSpinnerAdapter extends BaseAdapter implements SpinnerAdapter
{
	public static final String LOGTAG = "TextSizeSpinnerAdapter";
	public static final boolean LOGGING = false;		
	
	private final String TITLE_TAG = "TITLE";

	private final Spinner mSpinner;
	private final LayoutInflater mInflater;
	private final Context mContext;
	private final int mResIdButtonLayout;

	public TextSizeSpinnerAdapter(Spinner spinner, Context context, int resIdButtonLayout)
	{
		super();
		mSpinner = spinner;
		mContext = context;
		mResIdButtonLayout = resIdButtonLayout;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount()
	{
		return 2;
	}

	@Override
	public Object getItem(int position)
	{
		if (position == 0)
			return "+";
		return "-";
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView;
		if (view == null)
		{
			view = mInflater.inflate(mResIdButtonLayout, parent, false);
		}

		view.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mSpinner.performClick();
			}
		});

		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		View view = (convertView != null && convertView.getTag() != TITLE_TAG) ? convertView : createView(parent);
		TextView tv = (TextView) view.findViewById(R.id.tvItem);
		tv.setText((position == 0) ? "+" : "-");
		return view;
	}

	private View createView(ViewGroup parent)
	{
		View item = mInflater.inflate(R.layout.popup_menu_item, parent, false);
		return item;
	}
}
