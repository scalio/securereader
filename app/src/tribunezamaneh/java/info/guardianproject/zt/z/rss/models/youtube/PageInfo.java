package info.guardianproject.zt.z.rss.models.youtube;

import com.google.gson.annotations.SerializedName;

public class PageInfo {

    @SerializedName("totalResults")
    private int totalResults = 0;

    @SerializedName("resultsPerPage")
    private int resultsPerPage = 0;

    public PageInfo(int totalResults, int resultsPerPage) {
        this.totalResults = totalResults;
        this.resultsPerPage = resultsPerPage;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }
}
