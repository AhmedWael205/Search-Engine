import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class searchQuery extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	private ArrayList<String> NAMES = new ArrayList<String>();
	
	public void service(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		boolean Global = false;
        boolean DropTable = false;
        MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
        DBAdapeter.init(DropTable);
        
		res.setContentType("text/html");
		String Search_Query = req.getParameter("query");//.replaceAll("\\\"", "").trim();
		String Country = req.getParameter("country");
		
		boolean ImageSearch = (req.getParameter("imageSearch") != null && ((req.getParameter("imageSearch").equals("on")) || req.getParameter("imageSearch").equals("checked")));
		String ImageOn = "";
		if(ImageSearch)
			ImageOn = "checked";
		
		String pageNumber = req.getParameter("pageNumber");
		
		boolean unfinished = false;
		if (Search_Query.length() < 2)
			unfinished = true;
		
		ArrayList<URLResult> QPRes =  new ArrayList<>();
		ArrayList<ImageResult> ImgRes =  new ArrayList<>();
		int ResultCount = 0;
		if (!unfinished)
		{
			String personName = "";
			
			boolean isName = this.isName(Search_Query);
			if(isName)
			{
				personName = Search_Query.replaceAll("^\"|\"$", "");
				System.out.println(personName);
			}
			QueryProcessor Q = new QueryProcessor();
			if(!ImageSearch)
			{
				ArrayList<URLResult> Cache = DBAdapeter.getCached(Search_Query+" "+Country);
				if (Cache != null)
				{
					QPRes.addAll(Cache);
					if (QPRes.size() != 0)
						Q.AddQuery(personName,Country);
				}
			}
			if(QPRes.size() == 0)
			{
				
				Q.QuerySearch(Search_Query,ImageSearch, Country,personName);
				QPRes.addAll(Q.URLResults);
				ImgRes.addAll(Q.ImageResults);
				if(!ImageSearch)
					DBAdapeter.addCache(Search_Query+" "+Country,QPRes);
				
			}
			
			
			ResultCount = QPRes.size();
			if(ImageSearch) ResultCount = ImgRes.size();
			
//			if(!ImageSearch) {
//				for (int i = 0; i < 0; i++)
//					if (i%2 == 0)
//						QPRes.add(new URLResult("https://www.google.com/","google "+Integer.toString(i),"This is result website "+Integer.toString(i),"1970-01-01T00:00Z[GMT]"));
//					else
//						QPRes.add(new URLResult("https://www.google.com/","google "+Integer.toString(i),"This is result website "+Integer.toString(i),"-1"));
//				ResultCount = QPRes.size();
//			} 
//			else 
//			{
//				for (int i = 0; i < 55; i++) 
//					ImgRes.add(new ImageResult("https://icatcare.org/app/uploads/2018/07/Thinking-of-getting-a-cat.png","This is image number "+ Integer.toString(i)));
//				ResultCount = ImgRes.size();
//			}
			
			
		}
		int URLperPage = 10;
		int ImageperPage = 30;
        if(pageNumber==null)
        	pageNumber="1";
        
        try (PrintWriter out = res.getWriter()) {

            PrintHead(out);

            out.println("<body>\r\n" + 
            		"<form autocomplete=\"off\" action=\"SearchResult\" method=\"GET\" id=\"SearchResult\">\r\n" + 
            		"	<a href=\"http://localhost:8070/SearchEngine/\">\r\n" +
            		"    <h1>Chiefooo</h1>\r\n" +
            		"	</a>"+
            		"    <input class=\"box\" type=\"text\" name=\"query\" value='"+Search_Query+"' id=\"SearchQuery\">\r\n");
            out.println("	<input class =\"mic\" onclick=\"listen(); return false;\" type = \"submit\" value=\".\">");
            String countries = PrintCountries();
            String selectedCountries = countries.replace(Country+"\"",Country+"\" selected");
            out.println(selectedCountries);
            out.println( "   <input type=\"checkbox\" name=\"imageSearch\" id=\"ImageSearch\""+ImageOn+"/> Image      \r\n" );   
 			out.println("    <input type=\"submit\" value=\"Search\" class=\"button\">\r\n" +"    </form>");
 			if (!unfinished)
 				out.println("  <h5 style=\"margin-left: 20px;text-overflow: ellipsis; white-space: nowrap;\">Number of Found Results "+Integer.toString(ResultCount)+"</h5>");
           
            
            if(!unfinished && ResultCount ==0)
            {
            	out.println("<div>\n");
            	out.println("<h2 style=\"margin-left: 20px;text-overflow: ellipsis; white-space: nowrap; color:red;\">No results found for the search query \""+Search_Query+"\"</h2>");
            	out.println("</body>");
                out.println("</html>");
            }
            else if (!unfinished)
            {
	            int str = Integer.parseInt(pageNumber);
	            int j;
	            int end = 0;
	            if(!ImageSearch) {
	            	out.println("<div>\n");
	            	j = (str-1)*URLperPage;
		            while( j < URLperPage*str && j < QPRes.size())
		            {
		                out.println("<h3 style=\"display:inline-block;text-overflow: ellipsis; white-space: nowrap;\"><a href="+QPRes.get(j).url+" target=\"_self\">"+QPRes.get(j).Title+"</a></h3>");
		                out.println("<h4>"+QPRes.get(j).url+"</h4>");
		                
		                Pattern p = Pattern.compile("([A-Z][^.?!]*?)?(?<!\\w)(?i)("+Search_Query+")(?!\\w)[^.?!]*?[.?!]{1,2}\"?");
		                Matcher m = p.matcher(QPRes.get(j).Summary);
		                String Snippet = "";
		                if (m.find()) {
		                	Snippet = m.group();
		                }
		                if (Snippet== ""||Snippet.length()<2)
		                {
		                	Snippet = getSnippet(Search_Query,QPRes.get(j).Summary);
		                	 if (Snippet== ""||Snippet.length()<2)
				                {
				                	if (QPRes.get(j).Summary.length() <= 250)
				                		Snippet = QPRes.get(j).Summary;
					                else
					                	Snippet = QPRes.get(j).Summary.substring(0, 247)+"... ";
				                	
					                Snippet=Snippet.replaceAll("(?i)"+Search_Query,"<b>$0</b>");
				                }
		                	 else
		                		 Snippet = "... "+ Snippet + " ..."; 
		                }
		                else
		                {
		                	if(Snippet.length()>500)
		                		Snippet = getSnippet(Search_Query,Snippet);
		                	else
		                		Snippet=Snippet.replaceAll("(?i)"+Search_Query,"<b>$0</b>");
		                }
	
		                if (!QPRes.get(j).pubDate.equals("-1"))
		                	out.println("<p>"+QPRes.get(j).pubDate+", "+Snippet+"</p>");
		                else
		                	out.println("<p>"+Snippet+"</p>");
		                
		                
		                j++;
		            }
		            QueryProcessor Q = new QueryProcessor();
		            Q.QuerySearch(Search_Query,true, Country,"");
					ImgRes.addAll(Q.ImageResults);
					int i = 0;
					out.println("<br><br>\r\n<h4><a href='?query="+Search_Query+"&imageSearch=on&country="+Country+";\" target=\"_self\"'>See Images Result for '"+Search_Query+"'</a></h4>");
					out.println("</div>");
					out.println("<div class=\"Allimages\">");
					if(i < ImgRes.size())
					{
		                out.println("<div class=\"row\">");
	            		out.println("<div class=\"column\">");
		                out.println("<figure>\r\n" +
		                		"  <a href=\""+ImgRes.get(i).src+"\">\r\n" +
		                		"    <img src=\""+ImgRes.get(i).src+"\" alt=\""+ImgRes.get(i).alt+"\" style=\"width:100%\">\r\n" + 
		                		"  </a>\r\n" + 
		                		"</figure>\r\n" +
		                		"</div>\r\n");
		                i++;
					}
	                if(i < ImgRes.size())
	                {
	                	out.println("<div class=\"column\">");
		                out.println("<figure>\r\n" +
		                		"  <a href=\""+ImgRes.get(i).src+"\">\r\n" +
		                		"    <img src=\""+ImgRes.get(i).src+"\" alt=\""+ImgRes.get(i).alt+"\" style=\"width:100%\">\r\n" + 
		                		"  </a>\r\n" + 
		                		"</figure>\r\n" +
		                		"</div>\r\n");
		                i++;
	                }
	                if(i < ImgRes.size())
	                {
	                	out.println("<div class=\"column\">");
		                out.println("<figure>\r\n" +
		                		"  <a href=\""+ImgRes.get(i).src+"\">\r\n" +
		                		"    <img src=\""+ImgRes.get(i).src+"\" alt=\""+ImgRes.get(i).alt+"\" style=\"width:100%\">\r\n" + 
		                		"  </a>\r\n" + 
		                		"</figure>\r\n" +
		                		"</div>\r\n");
		                i++;
	                }
	                out.println("</div>\r\n<br>");
	                
		            end =(int) Math.ceil((double)QPRes.size()/(double)URLperPage); 
	            } else {
	            	j = (str-1)*ImageperPage;
	            	out.println("<div class=\"Allimages\">\n");
	            	while( j < ImageperPage*str && j < ImgRes.size())
		            {
	            		out.println("<div class=\"row\">");
	            		out.println("<div class=\"column\">");
		                out.println("<figure>\r\n" +
		                		"  <a href=\""+ImgRes.get(j).src+"\">\r\n" +
		                		"    <img src=\""+ImgRes.get(j).src+"\" alt=\""+ImgRes.get(j).alt+"\" style=\"width:100%\">\r\n" + 
		                		"  </a>\r\n" + 
		                		"</figure>\r\n" +
		                		"</div>\r\n");
		                j++;
		                if(j < ImgRes.size())
		                {
		                	out.println("<div class=\"column\">");
			                out.println("<figure>\r\n" +
			                		"  <a href=\""+ImgRes.get(j).src+"\">\r\n" +
			                		"    <img src=\""+ImgRes.get(j).src+"\" alt=\""+ImgRes.get(j).alt+"\" style=\"width:100%\">\r\n" + 
			                		"  </a>\r\n" + 
			                		"</figure>\r\n" +
			                		"</div>\r\n");
			                j++;
		                }
		                if(j < ImgRes.size())
		                {
		                	out.println("<div class=\"column\">");
			                out.println("<figure>\r\n" +
			                		"  <a href=\""+ImgRes.get(j).src+"\">\r\n" +
			                		"    <img src=\""+ImgRes.get(j).src+"\" alt=\""+ImgRes.get(j).alt+"\" style=\"width:100%\">\r\n" + 
			                		"  </a>\r\n" + 
			                		"</figure>\r\n" +
			                		"</div>\r\n");
			                j++;
		                }
		                out.println("</div>\r\n<br>");
		            }
		            end =(int) Math.ceil((double)ImgRes.size()/(double)ImageperPage); 
	            }
	            out.println("</div>\n");
	            out.println("<footer class=\"container\">\n" +
	                    "  <center><ul class=\"pagination\">\n" );
	                    
	            for(int i=1; i <= end; i++)
	            {
	                if(Integer.toString(i).equals(pageNumber))
	                    out.println(    "    <li class=\"active\"><a href=\"#\">"+Integer.toString(i)+"</a></li>\n" );   
	                else
	                    out.println(    "    <li><a href='?query="+Search_Query+"&imageSearch="+ImageOn+"&country="+Country+"&pageNumber="+i+"'>"+Integer.toString(i)+"</a></li>\n" );
	            }
	            out.println("  </ul></center>" );
	            out.println("</footer>");
	            out.println("</body>");
	            out.println("</html>");
	
	        }
            else
            {
            	out.println("</body>");
                out.println("</html>");
            }
        }      
	}
        
    private void PrintHead(PrintWriter out)
    {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"utf-8\">\n" +
        		"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n"+
        		"  <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
        out.println("<script src=\"platform.js\"></script>");
        out.println("<script src=\"webspeech.js\"></script>");
        out.println("<script type=\"text/javascript\" src=\"autocomplete.js\"></script>");
        out.println("<script src=\"https://code.jquery.com/jquery-1.12.4.js\"></script>");
        out.println("<script>");
        out.println("$.get('20k.txt', function(txtFile){\r\n" + 
        		"    var languages = txtFile.split(\"\\n\");\r\n" + 
        		"    autocomplete2(document.getElementById(\"SearchQuery\"),languages);\r\n" + 
        		"});\r\n"+
        		"    var listener;\r\n" + 
        		"    window.onload = function() {\r\n" + 
        		"        ws = webSpeechNoConflict();\r\n" + 
        		"        try{\r\n" + 
        		"            listener = new ws.Listener();\r\n" + 
        		"        }\r\n" + 
        		"        catch(ex){\r\n" + 
        		"            console.log(ex);\r\n" + 
        		"            listener = null;\r\n" + 
        		"            document.getElementById(\"status\").innerHTML = ex;\r\n" + 
        		"        }\r\n" + 
        		"    };\r\n" + 
        		"\r\n" + 
        		"    function listen() {\r\n" + 
        		"		if(listener){\r\n" + 
        		"			listener.listen(\"en\", function(text) {\r\n" + 
        		"				document.getElementById(\"SearchQuery\").value = text;\r\n" + 
        		"			});\r\n" + 
        		"		}\r\n" + 
        		"    }");
        out.println("</script>");
        out.println("<title>Chiefooo</title>\r\n" + 
        		"<style>\r\n" + 
        		"html {\r\n" + 
        		"    position: relative;\r\n" + 
        		"    min-height: 100%;\r\n" + 
        		"}\r\n" + 
        		"body {\r\n" + 
        		"    margin-bottom: 100px;\r\n" + 
        		"	background-image: url('cat3.jpg');\r\n" + 
        		"  	background-repeat: no-repeat;\r\n" + 
        		"  	background-attachment: fixed;\r\n" + 
        		"  	background-size: cover;\r\n"+
        		"}\r\n" +
        		"	.column {\r\n" + 
        		"  float: left;\r\n" + 
        		"  width: 25%;\r\n" + 
        		"  padding: 5px;\r\n" + 
        		"}\r\n" + 
        		"\r\n" + 
        		"/* Clearfix (clear floats) */\r\n" + 
        		".row::after {\r\n" + 
        		"  content: \"\";\r\n" + 
        		"  clear: both;\r\n" + 
        		"  display: table;\r\n" + 
        		"}\r\n"+
        		".Allimages{\r\n" + 
        		"	  margin-left: 20px;\r\n" + 
        		"	}\r\n"+
        		"figure {\r\n" + 
        		"  border: 1px #cccccc solid;\r\n" + 
        		"  padding: 4px;\r\n" + 
        		"  width:330px;\r\n" + 
        		"  height:auto;\r\n" + 
        		"}\r\n" + 
        		"img{\r\n" + 
        		"  width:330px;\r\n" + 
        		"  height:330px;\r\n" + 
        		"}\r\n" + 
        		"\r\n" + 
        		"figcaption {\r\n" + 
        		"  background-color: white;\r\n" + 
        		"  color:black ;\r\n" + 
        		"  font-style: italic;\r\n" + 
        		"  padding: 2px;\r\n" + 
        		"  text-align: center;\r\n" + 
        		"}\r\n"+
        		"div {    margin-left: 100px;\r\n" + 
        		"}\r\n" + 
        		"footer {\r\n" + 
        		"    position: absolute;\r\n" + 
        		"    left: 0;\r\n" + 
        		"    bottom: 0;\r\n" + 
        		"    height: 100px;\r\n" + 
        		"    color: white;\r\n" + 
        		"}\r\n" + 
        		".button {\r\n" + 
        		"    background-color: DodgerBlue;\r\n" + 
        		"    border: none;\r\n" + 
        		"    color: white;\r\n" + 
        		"    padding: 5px 32px;\r\n" + 
        		"    text-align: center;\r\n" + 
        		"    text-decoration: none;\r\n" + 
        		"    display: inline-block;\r\n" + 
        		"    font-size: 16px;\r\n" + 
        		"    margin: 4px 2px;\r\n" + 
        		"    cursor: pointer;\r\n" + 
        		"}\r\n" +
        		".mic {\r\n" + 
        		"	width:30px;\r\n" + 
        		"	Height:30px;\r\n" + 
        		"   background: url(mic.png);\r\n" + 
        		"   background-size: cover;\r\n" + 
        		"}\r\n"+
        		"h3 {\r\n" + 
        		"   line-height:10px;\r\n" + 
        		"}\r\n" +
        		"h1 {\r\n" + 
        		"    color:Blue;\r\n" + 
        		"    margin-left: 300px;\r\n" + 
        		"}\r\n"+
        		"h4 {\r\n" + 
        		"    color:rgb(215, 0 ,42);\r\n" + 
        		"    margin-bottom: 2px;\r\n" + 
        		"}\r\n" + 
        		"p {\r\n" + 
        		"    color: sienna;\r\n" + 
        		"    margin-bottom: 2px;\r\n" + 
        		"}\r\n" +
        		".country {\r\n" + 
        		"	height: 30px;\r\n" + 
        		"	width: 300px;\r\n" + 
        		"}\r\n" + 
        		"input.box {\r\n" + 
        		"    height: 30px;\r\n" + 
        		"    width: 300px;\r\n" + 
        		"    font-size: 16px;\r\n" + 
        		"    margin-left: 20px;\r\n" + 
        		"    background-color: white;\r\n" + 
        		"}\r\n" + 
        		".autocomplete-items {\r\n" + 
        		"  border: 1px solid #d4d4d4;\r\n" + 
        		"  border-bottom: none;\r\n" + 
        		"  border-top: none;\r\n" + 
        		"  z-index: 99;\r\n" + 
        		"  /*position the autocomplete items to be the same width as the container:*/\r\n" + 
        		"  position: relative;\r\n" + 
        		"    top: 0px;\r\n" + 
        		"    width: 300px;\r\n" + 
        		"    margin-left: -80px;\r\n" + 
        		"}\r\n" + 
        		"\r\n" + 
        		".autocomplete-items div {\r\n" + 
        		"  padding: 10px;\r\n" + 
        		"  cursor: pointer;\r\n" + 
        		"  background-color: #fff; \r\n" + 
        		"  border-bottom: 1px solid #d4d4d4; \r\n" + 
        		"}\r\n" + 
        		"\r\n" + 
        		"/*when hovering an item:*/\r\n" + 
        		".autocomplete-items div:hover {\r\n" + 
        		"  background-color: #e9e9e9; \r\n" + 
        		"}\r\n" + 
        		"\r\n" + 
        		"/*when navigating through the items using the arrow keys:*/\r\n" + 
        		".autocomplete-active {\r\n" + 
        		"  background-color: DodgerBlue !important; \r\n" + 
        		"  color: #ffffff; \r\n" + 
        		"}\r\n" + 
        		"a:link {\r\n" + 
        		"    color: blue;\r\n" + 
        		"    text-decoration: none;\r\n" + 
        		"}\r\n" + 
        		"a:visited {\r\n" + 
        		"    color: purple;\r\n" + 
        		"}\r\n" + 
        		"a:hover {\r\n" + 
        		"    text-decoration: underline;\r\n" + 
        		"}\r\n" + 
        		"</style>");
        out.println("</head>");
    }
    
    
    private String PrintCountries()
    {
    	String printCountries = "	<select id=\"country\" name=\"country\" class=\"country\">\r\n";
    	ArrayList<String> countries = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(("./resources/countries.txt")))){
            while(reader.ready())
            	countries.add(reader.readLine());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < countries.size(); i++)
        	printCountries = printCountries + "			<option value=\""+(countries.get(i)) + "\">" + (countries.get(i)) + "</option>\r\n";
        
        printCountries = printCountries +"	</select>";
    	
