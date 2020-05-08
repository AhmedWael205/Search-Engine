import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class QueryProcessor {
    //Access Words Collection
    private MongoDBAdapter DBAdapeter;

    public ArrayList<String> StopWords;

    public QueryProcessor(MongoDBAdapter DBA)
    {
        DBAdapeter = DBA;
        ReadStopWords();
    }

    public void ReadStopWords()
    {
        StopWords = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new FileReader((".\\StopWords.txt")))){
            while(reader.ready())
            {
                StopWords.add(reader.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void RemoveSWAndNEC(ArrayList<String> L, String[] Arr)
    {
        String newW;
        for(String w : Arr)
        {
            newW = w.replaceAll("([^a-z])", "");
            if(!newW.equals(""))
            {
                if(!StopWords.contains(newW))
                {
                    L.add(newW);
                }
            }
        }
    }

    public void QuerySearch(String Query)
    {
        //TODO 1: Remove the stop words
        //TODO 2: Stem the upcoming search query
        //TODO 3: Search the DB in Words Collection for the stemmed word
        //TODO 4; Return the IDF and TF and URL of the words
    }
}
