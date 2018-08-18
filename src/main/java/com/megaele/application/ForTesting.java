package com.megaele.application;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.megaele.crawling.UserAgentFactory;

public class ForTesting {

	
	final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";

	
	public static void main(String[] args) {
//		String result="";
//		String test = "Venta Online de Placa Vitrocerámica Bosch PKM875DP1D|Megaelectrodomesticos.com";
//		String[] tokens = test.split("\\|");
//		result = tokens[0].split(" ")[tokens[0].split(" ").length-1];
//		System.out.println(result);
		
		
		
		Document doc = null;
		try {
			doc = Jsoup.connect("https://free-proxy-list.net/").userAgent(USER_AGENT).get();
			Element table = doc.select("table").get(0); 
			Elements rows = table.select("tr");
			for (int i = 1; i < rows.size(); i++) { 
			    Element row = rows.get(i);
			    Elements cols = row.select("td");
			    if (cols != null && !cols.isEmpty()) {
			    	System.out.println(cols.get(0).text() + ":" + cols.get(1).text());
			    }
			}
			
		} catch (IOException e) {
			System.err.println(e);  
		}


		

	}

	
	

	
}
