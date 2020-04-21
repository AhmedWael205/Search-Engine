import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.jsoup.Jsoup;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoTimeoutException;
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
	private MongoClientURI uri;
	private MongoClient mongoClient;
	private MongoDatabase database;
	final double MAX_NO_DOC = 5000;
	
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
			      
			      // Creating a new Collections
			      database.createCollection("Unvisited");
			      database.createCollection("Visited");
			      database.createCollection("Robot");
				  database.createCollection("Words");
				  database.createCollection("URLs");
		      }
		      
		      UnvisitedCollection = database.getCollection("Unvisited");
		      VisitedCollection = database.getCollection("Visited");
		      RobotCollection = database.getCollection("Robot");
		      WordsCollection = database.getCollection("Words");
		      URLsCollection = database.getCollection("URLs");
		      
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
		System.out.printf("Changed Indexed Flag to 1 for URL %s before Parsing\n", URL);
	}

	public void deleteHTMLAfterVisit(String URL)
	{
		Document Query = new Document("url", URL);
		Document newDoc = new Document("Indexed",1).append("Document", "");
		Document UpdatedDoc = new Document("$set", newDoc);
		VisitedCollection.updateOne(Query,UpdatedDoc);
		System.out.printf("Removed Document from URL %s after Parsing\n", URL);
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
	
	public void addWord(String Word, String URL, String Title)
	{
		Document found = WordsCollection.find(Filters.eq("Word",Word)).first();
		if(found == null)
		{
			//Not Found hence only add to Collection
			ArrayList<Document> URLList = new ArrayList<>();
			Document Obj = new Document("Url", URL).append("Title", Title);
			URLList.add(Obj);
			Document newWord = new Document("Word", Word).append("URLs", URLList).append("IDF", 0);
			WordsCollection.insertOne(newWord);
			System.out.println("Inserted new Word into table");
		}
		else
		{
			//Found; hence we need to update URL list.
			Document Query = new Document("Word", Word);
			Document WordToUpdate = WordsCollection.find(Filters.eq("Word", Word)).first();
			ArrayList<Document> URLs = (ArrayList<Document>) WordToUpdate.get("URLs");
			System.out.println(URLs);
			Document URLtoAdd = new Document("Url", URL).append("Title", Title);
			//We need to make sure the URL wasn't previously added
			if(!prevAddedURL(URLs, URLtoAdd))
			{
				URLs.add(URLtoAdd);
				Document newDoc = new Document("URLs", URLs);
				Document UpdatedDoc = new Document("$set", newDoc);
				WordsCollection.updateOne(Query,UpdatedDoc);
				System.out.println("Updated URL List in Word");
			}
			else
			{
				System.out.println("URL already exists in URL List. No Update!");
			}
		}
	}

	public void addURL(String URL, ArrayList<Word> Words, String Title)
	{
		ArrayList<Document> HTMLAnalysis = new ArrayList<>();
		for(Word wrd : Words)
		{
			Document Obj = new Document("Word", wrd.text).append("TF", wrd.TF).append("Pos", wrd.Pos);
			HTMLAnalysis.add(Obj);
		}
		Document newURL = new Document("URL", URL).append("Title", Title).append("HTML Analysis", HTMLAnalysis);
		URLsCollection.insertOne(newURL);
		System.out.println("Inserted new URL into table");
	}

	public void calculateIDF()
	{
		FindIterable<Document> iterable = WordsCollection.find();
		for(Document Doc : iterable)
		{
			ArrayList<String> URLS = (ArrayList<String>) Doc.get("URLs");
			String word = Doc.get("Word").toString();
			//Note: Don't know how accurate it is maybe need to change later
			double IDF = (double)URLS.size()/MAX_NO_DOC;
			Document Query = new Document("Word", word);
			Document newDoc = new Document("IDF", IDF);
			Document UpdatedDoc = new Document("$set", newDoc);
			WordsCollection.updateOne(Query, UpdatedDoc);
		}
		System.out.println("Finished Calculating IDFs");
	}


	
	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter(false);
		DBAdapeter.init(false);
		DBAdapeter.inRobots("https://www.ft.co/search");
	}

}
