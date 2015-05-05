package info.guardianproject.zt.uiutil;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class AmazonS3FileUploadService {
	private static final String TAG = "AmazonS3FileUploadService";
	
    // USING: https://github.com/awslabs/aws-sdk-android-samples/blob/master/S3_Uploader/src/com/amazonaws/demo/s3uploader/S3UploaderActivity.java
    private final String UUID_CONSTANT = "rz-";
    private static AmazonS3FileUploadService service;
    
    private static final MediaType MEDIA_TYPE = MediaType.parse("");
    private static String S3_ENDPOINT;
    private static String S3_ACCESS_KEY;
    private static String S3_SECRET_KEY; 
    private static String S3_BUCKET;
    
    private Resources res;

    private AmazonS3FileUploadService(Context context) {
    	res = context.getResources();
    }

    public static synchronized AmazonS3FileUploadService getInstance(Context context) {
        if (service == null) {
            service = new AmazonS3FileUploadService(context);
        }
        return service;
    }
    
	public boolean uploadFile(String mediaPath) {
		Log.d(TAG, "Upload media: Entering upload");
		
		S3_ACCESS_KEY = res.getString(R.string.s3_key);
		S3_SECRET_KEY = res.getString(R.string.s3_secret);
		S3_BUCKET 	  = res.getString(R.string.s3_bucket);
		S3_ENDPOINT   = "http://" + S3_BUCKET + ".s3.amazonaws.com/";
        
		File file = new File(mediaPath);
		if (!file.exists()) {
			Log.d(TAG, "Invalid File");
			return false;
		}
		
		String fileName = file.getName();
		String urlPath = UUID_CONSTANT + UUID.randomUUID().toString();;
		String url = S3_ENDPOINT + urlPath + "/" + fileName;
		Log.d(TAG, "uploading to url: " + url);

		DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy H:mm:ss zzz", Locale.ENGLISH); /// Thu, 17 Nov 2005 18:49:58 GMT
		Date date = new Date();
		Log.d(TAG, "Date: " + dateFormat.format(date));
		
		try {
			S3_ACCESS_KEY = URLEncoder.encode(S3_ACCESS_KEY, "utf-8");
			S3_SECRET_KEY = URLEncoder.encode(S3_SECRET_KEY, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Request.Builder builder = new Request.Builder()
				.url(url)
				.put(RequestBody.create(MEDIA_TYPE, file))
				.addHeader("accept", "*/*")
                .addHeader("x-amz-auto-make-bucket", "1")
				.addHeader("content-type", "image/jpeg")
                .addHeader("x-amz-date", dateFormat.format(date))
				.addHeader("authorization", "AWS " + S3_ACCESS_KEY + ":" + S3_SECRET_KEY);
		
		Request request = builder.build();

		OkHttpClient client = new OkHttpClient();
		UploadFileTask uploadFileTask = new UploadFileTask(client, request);
		uploadFileTask.execute();
		return uploadFileTask.didUpload;
	}

	class UploadFileTask extends AsyncTask<String, String, Boolean> {
		private OkHttpClient client;
		private Request request;
		private Response response;
		protected boolean didUpload = false;

		public UploadFileTask(OkHttpClient client, Request request) {
			this.client = client;
			this.request = request;
		}

		@Override
		protected Boolean doInBackground(String... params) {
			Log.d(TAG, "Begin Upload");
			
			try {
				response = client.newCall(request).execute();
                Log.d(TAG, "response: " + response + ", body: " + response.body().string());
				if (!response.isSuccessful()) {
					//failed
					didUpload = false;
				} else {	
					didUpload = true;
				}
			} catch (IOException e) {
				try {
					if(null != response) {
						Log.d(TAG, response.body().string());
					} else {
						Log.d(TAG, "exception: " + e.getLocalizedMessage() + ", stacktrace: " + e.getStackTrace());
					}
				} catch (IOException e1) {
				    Log.d(TAG, "exception: " + e1.getLocalizedMessage() + ", stacktrace: " + e1.getStackTrace());
				}
			}

			return didUpload;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
		    super.onPostExecute(result);
		};
	}
}
