package info.guardianproject.securereaderinterface.z.rss.fragments;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.z.rss.activities.MainActivityViewPager;
import info.guardianproject.securereaderinterface.z.rss.views.MyTransitionViewPager;

public class BaseTabSupportFragment extends BaseRefreshFragment {
    public void initTabBar(View view, final int activeTab){
    	/*
        LinearLayout tab1Layout = (LinearLayout) view.findViewById(R.id.tab1Layout);
        tab1Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTab(Tab1ContainerFragment.TAB_ID);
            }
        });
        LinearLayout tab2Layout = (LinearLayout) view.findViewById(R.id.tab2Layout);
        tab2Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTab(VideoListFragment.TAB_ID);
            }
        });
        LinearLayout tab3Layout = (LinearLayout) view.findViewById(R.id.tab3Layout);
        tab3Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTab(AudioListFragment.TAB_ID);
            }
        });
        LinearLayout tab4Layout = (LinearLayout) view.findViewById(R.id.tab4Layout);
        tab4Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTab(Tab4ContainerFragment.TAB_ID);
            }
        });
        switch (activeTab){
            case Tab1ContainerFragment.TAB_ID:
                tab1Layout.setSelected(true);
                break;
            case VideoListFragment.TAB_ID:
                tab2Layout.setSelected(true);
                break;
            case AudioListFragment.TAB_ID:
                tab3Layout.setSelected(true);
                break;
            case Tab4ContainerFragment.TAB_ID:
                tab4Layout.setSelected(true);
                break;
            default:
                tab1Layout.setSelected(true);
        }*/
    }

    public void goToTab(final int tabPosition){
        Activity activity = getActivity();
        if(activity instanceof MainActivityViewPager){
            MainActivityViewPager mainActivityViewPager = (MainActivityViewPager) activity;
            MyTransitionViewPager viewPager = mainActivityViewPager.getViewPager();
            if(viewPager != null){
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                    viewPager.setScrollDurationFactor(8);
                    viewPager.setCurrentItem(tabPosition, true);
                    viewPager.setScrollDurationFactor(1);
//                }
//                else{
//                    viewPager.setCurrentItem(tabPosition);
//                }

            }
        }
    }


}
