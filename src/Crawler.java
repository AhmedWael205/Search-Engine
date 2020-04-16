import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
//import org.jsoup.nodes.Document;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class Crawler implements Runnable{
	private int maxVisited = 200;
	private int maxLinkfromSite = 50;
	private MongoDBAdapter DBAdapeter;
	public void run() {
		
		this.startCrawl(DBAdapeter);
	}
	
	public Crawler(MongoDBAdapter DBAdapeter) {
		this.DBAdapeter = DBAdapeter;
	}
	
	public boolean addRobots(String URI,MongoDBAdapter DBAdapeter) {
			String Robots = URI+"/robots.txt/";
			String pattern = "Disallow: (.*)";
			Pattern r = Pattern.compile(pattern);
			List<String> pathsRegex = new ArrayList<String>();

			try(BufferedReader in = new BufferedReader(new InputStreamReader(new URL(Robots).openStream()))) {
		        String line = null;
		        while((line = in.readLine()) != null) {
		        	
		        	if(line.equalsIgnoreCase("User-agent: *")) {
		        		
			        	while ((line = in.readLine()) != null) {
			        		
			        		if(line.contains("Disallow:")) {	
			        			Matcher m = r.matcher(line.toString());
			        			
			        			if (m.find( )) {
			        				
			        				String Temp = m.group(1).replace("*", ".*");
			        				pathsRegex.add(URI+Temp);
			        			}
			        		}
			        		else if (line.contains("User-agent"))
			        			break;
			        	}
		        	}
		        }
		    } catch (IOException e) {
		        return false;
		    }
		if(pathsRegex.size() != 0)
			DBAdapeter.addRobots(pathsRegex);
		return true;
	}
	
	
	
	 public void startCrawl(MongoDBAdapter DBAdapeter) {

		 	List<String> URIs = new ArrayList<String>();
		 	while(DBAdapeter.visitedCount() <= maxVisited)
            try {
            	String URL = "false";
            	synchronized (DBAdapeter) {
            		URL = DBAdapeter.getUnvisited();
            	}
            	if (URL != "false") {
	            	this.addRobots(URL, DBAdapeter);
	            	if(DBAdapeter.inRobots(URL))
	            		continue;
	                //2. Fetch the HTML code
	            	
	            	org.jsoup.nodes.Document document = Jsoup.connect(URL).get();
	            	
	            	String AllContent = document.toString();
	            	String Title = document.select("title").toString();
	            	String Text = document.text().toString();
	            	
	            	if(DBAdapeter.addVisited(URL,AllContent,Title,Text)) {
		                //3. Parse the HTML to extract links to other URLs
		            	
		                Elements linksOnPage = document.select("a[href]");
		
		                //5. For each extracted URL... go back to Step 4.
		                int count = 0 ;
		                for (Element page : linksOnPage) {
		                	String URI = page.attr("abs:href"); 	
		                	URIs.add(URI);
		                	count++;
		                	if(count >= maxLinkfromSite) 
		                		break;
		                }
		                if (URIs.size() != 0)
		                {
		                	Set<String> URISet = new HashSet<String>(); 
		                    for (String x : URIs) 
		                    	URISet.add(x); 
		                	DBAdapeter.addManyUnvisited(URISet);
		                }
	            	}
            	}
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
	    }
	
	public static void main( String args[] ) {
		boolean Global = true;
		boolean DropTable = false;
		MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
		DBAdapeter.init(DropTable);
		int ThreadNumbers = 100;
		
		Thread myThreads[] = new Thread[ThreadNumbers];
		for (int j = 0; j < ThreadNumbers; j++) {
		    myThreads[j] = new Thread(new Crawler(DBAdapeter));
		    myThreads[j].start();
		}
		for (int j = 0; j < ThreadNumbers; j++) {
		    try {
				myThreads[j].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
	}

}
