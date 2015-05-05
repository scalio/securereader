package info.guardianproject.zt.z.rss.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.activities.LoadingDialogActivity;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.dao.RssDao;
import info.guardianproject.zt.z.rss.models.RssItem;
import info.guardianproject.zt.z.rss.utils.AsyncTask;
import info.guardianproject.zt.z.rss.utils.Font;
import info.guardianproject.zt.z.rss.utils.Network;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class AudioListFragment extends BaseTabSupportFragment {
    public static final int TAB_ID = 2;
    private static final String TAG = "RZAudioListFrag";
    private PullToRefreshListView rssListView = null;
    private ArrayAdapter<RssItem> adapter = null;
    private GetRssAsyncTask getRssAsyncTask = null;
    private boolean firstRefresh = true;
    private RssItem selectedRssItem = null;
    private MediaPlayer mediaPlayer = null;


    public AudioListFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.zaudio_fragment, container, false);
        rssListView = (PullToRefreshListView) fragmentView.findViewById(R.id.rssListView);
        initFields(inflater);

        initTabBar(fragmentView, TAB_ID);
        return fragmentView;
	}


    private void initFields(final LayoutInflater inflater){
        if(adapter == null){
            /*
            List<RssItem> rssItems = new ArrayList<RssItem>();
            adapter = new ArrayAdapter<RssItem>(getActivity(), R.id.textRss, rssItems){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final RssItem rssItem = getItem(position);

                    final ViewHolder holder;
                    if (null == convertView) {
                        holder = new ViewHolder();
                        convertView = inflater.inflate(R.layout.zaudio_list_item, parent, false);
                        holder.textRss = (TextView)convertView.findViewById(R.id.textRss);
                        holder.imagePlay =(ImageView)convertView.findViewById(R.id.imagePlay);
                        holder.imagePause = (ImageView)convertView.findViewById(R.id.imagePause);
                        convertView.setTag(holder);
                    }
                    else{
                        holder = (ViewHolder) convertView.getTag();
                    }

                    if(rssItem.isSelected()){
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            convertView.setActivated(true);
                        }
                        else{
                            //TODO doesn't work
                            convertView.setSelected(true);
                        }

                        holder.imagePlay.setVisibility(View.GONE);
                        holder.imagePause.setVisibility(View.VISIBLE);
                    }
                    else{
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            convertView.setActivated(false);
                        }
                        else{
                            //TODO doesn't work
                            convertView.setSelected(false);
                        }
                        holder.imagePlay.setVisibility(View.VISIBLE);
                        holder.imagePause.setVisibility(View.GONE);
                    }

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(selectedRssItem != null && selectedRssItem.equals(rssItem)){
                                if(rssItem.isSelected()){
                                    selectedRssItem = null;
//                                stopPlay(rssItem);
                                    setStopState(rssItem);
                                    stopPlay();
                                }
                                else{
                                    selectedRssItem = rssItem;
                                    setPlayedState(rssItem);
                                    playAudio(rssItem);
                                }
                            }
                            else{
                                if(selectedRssItem != null && selectedRssItem.isSelected()){
                                    selectedRssItem.setSelected(false);
                                }
                                selectedRssItem = rssItem;
                                setPlayedState(rssItem);
                                playAudio(rssItem);
                            }
//                        adapter.notifyDataSetChanged();
                        }
                    });

                    Typeface typeface = Font.getMitraFont(getActivity());
                    if(typeface != null){
                        holder.textRss.setTypeface(typeface);
                    }
                    holder.textRss.setText(rssItem.getTitle());
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
                        holder.textRss.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    }
                    return convertView;
                }
            };*/
        }

        ImageView radioImage = new ImageView(getActivity());
        radioImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        radioImage.setAdjustViewBounds(true);
        radioImage.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
        radioImage.setImageResource(R.drawable.radio);
        //rssListView.addHeaderView(radioImage);

        rssListView.setAdapter(adapter);
        rssListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                refreshRssList();
            }
        });


    }

    private void playAudio(final RssItem rssItem){
        if(StringHelper.isEmptyString(rssItem.getAudioUrl())) {
            return;
        }
        if(mediaPlayer != null){
            mediaPlayer.setOnCompletionListener(null);
            try{
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }
            }catch (Exception e){
                Log.e(TAG, "playAudio stopPlay Error", e) ;
            }
            mediaPlayer.reset();
        }
        else{
            mediaPlayer = new MediaPlayer();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(getActivity(), Uri.parse(rssItem.getAudioUrl()));
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer Error", e);
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if(selectedRssItem != null && selectedRssItem.isSelected() && rssItem.equals(selectedRssItem)){
                    mp.start();
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if( selectedRssItem != null && selectedRssItem.isSelected() && rssItem.equals(selectedRssItem)){
                                setStopState(rssItem);
                            }
                        }
                    });
                }
            }
        });
    }

    private void stopPlay(){
        if(mediaPlayer != null){
            mediaPlayer.setOnCompletionListener(null);
            try{
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                }
            }catch (Exception e){
                Log.e(TAG, "playAudio stopPlay Error", e) ;
            }
            mediaPlayer.reset();
        }
    }

    private void setStopState(final RssItem rssItem){
        if(rssItem != null){
            rssItem.setSelected(false);
            adapter.notifyDataSetChanged();
        }
    }
    private void setPlayedState(final RssItem rssItem){
        if(rssItem != null){
            rssItem.setSelected(true);
            adapter.notifyDataSetChanged();
        }
    }


    private void refreshRssList(){
        if(Network.checkInternetConnectivity(getActivity())){
            if(getRssAsyncTask == null){
                getRssAsyncTask = new GetRssAsyncTask();
                getRssAsyncTask.execute();
            }
        }
        else{
            if(rssListView != null){
                rssListView.onRefreshComplete();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        setStopState();
//        stopPlay();
    }

    @Override
    public void onDestroy() {
        if(getRssAsyncTask != null){
            getRssAsyncTask.cancel(true);
            getRssAsyncTask = null;
        }
        stopPlay();
        if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
        selectedRssItem = null;
        super.onDestroy();
    }

    @Override
    public void refresh(Context context) {
        if(firstRefresh || adapter.isEmpty() ){
            refreshRssList();
            firstRefresh = false;
        }
    }

    private class GetRssAsyncTask extends AsyncTask<Void, Void, List<RssItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Intent intent = new Intent(getActivity(), LoadingDialogActivity.class);
            startActivity(intent);
        }

            @Override
        protected List<RssItem> doInBackground(Void... params) {
            if (isCancelled()) return null;
            List<RssItem> rssItems = RssDao.getRSS(Constants.RSS_AUDIO_URL);
            return rssItems;
        }

        @Override
        protected void onPostExecute(List<RssItem> result) {
            try {
                if(LoadingDialogActivity.getInstanceOfActivity()!= null){
                    LoadingDialogActivity.getInstanceOfActivity().setShowingDialog(false);
                    LoadingDialogActivity.getInstanceOfActivity().finish();
                }
            } catch (Exception e) {
                Log.e(TAG,"GetRssAsyncTask onPostExecute",e);
            }
            if(rssListView != null){
                rssListView.onRefreshComplete();
            }
            if (isCancelled()) return;
            if(result != null && !result.isEmpty()){
                adapter.clear();
                for(RssItem rssItem : result){
                    adapter.add(rssItem);
                }
                adapter.notifyDataSetChanged();
            }

            getRssAsyncTask = null;

        }
    }



    public static class ViewHolder {
        ImageView imagePlay;
        ImageView imagePause;
        TextView textRss;
    }
}
