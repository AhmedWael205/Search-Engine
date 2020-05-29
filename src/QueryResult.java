public class QueryResult {
    public String Word;
    public double BodyTF;
    public double H1TF;
    public double H2TF;
    public double H3TF;
    public double H4TF;
    public double H5TF;
    public double H6TF;
    public double PTF;
    public double IDF;
    public String URL;
    public String Summary;
    public String Title;
    public String Geo;
    public Double Pop;
    public String pubDate;
    public QueryResult(String W, double B, double H1, double H2, double H3, double H4, double H5, double H6, double P, double IDF, String url, String title, String Summ, String Geo, Double Pop, String pubDate)
    {
        this.Word = W;
        this.BodyTF = B;
        this.H1TF = H1;
        this.H2TF = H2;
        this.H3TF = H3;
        this.H4TF = H4;
        this.H5TF = H5;
        this.H6TF = H6;
        this.PTF = P;
        this.IDF = IDF;
        this.URL = url;
        this.Title = title;
        this.Summary = Summ;
        this.Geo = Geo;
        this.Pop = Pop;
        this.pubDate = pubDate;
    }
}
