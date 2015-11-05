package info.guardianproject.zt.z.rss.models.youtube;

import com.google.gson.annotations.SerializedName;

public class VideoItem {

    @SerializedName("id")
    private String id  ;

    @SerializedName("snippet")
    private Snippet snippet  ;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Snippet getSnippet() {
        return snippet;
    }

    public void setSnippet(Snippet snippet) {
        this.snippet = snippet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoItem videoItem = (VideoItem) o;

        if (id != null ? !id.equals(videoItem.id) : videoItem.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
