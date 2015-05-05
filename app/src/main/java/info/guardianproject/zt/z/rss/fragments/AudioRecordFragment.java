package info.guardianproject.zt.z.rss.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.*;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.activities.LoadingDialogActivity;
import info.guardianproject.zt.z.rss.listeners.OnSwipeTouchListener;
import info.guardianproject.zt.z.rss.utils.AmazonS3FileUploadService;
import info.guardianproject.zt.z.rss.utils.AsyncTask;
import info.guardianproject.zt.z.rss.utils.Network;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AudioRecordFragment extends Fragment {
    private static final String TAG = "RadioZamaneh AudioRecordFragment";
    private static final String AUDIO_RECORDS_DIRECTORY = "audio_records";
    private static final String FILE_EXT = ".mp4";
    public static boolean sDisableFragmentAnimations = false;

    private String mFileName = null;
    private String timeForRecord = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_SSS");
    private SimpleDateFormat simpleDateFormatForDisplay = new SimpleDateFormat(" dd MMM yyyy, hh:mm a");
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private boolean mStartRecording = true;
    private long startTime = 0;
    private TextView timerView = null;
    private ImageView recordButton = null;
    private ImageView pauseButton = null;
    private ListView recordListView = null;
    private Handler timerHandler = new Handler();
    private ArrayAdapter<SelectableFile> fileArrayAdapter = null;
    private SelectableFile selectedFileForPlaying = null;
    private S3PutObjectTask s3PutObjectTask = null;

    private View.OnClickListener recordListener = new View.OnClickListener() {
        public void onClick(View v) {
            onRecord(mStartRecording);
            mStartRecording = !mStartRecording;
            if (!mStartRecording) {
                startTime = System.currentTimeMillis();
                timerHandler.postDelayed(timerRunnable, 0);
                recordButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
            } else {
                recordButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                timerHandler.removeCallbacks(timerRunnable);
            }
        }
    };

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            if(timerView != null){
                timerView.setText(String.format("%02d:%02d", minutes, seconds));
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    public AudioRecordFragment() {}

    private List<SelectableFile> getRecordFilesList(){
        List<SelectableFile> files = new ArrayList<SelectableFile>();
        final File folder = getActivity().getExternalFilesDir(AUDIO_RECORDS_DIRECTORY);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile() && fileEntry.getName().endsWith(FILE_EXT)) {
                files.add(new SelectableFile(fileEntry));
            }
        }
       return files;
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        sDisableFragmentAnimations = true;
        if(outState != null){
            outState.putString("Time", timerView.getText().toString());
        }
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        sDisableFragmentAnimations = false;
        if(savedInstanceState != null){
            String timeForRecord = savedInstanceState.getString("Time","00:00");
            timerView.setText(timeForRecord);
        }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.zaudio_record_fragment, container, false);

        FrameLayout recordFrameLayout = (FrameLayout) fragmentView.findViewById(R.id.recordFrameLayout);
        recordFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(StringHelper.isEmptyString(mFileName) ){
                    Toast.makeText(getActivity(), R.string.recordAudioFirst, Toast.LENGTH_SHORT).show();
                    return;
                }
                sendFile(mFileName);

