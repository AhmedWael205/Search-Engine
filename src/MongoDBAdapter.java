import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.*;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.jsoup.Jsoup;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.html.HTMLDocument;

public class MongoDBAdapter {

	private MongoCollection<Document> WordsCollection;
	private MongoCollection<Document> URLsCollection;
	private MongoCollection<Document> UnvisitedCollection;
	private MongoCollection<Document> VisitedCollection;
	private MongoCollection<Document> RobotCollection;
	private MongoCollection<Document> ImagesCollection;
	private MongoCollection<Document> QueriesCollection;
	private MongoClientURI uri;
	private MongoClient mongoClient;
	private MongoDatabase database;
	private double MAX_NO_DOC;

	public MongoDBAdapter(boolean Global) {
		Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
		if(Global) {
			this.uri = new MongoClientURI( "mongodb+srv://Crawler2:Crawler123@crawlercluster-kgwg6.mongodb.net/retryWrites=true&w=majority" );
			this.mongoClient = new MongoClient(uri);
			this.database = mongoClient.getDatabase("Test");
		} else {
			this.mongoClient = new MongoClient( "localhost" , 27017 );
			this.database = mongoClient.getDatabase("Crawler");
		}
	}
	public void init(boolean DropTables) {
			 
		      System.out.println("Database Connected Successfully ...");
		      
		      if(DropTables) {
			      // Deleting Old Collections if Exists
			      MongoCollection<Document> OLDUnvisited = database.getCollection("Unvisited");
			      OLDUnvisited.drop();
			      MongoCollection<Document> OLDVisited = database.getCollection("Visited");
			      OLDVisited.drop();
			      MongoCollection<Document> OLDRobot = database.getCollection("Robot");
			      OLDRobot.drop();
				  MongoCollection<Document> OLDWords = database.getCollection("Words");
				  OLDWords.drop();
				  MongoCollection<Document> OLDURLs = database.getCollection("URLs");
				  OLDURLs.drop();
				  MongoCollection<Document> OLDImages = database.getCollection("Images");
				  OLDImages.drop();
				  MongoCollection<Document> OLDQueries = database.getCollection("Queries");
				  OLDQueries.drop();
			      
			      // Creating a new Collections
			      database.createCollection("Unvisited");
			      database.createCollection("Visited");
			      database.createCollection("Robot");
				  database.createCollection("Words");
				  database.createCollection("URLs");
				  database.createCollection("Images");
				  database.createCollection("Queries");
		      }
		      
		      UnvisitedCollection = database.getCollection("Unvisited");
		      VisitedCollection = database.getCollection("Visited");
		      RobotCollection = database.getCollection("Robot");
		      WordsCollection = database.getCollection("Words");
		      URLsCollection = database.getCollection("URLs");
		      ImagesCollection = database.getCollection("Images");
		      QueriesCollection = database.getCollection("Queries");
		      
		      if (DropTables) {
			      //Reading Initial Seed Set from File
			      List<Document> documents = new ArrayList<Document>();
				  BufferedReader reader;
				  Document docTemp;
				  System.out.println("Initiallizing Seed Set with ...");
				  try {
						reader = new BufferedReader(new FileReader((".\\SeedSet.txt")));
						String line = reader.readLine();
						while (line != null) {
							line = line.replaceAll("[^:]//", "/").toLowerCase();
							docTemp = new Document("url", line);
							System.out.println("Adding "+line+" to Unvisited");
							documents.add(docTemp);
							// read next line
								line = reader.readLine();
							}
						reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			    // Insert Multiple Documents
				  UnvisitedCollection.insertMany(documents);
		      }
			  
		  }
				
	
	
	public String getUnvisited() {
		
		if (UnvisitedCollection.countDocuments() > 0 ) {
			Document result = UnvisitedCollection.find(Filters.regex("url", ".*"+".com.*?")).first();
			String url;
			try{
				url = result.get("url").toString();
			}
			catch (NullPointerException e) {
				result = UnvisitedCollection.find().first();
				url = result.get("url").toString();	
			}
			UnvisitedCollection.deleteOne(Filters.eq("url",url));
			System.out.println("Removing "+url+" from Unvisited");
			System.out.println("Remaining unvisited urls: "+UnvisitedCollection.countDocuments());
			return url;
		}
		return "false";
	}
	
	Block<Document> printBlock = new Block<Document>() {
	       @Override
	       public void apply(final Document document) {
	           System.out.println(document.toJson());
	       }
	};
	
	public int addManyUnvisited(Set<String> URIs) {
		List<Document> documents = new ArrayList<Document>();
		Document docTemp;
		for(String URI : URIs){
			URI = URI.replaceAll("[^:]//", "/").toLowerCase();
			Document result1 = UnvisitedCollection.find(Filters.eq("url",URI)).first();
			Document result2 = VisitedCollection.find(Filters.eq("url",URI)).first();
			
			if(result1 == null &&  result2 == null) {

				docTemp = new Document("url", URI);
				System.out.println("Adding "+URI+" to Unvisited");
				documents.add(docTemp);
			} else {
				continue;
			}	
		}
		if(documents.size()!= 0)
			UnvisitedCollection.insertMany(documents);
		return documents.size();
	}
	
	public boolean addUnvisited(String URI) {
		
		URI = URI.replaceAll("[^:]//", "/").toLowerCase();
		Document result1 = UnvisitedCollection.find(Filters.eq("url",URI)).first();
		Document result2 = VisitedCollection.find(Filters.eq("url",URI)).first();
		
		if(result1 == null &&  result2 == null) {
			
			URI.toLowerCase();
			Document docTemp = new Document("url", URI);
			System.out.println("Adding "+URI+" to Unvisited");
			UnvisitedCollection.insertOne(docTemp);
			return true;
		}
		return false;
	}
	
	public int addRobots(List<String> URIs) {
		int count = 0;
		Document docTemp;
		Document result = null;
		for(String URI : URIs) {
			
			URI = URI.replaceAll("[^:]//", "/").toLowerCase();
			result = RobotCollection.find(Filters.eq("urlRegex",URI)).first();
			
			if(result == null) {
				docTemp = new Document("urlRegex", URI);
				System.out.println("Adding "+URI+" to Robot");
				RobotCollection.insertOne(docTemp);
				count++;
			} else {
				continue;
			}
		}		
		return count;
	}
	
	public boolean inRobots(String URI)	{
		if (RobotCollection.find(Filters.eq("urlRegex",URI)).first() != null) return true;

		FindIterable<Document> result = null;
		result = RobotCollection.find(Filters.regex("urlRegex", ".*"+"\\*"+".*?"));
		URI = URI.replaceAll("[^:]//", "/").toLowerCase();
		System.out.println("Check this in Robot "+ URI);
		for(Document currRegex : result)
		{
			String regex = currRegex.get("urlRegex").toString();
			
			if(URI.matches(regex)) {
				return true;
			}
		}		
		return false;
	}
	
	public long visitedCount() {return VisitedCollection.countDocuments();}
	public long unvisitedCount() {return UnvisitedCollection.countDocuments();}
	
	
	@SuppressWarnings("deprecation")
	public boolean addVisited(String URI , String AllContent,String Title,String Text) {
		
		if(VisitedCollection.find(Filters.eq("url",URI)).first() != null) return false;
		
		Document result = VisitedCollection.find(Filters.eq("Title",Title)).first();
		if(result != null)
		{	
			org.jsoup.nodes.Document document = Jsoup.parse(result.get("Document").toString());
			int distance = StringUtils.getLevenshteinDistance(Text.toLowerCase(), document.text().toString().toLowerCase());
			float percentage = (float)distance * 100 / Text.length(); 
			
			if(percentage < 20) {
				
				int distance2 = StringUtils.getLevenshteinDistance(AllContent.toLowerCase(), document.toString().toLowerCase());
				float percentage2 = (float)distance2 * 100 / AllContent.length(); 
				if(percentage2 < 20) {
					return false;
				}
			}				
		}
		Document docTemp = new Document("url", URI).append("Title", Title).append("Indexed", 0).append("Document", AllContent);
		System.out.println("Adding "+URI+" to Visited");
		VisitedCollection.insertOne(docTemp);
		return true;
	}

	public Document getDoctoIndex() {
		Document Res = VisitedCollection.find(Filters.eq("Indexed", 0)).first();
		updateIndexedVisits(Res.get("url").toString());
		return Res;
	}

	public long getIndexedCount()
	{
		Document Query = new Document("Indexed", 0);
		return VisitedCollection.countDocuments(Query);
	}

	public void updateIndexedVisits(String URL)
	{
		Document Query = new Document("url", URL);
		Document newDoc = new Document("Indexed",1);
		Document UpdatedDoc = new Document("$set", newDoc);
		VisitedCollection.updateOne(Query,UpdatedDoc);
//		System.out.printf("Changed Indexed Flag to 1 for URL %s before Parsing\n", URL);
	}

	public void deleteHTMLAfterVisit(String URL)
	{
		Document Query = new Document("url", URL);
		Document newDoc = new Document("Indexed",1).append("Document", "");
		Document UpdatedDoc = new Document("$set", newDoc);
		VisitedCollection.updateOne(Query,UpdatedDoc);
//		System.out.printf("Removed Document from URL %s after Parsing\n", URL);
	}

	public boolean prevAddedURL(ArrayList<Document> URLs, Document URL)
	{
		for(Document u : URLs)
		{
			if(u.equals(URL))
			{
				return true;
			}
		}
		return false;
	}
	
	public void addWord(String Word, String URL)
	{
		Document found = WordsCollection.find(Filters.eq("Word",Word)).first();
		if(found == null)
		{
			//Not Found hence only add to Collection
			ArrayList<Document> URLList = new ArrayList<>();
			Document Obj = new Document("Url", URL);
			URLList.add(Obj);
			Document newWord = new Document("Word", Word).append("URLs", URLList).append("IDF", 0);
			WordsCollection.insertOne(newWord);
		}
		else
		{
			//Found; hence we need to update IDF.
			Document Query = new Document("Word", Word);
			Document WordToUpdate = WordsCollection.find(Filters.eq("Word", Word)).first();
			ArrayList<Document> urls = (ArrayList<Document>) WordToUpdate.get("URLs");
			Set<Document> URLs = new HashSet<Document>(urls);
			//System.out.println(URLs);
			Document URLtoAdd = new Document("Url", URL);
			boolean added = URLs.add(URLtoAdd);
			//We need to make sure the URL wasn't previously added
			if(added)
			{
				Document newDoc = new Document("URLs", URLs);
				Document UpdatedDoc = new Document("$set", newDoc);
				WordsCollection.updateOne(Query,UpdatedDoc);
//				System.out.println("Updated URL List in Word");
			}
			else
			{
//				System.out.println("URL already exists in URL List. No Update!");
			}
		}
	}

	public void addURL(String URL, String Title, String Summary, ArrayList<Document> Body, ArrayList<Document> H1,  ArrayList<Document> H2, ArrayList<Document> H3,
					   ArrayList<Document> H4, ArrayList<Document> H5, ArrayList<Document> H6, ArrayList<Document> P, String Geo)
	{
		Document found = URLsCollection.find(Filters.eq("URL",URL)).first();
		if(found == null)
		{
			Document newURL = new Document("URL", URL).append("Title", Title).append("Summary", Summary).append("Country", Geo).append("Body", Body).append("H1",H1).append("H2",H2).append("H3",H3).append("H4",H4).append("H5",H5).append("H6",H6).append("P",P);
			URLsCollection.insertOne(newURL);
//			System.out.println("Inserted new URL into table");
		}
		else
		{
//			System.out.println("URL already exsists");
		}
	}

	public void addImages(ArrayList<Document> Images)
	{
		ImagesCollection.insertMany(Images);
	}

	public void calculateIDF()
	{
		System.out.println("Started Calculating IDFs");
		FindIterable<Document> iterable = WordsCollection.find();
		MAX_NO_DOC = visitedCount();
		for(Document Doc : iterable)
		{
			ArrayList<String> URLS = (ArrayList<String>) Doc.get("URLs");
			//int idf = (int) Doc.get("IDF");
			String word = Doc.get("Word").toString();
			//Note: Don't know how accurate it is maybe need to change later
			double IDF = Math.log((double)URLS.size()/MAX_NO_DOC);
			Document Query = new Document("Word", word);
			Document newDoc = new Document("IDF", IDF);
			Document UpdatedDoc = new Document("$set", newDoc);
			WordsCollection.updateOne(Query, UpdatedDoc);
		}
		System.out.println("Finished Calculating IDFs");
	}

	public ArrayList<String> WordSearchURL(String Word)
	{
		ArrayList<String> Res = new ArrayList<>();
		Document found = WordsCollection.find(Filters.eq("Word",Word)).first();
		if (found == null)
		{
			Res = null;
		}
		else
		{
			ArrayList<Document> URLs = (ArrayList<Document>) found.get("URLs");
			for (Document url : URLs)
			{
				Res.add(url.get("Url").toString());
			}
		}
		return Res;
	}

	public double ReturnIDF(String Word)
	{
		double Res = 10000000;
		Document found = WordsCollection.find(Filters.eq("Word", Word)).first();
		if (found == null) {
			return Res;
		} else {
			return (double)found.get("IDF");
		}
	}

	public QueryResult QueryProcessorRes(String Word, String URL, double IDF)
	{
		Document found = URLsCollection.find(Filters.eq("URL", URL)).first();
		String Title = (String) found.get("Title");
		String Summary = (String) found.get("Summary");
		String Geo = (String) found.get("Country");
		ArrayList<Document> Body = (ArrayList<Document>) found.get("Body");
		double BTF = RetTF(Body,Word);
		ArrayList<Document> H1 = (ArrayList<Document>) found.get("H1");
		double H1TF = RetTF(H1,Word);
		ArrayList<Document> H2 = (ArrayList<Document>) found.get("H2");
		double H2TF = RetTF(H2,Word);
		ArrayList<Document> H3 = (ArrayList<Document>) found.get("H13");
		double H3TF = RetTF(H3,Word);
		ArrayList<Document> H4 = (ArrayList<Document>) found.get("H4");
		double H4TF = RetTF(H4,Word);
		ArrayList<Document> H5 = (ArrayList<Document>) found.get("H5");
		double H5TF = RetTF(H5,Word);
		ArrayList<Document> H6 = (ArrayList<Document>) found.get("H6");
		double H6TF = RetTF(H6,Word);
		ArrayList<Document> P = (ArrayList<Document>) found.get("P");
		double PTF = RetTF(P,Word);
		return new QueryResult(Word,BTF,H1TF,H2TF,H3TF,H4TF,H5TF,H6TF,PTF,IDF,URL,Title,Summary,Geo);
	}

	public ArrayList<PhraseResult> PhraseSearchRes(String SearchPhrase, ArrayList<String> URLs)
	{
		ArrayList<PhraseResult> Res = new ArrayList<>();
		for(String url: URLs)
		{
			Document found = URLsCollection.find(Filters.eq("URL", url)).first();
			String Summary = (String) found.get("Summary");
			String SummaryLC = Summary.toLowerCase();
			String Title = (String) found.get("Title");
			String Geo = (String) found.get("Country");
			List <String> SummaryList = Arrays.asList(SummaryLC.split(" "));
			int TF = 0;
			if(SummaryLC.contains(SearchPhrase.toLowerCase()))
			{
				TF = Collections.frequency(SummaryList, SearchPhrase.toLowerCase());
				Res.add(new PhraseResult(SearchPhrase, url, Summary, Title, TF, Geo));
			}
		}
		return Res;
	}

	public double RetTF(ArrayList<Document> Arr, String Word)
	{
		double TF = 0.0;
		if(Arr == null)
		{
			return TF;
		}
		else{
			for(Document doc:Arr)
			{
				if(doc.get("Word").toString().equals(Word))
				{
					TF = Double.valueOf(doc.get("TF").toString());
				}
			}
			return TF;
		}
	}

	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter(false);
		DBAdapeter.init(false);
		DBAdapeter.inRobots("https://www.ft.co/search");
	}

}
