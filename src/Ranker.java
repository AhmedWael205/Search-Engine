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
        Pages = new ArrayList<>();
    }

    public  double calculateScore(QueryResult queryResult,PhraseResult phraseResult,String Location)
    {

        URLResult result = null;
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
//      System.out.println("Updated Date " + formatter.format(toCompareWith));
        formatter.format(toCompareWith);

        if (phraseResult == null || phraseResult.isEmpty()) {
            popularityScore = queryResult.Pop;
            relevanceScore = 6 * queryResult.H1TF + 5 * queryResult.H2TF + 4 * queryResult.H3TF + 3 * queryResult.H4TF + 2 * queryResult.H5TF + queryResult.H6TF + queryResult.BodyTF;

            result = new URLResult(queryResult.URL,queryResult.Title,queryResult.Summary,queryResult.pubDate);

            //Egypt for now (hard coded)
            if (queryResult.Geo == Location)
                geoScore=1;
            try {
                if (!queryResult.pubDate.equals("-1"))
                {
                    pubDate = formatter.parse(queryResult.pubDate);
                    if (pubDate.compareTo(toCompareWith) >= 0)
                        updateScore = 1;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        else {
            relevanceScore =  1000 ;
            popularityScore = phraseResult.Pop;
            result = new URLResult(phraseResult.URL,phraseResult.Title,phraseResult.Summary,phraseResult.pubDate);




            if (phraseResult.Geo == "Egypt")
                geoScore=1;
            try {
                if (!phraseResult.pubDate.equals("-1"))
                {
                    pubDate = formatter.parse(phraseResult.pubDate);
                    if (pubDate.compareTo(toCompareWith) >= 0)
                        updateScore = 1;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }


        }
        totalScore = relevanceScore + 100 * updateScore + 100 * geoScore + 100 * popularityScore ;
        result.setScore(totalScore);
        Pages.add(result);
        return totalScore;
    }

    public  void calculatePopularity (MongoCollection<Document>Indexed)
    {
        long totalIndexed1= Indexed.countDocuments();
        long totalIndexed2=totalIndexed1;
        System.out.print(totalIndexed1+ " "+ totalIndexed2);
        MongoCursor<Document> cursor1 =  Indexed.find(Filters.eq("PopCalc",0)).noCursorTimeout(true).iterator();

        double popularityScore;

        for(int j=0 ;j<totalIndexed1;j++)
        {
            if (!cursor1.hasNext())
                break;

            popularityScore =0;
            Document hala1 = cursor1.next();
            String mylink = hala1.get("URL").toString();
            //System.out.print(mylink + "\n");
            MongoCursor<Document> cursor2  = Indexed.find(Filters.exists("Links")).noCursorTimeout(true).iterator();
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
            System.out.println("Remaining to calculate: "+Long.toString(totalIndexed2-j));
            this.DB.updatePopularity(mylink,popularityScore);


        }
    }

    public  void run (){

    }


    public static void main(String args[]) {
        boolean Global = false;
        boolean DropTable = false;
        MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        MongoCollection<Document> Indexed = DBAdapeter.returnIndexed();

        Ranker ranker = new Ranker (DBAdapeter);
//        ranker.calculatePopularity(Indexed);

        QueryResult q1 = new QueryResult("hi",100,10,20,30,40,50,60,70,80,"q1","q1","q1","Egypt",0.1,"-1");
        QueryResult q2 = new QueryResult("hi",100,10,20,30,40,50,60,70,80,"q2","q1","q1","Egypt",0.4,"-1");
        QueryResult q3 = new QueryResult("hi",100,10,20,30,40,50,60,70,80,"q3","q1","q1","Egypt",0.6,"-1");
        QueryResult q4 = new QueryResult("hi",100,10,20,30,40,50,60,70,80,"q4","q1","q1","Egypt",0.8,"-1");
        ArrayList<QueryResult> Res = new ArrayList<>();
        Res.add(q1);
        Res.add(q2);
        Res.add(q3);
        Res.add(q4);


        for(QueryResult q : Res)
        {
            ranker.calculateScore(q,null,"Egypt");
        }
        Collections.sort(ranker.Pages, new ScoreComparator());
        for(URLResult p : ranker.Pages)
        {
            System.out.println(p.url +" " + p.score);
        }
    }
}
