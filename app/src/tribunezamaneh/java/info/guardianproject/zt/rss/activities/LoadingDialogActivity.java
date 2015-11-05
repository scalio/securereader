package info.guardianproject.zt.rss.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import info.guardianproject.securereaderinterface.R;

public class LoadingDialogActivity extends Activity {
    private static LoadingDialogActivity instance = null;
    private ProgressDialog dialog = null;
    private boolean isShowingDialog  = true;
    public static LoadingDialogActivity getInstanceOfActivity(){
        return instance;
    }

    public boolean isShowingDialog(){
        return isShowingDialog;
    }

    public void setShowingDialog(boolean showingDialog) {
        isShowingDialog = showingDialog;
    }

    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
        if(isShowingDialog){
            dialog = ProgressDialog.show(this, "",getString(R.string.loading));
        }
        else{
            if(dialog != null){
                dialog.dismiss();
            }
            instance = null;
            finish();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if(dialog != null){
            dialog.dismiss();
        }
        instance = null;
    }

}
