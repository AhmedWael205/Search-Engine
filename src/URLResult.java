public class URLResult {
    String url;
    String Title;
    String Summary;
    String pubDate;
    double score;
    public URLResult(String URL, String TITLE, String SUM, String pubDate)
    {
        this.url = URL;
        this.Title = TITLE;
        this.Summary = SUM;
        this.pubDate = pubDate;


    }
    public void setScore(double score)
    {
        this.score = score;
    }
}
