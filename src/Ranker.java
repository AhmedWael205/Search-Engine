import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import org.bson.Document;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class Ranker implements Runnable {

    //private static final double dampingFactor = 0.85;
    private static MongoDBAdapter DB;
    public ArrayList<URLResult> Pages;
   // private int pagesIndex;




    public Ranker(MongoDBAdapter mongoDBAdapter )
    {
        this.DB = mongoDBAdapter;
    }

    public  double calculateScore(QueryResult queryResult,PhraseResult phraseResult)
    {

        double relevanceScore=0;
        double updateScore=0;
        double geoScore=0;
        double totalScore=0;
        double popularityScore=0;


        SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date pubDate;
        Date dateNow = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateNow);
        calendar.add(Calendar.YEAR,-3);
        Date toCompareWith = calendar.getTime();
        System.out.println("Updated Date " + formatter.format(toCompareWith));

        if (phraseResult.isEmpty()) {
            relevanceScore = 6 * queryResult.H1TF + 5 * queryResult.H2TF + 4 * queryResult.H3TF + 3 * queryResult.H4TF + 2 * queryResult.H5TF + queryResult.H6TF + queryResult.BodyTF;

            URLResult result = new URLResult(queryResult.URL,queryResult.Title,queryResult.Summary,queryResult.pubDate);
            Pages.add(result);

            //Egypt for now (hard coded)
            if (queryResult.Geo == "Egypt")
                geoScore=1;
            try {
                pubDate = formatter.parse(queryResult.pubDate);
                if (pubDate.compareTo(toCompareWith) >= 0)
                    updateScore = 1;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        else {
            relevanceScore =  phraseResult.TF ;

            URLResult result = new URLResult(phraseResult.URL,phraseResult.Title,phraseResult.Summary,phraseResult.pubDate);
            Pages.add(result);



            if (phraseResult.Geo == "Egypt")
                geoScore=1;
            try {
                pubDate = formatter.parse(phraseResult.pubDate);
                if (pubDate.compareTo(toCompareWith) >= 0)
                    updateScore = 1;
            } catch (ParseException e) {
                e.printStackTrace();
            }


        }
        totalScore = relevanceScore + 100 * updateScore + 100 * geoScore + 100 * popularityScore ;

        return totalScore;
    }

    public  void calculatePopularity (MongoCollection<Document>Indexed)
    {
        long totalIndexed1= Indexed.countDocuments();
        long totalIndexed2=totalIndexed1;
        System.out.print(totalIndexed1+ " "+ totalIndexed2);
        MongoCursor<Document> cursor1 =  Indexed.find(Filters.exists("URL")).iterator();

        double popularityScore;

        for(int j=0 ;j<totalIndexed1;j++)
        {
            if (!cursor1.hasNext())
                break;

            popularityScore =0;
            Document hala1 = cursor1.next();
            String mylink = hala1.get("URL").toString();
            //System.out.print(mylink + "\n");
            MongoCursor<Document> cursor2  = Indexed.find(Filters.exists("Links")).iterator();
            for(int k=0 ;k<totalIndexed2;k++)
            {

                if (!cursor2.hasNext())
                    break;
                Document hala2 = cursor2.next();

                List links = hala2.getList("Links",Object.class);
                double linksOnPage = links.toArray().length;
                if (links.contains(mylink)) {
                    popularityScore += 1/linksOnPage;
                }

            }

             System.out.print(mylink+"   Popularity = "+ popularityScore + " \n");
            this.DB.updatePopularity(mylink,popularityScore);


        }
    }

    public  void run (){

    }
    public static Comparator<URLResult> scoreComparator = new Comparator<URLResult>() {
        @Override
        public int compare(URLResult jc1, URLResult jc2) {
            return (jc2.score < jc1.score? -1 :
                    (jc2.score == jc1.score ? 0 : 1));
        }
    };
    public ArrayList sortResults(ArrayList<URLResult> resultsArray)
    {
        int total = resultsArray.size();
        System.out.print(total);
        Collections.sort(resultsArray,scoreComparator);
        return resultsArray;
    }

    public static void main(String args[]) {
        boolean Global = false;
        boolean DropTable = false;
        MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        MongoCollection<Document> Indexed = DBAdapeter.returnIndexed();

        Ranker ranker = new Ranker (DBAdapeter);
        ranker.calculatePopularity(Indexed);


    }
}
