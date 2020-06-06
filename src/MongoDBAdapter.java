import java.io.*;
import java.lang.reflect.Array;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private MongoCollection<Document> CacheCollection;
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
				  MongoCollection<Document> OLDCache = database.getCollection("Cache");
				  OLDCache.drop();
			      
			      // Creating a new Collections
			      database.createCollection("Unvisited");
			      database.createCollection("Visited");
			      database.createCollection("Robot");
				  database.createCollection("Words");
				  database.createCollection("URLs");
				  database.createCollection("Images");
				  database.createCollection("Queries");
				  database.createCollection("Cache");
		      }
		      
		      UnvisitedCollection = database.getCollection("Unvisited");
		      VisitedCollection = database.getCollection("Visited");
		      RobotCollection = database.getCollection("Robot");
		      WordsCollection = database.getCollection("Words");
		      URLsCollection = database.getCollection("URLs");
		      ImagesCollection = database.getCollection("Images");
		      QueriesCollection = database.getCollection("Queries");
		      CacheCollection = database.getCollection("Cache");
		      
		      if (DropTables) {
			      //Reading Initial Seed Set from File
			      List<Document> documents = new ArrayList<Document>();
				  BufferedReader reader;
				  Document docTemp;
				  System.out.println("Initiallizing Seed Set with ...");
				  try {
						reader = new BufferedReader(new FileReader(("./resources/SeedSet.txt")));
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
			Document result = UnvisitedCollection.findOneAndDelete(Filters.regex("url", ".*"+".com.*?"));
			String url;
			try{
				url = result.get("url").toString();
			}
			catch (NullPointerException e) {
				result = UnvisitedCollection.findOneAndDelete(Filters.regex("url", ".*"));
				url = result.get("url").toString();	
			}
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
	public long indexedCount() {return URLsCollection.countDocuments();}
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
				result = VisitedCollection.find(Filters.eq("url",URL.substring(0, URL.length() - 1))).first();
			else
				result = VisitedCollection.find(Filters.eq("url",URL+"/")).first();
			
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
	
	public void SetIndextoValue(int x) {
		
		BasicDBObject searchQuery = new BasicDBObject();
		int y = 1- x;
		searchQuery.append("Indexed", y);

		BasicDBObject updateQuery = new BasicDBObject();
		updateQuery.append("$set",	new BasicDBObject().append("Indexed", x));

		VisitedCollection.updateMany(searchQuery, updateQuery);

	}
	
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
//		FindIterable<Document> iterable = WordsCollection.find();
		iterable.noCursorTimeout(true);
		MAX_NO_DOC = indexedCount();
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

	public ArrayList<PhraseResult> PhraseSearchRes(String SearchPhrase, Set<String> URLs)
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
		if(!Name.equals("")) {
			Document found = QueriesCollection.find(Filters.and(Filters.eq("Name", Name),Filters.eq("User Country", UserCountry))).first();
			int Freq = 0;
			if (found == null) {
				QueriesCollection.insertOne(new Document("Name", Name).append("Freq", 1).append("User Country", UserCountry));
			} else {
				Freq = (int) found.get("Freq");
//				System.out.println(Freq);
				Document Query = new Document("Name", Name);
				int x = Freq + 1;
				Document newDoc = new Document("Freq", x);
				Document UpdatedDoc = new Document("$set", newDoc);
				QueriesCollection.updateOne(Query, UpdatedDoc);
			}
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
	
	public void AddFieldPopCalc()
	{
		URLsCollection.updateMany(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject("PopCalc", 0)));
	}

	public void updatePopularity (String url,double popularityScore)
	{
		BasicDBObject updateFields = new BasicDBObject();
		updateFields.append("Popularity", popularityScore);
		updateFields.append("PopCalc", 1);
		BasicDBObject setQuery = new BasicDBObject();
		setQuery.append("$set", updateFields);
		
		Document Query = new Document("URL", url);
		
		URLsCollection.updateOne(Query,setQuery);
		
	}
	
	public ArrayList<TrendsResult> Trends (String Country)
	{
		ArrayList<TrendsResult> Res = new ArrayList<>();
		FindIterable<Document> found = QueriesCollection.find(Filters.eq("User Country", Country));
		found.noCursorTimeout(true);
		TrendsResult newT = null;
		for (Document f : found)
		{
			newT= new TrendsResult(f.get("Name").toString(), (int) f.get("Freq"));
			Res.add(newT);
		}
		Collections.sort(Res, new TrendsComparator());
		return Res;
	}
	
	public ArrayList<URLResult> getCached (String Query)
    {
		BasicDBObject ref = new BasicDBObject();
		ref.put("query", Pattern.compile(Query, Pattern.CASE_INSENSITIVE));
		Document Found = CacheCollection.find(ref).first();
		
		if (Found == null) return null;
		ArrayList<URLResult> Res = new ArrayList<>();
		ArrayList<Document> UrlResults =  (ArrayList<Document>) Found.get("Links");
		for(Document Link : UrlResults)
		{
			String url = (String)Link.get("URL");
			String Title = (String)Link.get("Title");
			String Summary = (String)Link.get("Summary");
			String pubDate = (String)Link.get("pubDate");
			Res.add(new URLResult(url,Title,Summary,pubDate));
		}  	
    	return Res;
    }
	
	public boolean addCache (String Query,ArrayList<URLResult> UrlResult)
    {
		Document Found = CacheCollection.find(Filters.eq("query", Query)).first();
		if(Found != null) return false;
		
		Document newCache = new Document("query", Query);
		ArrayList<Document> URLS = new ArrayList<>();
		newCache.append("Links", URLS);
		CacheCollection.insertOne(newCache);
		
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("query", Query);
		
		for(URLResult Url : UrlResult)
		{
			BasicDBObject listItem = new BasicDBObject("Links", new BasicDBObject("URL", Url.url).append("Title", Url.Title).append("pubDate", Url.pubDate).append("Summary", Url.Summary));
			BasicDBObject updateQuery = new BasicDBObject("$push", listItem);
			CacheCollection.findOneAndUpdate(searchQuery, updateQuery);
		}
		
		
		return true;
    }


	public static void main( String args[] ) {  

//		MongoDBAdapter DBAdapeter = new MongoDBAdapter(false);
//		DBAdapeter.init(false);
//		DBAdapeter.deleteRepeatedUrls();
//		DBAdapeter.indexerPostProcessing();
//		DBAdapeter.SetIndextoValue(0);
//		DBAdapeter.AddFieldPopCalc();
		
//		Object WordLock = new Object();
//        Object URLLock = new Object();
//		
//		LocalTime myObj1;
//        LocalTime myObj2;
//        Indexer index = new Indexer(DBAdapeter, WordLock, URLLock);
//		
//		myObj1 = LocalTime.now();
//        System.out.println(myObj1);
//        
//        index.FinalizeIDF();
//
//        myObj2 = LocalTime.now();
//        System.out.println(myObj2);
		ArrayList<String>StopWords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(("./resources/StopWords.txt")))) {
            while (reader.ready()) {
                StopWords.add(reader.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		String query = "\" messi on Car\"";
		ArrayList<String>SearchWords = new ArrayList<>();
		String SearchPhrase = "";
		
		String [] QueryArray = query.split(" ");
        for(String W : QueryArray)
        {
            SearchWords.add(W.replaceAll("([^a-zA-Z ])", ""));
        }
        String pattern = "\"([^\"]*)\"";
        Pattern r1 = Pattern.compile(pattern);
        Matcher m2 = r1.matcher(query);
        if(m2.find())
        {
            SearchPhrase = m2.group();
        }
        SearchPhrase = SearchPhrase.replaceAll("([^a-zA-Z ])", "").trim();
        System.out.println(SearchPhrase);
        System.out.println(SearchWords);
        ArrayList<String> SW = new ArrayList<>(SearchWords);
        SearchWords.clear();
        String toReplace = "|(";
//        String 
        for (String w : SW)
        {
            w = w.replaceAll("([^a-zA-Z])", "");
            if (!StopWords.contains(w) && w.length() >1)
            {
            	toReplace = toReplace + w + ")|(";
            }
        }
        toReplace = toReplace.substring(0, toReplace.length()-2);
        String x = SearchPhrase + toReplace;
        System.out.println(x);
        String z = x.substring(x.indexOf("|")+1,x.length()).trim();
        if(SearchPhrase=="")
        	x = z;
        System.out.println(z);
        
		int N = 40;
		

		Pattern p = Pattern.compile("((([a-zA-Z1234567890&(),@_':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?(?i)("+x+"[,:]?)\\s*((([a-zA-Z1234567890&()@_,':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?");
		Matcher m = p.matcher("Premier League Transfers Champions FanVoice The Switch EFL La Liga Serie A Bundesliga MLS Women's Football More EN EN ES ES-LATAM DE IT FR TR PT-BR VN ID TH Premier League Transfers Champions FanVoice The Switch EFL La Liga Serie A Bundesliga MLS Women's Football Liverpool vs Everton: The 'Friendly' Derby That Is More Important Than Just Football Ewan Murray | 2:00 PM GMT+1 Liverpool vs Everton: 6 Classic Merseyside Derby Clashes Ewan Murray | 3:45 PM GMT+1 Liverpool vs Everton: A Combined XI of Merseyside Derby Stars Ewan Murray | 3:47 PM GMT+1 Main Stories 8 of Football's Bright Young Talents Inspired by Magician Ronaldinho Grey Whitebloom | 1:06 PM GMT+1 Mikel Arteta Preparing to 'Initiate' Talks With Arsenal Captain Pierre-Emerick Aubameyang Adam Aladay | 12:25 PM GMT+1 5 Barcelona Players Tested Positive for Coronavirus Early in Pandemic Tom Gott | 10:13 AM GMT+1 Ralph Hasenhüttl Signs New Four-Year Contract at Southampton Tom Gott | 9:57 AM GMT+1 FA Pledges 'Common Sense' Approach to Dealing With Black Lives Matter Gestures in Premier League Tom Gott | 3:49 PM GMT+1 FanVoice Philippe Coutinho: Which Premier League Club Would Suit the Brazilian Best? Adam Aladay | 3:21 PM GMT+1 Sergio Aguero: Still Manchester City's Go-to Superstar After All These Years Robbie Walls | 11:31 AM GMT+1 9 English Players Who Have Scored Hat-tricks in Other Leagues Robbie Copeland | Jun 1, 2020 Why Casemiro Is Real Madrid's Most Important Player in the Post-Galacticos Era Josh Sim | Jun 1, 2020 Harvey Elliott: Liverpool's Little Diamond Who Has a Big Role to Play at Anfield Robbie Copeland | Jun 1, 2020 Editor's Picks Liverpool vs Everton: A Combined XI of Merseyside Derby Stars Ewan Murray | 3:47 PM GMT+1 Liverpool vs Everton: 6 Classic Merseyside Derby Clashes Ewan Murray | 3:45 PM GMT+1 Liverpool vs Everton: The 'Friendly' Derby That Is More Important Than Just Football Ewan Murray | 2:00 PM GMT+1 Ajax vs Feyenoord: 5 Classic Encounters From Dutch Football's Klassieker James Cormack | Jun 1, 2020 Ajax vs Feyenoord: The Netherlands' Klassieker Rooted in City Pride & Resentful Rotterdamers James Cormack | Jun 1, 2020 Inter vs Juventus: 6 of the Best Games in the Derby d'Italia's History Josh Sim | May 29, 2020 Premier League Tottenham Boss José Mourinho Sanctions Danny Rose Loan Extension at Newcastle United The extension of Danny Rose's loan spell from Tottenham to Newcastle United has been approved by José Mourinho Grey Whitebloom | 2:22 PM GMT+1 The Paused Premier League Narratives to Keep Your Eyes on When Football Returns Premier League narratives to remember for when football returns, including Liverpool's pursuit of records, Sheffield United's Champions League run & more Tom Gott | 12:21 PM GMT+1 Premier League Project Restart: Bigger Benches, Fewer Neutral Games Than Expected, In-Game Interviews & More Project restart Premier League news. Neutral venues, coronavirus health and safety measures. Jamie Spencer | 11:09 AM GMT+1 Ralph Hasenhüttl Signs New Four-Year Contract at Southampton Southampton confirm manager Ralph Hasenhuttl has signed a new four-year contract Tom Gott | 9:57 AM GMT+1 Chelsea Submit Proposal to Increase Number of Premier League Substitutes to Nine Chelsea propose allowing nine substitutes ahead of Premier League's return Ali Rampling | Jun 1, 2020 Kick It Out Chief Urges All Premier League Players to Take a Knee Over George Floyd Killing Kick It Out urges players to take a knee in protest of George Floyd's killing and in support of Black Lives Matter Ali Rampling | Jun 1, 2020 Transfers Mikel Arteta Preparing to 'Initiate' Talks With Arsenal Captain Pierre-Emerick Aubameyang Mikel Arteta will attempt to convince Pierre-Emerick Aubameyang to commit his future to Arsenal and sign a new deal. Adam Aladay | 12:25 PM GMT+1 Spurs Interested in Rennes Striker M'Baye Niang as Harry Kane Backup Tottenham Hotspor set to open talks for Rennes forward M'Baye Niang Robbie Walls | 11:50 AM GMT+1 Liverpool Agree to Extend Harry Wilson & Rhian Brewster Loans Until End of Season Liverpool have agreed to let Harry Wilson and Rhian Brewster finish the 2019/20 season with their loan clubs. Adam Aladay | 10:04 AM GMT+1 Marcus Rashford 'Said Yes' to Barcelona Before Backing Out of Move Marcus Rashford transfer news latest - Barcelona. Manchester United future uncertain in 2018 and 2019. Camp Nou talks. Jamie Spencer | 9:55 AM GMT+1 Chelsea 'Likely' to Make 2 Marquee Signings - Ben Chilwell Still a Priority Chelsea are plotting two marquee signings this summer, with Leicester City's Ben Chilwell a top priority alongside Jadon Sancho & Kai Havertz Tom Gott | 8:44 AM GMT+1 Arsenal Ready to Offer New Contract to David Luiz on Reduced Terms Arsenal are ready to offer David Luiz a new contract on less than his current £130,000-a-week. Tom Gott | 8:33 AM GMT+1 How Much Man Utd Will Pay for Odion Ighalo Loan Extension Manchester United pay £6m to Shanghai Shenhua in loan deal for Odion Ighalo Ali Rampling | Jun 1, 2020 Manchester United Would 'Go For' Raheem Sterling Should Manchester City Star Become Available Manchester United would move for Raheem Sterling if Manchester City winger became available Ali Rampling | Jun 1, 2020 Jeff Hendrick Link to AC Milan Confirms 2020 Is the Weirdest Football Year Ever Burnley's Jeff Hendrick has been linked with AC Milan. Robbie Copeland | Jun 1, 2020 The Switch Nike Football Drop The 'Neighbourhood Pack' Hunter Godson | Jun 1, 2020 New Leaks Appear to Confirm Manchester United 'Dazzle Camo' Third Kit for 2020/21 Robbie Copeland | Jun 1, 2020 Images of Striking Arsenal 2020/21 Away Kit Leaked Online Ali Rampling | May 28, 2020 Fresh Leaks of Manchester United's 2020/21 Home Kit Emerge & it's a Winner James Cormack | May 27, 2020 Nike Drop Striking New Club América Away Shirt Inspired by Glory Days of 1980s Krishan Davis | May 26, 2020 adidas Football Drop UNIFORIA Boot Pack Hunter Godson | May 26, 2020 Women's Football An Ode to Arsenal Women's Serial Winners of the Noughties Ali Rampling | Jun 1, 2020 FA Backing New English Women’s Football Archive Available for Free Online FA backs English women's football archive. Free online archive telling the story of women's football in England. Jamie Spencer | Jun 1, 2020 The England Women's Job Deserves to Be More Than the Launch Pad Phil Neville Used it for The England Women's job deserved to be more than the stepping stone that Phil Neville used it as Ali Rampling | May 28, 2020 6 Things to Know About New Manchester City Women's Boss Gareth Taylor Who is Manchester City Women's new manager Gareth Taylor? Ali Rampling | May 28, 2020 90min - Ranked The 100 Highest-Paid Athletes in 2020 - Ranked The 100 highest paid athletes in the world, including Roger Federer, Cristiano Ronaldo, Lionel Messi, Neymar, Kylian Mbappe, Mohamed Salah & Paul Pogba Tom Gott | May 31, 2020 The 20 Most Valuable Football Clubs in Europe - Ranked The 20 most valuable clubs in Europe including Real Madrid, Barcelona, Manchester United and Liverpool with other Premier League sides Ross Kennerley | May 29, 2020 The 10 Worst Serie A Kits of the Past Decade - Ranked Ranking 10 of the worst Serie A kits over the past decade Lee Bushe | May 28, 2020 Every PFA Young Player of the Year Winner Between 2000-2010 - Ranked Callum Altimas | May 31, 2020 LINKS About Privacy Policy Cookie Policy Contact Us Careers Partners Terms & Conditions FOLLOW US Minute Media © 2020 All rights reserved");
		if (m.find())
		 {
		 	String y = m.group();
		 	System.out.println(y.replaceAll("(?i)("+z+")", "<b>$0</b>"));
		
		 }
		
	}

}
