package info.guardianproject.securereaderinterface.z.rss.models.youtube;

import com.google.gson.annotations.SerializedName;

public class Thumbnails {

        @SerializedName("default")
        private ThumbnailUrl defaultImage;
        private ThumbnailUrl high;
        private ThumbnailUrl maxres;
        private ThumbnailUrl medium;
        private ThumbnailUrl standard;

    public Thumbnails(ThumbnailUrl defaultImage, ThumbnailUrl high, ThumbnailUrl maxres, ThumbnailUrl medium, ThumbnailUrl standard) {
        this.defaultImage = defaultImage;
        this.high = high;
        this.maxres = maxres;
        this.medium = medium;
        this.standard = standard;
    }

    public ThumbnailUrl getDefaultImage() {
        return defaultImage;
    }

    public void setDefaultImage(ThumbnailUrl defaultImage) {
        this.defaultImage = defaultImage;
    }

    public ThumbnailUrl getHigh() {
        return high;
    }

    public void setHigh(ThumbnailUrl high) {
        this.high = high;
    }

    public ThumbnailUrl getMaxres() {
        return maxres;
    }

    public void setMaxres(ThumbnailUrl maxres) {
        this.maxres = maxres;
    }

    public ThumbnailUrl getMedium() {
        return medium;
    }

    public void setMedium(ThumbnailUrl medium) {
        this.medium = medium;
    }

    public ThumbnailUrl getStandard() {
        return standard;
    }

    public void setStandard(ThumbnailUrl standard) {
        this.standard = standard;
    }

    public ThumbnailUrl getBestThumbnailUrl(){
        if(maxres != null){
            return maxres;
        }
        if(standard != null){
            return standard;
        }
        if(high != null){
            return high;
        }
        if(medium != null){
            return medium;
        }
        return defaultImage;
    }
}
