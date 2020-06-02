import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.*;
import com.mongodb.client.MongoCursor;
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
	public boolean addVisited(String URI , String AllContent,String Title,String Text,String urlLastModified) {
		
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
		Document docTemp = new Document("url", URI).append("Title", Title).append("Date", urlLastModified).append("Indexed", 0).append("Document", AllContent);
		System.out.println("Adding "+URI+" to Visited");
		VisitedCollection.insertOne(docTemp);
		return true;
	}
	
	public void deleteRepeatedUrls() {
		FindIterable<Document> iterable = VisitedCollection.find();
		iterable.noCursorTimeout(true);
		int count = 0;
		for(Document Doc : iterable)
		{
			String URL = (String) Doc.get("url");
			long found = VisitedCollection.countDocuments(Filters.eq("url", URL));
			Document result = null;
			
			
			if (URL.substring(URL.length() - 1).equals("/"))
				result = VisitedCollection.find(Filters.eq("URL",URL.substring(0, URL.length() - 1))).first();
			else
				result = VisitedCollection.find(Filters.eq("URL",URL+"/")).first();
			
			if(found > 1)
			{
				System.out.println("Repeated url "+ URL);
				VisitedCollection.deleteOne(Filters.eq("url",URL));
				count = count + 1;
			}
			
			if(result != null)
			{
				System.out.println("Repeated url with/without /: "+ URL);
				VisitedCollection.deleteOne(Filters.eq("url",URL));
				count = count + 1;
			}
		}
		System.out.println("Repeated Count = "+ Integer.toString(count));
	}
	
	public void removeBackSlash() {		
		FindIterable<Document> iterable = VisitedCollection.find();
		iterable.noCursorTimeout(true);
		for(Document Doc : iterable)
		{
			String URL = (String) Doc.get("url");
			if (URL.substring(URL.length() - 1).equals("/"))
			{
				Document Query = new Document("url", URL);
				Document newDoc = new Document("url",URL.substring(0, URL.length() - 1));
				Document UpdatedDoc = new Document("$set", newDoc);
				VisitedCollection.updateOne(Query,UpdatedDoc);
			}
		}
	}
	
