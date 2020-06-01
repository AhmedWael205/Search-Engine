public class PhraseResult {
    public String Word;
    public String URL;
    public String Summary;
    public String Title;
    public int TF;
    public String Geo;
    public Double Pop;
    public String pubDate;
    public PhraseResult(String word, String Url, String Summ, String Title, int tf, String geo, Double Pop, String pubDate)
    {
        this.Word = word;
        this.URL = Url;
        this.Summary = Summ;
        this.Title = Title;
        this.TF = tf;
        this.Geo = geo;
        this.Pop = Pop;
        this.pubDate = pubDate;
    }
    public boolean isEmpty()
    {
        if (URL.isEmpty())
            return true;
        else return false;
    }
}
