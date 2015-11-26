package info.guardianproject.zt.rss.fragments;

import android.support.v4.app.Fragment;

import java.util.Stack;

public abstract class BaseHostFragment extends BaseTabSupportFragment {
	
	protected Stack<Fragment> stack;
	
	public abstract boolean restoreFromBackStack();

}
