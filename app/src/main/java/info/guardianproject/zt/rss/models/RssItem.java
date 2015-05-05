package info.guardianproject.zt.rss.models;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEnclosure;
import info.guardianproject.zt.z.rss.constants.Constants;
import info.guardianproject.zt.z.rss.utils.StringHelper;

import java.util.List;

public class RssItem {
    private String title = "";
    private String link = "";
    private String url = "";
    private String description = "";

    private String descriptionContent = "";
    private String descriptionContentType ="";

    private List<SyndEnclosure> syndEnclosureList= null;
    private boolean selected = false;
    private boolean isDivider = false;

    public RssItem(boolean divider) {
        isDivider = divider;
    }

    public RssItem(String title, String link, List<SyndEnclosure> syndEnclosureList, String description, String descriptionContent, String descriptionContentType) {
        this.title = title;
        this.link = link;
        this.syndEnclosureList = syndEnclosureList;
        this.description = description;
        this.descriptionContent = descriptionContent;
        this.descriptionContentType = descriptionContentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isDivider() {
        return isDivider;
    }

    public void setDivider(boolean divider) {
        isDivider = divider;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public List<SyndEnclosure> getSyndEnclosureList() {
        return syndEnclosureList;
    }

    public void setSyndEnclosureList(List<SyndEnclosure> syndEnclosureList) {
        this.syndEnclosureList = syndEnclosureList;
    }

    public String getDescriptionContentType() {
        return descriptionContentType;
    }

    public void setDescriptionContentType(String descriptionContentType) {
        this.descriptionContentType = descriptionContentType;
    }

    public String getDescriptionContent() {
        return descriptionContent;
    }

    public void setDescriptionContent(String descriptionContent) {
        this.descriptionContent = descriptionContent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RssItem rssItem = (RssItem) o;

        if (link != null ? !link.equals(rssItem.link) : rssItem.link != null) return false;
        if (title != null ? !title.equals(rssItem.title) : rssItem.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (link != null ? link.hashCode() : 0);
        return result;
    }


    public String getAudioUrl(){
        if(!StringHelper.isEmptyString(url)){
            return url;
        }
        if(getSyndEnclosureList() != null ){
            for( Object obj: getSyndEnclosureList()){
                if(obj instanceof SyndEnclosure){
                    SyndEnclosure syndEnclosure = (SyndEnclosure) obj;
                    if(syndEnclosure.getType().equals(Constants.MIME_TYPE_AUDIO)){
                        url = syndEnclosure.getUrl();
                        break;
                    }
                }
            }
        }
        return url;
    }

    public String getImageUrl(){
        if(!StringHelper.isEmptyString(url)){
            return url;
        }
        if(getSyndEnclosureList() != null ){
            for( Object obj: getSyndEnclosureList()){
                if(obj instanceof SyndEnclosure){
                    SyndEnclosure syndEnclosure = (SyndEnclosure) obj;
                    if(syndEnclosure.getType().equals(Constants.MIME_TYPE_IMAGE) || syndEnclosure.getType().equals(Constants.MIME_TYPE_IMAGE2)){
                        url = syndEnclosure.getUrl();
                        break;
                    }
                }
            }
        }
        return url;
    }



}
