package info.guardianproject.zt.z.rss.views;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import com.abhi.animated.TransitionViewPager;
import info.guardianproject.zt.z.rss.adapters.MyFragmentStatePagerAdapter;

import java.lang.reflect.Field;

public class MyTransitionViewPager extends TransitionViewPager {
    private ScrollerCustomDuration mScroller = null;
    public MyTransitionViewPager(Context context) {
        super(context);
        postInitViewPager();
    }

    public MyTransitionViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        postInitViewPager();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent arg0) {
//        return false;
//    }

    /**
     * Override the Scroller instance with our own class so we can change the
     * duration
     */
    private void postInitViewPager() {
        try {
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            Field interpolator = ViewPager.class.getDeclaredField("sInterpolator");
            interpolator.setAccessible(true);

            mScroller = new ScrollerCustomDuration(getContext(),
                    (Interpolator) interpolator.get(null));
            scroller.set(this, mScroller);
        } catch (Exception e) {
        }
    }

    /**
     * Set the factor by which the duration will change
     */
    public void setScrollDurationFactor(double scrollFactor) {
        mScroller.setScrollDurationFactor(scrollFactor);
    }

    @Override
    protected View findViewFromObject(int position) {
         PagerAdapter pagerAdapter = getAdapter();
        if(pagerAdapter instanceof MyFragmentStatePagerAdapter){
            MyFragmentStatePagerAdapter myFragmentStatePagerAdapter = (MyFragmentStatePagerAdapter)pagerAdapter;
            Fragment fragment = myFragmentStatePagerAdapter.getExistingItem(position);
            if(fragment!= null ){
                return fragment.getView();
            }
            else{
                return super.findViewFromObject(position);
            }
        }
        else{
           return super.findViewFromObject(position);
        }
    }
}
