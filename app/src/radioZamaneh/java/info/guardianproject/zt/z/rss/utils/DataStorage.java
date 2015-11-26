package info.guardianproject.zt.z.rss.utils;

import info.guardianproject.zt.z.rss.models.RssItem;

public class DataStorage {
    private RssItem rssItem = null;

    public RssItem getRssItem() {
        return rssItem;
    }

    public void setRssItem(RssItem rssItem) {
        this.rssItem = rssItem;
    }
}
