import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
	public boolean addVisited(String URI , String AllContent,String Title,String Text) {
		FindIterable<Document> result = VisitedCollection.find();
		Document result2 = VisitedCollection.find(Filters.eq("url",URI)).first();
		if(result2 != null) return false;
		for(Document url : result)
		{
			String Title2 = url.get("Title").toString().toLowerCase();
			
			int distance = StringUtils.getLevenshteinDistance(Title.toLowerCase(), Title2);
			float percentage = (float)distance * 100 / Title.length(); 
			
			if(percentage < 20) {
				String Text2 = url.get("Text").toString().toLowerCase();
				int distance2 = StringUtils.getLevenshteinDistance(Text.toLowerCase(), Text2);
				float percentage2 = (float)distance2 * 100 / Text.length(); 
				
				if(percentage2 < 20) {
					String AllContent2 = url.get("Document").toString().toLowerCase();
					int distance3 = StringUtils.getLevenshteinDistance(AllContent.toLowerCase(), AllContent2);
					float percentage3 = (float)distance3 * 100 / AllContent.length(); 
					if(percentage3 < 20) {
						return false;
					}
				}
				
			}				
		}
		Document docTemp = new Document("url", URI).append("Title", Title).append("Text", Text).append("Document", AllContent);
		VisitedCollection.insertOne(docTemp);
		return true;
	}

	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter(false);
		DBAdapeter.init(false);
		DBAdapeter.getUnvisited();
	}
	
	

}