//        System.out.println(printCountries);
        return printCountries;
    }
    
    private void initNAMES()
    {
    	if(NAMES.size() != 0) return;
		 try(BufferedReader reader = new BufferedReader(new FileReader(("./resources/names.txt")))){
	            while(reader.ready())
	            	NAMES.add(reader.readLine().toLowerCase());
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
		 

    }
    
    private boolean isName(String SearchQuery)
    {
    	initNAMES();
    	if(NAMES == null || NAMES.size() == 0) return false;
    	String query = SearchQuery.replaceAll("^\"|\"$", "");
//    	System.out.println("query after removing quotes: "+query);
    	String[] words = query.split(" ");
    	
    	for (String word : words)
    		if(!NAMES.contains(word.toLowerCase()))
    			return false;
    	
    	return true;
    }
    
//    private String getNWordsBefandAft(String text,String query ,int N)
//    {
//    
//    	Pattern p = Pattern.compile("((([a-zA-Z1234567890&(),@_':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?(?i)("+query+"[,:]?)\\s*((([a-zA-Z1234567890&()@_,':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?");
//		Matcher m = p.matcher(text);
//		if (m.find())
//		{
//			System.out.println(m.group(4));
//			return m.group();	
//		}
//		return "";
//    }
    
    private String getSnippet(String query,String Text)
    {
    	ArrayList<String>StopWords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(("./resources/StopWords.txt")))) {
            while (reader.ready()) {
                StopWords.add(reader.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        ArrayList<String> SW = new ArrayList<>(SearchWords);
        SearchWords.clear();
        String toReplace = "|(";
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
        String z = x.substring(x.indexOf("|")+1,x.length()).trim();
        if(SearchPhrase=="")
        	x = z;
        
		int N = 40;
		

		Pattern p = Pattern.compile("((([a-zA-Z1234567890&(),@_':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?(?i)("+x+"[,:]?)\\s*((([a-zA-Z1234567890&()@_,':\\\"\\-]*\\s+){1,"+Integer.toString(N)+"}))?");
		Matcher m = p.matcher(Text);
		if (m.find())
		 {
		 	String y = m.group();
		 	return (y.replaceAll("(?i)("+z+")", "<b>$0</b>"));
		
		 }
		return "";
    }
}
