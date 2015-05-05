package info.guardianproject.zt.rss.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import info.guardianproject.zt.R;

import java.util.Stack;

public class Tab1ContainerFragment extends BaseHostFragment {
    public static final int TAB_ID = 0;
	private View view;

    public static FragmentManager childManager;
    public static Tab1ContainerFragment self = null;

    public Tab1ContainerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         if(view == null){
             view = inflater.inflate(R.layout.ztab1_container_layout, container, false);

             Fragment mainFragment = Fragment.instantiate(getActivity(), RssListFragment.class.getName());
             FragmentTransaction ft = childManager.beginTransaction();
             ft.replace(R.id.tab1content, mainFragment);
             ft.commit();
         } else {
        	 ((ViewGroup)view.getParent()).removeView(view);
         }
         initTabBar(view, TAB_ID);
         return view;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        childManager = getChildFragmentManager();
        self = this;
        if(stack == null){
            stack = new Stack<Fragment>();
        }

    }

    @Override
    public void refresh(Context context) {
        if(stack.size() > 0){
            Fragment fragment = stack.peek();
            if(fragment instanceof  BaseRefreshFragment){
                ((BaseRefreshFragment )fragment).refresh(context);
            }
        }
        else{
            if(childManager.getFragments().size() != 0 && childManager.getFragments().get(childManager.getFragments().size()-1) instanceof BaseRefreshFragment  ){
                BaseRefreshFragment baseRefreshFragment = (BaseRefreshFragment)childManager.getFragments().get(0);
                baseRefreshFragment.refresh(getActivity());
            }
        }
    }

    @Override
    public boolean restoreFromBackStack() {
        if(stack.size() == 0){
            return false;
        } else {
            FragmentTransaction ft = childManager.beginTransaction();
            //ft.setCustomAnimations( R.anim.slide_in_right, R.anim.slide_out_left , R.anim.slide_in_left,  R.anim.slide_out_right );
            Fragment restoring = stack.pop();
            ft.replace(R.id.tab1content, restoring);
            ft.commit();
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        self = null;
        childManager = null;
    }

}
