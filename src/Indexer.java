import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.tartarus.snowball.ext.PorterStemmer;
import org.tartarus.snowball.ext.EnglishStemmer;

public class Indexer {
    public String URLtoParse;
    public ArrayList<String> StopWords;
    public Document HTML;
    private MongoDBAdapter DBAdapeter;

    public Indexer(String URL)
    {
        URLtoParse = URL;
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
        try{
            HTML = Jsoup.connect(URLtoParse).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String Title = HTML.title();
        String body = HTML.body().text().toLowerCase();
        String [] Words = body.split(" ");
        ArrayList<String> W = new ArrayList<>();
        String newW;
        //Only Add Words to the List removing everything that's not a letter or a number
        for(String w : Words)
        {
            newW = w.replaceAll("([^a-z0-9-/])", "");
            if(!newW.equals(""))
            {
                W.add(newW);
            }
        }
        //Remove Stop Words
        for(String wrd : StopWords)
        {
            W.removeAll(Collections.singleton(wrd));
        }
        System.out.printf("Title: %s%n", Title);
        System.out.printf("Body: %s\n", body);
        System.out.printf("Body after removing Stop Words and Special Characters: %s\n", W);
        ArrayList<Word> Res = new ArrayList<>();
        Stem(W, Res, "body");
        Word Disp = Res.get(GetIndex(Res, new Word("health", "body")));
        System.out.printf("Word: %s\n", Disp.text);
        System.out.printf("TF: %s\n", Disp.TF);
        System.out.printf("Pos: %s\n", Disp.Pos);
        NormalizeTF(Res);
        Disp = Res.get(GetIndex(Res, new Word("health", "body")));
        System.out.printf("Word: %s\n", Disp.text);
        System.out.printf("TF: %s\n", Disp.TF);
        System.out.printf("Pos: %s\n", Disp.Pos);
    }

    public void Stem(ArrayList<String> Words, ArrayList<Word> Res, String Pos)
    {
        ArrayList<String> Test = new ArrayList<>();
        PorterStemmer PStem = new PorterStemmer();
        Word WafterStem = null;
        for(String w : Words)
        {
            PStem.setCurrent(w);
            PStem.stem();
            WafterStem = new Word(PStem.getCurrent(), Pos);
            //System.out.println(WafterStem);
            AddtoRes(Res, WafterStem, Test);
        }
        System.out.printf("Body after Stemming & removing Duplicates: %s\n", Test);
    }

    public int GetIndex(ArrayList<Word> Res, Word WafterStem)
    {
        int i = 0;
        for(Word r : Res)
        {
            if(r.text.equals(WafterStem.text) && r.Pos.equals(WafterStem.Pos))
            {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void AddtoRes(ArrayList<Word> Res, Word WafterStem, ArrayList<String> Test)
    {
        int index = GetIndex(Res, WafterStem);
        if(index == -1)
        {
            Res.add(WafterStem);
            Test.add(WafterStem.text);
        }
        else
        {
            Word temp = Res.get(index);
            temp.TF++;
            Res.set(index, temp);
        }
    }

    public void NormalizeTF(ArrayList<Word> Res)
    {
        for(Word w : Res)
        {
            w.TF/=Res.size();
        }
    }

    public static void main(String args[])
    {
        Indexer i = new Indexer("http://www.webmd.com/pain-management/carpal-tunnel/carpal-tunnel-syndrome#1");
        i.Index();
    }
}


