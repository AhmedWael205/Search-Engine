import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.commons.lang3.StringUtils;



public class Crawler {
	
	
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
		 	while(DBAdapeter.visitedCount() <= 50)
            try {
            	String URL = DBAdapeter.getUnvisited();
            	this.addRobots(URL, DBAdapeter);
            	if(DBAdapeter.inRobots(URL))
            		continue;
                //2. Fetch the HTML code
            	
            	org.jsoup.nodes.Document document = Jsoup.connect(URL).get();
            	
            	String content = document.html().toString();
            	System.out.println("here1");
            	if(DBAdapeter.addVisited(URL,content)) {
            		System.out.println("here2");
	                //3. Parse the HTML to extract links to other URLs
	            	
	                Elements linksOnPage = document.select("a[href]");
	
	                //5. For each extracted URL... go back to Step 4.
	                int count = 0 ;
	                for (Element page : linksOnPage) {
	                	String URI = page.attr("abs:href");
	                	URIs.add(URI);
	                	count++;
	                	if(count >= 100) 
	                		break;
	                }
	                System.out.println("here3");
	                if (URIs.size() != 0)
	                	DBAdapeter.addManyUnvisited(URIs);
            	}
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
	    }
	
	public static void main( String args[] ) {  
		
		MongoDBAdapter DBAdapeter = new MongoDBAdapter(true);
		DBAdapeter.init(false);
		Crawler c = new Crawler();
		c.startCrawl(DBAdapeter);

	}

}
