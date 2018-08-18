package com.megaele.crawling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.megaele.application.ClientApplication;

/**
 * 
 * Provides list of valid proxies
 * 
 * @author rpila 
 * 
 * @version 1.0 - 18/08/2018
 *
 */
public class ProxyFactory {

	final static Logger logger = Logger.getLogger(ProxyFactory.class);
	
	public final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";
	
	private LinkedHashMap<String, String> getProxyList() {
		LinkedHashMap<String, String> proxies = new LinkedHashMap<String, String>();
		Document doc = null;
		try {
			doc = Jsoup.connect("https://free-proxy-list.net/").userAgent(USER_AGENT).get();
			Element table = doc.select("table").get(0); 
			Elements rows = table.select("tr");
			for (int i = 1; i < rows.size(); i++) { 
			    Element row = rows.get(i);
			    Elements cols = row.select("td");
			    if (cols != null && !cols.isEmpty()) {
			    	proxies.put(cols.get(0).text(), cols.get(1).text());
			    }
			}
			
		} catch (IOException e) {
			logger.error(e);
			System.err.println(e);  
		}
		return proxies;
	}
	
	public String[] getProxy() {
		LinkedHashMap<String, String> proxies = getProxyList();
		int randomNumber = ThreadLocalRandom.current().nextInt(0, (proxies.size()-1) + 1);
		String key = (new ArrayList<String>(proxies.keySet())).get(randomNumber);		
		String value = (new ArrayList<String>(proxies.values())).get(randomNumber);
		String[] result = {key, value};
		logger.info("URL Proxy:" + key + ":" + value);
		return result;
	}
	
}
