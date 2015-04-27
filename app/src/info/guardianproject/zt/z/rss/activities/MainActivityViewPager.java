package info.guardianproject.zt.z.rss.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.adapters.MainAdapter;
import info.guardianproject.zt.z.rss.fragments.BaseRefreshFragment;
import info.guardianproject.zt.z.rss.fragments.Tab1ContainerFragment;
import info.guardianproject.zt.z.rss.fragments.Tab4ContainerFragment;
import info.guardianproject.zt.z.rss.utils.CrashReporter;

public class MainActivityViewPager extends FragmentActivity {
/*
    private MyTransitionViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.zmain_activity);

        String packageName = getPackageName();
        String versionName = getPackageVersion(getApplicationContext());
        String crashEmailSubject = "Error: " + packageName + " (" + versionName + ")";
        CrashReporter crashReporter = new CrashReporter(this, crashEmailSubject);
        Thread.setDefaultUncaughtExceptionHandler(crashReporter);

        setupViewPager();
	}
    public String  getPackageVersion(Context context) {
        try {
            // This should be the same as the package from AndroidManifest.xml
            String applicationPackage = R.class.getPackage().getName();
            return context.getPackageManager().getPackageInfo(applicationPackage, 0).versionName;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try{
            return super.dispatchTouchEvent(ev);
        }
        catch (Exception e){
            return  false;
        }

    }

    private void setupViewPager() {
        viewPager = (MyTransitionViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.CubeOut);

//        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.Accordion);
//        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.CubeIn);
//                viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.Standard);
//        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.RotateUp);
//        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.Tablet);
//        viewPager.setTransitionEffect(TransitionViewPager.TransitionEffect.CubeIn);
        viewPager.setAdapter(new MainAdapter(getSupportFragmentManager()));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
//                Log.e("viewPager", "OnPageChangeListener onPageScrolled i="+String.valueOf(i));
            }

            @Override
            public void onPageSelected(int i) {
//                Log.e("viewPager", "OnPageChangeListener onPageSelected i="+String.valueOf(i));
                // call refresh method in fragment to load/refresh data
                try {
                    MainAdapter adapter = ((MainAdapter) viewPager.getAdapter());
                    BaseRefreshFragment fragment = (BaseRefreshFragment) adapter.getExistingItem(i);
                    fragment.refresh(MainActivityViewPager.this);
                } catch (Exception e) {
                    Log.e("RadioZamaneh MainActivityViewPager", "Can't call refresh for fragment in pager. Error " + e.toString());
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
//                Log.e("viewPager", "OnPageChangeListener onPageScrollStateChanged i="+String.valueOf(i));
            }
        });
    }

    public MyTransitionViewPager getViewPager(){
      return viewPager;
    }

    private void equalHomePressed(){
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    public void onBackPressed() {
        if(viewPager == null ){
//            super.onBackPressed();
            equalHomePressed();
            return;
        }
        switch (viewPager.getCurrentItem()) {
            case Tab1ContainerFragment.TAB_ID:
                if (Tab1ContainerFragment.self != null && !Tab1ContainerFragment.self.restoreFromBackStack()) {
//                    super.onBackPressed();
                equalHomePressed();
                }
                break;
            case Tab4ContainerFragment.TAB_ID:
                if (Tab4ContainerFragment.self != null && !Tab4ContainerFragment.self.restoreFromBackStack()) {
//                    super.onBackPressed();
                    equalHomePressed();
                }
                break;
            default:
//                super.onBackPressed();
            equalHomePressed();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            MainAdapter adapter = ((MainAdapter) viewPager.getAdapter());
            Fragment fragment = adapter.getExistingItem(viewPager.getCurrentItem());
            fragment.onActivityResult(requestCode, resultCode, data);
        } catch (Exception e) {
            Log.e("RadioZamaneh MainActivityViewPager", "Can't call onActivityResult for fragment in pager. Error " + e.toString());
        }
    }
*/
}