package info.guardianproject.zt.z.rss.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.activities.LoadingDialogActivity;
import info.guardianproject.zt.z.rss.activities.youtube.YouTubePlayerViewActivity;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.models.youtube.*;
import info.guardianproject.zt.z.rss.utils.AsyncTask;
import info.guardianproject.zt.z.rss.utils.Font;
import info.guardianproject.zt.z.rss.utils.Network;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoListFragment extends BaseTabSupportFragment {
    public static final int TAB_ID = 1;
    private static final String TAG = "RadioZamaneh VideoListFragment";
    private PullToRefreshListView  videoListView = null;
    private  ImageLoader imageLoader = null;
    private ArrayAdapter<VideoItem> adapter = null;
    private GetVideoListAsyncTask getVideoListAsyncTask = null ;
    private boolean firstRefresh = true;
    private YouTubeResult lastPageOfResult = null;

    public VideoListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        imageLoader = ImageLoader.getInstance();
    }

	@Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.zvideo_list_fragment, container, false);
        videoListView = (PullToRefreshListView) fragmentView.findViewById(R.id.videoListView);
        initFields(inflater);

        initTabBar(fragmentView, TAB_ID);
        return fragmentView;
    }

    private void initFields(final LayoutInflater inflater){
        if(adapter == null){
            List<VideoItem> videoItems = new ArrayList<VideoItem>();
            adapter = new ArrayAdapter<VideoItem>(getActivity(), R.id.videoTitle, videoItems){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final VideoItem videoItem = getItem(position);
                    final Snippet snippet = videoItem.getSnippet();
                    final ViewHolder holder;
                    if (null == convertView) {
                        holder = new ViewHolder();
                        convertView = inflater.inflate(R.layout.zvideo_list_item, parent, false);
                        holder.textTitle = (TextView)convertView.findViewById(R.id.videoTitle);
                        holder.imageView =(ImageView)convertView.findViewById(R.id.imagePreview);
                        convertView.setTag(holder);
                    }
                    else{
                        holder = (ViewHolder) convertView.getTag();
                    }

                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(snippet!= null && snippet.getResourceId()!= null && !StringHelper.isEmptyString(snippet.getResourceId().getVideoId())){
                                Intent intent = new Intent(getActivity(), YouTubePlayerViewActivity.class);
                                intent.putExtra(YouTubePlayerViewActivity.YOUTUBE_VIDEO_ID_KEY, snippet.getResourceId().getVideoId());
                                intent.putExtra(YouTubePlayerViewActivity.ACTIVITY_STYLE, YouTubePlayerViewActivity.STYLE_BLACK);
                                startActivity(intent);
                            }
                        }
                    });

                    if(videoItem.getSnippet() != null && !StringHelper.isEmptyString(videoItem.getSnippet().getTitle())){
                        Typeface typeface = Font.getMitraFont(getActivity());
                        if(typeface != null){
                            holder.textTitle.setTypeface(typeface);
                        }
                        holder.textTitle.setText(videoItem.getSnippet().getTitle());
                    }
                    else{
                        holder.textTitle.setText("");
                    }
