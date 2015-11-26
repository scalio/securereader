package info.guardianproject.zt.rss.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.zt.z.rss.utils.Network;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ShareAppFragment extends Fragment {

    public static final String SHARING_ITEM = "TWITTER_SHARING_ITEM_TEXT";
    public static final String SHARING_PICTURE = "TWITTER_SHARING_PICTURE";

    private ImageView emailButton;
    private ImageView smsButton;
    private String mSharingItem;
    private String mSharingPicture;
    private static final String WEB_SITE = "http://www.radiozamaneh.com";
    private static String WEB_URL_TO_SHARE = "";
    private static final String WEB_URL_TO_SHARE_2 = "https://play.google.com/store/apps/details?id=info.guardianproject.zt.z.rss";

    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private ProgressDialog progressDialog = null;
    private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
    private boolean pendingPublishReauthorization = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pendingPublishReauthorization =
                    savedInstanceState.getBoolean(PENDING_PUBLISH_KEY, false);
        }
        View view =  inflater.inflate(R.layout.zshare_fragment,null, false);
        try {
            WEB_URL_TO_SHARE = "https://itunes.apple.com/us/app/en-jobbigare-morgon/id717083586?ls=1"+ URLEncoder.encode("&mt=8", "utf-8");
        } catch (UnsupportedEncodingException e) {
            WEB_URL_TO_SHARE = "https://itunes.apple.com/us/app/en-jobbigare-morgon/id717083586?ls=1";
        }

        if(getArguments()!= null  ){
            if(getArguments().containsKey(SHARING_ITEM)){
                mSharingItem = getArguments().getString(SHARING_ITEM);
            }
            if(getArguments().containsKey(SHARING_PICTURE)){
                mSharingPicture = WEB_SITE + getArguments().getString(SHARING_PICTURE);
            }
        }

        emailButton = (ImageView) view.findViewById(R.id.sendMail);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:"+
                        "?subject=" + Uri.encode("Tribune Zamaneh") +
//                        "&body=" + Uri.encode(getString(R.string.sharingText));
                         "&body=" + Uri.encode(getString(R.string.sharingText)) +"\n"+ WEB_URL_TO_SHARE+"\n"+WEB_URL_TO_SHARE_2+"\n";

                Uri uri = Uri.parse(uriText);

                intent.setData(uri);
                try {
                    startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(),
                                    getString(R.string.noEmailClients),
                                    Toast.LENGTH_SHORT).show();
                }

            }
        });


        return view;
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);
    }

    private void publishStory() {

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
