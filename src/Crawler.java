import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	private int maxVisited = 1000;
	private int maxLinkfromSite = 50;
	private MongoDBAdapter DBAdapeter;
	private Object VisitedLock;
	private Object UnvisitedLock;
	private Object RobotLock;
	public void run() {
		
		this.startCrawl(DBAdapeter);
	}
	
	public Crawler(MongoDBAdapter DBAdapeter, Object VisitedLock,Object UnvisitedLock, Object RobotLock) {
		this.DBAdapeter = DBAdapeter;
		this.VisitedLock = VisitedLock;
		this.UnvisitedLock = UnvisitedLock;
		this.RobotLock = RobotLock;
	}
	
	public boolean addRobots(String URI,MongoDBAdapter DBAdapeter,Object RobotLock) {
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
			DBAdapeter.addRobots(pathsRegex,RobotLock);
		return true;
	}
	
	
	
	 public void startCrawl(MongoDBAdapter DBAdapeter) {
		 	
		 	while(DBAdapeter.visitedCount() <= maxVisited)
            try {
            	String URL = "false";
            	synchronized (UnvisitedLock) {
            		URL = DBAdapeter.getUnvisited();
            	}
            	if (URL != "false") {
	            	this.addRobots(URL, DBAdapeter,RobotLock);
	            	if(DBAdapeter.inRobots(URL,RobotLock))
	            		continue;
	            	
	            	Set<String> URISet = new HashSet<String>();
	            	Document document = Jsoup.connect(URL).get();

	            	for (Element script : document.getElementsByTag("script")) {
	                    script.remove();
	                }
	            	int count = 0 ;
	                for (Element a : document.getElementsByTag("a")) { 
	                    a.removeAttr("onclick");
	                    String URI =  a.attr("abs:href");
	                    if(count <= maxLinkfromSite)
		                	URISet.add(URI); 
			                if(URISet.size() - count == 1)  {
		                		count++;
		                	}
	                    a.removeAttr("href");
	                }
	            	
	            	document.outputSettings()
	                        .syntax(Document.OutputSettings.Syntax.xml)
	                        .escapeMode(Entities.EscapeMode.xhtml);
	            	
	                String AllContent = document.toString();
	            	String Title = document.select("title").toString();
	            	String Text = document.text().toString();
	            	
	            	if(DBAdapeter.addVisited(URL,AllContent,Title,Text,VisitedLock)) {
		
		                for(String url : URISet) {
//		                	synchronized (UnvisitedLock) {
	                		DBAdapeter.addUnvisited(url);
//		                	}
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
		int ThreadNumbers = 50;
		Object VisitedLock = new Object();
		Object UnvisitedLock = new Object();
		Object RobotLock = new Object();
		
		
		Thread myThreads[] = new Thread[ThreadNumbers];
		for (int j = 0; j < ThreadNumbers; j++) {
		    myThreads[j] = new Thread(new Crawler(DBAdapeter,VisitedLock,UnvisitedLock,RobotLock));
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
