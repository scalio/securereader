package info.guardianproject.zt.rss.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.zt.z.rss.activities.Help2Activity;
import info.guardianproject.zt.z.rss.listeners.OnSwipeTouchListener;
import info.guardianproject.zt.z.rss.models.RssItem;
import info.guardianproject.zt.z.rss.utils.DataStorage;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ShowRssFragment extends Fragment{
    public static final String SHOW_HELP_KEY = "ShowHelpScreenKey";
    public static boolean sDisableFragmentAnimations = false;

    public static final String SHARING_ITEM = "TWITTER_SHARING_ITEM_TEXT";
    public static final String SHARING_PICTURE = "TWITTER_SHARING_PICTURE";

    private static final String TAG = "RadioZamaneh ShowRssFragment";
    private DataStorage dataStorage = null;
    private RssItem rssItem = null;
    private ImageView emailButton;


    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static final int TWITTER_RESULT = 70;

    private ProgressDialog progressDialog = null;
    private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
    private boolean pendingPublishReauthorization = false;


    public ShowRssFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataStorage = ((App) getActivity().getApplication()).getDataStorage();
        rssItem = dataStorage.getRssItem();

        if( getArguments()!= null && getArguments().containsKey(SHOW_HELP_KEY) && getArguments().getBoolean(SHOW_HELP_KEY)){
            Intent intent = new Intent(getActivity(), Help2Activity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        sDisableFragmentAnimations = true;
        outState.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.zshow_rss_fragment, container, false);
        TextView titleRss = (TextView)fragmentView.findViewById(R.id.titleRss);
        ImageView headerImage = (ImageView)fragmentView.findViewById(R.id.headerImage);
        headerImage.setImageResource(R.drawable.header_img_rss);
        WebView descriptionWebView = (WebView)fragmentView.findViewById(R.id.descriptionWebView);
        ScrollView scrollView = (ScrollView) fragmentView.findViewById(R.id.scrollView);

        if(rssItem!= null){
            if(!StringHelper.isEmptyString(rssItem.getTitle())){
                titleRss.setText(rssItem.getTitle());
            }

            if(!StringHelper.isEmptyString(rssItem.getDescriptionContent())){
                descriptionWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        try{
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        }catch (Exception e){
//                            Log.e(ShowRssFragment.TAG, "Error",e);
                        }
                        return true;
                    }
                });
                descriptionWebView.getSettings().setJavaScriptEnabled(true);
                descriptionWebView.setWebChromeClient(new WebChromeClient() {});
                descriptionWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                descriptionWebView.loadDataWithBaseURL(null, rssItem.getDescriptionContent(), "text/html", "utf-8", null);

            }

        }

        emailButton = (ImageView) fragmentView.findViewById(R.id.sendMail);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String sharingArticle = "";
                if(rssItem != null){
                    sharingArticle = rssItem.getTitle()+"\n";
                    if(!StringHelper.isEmptyString(rssItem.getLink())){
                        sharingArticle = sharingArticle + rssItem.getLink();
                    }
                }

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:"+
                        "?subject=" + Uri.encode("Tribune Zamaneh") +
//                        "&body=" + Uri.encode(getString(R.string.sharingText));
                        "&body=" + Uri.encode(getString(R.string.sharingText)) +"\n"+ sharingArticle+"\n";

                Uri uri = Uri.parse(uriText);

                intent.setData(uri);
                try {
                    startActivity(Intent.createChooser(intent, getString(R.string.sendEmail)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(),
                            getString(R.string.noEmailClients),
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

//        gplusButton = (ImageView) fragmentView.findViewById(R.id.googlePlus);
//        gplusButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (Network.checkInternetConnectivity(getActivity())) {
//                    Toast.makeText(getActivity(), "Will be implemented later", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        OnSwipeTouchListener onSwipeTouchListener =  new OnSwipeTouchListener(getActivity()){
            @Override
            public void onSwipeRight() {
                Tab1ContainerFragment.self.restoreFromBackStack();
            }
        };
        scrollView.setOnTouchListener(onSwipeTouchListener);
        descriptionWebView.setOnTouchListener(onSwipeTouchListener);
        fragmentView.setOnTouchListener(onSwipeTouchListener);


        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        }
    }

}