//	public void removeFromWords(String Url) {
//		FindIterable<Document> iterable = WordsCollection.find(new BasicDBObject("URLs.Url",Url));
//		iterable.noCursorTimeout(true);
//		int count = 0;
//		for(Document Doc : iterable) {
//			count = count + 1;
//		}
//		System.out.println("Was included in "+Integer.toString(count)+" Words");
//	}
	
	public void indexerPostProcessing() {
		FindIterable<Document> iterable = URLsCollection.find();
		iterable.noCursorTimeout(true);
		int count = 0;
		for(Document Doc : iterable)
		{
			try {
				Set<String> FoundLinks = new HashSet<String>();
				ArrayList<Document> LinksDocument =  (ArrayList<Document>) Doc.get("Links");
				Set<Document> SetLinksDocument = new HashSet<Document>(LinksDocument);
				String currURL = (String )Doc.get("URL");
	//			System.out.println(currURL);
				for(Document Link : SetLinksDocument)
				{
					String LinkString = (String) Link.get("Link");
					Document result1 = URLsCollection.find(Filters.eq("URL",LinkString)).first();
					Document result2 = null;
					
					if (LinkString.substring(LinkString.length() - 1).equals("/"))
						result2 = URLsCollection.find(Filters.eq("URL",LinkString.substring(0, LinkString.length() - 1))).first();
					else
						result2 = URLsCollection.find(Filters.eq("URL",LinkString+"/")).first();
					
					if (result1 != null || result2 != null)
						FoundLinks.add(LinkString);
	//				else
	//					System.out.println("Not in our DB: "+LinkString);
				}
	//			System.out.println(FoundLinks.toString());
				URLsCollection.updateOne(new BasicDBObject("URL", currURL), new BasicDBObject("$unset", new BasicDBObject("Links", "")));
				URLsCollection.updateOne(new BasicDBObject("URL", currURL), new BasicDBObject("$set", new BasicDBObject("Links", FoundLinks)));
				count = count + 1;
				System.out.println("Number of processed documents: "+Integer.toString(count));
			} catch (Exception e) {
				count = count + 1;
				System.out.println("(Already processed) Number of processed documents: "+Integer.toString(count));
			}
		}
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
					   ArrayList<Document> H4, ArrayList<Document> H5, ArrayList<Document> H6, ArrayList<Document> P, String Geo, String pubDate, ArrayList<Document> Links)
	{
		Document found = URLsCollection.find(Filters.eq("URL",URL)).first();
		if(found == null)
		{
			Document newURL = new Document("URL", URL).append("Title", Title).append("Country", Geo).append("pubDate", pubDate).append("Summary", Summary).append("Body", Body).append("H1",H1).append("H2",H2).append("H3",H3).append("H4",H4).append("H5",H5).append("H6",H6).append("P",P).append("Links",Links).append("Popularity",0);
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
		FindIterable<Document> iterable = WordsCollection.find(Filters.eq("IDF",0));
		iterable.noCursorTimeout(true);
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
		String pubDate = (String) found.get("pubDate");
		Double Pop = (Double) found.get("Popularity");
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
		return new QueryResult(Word,BTF,H1TF,H2TF,H3TF,H4TF,H5TF,H6TF,PTF,IDF,URL,Title,Summary,Geo,Pop, pubDate);
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
			String pubDate = (String) found.get("pubDate");
			Double Pop = (Double) found.get("Popularity");
			List <String> SummaryList = Arrays.asList(SummaryLC.split(" "));
			int TF = 0;
			if(SummaryLC.contains(SearchPhrase.toLowerCase()))
			{
				TF = Collections.frequency(SummaryList, SearchPhrase.toLowerCase());
				Res.add(new PhraseResult(SearchPhrase, url, Summary, Title, TF, Geo, Pop, pubDate));
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

	public void AddtoQueryCollection(String Name, String UserCountry)
	{
		Document found = QueriesCollection.find(Filters.eq("Name",Name)).first();
		int Freq = 0;
		if(found == null) {
			QueriesCollection.insertOne(new Document("Name", Name).append("Freq",0).append("User Country", UserCountry));
		}
		else
		{
			Freq = (int) found.get("Freq");
			Document Query = new Document("Name", Name);
			Document newDoc = new Document("Freq", Freq+1);
			Document UpdatedDoc = new Document("$set", newDoc);
			QueriesCollection.updateOne(Query,UpdatedDoc);
		}
	}

	public ArrayList<ImageResult> getImage(String Word)
	{
		ArrayList<ImageResult> Res = new ArrayList<>();
		FindIterable<Document> iterable = ImagesCollection.find(Filters.and(Filters.regex("altText", ".*"+Word+".*"),(Filters.regex("src", "http.*"))));
		iterable.noCursorTimeout(true);
		for(Document Doc : iterable)
			Res.add(new ImageResult(Doc.get("src").toString(), Doc.get("altText").toString()));

		return Res;
	}
	public MongoCollection<Document> returnIndexed()
	{
		return URLsCollection;
	}

	public void updatePopularity (String url,double popularityScore)
	{
		Document Query = new Document("URL", url);
		Document newDoc = new Document("Popularity",popularityScore);
		Document UpdatedDoc = new Document("$set", newDoc);
		URLsCollection.updateOne(Query,UpdatedDoc);
	}

	public ArrayList<TrendsResult> Trends (String Country)
	{
		ArrayList<TrendsResult> Res = new ArrayList<>();
		ArrayList<Document> found = (ArrayList<Document>) QueriesCollection.find(Filters.eq("User Country", Country));
		TrendsResult newT = null;
		for (Document f : found)
		{
			newT= new TrendsResult(f.get("Name").toString(), (int) f.get("Freq"));
			Res.add(newT);
		}
		Collections.sort(Res, new TrendsComparator());
		return Res;
	}

	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter(false);
		DBAdapeter.init(false);
//		DBAdapeter.deleteRepeatedUrls();
		DBAdapeter.indexerPostProcessing();
	}

}
