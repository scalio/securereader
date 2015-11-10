package info.guardianproject.zt.rss.activities;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.utils.AsyncTask;

public class SplashScreenActivity extends Activity {
    private WaitAndShowMainActivityAsyncTask waitAndShowMainActivityAsyncTask = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zsplashscreen_activity);

        waitAndShowMainActivityAsyncTask = new WaitAndShowMainActivityAsyncTask();
        waitAndShowMainActivityAsyncTask.execute();
    }



    private void showMainActivity() {
        SplashScreenActivity.this.finish();
    }

    private class WaitAndShowMainActivityAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            //show splash screen
            if (isCancelled()) return null;
            try {
                Thread.sleep(Constants.SPLASH_TIME);
            } catch (InterruptedException e) {
                Log.e("Tribune Zamaneh error", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isCancelled()) return;
            showMainActivity();
            waitAndShowMainActivityAsyncTask= null;
        }
    }



    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        android.view.Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(waitAndShowMainActivityAsyncTask != null){
            waitAndShowMainActivityAsyncTask.cancel(true);
            waitAndShowMainActivityAsyncTask= null;
        }
    }

}
