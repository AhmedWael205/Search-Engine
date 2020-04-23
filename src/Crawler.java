import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
//import org.jsoup.nodes.Document;
import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class Crawler implements Runnable{
	private int maxVisited = 7500;
	private int maxLinkfromSite = 50;
	private int maxUnvisited = 20000;
	private MongoDBAdapter DBAdapeter;
	private Object UnvisitedLock;
	
	public void run() {
		
		this.startCrawl(DBAdapeter);
	}
	
	public Crawler(MongoDBAdapter DBAdapeter,Object UnvisitedLock) {
		this.DBAdapeter = DBAdapeter;
		this.UnvisitedLock = UnvisitedLock;
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
		 	
		 	while(DBAdapeter.visitedCount() <= maxVisited)
            try {
            	String URL = "false";
            	
            	// Synchronized because we delete it from table after we find it
            	synchronized (UnvisitedLock) {
            		URL = DBAdapeter.getUnvisited();
            	}
            	long unvisitedCount = DBAdapeter.unvisitedCount();
            	if (URL != "false") {
            		this.addRobots(URL, DBAdapeter);
	            	if(DBAdapeter.inRobots(URL))
	            		continue;
	            	
	            	Set<String> URISet = new HashSet<String>();
	            	Connection connection = Jsoup.connect(URL);
	            	Connection.Response r = connection.url(URL).timeout(10000).execute();
	            	
	            	 if (!r.contentType().contains("text/html")) continue;

	            	Document document = connection.get();

	            	for (Element script : document.getElementsByTag("script")) {
	                    script.remove();
	                }
	            	int count = 0 ;
	            	if(unvisitedCount < maxUnvisited) {
		                for (Element a : document.getElementsByTag("a")) { 
		                    a.removeAttr("onclick");
		                    String URI =  a.attr("abs:href");
		                    if(count <= maxLinkfromSite && URI != "")
			                	URISet.add(URI); 
				                if(URISet.size() - count == 1)  {
			                		count++;
			                	}
		                    a.removeAttr("href");
		                }
	            	} else {
	            		for (Element a : document.getElementsByTag("a")) { 
		                    a.removeAttr("onclick");
		                    a.removeAttr("href");
	            		}
	            	}
	            	
	            	document.outputSettings()
	                        .syntax(Document.OutputSettings.Syntax.xml)
	                        .escapeMode(Entities.EscapeMode.xhtml);
	            	
	                String AllContent = document.toString();
	            	String Title = document.select("title").toString();
	            	String Text = document.text().toString();
	            	
	            	if(DBAdapeter.addVisited(URL,AllContent,Title,Text)) {
	            		if(unvisitedCount < maxUnvisited) {
			                for(String url : URISet) {
			                	DBAdapeter.addUnvisited(url);
		                	}
	            		}
	            	}
            	}
            } catch (IOException | IllegalArgumentException | NullPointerException ex) {
                continue;
            }
	    }
	
	public static void main( String args[] ) {
		boolean Global = false;
		boolean DropTable = true;
		int ThreadNumbers = 200;
		Object UnvisitedLock = new Object();
		
		MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
		DBAdapeter.init(DropTable);
		
		
		Thread myThreads[] = new Thread[ThreadNumbers];
		for (int j = 0; j < ThreadNumbers; j++) {
		    myThreads[j] = new Thread(new Crawler(DBAdapeter,UnvisitedLock));
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
