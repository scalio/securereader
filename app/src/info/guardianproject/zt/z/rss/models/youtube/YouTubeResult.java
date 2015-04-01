package info.guardianproject.zt.z.rss.models.youtube;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class YouTubeResult {

    public static final int MAX_COUNT_ON_PAGE = 20;
    public static final String NEXT_PAGE_TOKEN_PARAM = "&pageToken=";
    public static final String MAX_RESULTS_PARAM = "&maxResults="+String.valueOf(MAX_COUNT_ON_PAGE);


    @SerializedName("pageInfo")
    private PageInfo pageInfo;

    @SerializedName("items")
    private List<VideoItem> videoItems;

    private String nextPageToken = null;

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<VideoItem> getVideoItems() {
        return videoItems;
    }

    public void setVideoItems(List<VideoItem> videoItems) {
        this.videoItems = videoItems;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}
