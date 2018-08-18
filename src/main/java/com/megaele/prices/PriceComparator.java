package com.megaele.prices;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.megaele.crawling.UserAgentFactory;

/**
 * 
 * All logic around prices
 * 
 * @author rpila
 * 
 * @version 1.0 - 15/08/2018
 * 
 */
public class PriceComparator {

	
	/**
	 * Returns best price among all our competitors
	 * 
	 * @param reference
	 * @return
	 * @throws InterruptedException 
	 */
	public String getPricefromCompetitors(String reference) throws InterruptedException {
		String price = null;
		Document doc = null;
		try {
			doc = Jsoup.connect("http://idealo.es/resultados.html?q=" + reference)
//					.proxy(proxyUrl[0], Integer.valueOf(proxyUrl[1]))
					.userAgent(new UserAgentFactory().getUserAgent())
					.get();
			int randomNumber =  ThreadLocalRandom.current().nextInt(5, 10 + 1);
			System.out.println("Waiting " + randomNumber*1000 + "ms");
			Thread.sleep(randomNumber*1000);
			if (doc.getElementsByClass("priceRange-from").size() > 0) {
				price = doc.getElementsByClass("priceRange-from").get(0).text();
				price = price.replaceAll("€", "").trim();
				if (price.contains(".")) {
					String[] pricesArray = price.split(",");
					price = pricesArray[0];
				} else {
					price = price.replaceAll(",", ".");
				}
			}
		} catch (IOException e) {
			System.err.println(e);  
		}
		return price;
	}
	
	
	/**
	 * Determines our price based on competitors price
	 * 
	 * @param ourPrice
	 * @param competitorPrice
	 * @return
	 */
	public Float calculatePrice(String ourPrice, String competitorPrice) {
		Float ourPriceF = Float.valueOf(ourPrice);
		Float competitorsPriceF = Float.valueOf(competitorPrice);
		
		Float difference = competitorsPriceF - ourPriceF;
		
		if ((competitorsPriceF > ourPriceF) && (between(difference, 11, 1000))) {
			ourPriceF = competitorsPriceF - 10;
		}
		
		return ourPriceF;
	}
	
	/**
	 * Compares two numbers
	 * 
	 * @param difference
	 * @param minValueInclusive
	 * @param maxValueInclusive
	 * @return
	 */
	private boolean between(Float difference, int minValueInclusive, int maxValueInclusive) {
	    if (difference >= minValueInclusive && difference <= maxValueInclusive)
	        return true;
	    else
	        return false;
	}
}
