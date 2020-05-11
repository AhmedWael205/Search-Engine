import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.tartarus.snowball.ext.PorterStemmer;

public class Indexer implements Runnable
{
    public String URLtoParse;
    public ArrayList<String> StopWords;

    //Array to accumulate Res of ALL Stemmed Words
    public ArrayList<Word> Res;

    public String Title;
    public Document HTML;
    public String Summary;
    public String Geo;
    public ArrayList<org.bson.Document> BD;
    public ArrayList<org.bson.Document> H1D;
    public ArrayList<org.bson.Document> H2D;
    public ArrayList<org.bson.Document> H3D;
    public ArrayList<org.bson.Document> H4D;
    public ArrayList<org.bson.Document> H5D;
    public ArrayList<org.bson.Document> H6D;
    public ArrayList<org.bson.Document> PD;


    //Access Words Collection
    private MongoDBAdapter DBAdapeter;

    private Object WordLock;
    private Object URLLock;

    public org.bson.Document HTMLDoc;
    public String HTMLString;



    @Override
    public void run() {
        this.Index();
    }

    public Indexer(MongoDBAdapter DBA, Object WL, Object UL)
    {
        DBAdapeter = DBA;
        Res = new ArrayList<>();

        WordLock = WL;
        URLLock = UL;

        Title = "";
        Summary = "";
        Geo = "";
        BD = new ArrayList<>();
        H1D = new ArrayList<>();
        H2D = new ArrayList<>();
        H3D = new ArrayList<>();
        H4D = new ArrayList<>();
        H5D = new ArrayList<>();
        H6D = new ArrayList<>();
        PD = new ArrayList<>();

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

    public void Index()
    {
        while(RemainingToIndex() >= 1)
        {
            ClearAll();
            ReadHTMLDoc();

            HTMLString = HTMLDoc.get("Document").toString();
            URLtoParse = HTMLDoc.get("url").toString();
            HTML = Jsoup.parse(HTMLString);

            Title = HTML.title();
            String body = HTML.body().text();
            Geo = HTML.location();
            //For phrase searching purposes :D
            Summary = body;
            String smbody = body.toLowerCase();
            String Header1 = HTML.getElementsByTag("h1").text().toLowerCase();
            String Header2 = HTML.getElementsByTag("h2").text().toLowerCase();
            String Header3 = HTML.getElementsByTag("h3").text().toLowerCase();
            String Header4 = HTML.getElementsByTag("h4").text().toLowerCase();
            String Header5 = HTML.getElementsByTag("h5").text().toLowerCase();
            String Header6 = HTML.getElementsByTag("h6").text().toLowerCase();
            String Paragragh = HTML.getElementsByTag("p").text().toLowerCase();

            String [] BodyArray = smbody.split(" ");
            String [] H1Array = Header1.split(" ");
            String [] H2Array = Header2.split(" ");
            String [] H3Array = Header3.split(" ");
            String [] H4Array = Header4.split(" ");
            String [] H5Array = Header5.split(" ");
            String [] H6Array = Header6.split(" ");
            String [] ParaArray = Paragragh.split(" ");

            ArrayList<String> Body = new ArrayList<>();
            ArrayList<String> H1 = new ArrayList<>();
            ArrayList<String> H2 = new ArrayList<>();
            ArrayList<String> H3 = new ArrayList<>();
            ArrayList<String> H4 = new ArrayList<>();
            ArrayList<String> H5 = new ArrayList<>();
            ArrayList<String> H6 = new ArrayList<>();
            ArrayList<String> P = new ArrayList<>();

            //Only Add Words to the List removing everything that's not a letter or a number
            //While also NOT ADDING Stop Words
            RemoveSWAndNEC(Body, BodyArray);
            RemoveSWAndNEC(H1, H1Array);
            RemoveSWAndNEC(H2, H2Array);
            RemoveSWAndNEC(H3, H3Array);
            RemoveSWAndNEC(H4, H4Array);
            RemoveSWAndNEC(H5, H5Array);
            RemoveSWAndNEC(H6, H6Array);
            RemoveSWAndNEC(P, ParaArray);

            //System.out.printf("Title: %s%n", Title);
            //System.out.printf("Body: %s\n", body);
            //System.out.printf("Header 2: %s\n", Header2);
            //System.out.printf("Body after removing Stop Words and Special Characters: %s\n", Body);
            //System.out.printf("H1 after removing Stop Words and Special Characters: %s\n", H1);
            //System.out.printf("H2 after removing Stop Words and Special Characters: %s\n", H2);
            //System.out.printf("H3 after removing Stop Words and Special Characters: %s\n", H3);
            //System.out.printf("H4 after removing Stop Words and Special Characters: %s\n", H4);
            //System.out.printf("H5 after removing Stop Words and Special Characters: %s\n", H5);
            //System.out.printf("H6 after removing Stop Words and Special Characters: %s\n", H6);
            //System.out.printf("P after removing Stop Words and Special Characters: %s\n", P);

            //Stemming Words and Calculating their TF
            Stem(Body, BD);
            Stem(H1, H1D);
            Stem(H2, H2D);
            Stem(H3, H3D);
            Stem(H4, H4D);
            Stem(H5, H5D);
            Stem(H6, H6D);
            Stem(P, PD);
            NormalizeTF();

//            if(body.length() <= 150)
//            {
//                Summary = body;
//            }
//            else
//            {
//                Summary = body.substring(0,150) + "...";
//            }

            InsertURLAnalysis();
            InsertWordsToCollection();
            //System.out.printf("Finished Indexing Page with URL: %s\n", URLtoParse);
            System.out.printf("Remaining to index %d\n", RemainingToIndex());
        }
        //InsertWordsToCollection();
    }

    public void ClearAll()
    {
        Res.clear();
        BD.clear();
        H1D.clear();
        H2D.clear();
        H3D.clear();
        H4D.clear();
        H5D.clear();
        H6D.clear();
        PD.clear();
    }

    public long RemainingToIndex()
    {
        return DBAdapeter.getIndexedCount();
    }

    public void ReadHTMLDoc()
    {
        HTMLDoc = DBAdapeter.getDoctoIndex();
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

    public void Stem(ArrayList<String> Words, ArrayList<org.bson.Document> HTMLAnalysis)
    {
        if(Words == null)
        {
            return;
        }
        PorterStemmer PStem = new PorterStemmer();
        Word WafterStem = null;
        for(String w : Words)
        {
            PStem.setCurrent(w);
            PStem.stem();
            WafterStem = new Word(PStem.getCurrent());
            AddtoRes(WafterStem);
        }
        for(Word wrd : Res)
        {
            org.bson.Document Obj = new org.bson.Document("Word", wrd.text).append("TF", wrd.TF);
            HTMLAnalysis.add(Obj);
        }
    }

    public int GetIndex(Word WafterStem)
    {
        int i = 0;
        for(Word r : Res)
        {
            if(r.text.equals(WafterStem.text))
            {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void AddtoRes(Word WafterStem)
    {
        int index = GetIndex(WafterStem);
        if(index == -1)
        {
            Res.add(WafterStem);
        }
        else
        {
            Word temp = Res.get(index);
            temp.TF++;
            Res.set(index, temp);
        }
    }

    public void NormalizeTF()
    {
        for(Word w : Res)
        {
            w.TF/=Res.size();
        }
    }

    public void InsertWordsToCollection()
    {
        synchronized (WordLock)
        {
            for(Word w : Res)
            {
                DBAdapeter.addWord(w.text,URLtoParse);
            }
        }
    }

    public void InsertURLAnalysis()
    {
        synchronized (URLLock)
        {
            DBAdapeter.addURL(URLtoParse, Title, Summary, BD, H1D, H2D, H3D, H4D, H5D, H6D, PD);
        }
    }

    public void FinalizeIDF()
    {
        //synchronized (WordLock)
        {
            DBAdapeter.calculateIDF();
        }
    }

    public static void main(String args[])
    {
        boolean Global = false;
        boolean DropTable = false;
        MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        int ThreadNumbers = 10;
        Thread myThreads[] = new Thread[ThreadNumbers];

        Object WordLock = new Object();
        Object URLLock = new Object();

        LocalTime myObj = LocalTime.now();
        System.out.println(myObj);

        for (int j = 0; j < ThreadNumbers; j++) {
            myThreads[j] = new Thread(new Indexer(DBAdapeter, WordLock, URLLock));
            myThreads[j].start();
        }
        for (int j = 0; j < ThreadNumbers; j++) {
            try {
                myThreads[j].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LocalTime myObj1 = LocalTime.now();


        System.out.println(myObj1);

        Indexer index = new Indexer(DBAdapeter, WordLock, URLLock);
        index.FinalizeIDF();

        LocalTime myObj2 = LocalTime.now();
        System.out.println(myObj2);
    }
}


