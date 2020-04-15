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

import org.bson.*;

public class MongoDBAdapter {
	public MongoDBAdapter() {}
	private MongoCollection<Document> UnvisitedCollection;
	private MongoCollection<Document> VisitedCollection;
	private MongoClientURI uri = new MongoClientURI( "mongodb+srv://CrawlerDB:Crawler123@crawlercluster-kgwg6.mongodb.net/Test?retryWrites=true&w=majority"  ); ;
	private MongoClient mongoClient = new MongoClient(uri);
	private MongoDatabase database = mongoClient.getDatabase("Test");
	public void init() {
		Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
		
			  
			  
			  
		      System.out.println("Database Connected Successfully ...");
		      
		      // Deleting Old Collections if Exists
		      MongoCollection<Document> OLDUnvisited = database.getCollection("Unvisited");
		      OLDUnvisited.drop();
		      MongoCollection<Document> OLDVisited = database.getCollection("Visited");
		      OLDVisited.drop();
		      
		      // Creating a new Unvisited and Visited Collections
		      database.createCollection("Unvisited");
		      database.createCollection("Visited");
		      UnvisitedCollection = database.getCollection("Unvisited");
		      VisitedCollection = database.getCollection("Visited");
		      
		      //Reading Initial Seed Set from File
		      List<Document> documents = new ArrayList<Document>();
			  BufferedReader reader;
			  Document docTemp;
			  System.out.println("Initiallizing Seed Set with ...");
			  try {
					reader = new BufferedReader(new FileReader((".\\Sports Sites.txt")));
					String line = reader.readLine();
					while (line != null) {
						
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
			System.out.println("Removing "+url+" from Visited");
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
		
		Document result1 = UnvisitedCollection.find(Filters.eq("url",URI)).first();
		Document result2 = VisitedCollection.find(Filters.eq("url",URI)).first();
		
		if(result1 == null &&  result2 == null) {
			Document docTemp = new Document("url", URI);
			System.out.println("Adding "+URI+" to Unvisited");
			UnvisitedCollection.insertOne(docTemp);
			return true;
		}
		return false;
	}
	

	public static void main( String args[] ) {  

		MongoDBAdapter DBAdapeter = new MongoDBAdapter();
		DBAdapeter.init();
		DBAdapeter.getUnvisited();
	}
	
	

}