//                holder.textTitle.setText(String.valueOf(position));

                    Thumbnails thumbnails = snippet.getThumbnails();

                    holder.imageView.setImageDrawable(null);
                    if(thumbnails != null ){
                        ThumbnailUrl thumbnailUrl = thumbnails.getBestThumbnailUrl();
                        if(thumbnailUrl != null){
                            String url = thumbnailUrl.getUrl();
                            if (!StringHelper.isEmptyString(url) ) {
                                imageLoader.displayImage(url, holder.imageView, new DisplayImageOptions.Builder().cacheOnDisc(true).imageScaleType(ImageScaleType.IN_SAMPLE_INT).build());
                            }
                        }
                    }
                    //load next page
                    if(lastPageOfResult != null && lastPageOfResult.getVideoItems()!= null && lastPageOfResult.getVideoItems().size() >= YouTubeResult.MAX_COUNT_ON_PAGE){
                        VideoItem videoItemOnLastPage = lastPageOfResult.getVideoItems().get(YouTubeResult.MAX_COUNT_ON_PAGE - 10);
                        if(videoItemOnLastPage.equals(videoItem) && videoItemOnLastPage.getSnippet()!=null ){
                            refreshVideoList(false);
                        }
                    }
                    return convertView;
                }
            };
        }
        videoListView.setAdapter(adapter);
        videoListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                lastPageOfResult = null;
                refreshVideoList(true);
            }
        });
    }

    @Override
    public void refresh(Context context) {
        if(firstRefresh || adapter.isEmpty() ){
            lastPageOfResult = null;
            refreshVideoList(true);
            firstRefresh = false;
        }
    }

    private void refreshVideoList(final boolean showLoadingDialog){
        if(Network.checkInternetConnectivity(getActivity())){
            if(getVideoListAsyncTask == null){

                String url = Constants.YOUTUBE_VIDEO_LIST + YouTubeResult.MAX_RESULTS_PARAM ;
                if(!showLoadingDialog && lastPageOfResult!= null && !StringHelper.isEmptyString(lastPageOfResult.getNextPageToken())){
                    url = url + YouTubeResult.NEXT_PAGE_TOKEN_PARAM + lastPageOfResult.getNextPageToken();
                }

                getVideoListAsyncTask = new GetVideoListAsyncTask(showLoadingDialog);
                getVideoListAsyncTask.execute(url);
            }
        }
        else{
            if(videoListView != null){
                videoListView.onRefreshComplete();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(getVideoListAsyncTask != null){
            getVideoListAsyncTask.cancel(true);
            getVideoListAsyncTask = null;
        }
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView textTitle;
    }

    private class GetVideoListAsyncTask extends AsyncTask<String, Void, YouTubeResult> {
        private boolean showLoadingDialog = true;

        public GetVideoListAsyncTask(boolean showLoadingDialog){
            this.showLoadingDialog =showLoadingDialog;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(showLoadingDialog){
                Intent intent = new Intent(getActivity(), LoadingDialogActivity.class);
                startActivity(intent);
            }
        }

        @Override
        protected YouTubeResult doInBackground(String... params) {
            String url = params[0];
            YouTubeResult youTubeResult = null;
            if (isCancelled()) return youTubeResult;
            String jsonString = null;
            try {
                jsonString = Network.getDataViaHttp(url);
            } catch (IOException e) {
                Log.e(TAG,"GetVideoListAsyncTask load video list error",e);
            }
            if(!StringHelper.isEmptyString(jsonString)){
                Gson gson = new Gson();
                youTubeResult = gson.fromJson(jsonString, YouTubeResult.class);
            }
            return youTubeResult;
        }

        @Override
        protected void onPostExecute(YouTubeResult result) {
            try {
                if (showLoadingDialog && LoadingDialogActivity.getInstanceOfActivity()!= null) {
                    LoadingDialogActivity.getInstanceOfActivity().setShowingDialog(false);
                   LoadingDialogActivity.getInstanceOfActivity().finish();
                }
            } catch (Exception e) {
                Log.e(TAG,"GetVideoListAsyncTask onPostExecute",e);
            }
            if(videoListView != null){
                videoListView.onRefreshComplete();
            }
            if (isCancelled()) return;
            if(result != null && result.getVideoItems()!= null){
                if(showLoadingDialog){
                    adapter.clear();
                }
                for(VideoItem videoItem : result.getVideoItems()){
                    if(adapter.getPosition(videoItem)< 0){
                        adapter.add(videoItem);
                    }
                }
                if(showLoadingDialog && !result.getVideoItems().isEmpty()){
                    adapter.notifyDataSetChanged();
                }
            }
            lastPageOfResult = result;
            getVideoListAsyncTask = null;

//            if(showLoadingDialog){
//                refreshVideoList(false);
//            }
        }
    }

}

