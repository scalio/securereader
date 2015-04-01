package info.guardianproject.zt.z.rss.models.youtube;

import com.google.gson.annotations.SerializedName;

public class Snippet {

    @SerializedName("position")
    private int position;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("resourceId")
    private ResourceId resourceId;

    @SerializedName("thumbnails")
    private Thumbnails thumbnails;

    public Snippet(int position, String title, String description, ResourceId resourceId, Thumbnails thumbnails) {
        this.position = position;
        this.title = title;
        this.description = description;
        this.resourceId = resourceId;
        this.thumbnails = thumbnails;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

    public void setResourceId(ResourceId resourceId) {
        this.resourceId = resourceId;
    }

    public Thumbnails getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(Thumbnails thumbnails) {
        this.thumbnails = thumbnails;
    }
}
