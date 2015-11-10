package info.guardianproject.zt.rss.dao;

import android.util.Log;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndContentImpl;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import info.guardianproject.zt.z.rss.models.RssItem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RssDao {
    public static final String TAG = "RadioZamaneh RssDao";
//    public static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    public static List<RssItem> getRSS(String rssUrl)
    {
        List<RssItem> resultList = new ArrayList<RssItem>();
        URL feedUrl;
        try
        {
//            Log.e(TAG, "RssDao getRSS Start url "+rssUrl+" time:" +sdf.format(new Date(System.currentTimeMillis())));
            feedUrl = new URL(rssUrl);
//            Log.e(TAG, "RssDao getRSS Start url "+rssUrl+" time:" +sdf.format(new Date(System.currentTimeMillis())));
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
//            Log.e(TAG, "RssDao getRSS SyndFeed feed = input.build(new XmlReader(feedUrl))"+" time:" +sdf.format(new Date(System.currentTimeMillis())));
            List entries = feed.getEntries();

            Iterator iterator = entries.listIterator();
            while (iterator.hasNext())
            {
                SyndEntry entry = (SyndEntry) iterator.next();

                String content = "";
                String contentType = "";
                String description = "";
                if(entry.getDescription() != null){
                    description = entry.getDescription().getValue();
                }
                if(!entry.getContents().isEmpty()){
                    SyndContentImpl syndContent = (SyndContentImpl) entry.getContents().get(0);
                    content = syndContent.getValue();
                    contentType = syndContent.getType();
                }
                resultList.add(new RssItem(entry.getTitle(), entry.getLink(), entry.getEnclosures(), description, content, contentType));
            }
//            Log.e(TAG, "RssDao getRSS after  while (iterator.hasNext())"+" time:" +sdf.format(new Date(System.currentTimeMillis())));
        }
        catch (MalformedURLException e)
        {
            Log.e(TAG, "Error MalformedURLException", e);
        }
        catch (IllegalArgumentException e)
        {
            Log.e(TAG, "Error IllegalArgumentException", e);
        }
        catch (FeedException e)
        {
            Log.e(TAG, "Error FeedException", e);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error IOException", e);
        }
        return resultList;
    }
}