//                Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", uri);
//                Tab4ContainerFragment.self.restoreFromBackStack();
            }
        });

        timerView = (TextView)  fragmentView.findViewById(R.id.timerView);

        recordButton = (ImageView) fragmentView.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(recordListener);

        pauseButton  = (ImageView) fragmentView.findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(recordListener);

        if(mStartRecording){
            recordButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
        }
        else{
            recordButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
        }
        final OnSwipeTouchListener onSwipeTouchListener = new OnSwipeTouchListener(getActivity()){
            @Override
            public void onSwipeRight() {
                Tab4ContainerFragment.self.restoreFromBackStack();
            }
        };

        recordListView  = (ListView) fragmentView.findViewById(R.id.recordListView);
        if(fileArrayAdapter == null){
            fileArrayAdapter = new ArrayAdapter<SelectableFile>(getActivity(),R.id.textFile, getRecordFilesList()){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final SelectableFile file  = getItem(position);
                    View view = getActivity().getLayoutInflater().inflate(R.layout.zrecord_file_list_item, parent, false);
                    TextView textFile = (TextView)view.findViewById(R.id.textFile);
//                    Typeface typeface = Font.getMitraFont(getActivity());
//                    if(typeface != null){
//                        textFile.setTypeface(typeface);
//                    }
                    ImageView  imagePlay = (ImageView)view.findViewById(R.id.imagePlay);
                    ImageView  imagePause = (ImageView)view.findViewById(R.id.imagePause);
                    if(file.isSelected()){
                        imagePlay.setVisibility(View.GONE);
                        imagePause.setVisibility(View.VISIBLE);
                    }
                    else{
                        imagePlay.setVisibility(View.VISIBLE);
                        imagePause.setVisibility(View.GONE);
                    }
                    if(file.getFile().exists() && file.getFile().isFile() && file.getFile().lastModified() != 0){
                        textFile.setText(getString(R.string.recordingOf)+simpleDateFormatForDisplay.format(new Date(file.getFile().lastModified())));
                    }
                    final View.OnClickListener clickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(selectedFileForPlaying != null && selectedFileForPlaying.equals(file)){
                                if(file.isSelected()){
                                    selectedFileForPlaying = null;
                                    setStopState(file);
                                    stopPlay();
                                }
                                else{
                                    selectedFileForPlaying = file;
                                    setPlayedState(file);
                                    playAudio(file);
                                }
                            }
                            else{
                                if(selectedFileForPlaying != null && selectedFileForPlaying.isSelected()){
                                    selectedFileForPlaying.setSelected(false);
                                }
                                selectedFileForPlaying = file;
                                setPlayedState(file);
                                playAudio(file);
                            }
                        }
                    };
                    imagePlay.setOnClickListener(clickListener);
                    imagePause.setOnClickListener(clickListener);

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //show action dialog
                            if (!mStartRecording) {
                                stopRecording();
                            }

                            if(file.isSelected()){
                                selectedFileForPlaying = null;
                                setStopState(file);
                                stopPlay();
                            }

                            showActionsDialog(file);
                        }
                    });

                    return view;
                }
            };
            fileArrayAdapter.sort(new Comparator<SelectableFile>() {
                @Override
                public int compare(SelectableFile lhs, SelectableFile rhs) {
                    if(lhs == null || rhs == null){
                        return 0;
                    }
                    Date l = new Date(lhs.getFile().lastModified());
                    Date r = new Date(rhs.getFile().lastModified());
                    return  (-1) * l.compareTo(r);  //last record at the top
                }
            });
            recordListView.setAdapter(fileArrayAdapter);
        }




        fragmentView.setOnTouchListener(onSwipeTouchListener);

        return fragmentView;
    }




    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
