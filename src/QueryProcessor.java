import org.tartarus.snowball.ext.PorterStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class QueryProcessor {
    //Access Words Collection
    private MongoDBAdapter DBAdapeter;

    public ArrayList<String> StopWords;
    public ArrayList<String> SearchWords;
    public String SearchPhrase;

    public QueryProcessor(MongoDBAdapter DBA) {
        DBAdapeter = DBA;
        SearchWords = new ArrayList<>();
        SearchPhrase = "";
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
            if(W.matches("([a-zA-Z0-9-?])*"))
            {
                SearchWords.add(W);
            }
            else
            {
                if(SearchPhrase == "")
                {
                    SearchPhrase = SearchPhrase + W;
                }
                else
                {
                    SearchPhrase = SearchPhrase + " " + W;
                }
                SearchWords.add(W.replaceAll("([^a-zA-Z ])", ""));
            }
        }
        SearchPhrase = SearchPhrase.replaceAll("([^a-zA-Z ])", "");
        //System.out.println(SearchPhrase);
        //System.out.println(SearchWords);
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

    public void QuerySearch(String Query) {

        //TODO 1: Remove the stop words
        SearchPhrase(Query);

        RemoveSWAndNEC();
        //TODO 2: Stem the upcoming search query
        ArrayList<String> StemmedQ = new ArrayList<>();

        Stem(StemmedQ);
        //TODO 3: Search the DB in URL Collection for the stemmed word and Return the IDF and TF and URL of the words
        //Word Search
        ArrayList<String> Res = new ArrayList<>();
        ArrayList<QueryResult> QPRes = new ArrayList<>();
        double IDF;
        for (String word : StemmedQ) {
            Res.clear();
            IDF = RetIDF(word);
            if(IDF != 10000000)
            {
                Res.addAll(URLs(word));
                for(String url : Res)
                {
                    QPRes.add(QPSearch(word, url, IDF));
                }
            }
            else
            {
                System.out.println("Word doesn't exist in any Document Indexed!");
                break;
            }
        }
        //TODO 4: Phrase Search

    }

    public static void main(String args[])
    {
        boolean Global = false;
        boolean DropTable = false;
        MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        QueryProcessor Q = new QueryProcessor(DBAdapeter);
        System.out.println("Please enter a Query to search for");
        Scanner sc= new Scanner(System.in);
        String str = sc.nextLine();
        Q.QuerySearch(str);
    }
}
