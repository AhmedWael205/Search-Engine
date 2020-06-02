import org.tartarus.snowball.ext.PorterStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor {
    //Access Words Collection
    private MongoDBAdapter DBAdapeter;

    public boolean ImageSearch;
    public String UserCountry;
    public String Name;

    public ArrayList<String> StopWords;
    public ArrayList<String> SearchWords;
    public String SearchPhrase;
    public ArrayList<QueryResult> QPRes;
    public ArrayList<PhraseResult> PSRes;

    //For GUI
    public ArrayList<ImageResult> ImageResults;
    public ArrayList<URLResult> URLResults;

    public QueryProcessor(boolean IS, String UC, String N) {
        boolean Global = false;
        boolean DropTable = false;
        DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        SearchWords = new ArrayList<>();
        ImageResults = new ArrayList<>();
        URLResults = new ArrayList<>();
        SearchPhrase = "";
        ImageSearch = IS;
        UserCountry = UC;
        Name = N;
        QPRes = new ArrayList<>();
        PSRes = new ArrayList<>();
        ReadStopWords();
    }

    public void ReadStopWords() {
        StopWords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader((".\\StopWords.txt")))) {
            while (reader.ready()) {
                StopWords.add(reader.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void RemoveSWAndNEC()
    {
        ArrayList<String> SW = new ArrayList<>(SearchWords);
        SearchWords.clear();
        for (String w : SW)
        {
            w = w.toLowerCase();
            w = w.replaceAll("([^a-z])", "");
            if (!StopWords.contains(w))
            {
                SearchWords.add(w);
            }
        }
    }

    public void Stem(ArrayList<String> StemmedQuery) {
        if (SearchWords == null) {
            return;
        }
        PorterStemmer PStem = new PorterStemmer();

        for (String w : SearchWords) {
            PStem.setCurrent(w);
            PStem.stem();
            StemmedQuery.add(PStem.getCurrent());
        }
    }

    public void SearchPhrase(String Query)
    {
        String [] QueryArray = Query.split(" ");
        for(String W : QueryArray)
        {
            SearchWords.add(W.replaceAll("([^a-zA-Z ])", ""));
        }
        String pattern = "\"([^\"]*)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(Query);
        if(m.find())
        {
            SearchPhrase = m.group();
        }
        SearchPhrase = SearchPhrase.replaceAll("([^a-zA-Z ])", "");
        System.out.println(SearchPhrase);
        System.out.println(SearchWords);
    }

    public ArrayList<String> URLs(String Word)
    {
        return DBAdapeter.WordSearchURL(Word);
    }

    public double RetIDF(String Word)
    {
        return DBAdapeter.ReturnIDF(Word);
    }

    public QueryResult QPSearch(String Word, String URL, double IDF)
    {
        return DBAdapeter.QueryProcessorRes(Word, URL, IDF);
    }

    public ArrayList<PhraseResult> PhraseResult(ArrayList<String> URLs)
    {
        return DBAdapeter.PhraseSearchRes(SearchPhrase, URLs);
    }

    public void QuerySearch(String Query) {

        //TODO 1: Remove the stop words
        SearchPhrase(Query);

        RemoveSWAndNEC();
        //TODO 2: Stem the upcoming search query
        ArrayList<String> StemmedQ = new ArrayList<>();

        Stem(StemmedQ);
        //TODO 3: Search the DB in URL Collection for the stemmed word and Return the IDF and TF and URL of the words
        //Word Search
        ArrayList<String> WordSearchUrls = new ArrayList<>();
        ArrayList<String> PhraseSearchUrls = new ArrayList<>();
        double IDF;
        if(!ImageSearch) {
            for (String word : StemmedQ) {
                WordSearchUrls.clear();
                IDF = RetIDF(word);
                if (IDF != 10000000) {
                    WordSearchUrls.addAll(URLs(word));
                    if (SearchPhrase.contains(word)) {
                        PhraseSearchUrls.addAll(URLs(word));
                    }
                    for (String url : WordSearchUrls) {
                        QPRes.add(QPSearch(word, url, IDF));
                    }
                } else {
                    System.out.println("Word doesn't exist in any Document Indexed!");
                    break;
                }
            }
            //TODO 4: Phrase Search
            PSRes.addAll(PhraseResult(PhraseSearchUrls));
//            System.out.println(PSRes.get(0).URL);
//            System.out.println(PSRes.size());
        }
        else
        {
            for (String word : StemmedQ) {
                ImageResults.addAll(SearchImagesInDB(word));
            }
        }
    }

    public ArrayList<ImageResult> SearchImagesInDB(String Word)
    {
        return DBAdapeter.getImage(Word);
    }

    public void AddQuery(String Query, String UserCountry)
    {
        DBAdapeter.AddtoQueryCollection(Query,UserCountry);
    }

    public static void main(String args[])
    {
        QueryProcessor Q = new QueryProcessor(true, "Egypt", "Wael");
//        System.out.println("Please enter a Query to search for");
//        Scanner sc= new Scanner(System.in);
//        String Query = sc.nextLine();
        String Query = "";
        Q.QuerySearch(Query);
        //Call Ranker to return URLResults
        //For Right now till the GUI send the correct ONE
        Q.AddQuery(Query,Q.UserCountry);
    }
}
