package info.guardianproject.zt.z.rss.fragments;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.zt.z.rss.activities.LoadingDialogActivity;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.utils.*;

import java.io.FileOutputStream;
import java.io.IOException;

public class ShareFragment extends Fragment {
    private static final String TAG = "RadioZamaneh ShareFragment";

    public static boolean sDisableFragmentAnimations = false;
    private Uri mCapturedImageURI = null;
    private Uri mCapturedVideoURI = null;

    private S3PutObjectTask s3PutObjectTask = null;
    public ShareFragment() { }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mCapturedImageURI = savedInstanceState.getParcelable("pictureUri");
            mCapturedVideoURI = savedInstanceState.getParcelable("videoUri");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle)
    {
        sDisableFragmentAnimations = true;
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("pictureUri", mCapturedImageURI);
        bundle.putParcelable("videoUri", mCapturedVideoURI);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        sDisableFragmentAnimations = false;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (sDisableFragmentAnimations) {
            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.zshare_fragment, container, false);

        Typeface typeface = Font.getMitraBdFont(getActivity());
        TextView photoText = (TextView)fragmentView.findViewById(R.id.sendText);
        photoText.setTypeface(typeface);

        TextView videoText = (TextView)fragmentView.findViewById(R.id.sendText);
        videoText.setTypeface(typeface);

        TextView recordAudioText = (TextView)fragmentView.findViewById(R.id.sendText);
        recordAudioText.setTypeface(typeface);

        TextView sendText = (TextView)fragmentView.findViewById(R.id.sendText);
        sendText.setTypeface(typeface);

        Button selectPhotoButton = (Button)fragmentView.findViewById(R.id.selectPhotoButton);
        selectPhotoButton.setTypeface(typeface);
        selectPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoPickerIntent.setType("image/*");
                getActivity().startActivityForResult(photoPickerIntent, Constants.INTENT_REQUEST_CODE_SELECT_PHOTO);
            }
        });

        Button takePhotoButton = (Button)fragmentView.findViewById(R.id.takePhotoButton);
        takePhotoButton.setTypeface(typeface);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeFromCamera();
            }
        });

        Button takeVideoButton = (Button)fragmentView.findViewById(R.id.takeVideoButton);
        takeVideoButton.setTypeface(typeface);
        takeVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeVideoFromCamera();
            }
        });


        Button selectVideoButton = (Button)fragmentView.findViewById(R.id.selectVideoButton);
        selectVideoButton.setTypeface(typeface);
        selectVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                photoPickerIntent.setType("video/*");
                getActivity().startActivityForResult(photoPickerIntent, Constants.INTENT_REQUEST_CODE_SELECT_VIDEO);
            }
        });

        Button recordButton = (Button)fragmentView.findViewById(R.id.recordButton);
        recordButton.setTypeface(typeface);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sDisableFragmentAnimations = false;
                AudioRecordFragment audioRecordFragment = new AudioRecordFragment();
                FragmentTransaction ft = Tab4ContainerFragment.childManager.beginTransaction();
                //ft.setCustomAnimations( R.anim.slide_in_left, R.anim.slide_out_right,  R.anim.slide_in_right, R.anim.slide_out_left);
                ft.replace(R.id.tab4content, audioRecordFragment);
                ft.addToBackStack(audioRecordFragment.getClass().getSimpleName());
                Tab4ContainerFragment.self.stack.push(audioRecordFragment);
                ft.commit();

            }
        });

        Button sendTextButton = (Button)fragmentView.findViewById(R.id.sendTextButton);
        sendTextButton.setTypeface(typeface);
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sDisableFragmentAnimations = false;
                WriteTextFragment writeTextFragment = new WriteTextFragment();
                FragmentTransaction ft = Tab4ContainerFragment.childManager.beginTransaction();
                //ft.setCustomAnimations( R.anim.slide_in_left, R.anim.slide_out_right,  R.anim.slide_in_right, R.anim.slide_out_left);
                ft.replace(R.id.tab4content, writeTextFragment);
                ft.addToBackStack(writeTextFragment.getClass().getSimpleName());
                Tab4ContainerFragment.self.stack.push(writeTextFragment);
                ft.commit();
            }
        });
        return fragmentView;
    }
    private String getRealPathFromURI(Uri contentURI) {
        Cursor cursor = getActivity().getContentResolver().query(contentURI, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String path = cursor.getString(idx);
        cursor.close();
        return path;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.INTENT_REQUEST_CODE_SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String filePath = getRealPathFromURI(getActivity(),selectedImage);
                    sendFile(filePath);
//                    Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", selectedImage);
                }
                break;
            case Constants.INTENT_REQUEST_CODE_SELECT_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedVideo = data.getData();
                    sendFile(getRealVideoPathFromURI(getActivity(),selectedVideo));

//                    Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", selectedVideo);
                }
                break;
            case Constants.INTENT_REQUEST_CODE_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    if (mCapturedImageURI != null) {
                        try {
                            ExifInterface exif = new ExifInterface(getRealPathFromURI(mCapturedImageURI));
                            int pictureOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                            switch (pictureOrientation) {
                                case 3:
                                    // rotate 180
                                    rotateGalleryImage(180);
                                    break;
                                case 6:
                                    // rotate 90
                                    rotateGalleryImage(90);
                                    break;
                                case 8:
                                    // rotate 270
                                    rotateGalleryImage(270);
                                    break;
                            }
                        } catch (IOException e) {
                            Log.e(TAG,"IOException Error",e);
                        }
                        sendFile(getRealPathFromURI(getActivity(),mCapturedImageURI));

//                        Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", mCapturedImageURI);
                    }
                }
                break;
            case Constants.INTENT_REQUEST_CODE_TAKE_VIDEO:
                if (resultCode == Activity.RESULT_OK) {
                    if (mCapturedVideoURI != null) {
                        sendFile(getRealVideoPathFromURI(getActivity(), mCapturedImageURI));
//                        Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", mCapturedVideoURI);
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }

     private void sendFile(String filePath){
         if(StringHelper.isEmptyString(filePath)) return;
         if(Network.checkInternetConnectivity(getActivity())){
             if(s3PutObjectTask != null){
                 s3PutObjectTask.cancel(true);
             }
             s3PutObjectTask = new S3PutObjectTask();
             s3PutObjectTask.execute(filePath);
         }
     }



    private void rotateGalleryImage(int angle) {
        String filePath = getRealPathFromURI(mCapturedImageURI);
        Bitmap originalBitmap = BitmapFactory.decodeFile(filePath);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true).compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void takeFromCamera() {
        //TODO NEED TEST!
        String fileName = "temp.jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        mCapturedImageURI = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
            cameraIntent.putExtra("return-data", false);
            getActivity().startActivityForResult(cameraIntent, Constants.INTENT_REQUEST_CODE_TAKE_PHOTO);
        } catch (ActivityNotFoundException anfe) {
            Toast.makeText(getActivity(), getResources().getString(R.string.notSupportCamera), Toast.LENGTH_LONG).show();
        }
    }


    public void takeVideoFromCamera() {
        String fileName = "temp.mp4";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        mCapturedVideoURI = getActivity().getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE );
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedVideoURI);
            cameraIntent.putExtra("return-data", false);
            getActivity().startActivityForResult(cameraIntent, Constants.INTENT_REQUEST_CODE_TAKE_VIDEO );
        } catch (ActivityNotFoundException anfe) {
            Toast.makeText(getActivity(), getResources().getString(R.string.notSupportCamera), Toast.LENGTH_LONG).show();
        }
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getRealVideoPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Video.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class S3PutObjectTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(!isCancelled()){
                Intent intent = new Intent(getActivity(), LoadingDialogActivity.class);
                startActivity(intent);
            }
        }

        protected Boolean doInBackground(String... path) {
            if(isCancelled()){
                return null;
            }
            String selectedImage = path[0];
            if (StringHelper.isEmptyString(selectedImage)) {
                return null;
            }

            Boolean result = false;
            try {
//                urlToFile = AmazonS3FileUploadService.getInstance(getActivity()).uploadPhoto2(selectedImage);
                result = AmazonS3FileUploadService.getInstance(getActivity()).uploadFile(selectedImage);
            } catch (Exception e) {
                Log.e("radioZamaneh", "Error", e);
            }

            return result;
        }

        protected void onPostExecute(Boolean result) {

            try {
                if(LoadingDialogActivity.getInstanceOfActivity()!= null){
                    LoadingDialogActivity.getInstanceOfActivity().setShowingDialog(false);
                    LoadingDialogActivity.getInstanceOfActivity().finish();
                }
            } catch (Exception e) {
                Log.e(TAG,"GetRssAsyncTask onPostExecute",e);
            }
            if(isCancelled()){
                return;
            }
            if(!result){
                 Toast.makeText(getActivity(),R.string.loadToS3Failed,Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getActivity(),R.string.loadToS3Ok,Toast.LENGTH_SHORT).show();
            }
            s3PutObjectTask = null;
        }
    }


    @Override
    public void onDestroy() {
        if(s3PutObjectTask != null){
            s3PutObjectTask.cancel(true);
            s3PutObjectTask = null;
        }
        super.onDestroy();
    }
}
