import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Trends extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public void service(HttpServletRequest req, HttpServletResponse res) throws IOException
	{
		res.setContentType("text/html");
		String Country = req.getParameter("country");
		boolean Selected = false;
		if(Country != null)
			Selected = true;

		QueryProcessor Q = new QueryProcessor();
		ArrayList<TrendsResult> trendsResult = Q.retunTrends(Country);

//		ArrayList<TrendsResult> trendsResult =  new ArrayList<>();
//
//		for (int i = 0; i < 5; i++)
//			trendsResult.add(new TrendsResult("Ahmed Wael "+ Integer.toString(i+1),10-i));

		if (trendsResult.size() < 10)
		{
			for (int i = trendsResult.size(); i < 10; i++)
				trendsResult.add(new TrendsResult("",0));
		}

        try (PrintWriter out = res.getWriter()) {

        	out.println("<!DOCTYPE html>\r\n" +
        			"<html>\r\n" +
        			"<head>\r\n" +
        			"<meta charset=\"utf-8\">\r\n" +
        			"  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n" +
        			"  <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
        	out.println("<title>Chiefooo</title>");
        	if(Selected)
        	{
	        	out.println("  <script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\r\n" +
	        			"  <script type=\"text/javascript\">"+" google.charts.load(\"current\", {packages:['corechart']});\r\n" +
						"    google.charts.setOnLoadCallback(drawChart);\r\n" +
						"    function drawChart() {\r\n" +
						"      var data = google.visualization.arrayToDataTable([\r\n" +
						"        [\"Element\", \"Number of Searches\", { role: \"style\" } ],");

	        	int size = trendsResult.size();

	        	for (int i = 0; i < size - 1; i++)
	        		out.println("[\""+trendsResult.get(i).Name+"\", "+trendsResult.get(i).Freq+", \"\"],");

	        	out.println("[\""+trendsResult.get(size - 1).Name+"\", "+trendsResult.get(size - 1).Freq+", \"\"]\r\n ]);");

	        	out.println(" var view = new google.visualization.DataView(data);\r\n" +
	        			"      view.setColumns([0, 1,\r\n" +
	        			"                       { calc: \"stringify\",\r\n" +
	        			"                         sourceColumn: 1,\r\n" +
	        			"                         type: \"string\",\r\n" +
	        			"                         role: \"annotation\" },\r\n" +
	        			"                       2]);\r\n" +
	        			"\r\n" +
	        			"      var options = {\r\n" +
	        			"        title: \"Trends in "+Country+"\",\r\n" +
	        			"        width: 1000,\r\n" +
	        			"        height: 400,\r\n" +
	        			"        bar: {groupWidth: \"95%\"},\r\n" +
	        			"        legend: { position: \"none\" },\r\n" +
	        			"        chartArea : { left: 0 },\r\n" +
	        			"        backgroundColor: { fill:'transparent' },\r\n" +
	        			"      };\r\n" +
	        			"      var chart = new google.visualization.ColumnChart(document.getElementById(\"columnchart_values\"));\r\n" +
	        			"      chart.draw(view, options);\r\n" +
	        			"  }\r\n" +
	        			"  </script>");
	        	}


	            PrintStyle(out);

	            out.println("</head>\r\n" +
	            		"<body>\r\n" +
	            		"  <form  action=\"Trends\" method=\"GET\" id=\"Trends\">\r\n" +
	            		"	<a href=\"http://localhost:8070/SearchEngine/index.html\">"+
	            		"  		<h1>Chiefooo</h1>\r\n"+
	            		"	</a>");

	            String countries = PrintCountries();
	            String selectedCountries = countries.replace(Country+"\"",Country+"\" selected");
	            out.println(selectedCountries);

	            out.println("<input type=\"submit\" value=\"Trends\" class=\"Trends\">\r\n    </form>");

	            if(Selected)
	            	out.println("    <div id=\"columnchart_values\" style=\"display:block;margin:0 auto;\"></div>");

	            out.println("</body>\r\n</html>");
	        }
	}

    private void PrintStyle(PrintWriter out)
    {
        out.println("<title>Chiefo</title>\r\n" +
        		"<style>\r\n" +
        		"html {\r\n" +
        		"    position: relative;\r\n" +
        		"    min-height: 100%;\r\n" +
        		"}\r\n" +
        		"body {\r\n" +
        		"    margin-bottom: 100px;\r\n" +
        		"	  background-image: url('cat3.jpg');\r\n" +
        		"  	background-repeat: no-repeat;\r\n" +
        		"  	background-attachment: fixed;\r\n" +
        		"  	background-size: cover;\r\n" +
        		"}\r\n" +
        		"\r\n" +
        		"div {    \r\n" +
        		"  margin-left: 100px;\r\n" +
        		"}\r\n" +
        		"\r\n" +
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
        		".Trends {\r\n" +
        		"    background-color: red;\r\n" +
        		"    border: none;\r\n" +
        		"    color: yellow;\r\n" +
        		"    padding: 5px 32px;\r\n" +
        		"    text-align: center;\r\n" +
        		"    text-decoration: none;\r\n" +
        		"    display: inline-block;\r\n" +
        		"    font-size: 16px;\r\n" +
        		"    margin: 4px 2px;\r\n" +
        		"    cursor: pointer;\r\n" +
        		"}\r\n" +
        		"\r\n" +
        		"h3 {\r\n" +
        		"   line-height:10px;\r\n" +
        		"}\r\n" +
        		"h1 {\r\n" +
        		"    color:Blue;\r\n" +
        		"    margin-left: 150px;\r\n" +
        		"}\r\n" +
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
        		"	margin-left:20px;\r\n" +
        		"}\r\n" +
        		"\r\n" +
        		"\r\n" +
        		"</style>");
    }


    private String PrintCountries()
    {
    	String printCountries = "	<select id=\"country\" name=\"country\" class=\"country\">\r\n";
    	ArrayList<String> countries = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(("D:\\University Materials\\CMPN courses\\Advanced Programming Techniques\\Spring 2020\\Project\\SearchEngine\\src\\countries.txt")))){
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
}
