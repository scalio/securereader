package info.guardianproject.zt.publish.sites;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import info.guardianproject.zt.R;
import info.guardianproject.zt.publish.Utility;
import info.guardianproject.zt.publish.UploadWorker;
import info.guardianproject.zt.publish.UploaderBase;
import info.guardianproject.zt.publish.model.Job;
import info.guardianproject.zt.publish.model.PublishJob;

import io.scal.secureshareui.controller.ArchiveSiteController;
import io.scal.secureshareui.controller.SiteController;

public class ArchiveUploader extends UploaderBase {
    private final String TAG = "ArchiveUploader";
    public static final int ERROR_NO_RENDER_FILE = 761276123;

	public ArchiveUploader(Context context, UploadWorker worker, Job job) {
		super(context, worker, job);
	}

	// FIXME move the render file checks into base class
    @Override
    public void start() {
        final SiteController controller = SiteController.getSiteController(ArchiveSiteController.SITE_KEY, mContext, mHandler, ""+mJob.getId());
        final PublishJob publishJob = mJob.getPublishJob();
        final String path = publishJob.getLastRenderFilePath();
        //final Auth auth = (new AuthTable()).getAuthDefault(mContext, ArchiveSiteController.SITE_KEY);
        if (Utility.stringNotBlank(path) && (new File(path)).exists()) {
            jobProgress(mJob, 0, mContext.getString(R.string.abc_action_mode_done)); //TODO get correct string
            HashMap<String, String> valueMap = publishJob.getMetadata();
            addValuesToHashmap(valueMap, "project.getTitle()", "project.getDescription()", path);
            //controller.upload(new Account(), valueMap);//auth.convertToAccountObject(), valueMap); // FIXME need to hookup Account to this
        } else {
            Log.d(TAG, "Can't upload to Internet Archive server, last rendered file doesn't exist.");
            // TODO get this error back to the activity for display 
            jobFailed(ERROR_NO_RENDER_FILE, "Can't upload to Internet Archive server, last rendered file doesn't exist."); // FIXME move to strings.xml
        }
    }
}
