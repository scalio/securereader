package info.guardianproject.zt.rss.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.zt.z.rss.activities.HelpActivity;
import info.guardianproject.zt.z.rss.activities.LoadingDialogActivity;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.dao.RssDao;
import info.guardianproject.zt.z.rss.models.RssItem;
import info.guardianproject.zt.z.rss.utils.*;
import info.guardianproject.zt.z.rss.views.StreamDrawable;

import java.util.ArrayList;
import java.util.List;

public class RssListFragment extends BaseRefreshFragment  {
    public static final String TAG = "RssListFragment";
    public static boolean sDisableFragmentAnimations = false;

    private PullToRefreshListView rssListView = null;
    private ArrayAdapter<RssItem> adapter = null;
    private GetRssAsyncTask getRssAsyncTask = null;
    private ImageLoader imageLoader = null;
    private boolean showHelp = true;
    private boolean showHelpOnChild = true;
    private boolean firstRefresh = true;

    private DataStorage dataStorage = null;

    public RssListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageLoader = ImageLoader.getInstance();
//        imageLoader.setShowPlaceholderBitmap(false);
        dataStorage = ((App) getActivity().getApplication()).getDataStorage();
    }

    @Override
    public void refresh(Context context) {
        if(firstRefresh || adapter.isEmpty()){
            refreshRssList();
            firstRefresh = false;
        }

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        sDisableFragmentAnimations = true;
        super.onSaveInstanceState(outState);

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
    public void onDestroyView() {
//        if(getRssAsyncTask != null && getRssAsyncTask.getDialog()!= null){
//            getRssAsyncTask.getDialog().setOnD
//        }
        super.onDestroyView();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

//        if (null != fragmentView) {
//            ((ViewGroup) fragmentView.getParent()).removeView(fragmentView);
//        } else {
            View fragmentView = inflater.inflate(R.layout.zrss_list_fragment, container, false);

            rssListView = (PullToRefreshListView) fragmentView.findViewById(R.id.rssListView);
//            rssListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);

            initFields(inflater);
//        }

//        refresh(getActivity());
        return fragmentView;
    }



    private void initFields(final LayoutInflater inflater){
        if(adapter == null){
            List<RssItem> rssItems = new ArrayList<RssItem>();
            adapter = new ArrayAdapter<RssItem>(getActivity(), R.id.textRss, rssItems){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    final RssItem rssItem = getItem(position);
                    if(rssItem.isDivider()){
                        convertView = inflater.inflate(R.layout.zrss_listview_item_divider, parent, false);
                        Typeface typeface = Font.getMitraBdFont(getActivity());
                        if(typeface != null){
                            TextView dividerText = (TextView)convertView.findViewById(R.id.dividerText);
                            dividerText.setTypeface(typeface);
                        }
                    }
                    else{
                        final ViewHolder holder;
                        if (null == convertView || convertView.getTag() == null) {
                            holder = new ViewHolder();
                            convertView = inflater.inflate(R.layout.zrss_listview_item, parent, false);
                            holder.textRss = (TextView)convertView.findViewById(R.id.textRss);
                            holder.imageRss =(ImageView)convertView.findViewById(R.id.imageRss);
                            holder.imageRssRoundedBorder = (ImageView)convertView.findViewById(R.id.imageRssRoundedBorder);


                            convertView.setTag(holder);
                        }
                        else{
                            holder = (ViewHolder) convertView.getTag();

                        }

                        convertView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dataStorage.setRssItem(rssItem);


                                ShowRssFragment showRssFragment = new ShowRssFragment();
                                Bundle bundle = new Bundle();
                                bundle.putBoolean(ShowRssFragment.SHOW_HELP_KEY, showHelpOnChild );
                                showRssFragment.setArguments(bundle);
                                FragmentTransaction ft = Tab1ContainerFragment.childManager.beginTransaction();
                                //ft.setCustomAnimations( R.anim.slide_in_left, R.anim.slide_out_right,  R.anim.slide_in_right, R.anim.slide_out_left);
                                ft.replace(R.id.tab1content, showRssFragment);
                                ft.addToBackStack(showRssFragment.getClass().getSimpleName());
                                Tab1ContainerFragment.self.stack.push(RssListFragment.this);
                                ft.commit();
                                showHelpOnChild = false;
//                        if(!StringHelper.isEmptyString(url)){
//                            if (!url.startsWith("http://") && !url.startsWith("https://"))
//                                url = "http://" + url;
//                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                            startActivity(browserIntent);
//                        }
                            }
                        });

                        holder.imageRss.setImageResource(R.drawable.image_placeholder);
                        holder.imageRss.setPadding(0,0,0,0);
                        holder.imageRssRoundedBorder.setVisibility(View.INVISIBLE);

                        String url = rssItem.getImageUrl();
                        if (!StringHelper.isEmptyString(url) ) {
                            holder.imageRssRoundedBorder.setVisibility(View.VISIBLE);
                            imageLoader.displayImage(url, holder.imageRss, new DisplayImageOptions.Builder().cacheOnDisc(true).imageScaleType(ImageScaleType.IN_SAMPLE_INT).build(), new ImageLoadingListener() {
                                @Override
                                public void onLoadingStarted(String imageUri, View view) {
                                }

                                @Override
                                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                    Log.e(TAG, "Error while loading theme image: " + failReason.toString());
                                    holder.imageRssRoundedBorder.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                    // holder.themePictureImageView.setImageDrawable(new StreamDrawable(loadedImage, 18, false));
                                    holder.imageRss.setImageDrawable(new StreamDrawable(loadedImage, 0.5f));

                                    int paddingInDp = 5;
                                    final float scale = getResources().getDisplayMetrics().density;
                                    int paddingInPx = (int) (paddingInDp * scale + 0.5f);
                                    holder.imageRss.setPadding(paddingInPx,paddingInPx,paddingInPx,paddingInPx);

                                    holder.imageRssRoundedBorder.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onLoadingCancelled(String imageUri, View view) {
//                            holder.imageRssRoundedBorder.setVisibility(View.INVISIBLE);
                                }
                            });
                        }

                        Typeface typeface = Font.getMitraFont(getActivity());
                        if(typeface != null){
                            holder.textRss.setTypeface(typeface);
                        }
                        holder.textRss.setText(rssItem.getTitle());
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
                            holder.textRss.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                        }
                    }
                    return convertView;
                }
            };
            refresh(getActivity());
        }
        rssListView.setAdapter(adapter);
        rssListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                refreshRssList();
            }
        });
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
            if(showHelp && RssListFragment.this.isVisible()){
                showHelp();
            }

        }
    }

    private void showHelp(){
          Intent intent = new Intent(getActivity(), HelpActivity.class);
          startActivity(intent);
          showHelp = false;
    }

    @Override
    public void onDestroy() {
        if(getRssAsyncTask != null){
            getRssAsyncTask.cancel(true);
            getRssAsyncTask = null;
        }
        dataStorage.setRssItem(null);
        super.onDestroy();
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
//            Log.e(TAG, "doInBackground Start first url" +" time:"+ new Date(System.currentTimeMillis()).toString());
            List<RssItem> rssItems = RssDao.getRSS(Constants.RSS_URL);
//            Log.e(TAG, "doInBackground get first 15 from URL1" +" time:"+ new Date(System.currentTimeMillis()).toString());
            //get first 15 from URL1
            if(rssItems != null && rssItems.size() > 15){
                while(rssItems.size() > 15 ){
                    rssItems.remove(rssItems.size()-1);
                }
            }
//            Log.e(TAG, "doInBackground Start second url" +" time:"+ new Date(System.currentTimeMillis()).toString());
            //TODO commited for test
            //add first 10 from URL2
            List<RssItem> rssItems2 = RssDao.getRSS(Constants.RSS_URL2);
//            Log.e(TAG, "doInBackground get first 10 from URL2" +" time:"+ new Date(System.currentTimeMillis()).toString());
            if(rssItems2 != null && rssItems2.size() > 0){
                //add divider
                rssItems.add(new RssItem(true));
                //add rss
                for(int i = 0; i < 10 && i < rssItems2.size(); i++ ){
                    rssItems.add(rssItems2.get(i));
                }
            }

//            //add first 5 from URL3
//            List<RssItem> rssItems3 = RssDao.getRSS(Constants.RSS_URL3);
//            if(rssItems3 != null && rssItems3.size() > 0){
//                //add divider
//                rssItems.add(new RssItem(true));
//                //add rss
//                for(int i = 0; i < 5 && i < rssItems3.size(); i++ ){
//                    rssItems.add(rssItems3.get(i));
//                }
//            }
//            Log.e(TAG, "doInBackground finish!!!!!!!!!!!!!11" +" time:"+ new Date(System.currentTimeMillis()).toString());
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
//                Log.e(TAG, "onPostExecute adapter.notifyDataSetChanged()"+" time:" + new Date(System.currentTimeMillis()).toString());
            }

            getRssAsyncTask = null;

            if(showHelp && RssListFragment.this.isVisible()){
                showHelp();
            }
        }
    }



    public static class ViewHolder {
        ImageView imageRss;
        ImageView imageRssRoundedBorder;
        TextView textRss;
    }
}
