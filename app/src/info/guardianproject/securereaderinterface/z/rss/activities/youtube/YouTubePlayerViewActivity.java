package info.guardianproject.securereaderinterface.z.rss.activities.youtube;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.z.rss.constants.Constants;
import info.guardianproject.securereaderinterface.z.rss.utils.StringHelper;

public class YouTubePlayerViewActivity  extends YouTubeFailureRecoveryActivity {

    public static final String YOUTUBE_VIDEO_ID_KEY = "YOUTUBE_VIDEO_ID_KEY";
    public static final String ACTIVITY_STYLE = "ACTIVITY_STYLE";

    public static final int STYLE_WITH_STANDARD_BG_AND_HEADER = 1;
    public static final int STYLE_BLACK_WITH_HEADER = 2;
    public static final int STYLE_BLACK = 3;
    private String videoIdKey = "";
    private int activityStyle = STYLE_WITH_STANDARD_BG_AND_HEADER;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if(getIntent()!= null && getIntent().getExtras()!= null ){
            activityStyle = getIntent().getExtras().getInt(ACTIVITY_STYLE, STYLE_WITH_STANDARD_BG_AND_HEADER) ;
        }

        switch (activityStyle){
            case STYLE_WITH_STANDARD_BG_AND_HEADER:
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setContentView(R.layout.zyoutube_player_view_activity);
                break;
            case STYLE_BLACK_WITH_HEADER:
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setContentView(R.layout.zyoutube_player_view_activity2);

                break;
            case STYLE_BLACK:
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setContentView(R.layout.zyoutube_player_view_activity2);
                break;
            default:
            setContentView(R.layout.zyoutube_player_view_activity);
        }

        if(getIntent()!= null && getIntent().getExtras()!= null && !StringHelper.isEmptyString(getIntent().getExtras().getString(YOUTUBE_VIDEO_ID_KEY))){
            videoIdKey = getIntent().getExtras().getString(YOUTUBE_VIDEO_ID_KEY);
        }
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(Constants.DEVELOPER_KEY, this);


    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        if (!wasRestored && !StringHelper.isEmptyString(videoIdKey)) {
            player.cueVideo(videoIdKey);
        }
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }

}
