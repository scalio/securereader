package info.guardianproject.securereaderinterface.z.rss.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import info.guardianproject.securereaderinterface.R;

import java.util.Stack;

public class Tab4ContainerFragment extends BaseHostFragment {
    //**** This class not equal Tab1ContainerFragment, be careful
    //stack contain all opened fragments!!!


    public static final int TAB_ID = 3;
	private View view;
    public static FragmentManager childManager;
    public static Tab4ContainerFragment self = null;

    public Tab4ContainerFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         if(view == null){
             view = inflater.inflate(R.layout.ztab4_container_layout, container, false);

             Fragment mainFragment = Fragment.instantiate(getActivity(), ShareFragment.class.getName());
             FragmentTransaction ft = childManager.beginTransaction();
             ft.replace(R.id.tab4content, mainFragment);
             Tab4ContainerFragment.self.stack.push(mainFragment);
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
//            Log.e("Tab4ContainerFragment","onCreate Stack was null, new stack created! stack = new Stack<Fragment>()");
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
        if(stack.size() <= 1){
//            Log.e("Tab4ContainerFragment","restoreFromBackStack stack.size() == "+String.valueOf(stack.size())+", Close App");
            return false;
        } else {
//            Log.e("Tab4ContainerFragment","restoreFromBackStack stack.size() == "+String.valueOf(stack.size()));
            stack.pop();
//            Log.e("Tab4ContainerFragment","restoreFromBackStack stack.pop() stack.size() =="+String.valueOf(stack.size()));
            if(stack.size() > 0){
                FragmentTransaction ft = childManager.beginTransaction();
                //ft.setCustomAnimations( R.anim.slide_in_right, R.anim.slide_out_left , R.anim.slide_in_left,  R.anim.slide_out_right );
                Fragment restoring = stack.peek();
                ft.replace(R.id.tab4content, restoring);
                ft.commit();
//                Log.e("Tab4ContainerFragment","restoreFromBackStack restoring="+restoring.getClass().getName());
                return true;
            }
            else{
//                Log.e("Tab4ContainerFragment","restoreFromBackStack stack.size() == 0 after pop, Close App");
                return false;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(stack.size() > 0){
            Fragment fragment = stack.peek();
            if(fragment!= null){
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
        else{
            if(childManager.getFragments().size() != 0  ){
                Fragment fragment = childManager.getFragments().get(0);
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onDestroy() {
        self = null;
        childManager = null;
        stack = null;
        super.onDestroy();
    }
}
