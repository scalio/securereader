package info.guardianproject.zt.rss.utils;

import android.content.Context;
//import com.amazonaws.auth.AWSCredentials;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.PutObjectRequest;
import info.guardianproject.zt.z.rss.constants.Constants;

import java.io.File;
import java.io.FileNotFoundException;

public class AmazonS3FileUploadService {
    // USING: https://github.com/awslabs/aws-sdk-android-samples/blob/master/S3_Uploader/src/com/amazonaws/demo/s3uploader/S3UploaderActivity.java
    private static String S3_PHOTO_BUCKET = "";
    private final String UUID_CONSTANT = "rz-";
    private static AmazonS3FileUploadService service;

    //private AmazonS3Client s3Client;

    private AmazonS3FileUploadService(Context context ) {
        S3_PHOTO_BUCKET = StringHelper.shuffleDe("54ttehre",4); //andapp01
/*
        final AWSCredentials credentials = new BasicAWSCredentials( // (access_key_id, secret_access_key)
                StringHelper.shuffleDe("SN894KXZQC9O4ULLCKMC",2), //AKIAJJS2M7AOXVI276LQ
                StringHelper.shuffleDe("|GSMm39RGTd{ewn;mNJkip4jSPvpTNhqXqlk;{Kw",2)); //uIy9ijoVofLRntNQh2ngiHLk9lucybREP71kKQEz
        
        s3Client = new AmazonS3Client(credentials);*/
    }

    public static synchronized AmazonS3FileUploadService getInstance(Context context) {
        if (service == null) {
            service = new AmazonS3FileUploadService(context);
        }
        return service;
    }

    /**
     * Upload the photo and return the uuid identifying the remote file.
     *
     * @return the uuid of the image.
     */
    public boolean uploadFile(String filePath) throws FileNotFoundException {
    	
        final File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        /*
        String uuid = UUID_CONSTANT + UUID.randomUUID().toString();
        String fileName = file.getName();
        String fileExt = "";
        if(fileName.contains(".")){
            String filenameArray[] = fileName.split("\\.");
            fileExt = filenameArray[filenameArray.length-1];
        }
        final String remoteFileName = uuid + "."+fileExt;
        if(Constants.DEBUG){
            Log.i("RadioZamaneh", "Uploading " + file + " to " + S3_PHOTO_BUCKET + "/" + remoteFileName);
        }

        final PutObjectRequest request = new PutObjectRequest(
                S3_PHOTO_BUCKET, remoteFileName, file);

//        // Request server-side encryption.
//        ObjectMetadata objectMetadata = new ObjectMetadata();
//        objectMetadata.setServerSideEncryption(
//                ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
//        request.setMetadata(objectMetadata);
        s3Client.putObject(request);*/
        return true;
    }





    public String uploadPhoto(String filePath) throws FileNotFoundException {
        final File photoFile = new File(filePath);
        if (!photoFile.exists()) {
            throw new FileNotFoundException();
        }
        /*
        String uuid = UUID.randomUUID().toString();
        final String remoteFileName = getRemoteFileName(uuid);
        Log.i("RadioZamaneh", "Uploading " + photoFile + " to " + S3_PHOTO_BUCKET + "/" + remoteFileName);
        final PutObjectRequest request = new PutObjectRequest(
                S3_PHOTO_BUCKET, remoteFileName, photoFile);
        s3Client.putObject(request);
        String result = getRemoteFileUrl(uuid);
        result = result;*/
        String result = "result";
        return result;
    }


    private static String getRemoteFileName(String uuid) {
        return uuid + ".jpg";
    }

    public static String getRemoteFileUrl(String uuid) {
        if (uuid == null) {
            return null;
        }
        return "https://s3-eu-west-1.amazonaws.com/"+ S3_PHOTO_BUCKET+"/"+uuid;
//        return "http://" + S3_PHOTO_BUCKET + ".s3.amazonaws.com/" + getRemoteFileName(uuid);
//        return "http://" + "s3-eu-west-1.s3.amazonaws.com/"+S3_PHOTO_BUCKET+"/"+ getRemoteFileName(uuid);
    }
    public static String getRemoteFileUrl2(String fileName) {
        if (StringHelper.isEmptyString(fileName)) {
            return null;
        }
        return "http://" + S3_PHOTO_BUCKET + ".s3.amazonaws.com/" + fileName;
    }
}
