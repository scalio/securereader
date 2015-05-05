package info.guardianproject.zt.z.rss.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import info.guardianproject.zt.z.rss.fragments.AudioListFragment;
import info.guardianproject.zt.z.rss.fragments.Tab1ContainerFragment;
import info.guardianproject.zt.z.rss.fragments.Tab4ContainerFragment;
import info.guardianproject.zt.z.rss.fragments.VideoListFragment;

public class MainAdapter extends MyFragmentStatePagerAdapter {
    public MainAdapter(FragmentManager fm ) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                Tab1ContainerFragment tab1ContainerFragment = new Tab1ContainerFragment();
                return tab1ContainerFragment;
            case 1:
                VideoListFragment videoListFragment = new VideoListFragment();
                return videoListFragment;
            case 2:
                AudioListFragment audioListFragment = new AudioListFragment();
                return audioListFragment;
            case 3:
                Tab4ContainerFragment shareFragment = new Tab4ContainerFragment();
                return shareFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

}
