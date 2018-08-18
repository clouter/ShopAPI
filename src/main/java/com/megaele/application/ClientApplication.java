package com.megaele.application;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.megaele.crawling.ProxyFactory;
import com.megaele.prices.PriceComparator;
import com.megaele.request.PSRequests;
import com.megaele.webservice.PSWebServiceClient;

/**
 *
 * Main application
 * 
 * @author rpila
 * 
 * @version 1.0 - 04/04/2018
 *
 */
public class ClientApplication {

	static PSRequests psRequests = new PSRequests();
	static PriceComparator priceComparator = new PriceComparator();
	static String PRODUCT_URL = "http://megaelectrodomesticos.com/api/products/";
	static String PRODUCTS_URL = "http://www.megaelectrodomesticos.com/api/products";
	private static Stream<String> stream;

	public static void main(String[] args) {
		Instant start = Instant.now();
		try {
			
//			String[] proxyUrl = new ProxyFactory().getProxy();
			PSWebServiceClient ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
			List<String> productIds = psRequests.getProductAttribute(ws, "id", PRODUCTS_URL);
			
			
			updatePricesFromOurCompetitorsPrice(ws, productIds);
			
			
//			updatePricesForSpecificReferences(ws, productIds);
						
			Instant end = Instant.now();
			System.out.println("Duration of the application: " + Duration.between(start, end));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Update the whole product stack in site using the competitor's price var
	 */
	private static void updatePricesFromOurCompetitorsPrice(PSWebServiceClient ws, List<String> productIds)
			throws Exception {
		Map<String, String> refsAndPrices = new HashMap<String, String>();
		for (String id: productIds) {
			System.out.println("ID: " + id);
			refsAndPrices = psRequests.getProductRefAndPriceFromMetaTitle(ws,PRODUCT_URL+id);
			for (Entry<String, String> entry: refsAndPrices.entrySet()) {
				System.out.println("Evaluating " + entry.getKey());
				String competitorPrice =  priceComparator.getPricefromCompetitors(entry.getKey());
				if (competitorPrice != null && !competitorPrice.isEmpty()) {
					Float finalPrice = priceComparator.calculatePrice(entry.getValue(), competitorPrice);
					System.out.println(entry.getKey() + "| Our Price " + entry.getValue() + "| Competitors Price " + competitorPrice + "| Our Final Price " + finalPrice);
					if (finalPrice.compareTo(Float.valueOf(entry.getValue())) != 0) {
						psRequests.updatePrice(ws, PRODUCT_URL+id, finalPrice);
						System.out.println(PRODUCT_URL+id + " price has been updated!");
						ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
					}

				}
				
			}
		}
	}


	/**
	 * Just update the price of the products with references in src/main/resources/references.properties
	 */
	@SuppressWarnings("unused")
	private static void updatePricesForSpecificReferences(PSWebServiceClient ws, List<String> productIds) {
		List<String> referencesList = getSpecificReferences();
		try {			
			Map<String, String> refsAndPrices = new HashMap<String, String>();
			for (String id: productIds) {
				System.out.println("ID: " + id);
				refsAndPrices = psRequests.getProductRefAndPriceFromMetaTitle(ws,PRODUCT_URL+id);
				for (Entry<String, String> entry: refsAndPrices.entrySet()) {
					// System.out.println(entry.getKey() + "| Our Price " + entry.getValue() + "| Competitors Price " + priceComparator.getPricefromCompetitors(entry.getKey()));
					String reference = entry.getKey();
					System.out.println("Evaluating " + reference);
					Optional<String> matched = referencesList.stream().filter(refFromFile->reference.equals(refFromFile)).findFirst();
					if (matched.isPresent()) {
						System.out.println("There is match with: " + matched);
						psRequests.updatePriceFromModifier(ws, PRODUCT_URL+id, (float) 30.0);
						System.out.println(PRODUCT_URL+id + " price has been updated!");
						ws = psRequests.connect("http://megaelectrodomesticos.com/", "DQC6RCXBXH379EA38I75P7XXHA52HHB8", false);
					}
				}
			}

		}catch (Exception e) {
			e.printStackTrace();
		}
	}



	private static List<String> getSpecificReferences() {
		List<String> referencesList = new ArrayList<String>();
		try {
			referencesList = readFile();
		}catch (Exception e){
			e.printStackTrace();
		}
		return referencesList;
	}



	@SuppressWarnings("unused")
	private static void readProperties() throws FileNotFoundException, IOException {
		ResourceBundle mybundle = ResourceBundle.getBundle("references");
		Enumeration<String> keys = mybundle.getKeys();
		for (String key : Collections.list(keys)) {
			System.out.println(key);
		}
	}

	private static List<String> readFile() throws IOException {
		String fileName = "src/main/resources/references.properties";
		stream = Files.lines(Paths.get(fileName));
		return stream.collect(Collectors.toList());
	}
}
