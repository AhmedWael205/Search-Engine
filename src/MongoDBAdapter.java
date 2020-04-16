import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;

import org.bson.*;

public class MongoDBAdapter {
	
	private MongoCollection<Document> UnvisitedCollection;
	private MongoCollection<Document> VisitedCollection;
	private MongoCollection<Document> RobotCollection;
	private MongoClientURI uri = new MongoClientURI( "mongodb+srv://CrawlerDB:Crawler123@crawlercluster-kgwg6.mongodb.net/Test?retryWrites=true&w=majority"  ); ;
//	private MongoClient mongoClient = new MongoClient(uri);
//	private MongoDatabase database = mongoClient.getDatabase("Test");
//	private MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
//	private MongoDatabase database = mongoClient.getDatabase("Crawler");
	private MongoClient mongoClient;
	private MongoDatabase database;
	
	public MongoDBAdapter(boolean Global) {
		if(Global) {
			this.mongoClient = new MongoClient(uri);
			this.database = mongoClient.getDatabase("Test");
		} else {
			this.mongoClient = new MongoClient( "localhost" , 27017 );
			this.database = mongoClient.getDatabase("Crawler");
		}
	}
	public void init(boolean DropTables) {
		Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
		
			  
			  
			  
		      System.out.println("Database Connected Successfully ...");
		      
		      if(DropTables) {
			      // Deleting Old Collections if Exists
			      MongoCollection<Document> OLDUnvisited = database.getCollection("Unvisited");
			      OLDUnvisited.drop();
			      MongoCollection<Document> OLDVisited = database.getCollection("Visited");
			      OLDVisited.drop();
			      MongoCollection<Document> OLDRobot = database.getCollection("Robot");
			      OLDRobot.drop();
			      
			      // Creating a new Collections
			      database.createCollection("Unvisited");
			      database.createCollection("Visited");
			      database.createCollection("Robot");
		      }
		      
		      UnvisitedCollection = database.getCollection("Unvisited");
		      VisitedCollection = database.getCollection("Visited");
		      RobotCollection = database.getCollection("Robot");
		      
		      if (DropTables) {
			      //Reading Initial Seed Set from File
			      List<Document> documents = new ArrayList<Document>();
				  BufferedReader reader;
				  Document docTemp;
				  System.out.println("Initiallizing Seed Set with ...");
				  try {
						reader = new BufferedReader(new FileReader((".\\Sports Sites.txt")));
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
		if (UnvisitedCollection.countDocuments() >= 0) {
			Document result = UnvisitedCollection.find(Filters.regex("url", ".*"+".com")).first();
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
	
	//TODO Add Does not Exist in ROBOT check.
	public int addManyUnvisited(List<String> URIs) {
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
		List<Document> documents = new ArrayList<Document>();
		Document docTemp;
		for(String URI : URIs){
			URI = URI.replaceAll("[^:]//", "/").toLowerCase();
			Document result = RobotCollection.find(Filters.eq("urlRegex",URI)).first();
			
			if(result == null) {
				docTemp = new Document("urlRegex", URI);
				System.out.println("Adding "+URI+" to Robot");
				documents.add(docTemp);
			} else {
				continue;
			}
		}
		RobotCollection.insertMany(documents);
		return documents.size();
	}
	
	public boolean inRobots(String URI)	{
		FindIterable<Document> result = RobotCollection.find();
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
	
	
	@SuppressWarnings("deprecation")
	public boolean addVisited(String URI , String content) {
		FindIterable<Document> result = VisitedCollection.find();
		System.out.println("here11");
		for(Document url : result)
		{
			String content2 = url.get("Document").toString().toLowerCase();
			String url2 = url.get("url").toString();
			
			int distance = StringUtils.getLevenshteinDistance(content.toLowerCase(), content2);
			float percentage = (float)distance * 100 / content.length(); 
			
			if(percentage < 20 || url2.equalsIgnoreCase(URI)) {
				System.out.println("Content Matched to " + url2);
				return false;
			}				
		}
		System.out.println("here12");
		Document docTemp = new Document("url", URI).append("Document", content);
		VisitedCollection.insertOne(docTemp);
		System.out.println("here13");
		return true;
	}

	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter();
		DBAdapeter.init();
		DBAdapeter.getUnvisited();
	}
	
	

}