//            sendButton.setVisibility(View.VISIBLE);
        }
    }


    private void playAudio(final SelectableFile file){
        if(file == null || file.getFile() == null || !file.getFile().exists()) {
            Toast.makeText(getActivity(), R.string.recordAudioFirst, Toast.LENGTH_SHORT).show();
            return ;
        }

        if(!mStartRecording){
            //stop record
            recordListener.onClick(null);
        }

        if(mPlayer != null){
            mPlayer.setOnCompletionListener(null);
            try{
                if(mPlayer.isPlaying()){
                    mPlayer.stop();
                }
            }catch (Exception e){
                Log.e(TAG, "playAudio stopPlay Error", e) ;
            }
            mPlayer.reset();
        }
        else{
            mPlayer = new MediaPlayer();
        }

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mPlayer.setDataSource(file.getFile().getAbsolutePath());
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (selectedFileForPlaying != null && selectedFileForPlaying.isSelected() && file.equals(selectedFileForPlaying)) {
                        mp.start();
                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                if (selectedFileForPlaying != null && selectedFileForPlaying.isSelected() && file.equals(selectedFileForPlaying)) {
                                    setStopState(file);
                                }
                            }
                        });
                    }
                }
            });
            mPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer Error", e);
        }

    }

    private void stopPlay(){
        if(mPlayer != null){
            mPlayer.setOnCompletionListener(null);
            try{
                if(mPlayer.isPlaying()){
                    mPlayer.stop();
                }
            }catch (Exception e){
                Log.e(TAG, "playAudio stopPlay Error", e) ;
            }
            mPlayer.reset();
        }
    }

    private void setStopState(final SelectableFile file){
        if(file != null){
            file.setSelected(false);
            fileArrayAdapter.notifyDataSetChanged();
        }
    }
    private void setPlayedState(final SelectableFile file){
        if(file != null){
            file.setSelected(true);
            fileArrayAdapter.notifyDataSetChanged();
        }
    }

    private void startRecording() {

        mFileName = getActivity().getExternalFilesDir(AUDIO_RECORDS_DIRECTORY) + "/"+simpleDateFormat.format(new Date()) + FILE_EXT;
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.e(TAG, "MediaRecorder error! id_error="+ what);
                //TODO
            }
        });
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    private void stopRecording() {
        if(mRecorder != null){

            if(!mStartRecording){
                mRecorder.stop();
            }
            mRecorder.release();
            mRecorder = null;
        }
        if(fileArrayAdapter!= null && mFileName != null && !mStartRecording){
            SelectableFile selectableFile = new SelectableFile(new File(mFileName));
            if(fileArrayAdapter.getPosition(selectableFile) < 0){
                fileArrayAdapter.insert(selectableFile, 0);
                fileArrayAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecording();
        stopPlay();
    }

    @Override
    public void onDestroy() {
        stopRecording();
        stopPlay();
        if(mPlayer != null){
            mPlayer.release();
            mPlayer = null;
        }
        selectedFileForPlaying = null;
        super.onDestroy();
    }

    private void showActionsDialog(final SelectableFile file) {

        final Dialog builder = new Dialog(getActivity());
        builder.setCancelable(true);
        final LayoutInflater inflater = builder.getLayoutInflater();
        ArrayList<String> actions = new ArrayList<String>();
        actions.add(getActivity().getResources().getString(R.string.sendAction));
        actions.add(getActivity().getResources().getString(R.string.deleteAction));

        final ArrayAdapter<String> actionsAdapter = new ArrayAdapter<String>(builder.getContext(), R.layout.zaction_dialog_item, R.id.actionValue, actions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent );
                View.OnClickListener actionListener = null;
                switch (position){
                    case 0:
                        actionListener =  new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //send
                                builder.dismiss();

                                if(file == null || file.getFile() == null){
                                    Toast.makeText(getActivity(), R.string.recordAudioFirst, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                sendFile(file.getFile().getAbsolutePath());
//                                Feedback.sendEmailWithAttach(getActivity(), getResources().getString(R.string.sendEmail),"", "", uri);

                            }
                        };
                        break;

                    case 1:
                        //delete
                        actionListener =  new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                builder.dismiss();
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                //delete
                                                if(file == null || file.getFile() == null || !file.getFile().exists()) {
                                                    return;
                                                }
                                                fileArrayAdapter.remove(file);
                                                file.getFile().delete();
                                                fileArrayAdapter.notifyDataSetChanged();
                                                break;

                                            case DialogInterface.BUTTON_NEGATIVE:
                                                break;
                                        }
                                    }
                                };

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(getActivity().getResources().getString(R.string.confirmDelete)).setPositiveButton(getActivity().getResources().getString(R.string.yes), dialogClickListener)
                                        .setNegativeButton(getActivity().getResources().getString(R.string.no), dialogClickListener).show();
                            }
                        };
                        break;
                }
                view.setOnClickListener(actionListener);

                return view;
            }


        };
        View dialogView = inflater.inflate(R.layout.zaction_dialog, null, false);
        ListView listView = (ListView) dialogView.findViewById(R.id.actionListView);
        listView.setAdapter(actionsAdapter);
        builder.setContentView(dialogView);
        builder.setTitle(getActivity().getResources().getString(R.string.selectAction));
        builder.show();
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
//                Tab4ContainerFragment.self.restoreFromBackStack();
            }
            s3PutObjectTask = null;
        }
    }

    private class SelectableFile {
          private File file = null;
          private boolean selected = false;

        private SelectableFile(File file) {
            this.file = file;
        }

        private File getFile() {
            return file;
        }

        private void setFile(File file) {
            this.file = file;
        }

        private boolean isSelected() {
            return selected;
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SelectableFile that = (SelectableFile) o;

            if (file != null ? !file.equals(that.file) : that.file != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return file != null ? file.hashCode() : 0;
        }
    }
}
