package info.guardianproject.zt.rss.utils;

import android.content.Context;
import android.util.Log;

import info.guardianproject.zt.z.rss.constants.Constants;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class AmazonS3FileUploadService2 {
	private static final String TAG = "AmazonS3FileUploadService2";
	
    // USING: https://github.com/awslabs/aws-sdk-android-samples/blob/master/S3_Uploader/src/com/amazonaws/demo/s3uploader/S3UploaderActivity.java
    private final String UUID_CONSTANT = "rz-";
    private static AmazonS3FileUploadService2 service;

    
    private static final MediaType MEDIA_TYPE = MediaType.parse("");
    private static String S3_ENDPOINT = "https://s3-eu-west-1.amazonaws.com";
    private static String S3_BUCKET = StringHelper.shuffleDe("54ttehre",4); //andapp01
    private String S3_ACCESS_KEY = StringHelper.shuffleDe("SN894KXZQC9O4ULLCKMC",2); //AKIAJJS2M7AOXVI276LQ
    private String S3_SECRET_KEY = StringHelper.shuffleDe("|GSMm39RGTd{ewn;mNJkip4jSPvpTNhqXqlk;{Kw",2);

    private AmazonS3FileUploadService2(Context context) {
    }

    public static synchronized AmazonS3FileUploadService2 getInstance(Context context) {
        if (service == null) {
            service = new AmazonS3FileUploadService2(context);
        }
        return service;
    }
    
	public boolean uploadFile(String mediaPath) {
		Log.d(TAG, "Upload media: Entering upload"); 
        
		File file = new File(mediaPath);
		if (!file.exists()) {
			Log.d(TAG, "Invalid File");
			return false;
		}
		
		String fileName = file.getName();
		String urlPath = UUID_CONSTANT + UUID.randomUUID().toString();;
		String url = S3_BUCKET  + "/" + urlPath + "/" + fileName;
		Log.d(TAG, "uploading to url: " + url);

		Request.Builder builder = new Request.Builder()
				.url(url)
				.put(RequestBody.create(MEDIA_TYPE, file))
				.addHeader("Accept", "*/*")
                .addHeader("x-amz-auto-make-bucket", "1")
				.addHeader("authorization", "LOW " + S3_ACCESS_KEY + ":" + S3_SECRET_KEY);
		
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
					int j = 2;
				} else {	
					didUpload = true;
					int i = 1;
				}
			} catch (IOException e) {
				try {
					Log.d(TAG, response.body().string());
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
